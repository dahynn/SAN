import asyncio
import hashlib
import json

from app.core.ai_execution_audit import AIExecutionStatus, get_ai_execution_audit_store
from app.core.ai_release import TIL_AI_CONTRACT, require_approved_ai_release
from app.core.config import get_settings
from app.core.exceptions import ContentValidationError
from app.llms import EmbeddingClient, LLMClient
from app.prompts import TIL_GROUP_PROMPT, TIL_SUMMARY_PROMPT
from app.schemas.til import TilRequest, TilResponse
from app.schemas.til_output import validate_til_generation_output
from app.services.til_cache import AsyncTtlCache
from app.services.preprocessor import preprocess

_BLOCK_SEPARATOR = "\n\n---\n\n"
_BATCH_SIZE = 3
_SOURCE_OPEN = "<source-content>"
_SOURCE_CLOSE = "</source-content>"
_SUMMARY_CACHE = AsyncTtlCache[str](max_entries=512)
_RESULT_CACHE = AsyncTtlCache[TilResponse](max_entries=128)


async def _summarize(llm: LLMClient, preprocessed: str) -> str:
    return await llm.acall(
        prompt=f"{TIL_SUMMARY_PROMPT}\n\n{_SOURCE_OPEN}\n{preprocessed}\n{_SOURCE_CLOSE}",
        error_code="til_summarize_failed",
    )


def _cache_key(namespace: str, payload: object) -> str:
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    return f"{namespace}:{hashlib.sha256(encoded).hexdigest()}"


def _summary_cache_key(preprocessed: str) -> str:
    return _cache_key("til-summary-v1", {"prompt": TIL_SUMMARY_PROMPT, "content": preprocessed})


def _result_cache_key(request: TilRequest) -> str:
    settings = get_settings()
    return _cache_key(
        "til-result-v1",
        {
            "contents": [(item.input_type, item.content) for item in request.contents],
            "summary_prompt": TIL_SUMMARY_PROMPT,
            "group_prompt": TIL_GROUP_PROMPT,
            "model": settings.openai_model,
            "prompt_version": TIL_AI_CONTRACT.prompt_version,
            "output_schema_version": TIL_AI_CONTRACT.output_schema_version,
            "direct_reduce_max_chars": settings.til_direct_reduce_max_chars,
        },
    )


async def _bounded_gather(limit: int, coroutines):
    """Keep large document collections from flooding one model connection."""
    semaphore = asyncio.Semaphore(max(1, limit))

    async def run(coroutine):
        async with semaphore:
            return await coroutine

    return await asyncio.gather(*(run(coroutine) for coroutine in coroutines))


async def _reduce(llm: LLMClient, texts: list[str]) -> dict:
    joined = _BLOCK_SEPARATOR.join(texts)
    result = await llm.acall_json(
        prompt=f"{TIL_GROUP_PROMPT}\n\n<source-summaries>\n{joined}\n</source-summaries>",
        error_code="til_generation_failed",
    )
    return validate_til_generation_output(result).model_dump()


async def generate_til(request: TilRequest) -> TilResponse:
    if not request.contents:
        raise ContentValidationError(code="missing_contents", message="contents는 비어있을 수 없습니다.")

    settings = get_settings()
    release = None
    if request.generate_til:
        release = require_approved_ai_release(TIL_AI_CONTRACT, settings.openai_model)

    if request.generate_til and settings.til_cache_enabled:
        return await _RESULT_CACHE.get_or_create(
            _result_cache_key(request),
            settings.til_result_cache_ttl_seconds,
            lambda: _generate_til_uncached(request, release),
        )
    return await _generate_til_uncached(request, release)


async def _generate_til_uncached(request: TilRequest, release=None) -> TilResponse:
    settings = get_settings()

    audit_store = None
    audit = None
    if release is not None:
        audit_store = get_ai_execution_audit_store()
        audit = audit_store.start(release, input_count=len(request.contents))

    try:
        response = await _execute_til(request, settings)
    except Exception as exc:
        if audit_store is not None and audit is not None:
            error_code = getattr(exc, "code", exc.__class__.__name__)
            audit_store.finish(
                audit.execution_id,
                AIExecutionStatus.FAILED,
                error_code=error_code,
            )
        raise

    if audit_store is not None and audit is not None:
        audit_store.finish(audit.execution_id, AIExecutionStatus.SUCCEEDED)
    return response


async def _execute_til(request: TilRequest, settings) -> TilResponse:

    preprocessed_list = await asyncio.gather(
        *(preprocess(item.input_type, item.content) for item in request.contents)
    )

    title: str | None = None
    til_markdown: str | None = None

    if request.generate_til:
        llm = LLMClient()

        # Step 1: 카드별 개별 요약. 대량 입력에서도 모델 연결을 과도하게 점유하지 않는다.
        summaries = list(await _bounded_gather(
            settings.til_summary_max_concurrency,
            (
                _SUMMARY_CACHE.get_or_create(
                    _summary_cache_key(p),
                    settings.til_summary_cache_ttl_seconds,
                    lambda p=p: _summarize(llm, p),
                )
                if settings.til_cache_enabled
                else _summarize(llm, p)
                for p in preprocessed_list
            ),
        ))

        joined_summaries = _BLOCK_SEPARATOR.join(summaries)
        if len(summaries) <= _BATCH_SIZE or len(joined_summaries) <= settings.til_direct_reduce_max_chars:
            # 카드 수가 배치 크기 이하면 바로 최종 TIL 생성
            result = await _reduce(llm, summaries)
        else:
            # Step 2: 3개씩 묶어 중간 TIL 생성 (배치 간 병렬)
            batches = [summaries[i:i + _BATCH_SIZE] for i in range(0, len(summaries), _BATCH_SIZE)]
            intermediate = await _bounded_gather(
                settings.til_reduce_max_concurrency,
                (_reduce(llm, batch) for batch in batches),
            )
            # Step 3: 중간 TIL들을 합쳐 최종 TIL 생성
            result = await _reduce(llm, [r["til_markdown"] for r in intermediate])

        title = result["title"]
        til_markdown = result["til_markdown"]

    embedding_input = til_markdown if til_markdown is not None else _BLOCK_SEPARATOR.join(preprocessed_list)
    embedding = EmbeddingClient().embed(embedding_input)

    return TilResponse(title=title, til_markdown=til_markdown, embedding=embedding)
