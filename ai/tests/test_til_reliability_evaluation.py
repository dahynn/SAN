import json
from pathlib import Path
from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.core.exceptions import AIProcessingError
from app.main import app


client = TestClient(app)
CASES = json.loads((Path(__file__).parent / "fixtures" / "til_reliability_cases.json").read_text())


def _case(case_id: str) -> dict:
    return next(case for case in CASES if case["id"] == case_id)


def _request(case: dict) -> dict:
    return {
        "contents": [{"input_type": "text", "content": text} for text in case["contents"]],
        "generate_til": True,
    }


def test_normal_korean_fixture_returns_contract_response_without_external_model() -> None:
    case = _case("normal_korean")
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as embedding_cls:
        llm_cls.return_value.acall = AsyncMock(return_value="요약")
        llm_cls.return_value.acall_json = AsyncMock(return_value={"title": "트랜잭션", "til_markdown": "# TIL\n\n## 트랜잭션"})
        embedding_cls.return_value.embed.return_value = [0.1]
        response = client.post("/ai/til", json=_request(case))

    assert response.status_code == case["expected_status"]
    assert response.json()["til_markdown"].startswith("# TIL")


def test_empty_source_fixture_is_rejected_before_model_call() -> None:
    case = _case("insufficient_source")
    with patch("app.services.til.LLMClient") as llm_cls:
        response = client.post("/ai/til", json=_request(case))

    assert response.status_code == case["expected_status"]
    assert response.json()["error"] == "missing_contents"
    llm_cls.assert_not_called()


def test_prompt_injection_fixture_is_delimited_as_untrusted_source() -> None:
    case = _case("prompt_injection")
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as embedding_cls:
        llm = llm_cls.return_value
        llm.acall = AsyncMock(return_value="DI 요약")
        llm.acall_json = AsyncMock(return_value={"title": "DI", "til_markdown": "# TIL\n\n## DI"})
        embedding_cls.return_value.embed.return_value = [0.1]
        response = client.post("/ai/til", json=_request(case))

    prompt = llm.acall.await_args.kwargs["prompt"]
    assert response.status_code == case["expected_status"]
    assert "<source-content>" in prompt and "</source-content>" in prompt
    assert "신뢰하지 않는 참고 원문" in prompt


def test_model_error_fixture_returns_safe_error_code() -> None:
    case = _case("timeout_or_model_error")
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient"):
        llm_cls.return_value.acall = AsyncMock(side_effect=AIProcessingError("til_generation_failed", "timeout"))
        response = client.post("/ai/til", json=_request(case))

    assert response.status_code == case["expected_status"]
    assert response.json()["error"] == "til_generation_failed"
    assert response.json()["message"] == "TIL 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."
    assert "timeout" not in response.json()["message"]


def test_oversized_input_is_rejected_before_model_call() -> None:
    payload = {"contents": [{"input_type": "text", "content": "가" * 12001}], "generate_til": True}
    with patch("app.services.til.LLMClient") as llm_cls:
        response = client.post("/ai/til", json=payload)

    assert response.status_code == 400
    llm_cls.assert_not_called()


def test_user_isolation_fixture_declares_an_explicit_denial_contract() -> None:
    case = _case("user_isolation")

    assert case["owner"] != case["requester"]
    assert case["expected_error"] == "UNAUTHORIZED"
