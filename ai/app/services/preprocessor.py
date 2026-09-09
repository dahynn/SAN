import asyncio
import ipaddress
import socket
from urllib.parse import urljoin, urlparse

import httpx
import trafilatura

from app.core.exceptions import AIProcessingError, ContentValidationError
from app.llms import LLMClient
from app.prompts import IMAGE_DESCRIBE_PROMPT
from app.schemas.common import InputType

_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
}

_ALLOWED_SCHEMES = {"http", "https"}
_LOCAL_HOSTNAMES = {"localhost", "localhost.localdomain"}


async def _resolve_public_host(hostname: str, port: int | None) -> None:
    """요청 직전 DNS 결과가 모두 전역 IP인지 확인한다.

    DNS 재바인딩의 완전한 방어는 연결 계층의 IP 고정이 필요하므로, 운영 환경에서는
    egress proxy 또는 네트워크 정책을 함께 적용해야 한다.
    """
    try:
        addresses = await asyncio.get_running_loop().getaddrinfo(
            hostname,
            port or 443,
            type=socket.SOCK_STREAM,
        )
    except socket.gaierror as exc:
        raise ContentValidationError(code="invalid_url", message="공개 URL의 호스트를 확인할 수 없습니다.") from exc

    resolved_ips = {item[4][0] for item in addresses}
    if not resolved_ips or any(not ipaddress.ip_address(address).is_global for address in resolved_ips):
        raise ContentValidationError(code="blocked_url", message="내부망 또는 예약된 주소는 사용할 수 없습니다.")

# 전처리 모듈은 입력된 콘텐츠 유형에 따라 텍스트, URL, 이미지에 대한 전처리를 수행. URL의 경우 본문을 추출, 이미지의 경우 LLM을 활용하여 설명 텍스트로 변환.
async def _validate_url(content: str) -> None:
    parsed = urlparse(content)
    if parsed.scheme not in _ALLOWED_SCHEMES or not parsed.hostname or parsed.username or parsed.password:
        raise ContentValidationError(code="invalid_url", message="유효하지 않은 URL입니다.")

    hostname = parsed.hostname.rstrip(".").lower()
    if hostname in _LOCAL_HOSTNAMES or hostname.endswith(".localhost"):
        raise ContentValidationError(code="blocked_url", message="내부망 또는 예약된 주소는 사용할 수 없습니다.")

    try:
        port = parsed.port
    except ValueError as exc:
        raise ContentValidationError(code="invalid_url", message="유효하지 않은 URL 포트입니다.") from exc

    try:
        address = ipaddress.ip_address(hostname)
    except ValueError:
        await _resolve_public_host(hostname, port)
    else:
        if not address.is_global:
            raise ContentValidationError(code="blocked_url", message="내부망 또는 예약된 주소는 사용할 수 없습니다.")

# 텍스트 콘텐츠는 공백을 제거한 후 비어있지 않은지 확인. 유효하지 않은 경우 ContentValidationError를 발생.
def _preprocess_text(content: str) -> str:
    text = content.strip()
    if not text:
        raise ContentValidationError(code="missing_content", message="content는 비어있을 수 없습니다.")
    return text

# URL 콘텐츠는 유효한 URL인지 검증한 후 HTTP GET 요청을 통해 페이지를 가져와 trafilatura로 본문을 추출. 실패 시 AIProcessingError를 발생.
async def _preprocess_url(content: str) -> str:
    try:
        async with httpx.AsyncClient(timeout=15.0, headers=_HEADERS) as client:
            current_url = content
            for _ in range(4):
                await _validate_url(current_url)
                response = await client.get(current_url, follow_redirects=False)
                if response.is_redirect:
                    location = response.headers.get("location")
                    if not location:
                        raise AIProcessingError(code="url_fetch_failed", message="리디렉션 대상이 없습니다.")
                    current_url = urljoin(current_url, location)
                    continue
                response.raise_for_status()
                break
            else:
                raise AIProcessingError(code="url_fetch_failed", message="리디렉션 횟수를 초과했습니다.")
    except ContentValidationError:
        raise
    except Exception as e:
        raise AIProcessingError(code="url_fetch_failed", message=f"URL 요청 실패: {e}") from e

    body = trafilatura.extract(
        response.text,
        include_tables=True,
        no_fallback=False,
        favor_recall=True,
    )
    if not body:
        raise AIProcessingError(code="url_content_empty", message="URL에서 본문을 추출할 수 없습니다.")

    return body

# 이미지 콘텐츠는 유효한 URL인지 검증한 후 HTTP GET stream으로 접근 가능 여부를 확인. 이후 LLMClient의 call_with_image 메서드를 사용하여 이미지 설명 텍스트를 생성. 실패 시 AIProcessingError를 발생.
async def _preprocess_image(content: str) -> str:
    await _validate_url(content)

    try:
        async with httpx.AsyncClient(timeout=10.0, headers=_HEADERS) as client:
            async with client.stream("GET", content, follow_redirects=False) as response:
                if response.is_redirect:
                    raise ContentValidationError(code="blocked_url", message="이미지 URL의 리디렉션은 허용되지 않습니다.")
                response.raise_for_status()
    except ContentValidationError:
        raise
    except Exception as e:
        raise AIProcessingError(code="image_access_failed", message=f"이미지 URL 접근 실패: {e}") from e

    llm = LLMClient()
    return await asyncio.to_thread(
        llm.call_with_image,
        prompt=IMAGE_DESCRIBE_PROMPT,
        image_url=content,
        error_code="image_analysis_failed",
    )

# preprocess 함수는 입력된 콘텐츠 유형에 따라 적절한 전처리 함수를 호출하여 텍스트, URL, 이미지에 대한 전처리를 수행. 콘텐츠가 유효하지 않거나 처리 중 오류가 발생하면 예외를 발생시킴.
async def preprocess(input_type: InputType, content: str) -> str:
    if not content or not content.strip():
        raise ContentValidationError(code="missing_content", message="content는 비어있을 수 없습니다.")

    if input_type == InputType.text:
        return _preprocess_text(content)
    if input_type == InputType.url:
        return await _preprocess_url(content)
    return await _preprocess_image(content)
