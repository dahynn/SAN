from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.core.exceptions import AIProcessingError
from app.main import app

client = TestClient(app)


# ── 입력 검증 ─────────────────────────────────────────────────────────────────
def test_empty_contents_returns_400() -> None:
    response = client.post("/ai/til", json={"contents": [], "generate_til": True})
    assert response.status_code == 400
    assert response.json()["error"] == "missing_contents"


def test_missing_contents_field_returns_400() -> None:
    response = client.post("/ai/til", json={"generate_til": True})
    assert response.status_code == 400
    assert response.json()["error"] == "missing_contents"


def test_invalid_input_type_returns_400() -> None:
    response = client.post(
        "/ai/til",
        json={"contents": [{"input_type": "video", "content": "x"}], "generate_til": True},
    )
    assert response.status_code == 400
    assert response.json()["error"] == "invalid_input_type"


def test_missing_generate_til_returns_400() -> None:
    response = client.post(
        "/ai/til",
        json={"contents": [{"input_type": "text", "content": "hi"}]},
    )
    assert response.status_code == 400


def test_unapproved_release_is_blocked_before_model_execution() -> None:
    with patch(
        "app.services.til.require_approved_ai_release",
        side_effect=AIProcessingError(
            code="ai_release_not_approved",
            message="internal release details",
        ),
    ), patch("app.services.til.LLMClient") as llm_cls:
        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": True,
            },
        )

    assert response.status_code == 422
    assert response.json() == {
        "error": "ai_release_not_approved",
        "message": "승인되지 않은 AI 버전 조합이어서 실행을 중단했습니다.",
    }
    llm_cls.assert_not_called()


# ── 정상 처리 ─────────────────────────────────────────────────────────────────
# generate_til=true: Step1 acall(요약) → Step2 acall_json(TIL) → embed
def test_generate_true_returns_markdown_and_embedding() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as emb_cls:
        mock_llm = llm_cls.return_value
        mock_llm.acall = AsyncMock(return_value="요약 내용")
        mock_llm.acall_json = AsyncMock(return_value={
            "title": "오늘의 학습 정리",
            "til_markdown": "# TIL\n\n## 주제\n본문",
        })
        emb_cls.return_value.embed.return_value = [0.1, 0.2]

        response = client.post(
            "/ai/til",
            json={
                "contents": [
                    {"input_type": "text", "content": "원문 A"},
                    {"input_type": "text", "content": "원문 B"},
                ],
                "generate_til": True,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "오늘의 학습 정리"
    assert body["til_markdown"] == "# TIL\n\n## 주제\n본문"
    assert body["embedding"] == [0.1, 0.2]
    # 임베딩은 생성된 마크다운을 입력으로 받는다.
    emb_cls.return_value.embed.assert_called_once_with("# TIL\n\n## 주제\n본문")


# generate_til=false: LLMClient 미호출, embedding만 반환 (preprocessed 텍스트 join)
def test_generate_false_returns_null_markdown_only() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as emb_cls:
        emb_cls.return_value.embed.return_value = [0.5]

        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": False,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] is None
    assert body["til_markdown"] is None
    assert body["embedding"] == [0.5]
    llm_cls.assert_not_called()


# ── AI 처리 실패 ───────────────────────────────────────────────────────────────
# Step1 acall 실패 → 422 til_generation_failed
def test_llm_summarize_failure_returns_422() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient"):
        mock_llm = llm_cls.return_value
        mock_llm.acall = AsyncMock(side_effect=AIProcessingError(
            code="til_generation_failed", message="LLM 실패"
        ))

        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": True,
            },
        )

    assert response.status_code == 422
    assert response.json()["error"] == "til_generation_failed"
    assert response.json()["message"] == "TIL 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."


# Step2 acall_json 응답이 출력 계약 위반 → 422 safe contract error
def test_llm_missing_required_field_returns_422() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as emb_cls:
        mock_llm = llm_cls.return_value
        mock_llm.acall = AsyncMock(return_value="요약 내용")
        mock_llm.acall_json = AsyncMock(return_value={"til_markdown": "# TIL"})  # title 누락

        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": True,
            },
        )

    assert response.status_code == 422
    assert response.json() == {
        "error": "til_output_contract_violation",
        "message": "TIL 생성 결과 형식이 올바르지 않아 처리를 중단했습니다.",
    }
    emb_cls.return_value.embed.assert_not_called()


def test_llm_wrong_field_type_is_not_embedded_or_returned() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as emb_cls:
        mock_llm = llm_cls.return_value
        mock_llm.acall = AsyncMock(return_value="요약 내용")
        mock_llm.acall_json = AsyncMock(return_value={
            "title": ["제목"],
            "til_markdown": "# TIL",
        })

        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": True,
            },
        )

    assert response.status_code == 422
    assert response.json()["error"] == "til_output_contract_violation"
    emb_cls.return_value.embed.assert_not_called()


# 임베딩 실패 → 422 embedding_failed
def test_embedding_failure_returns_422() -> None:
    with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda t, c: c)), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as emb_cls:
        mock_llm = llm_cls.return_value
        mock_llm.acall = AsyncMock(return_value="요약 내용")
        mock_llm.acall_json = AsyncMock(return_value={
            "title": "제목",
            "til_markdown": "# TIL",
        })
        emb_cls.return_value.embed.side_effect = AIProcessingError(
            code="embedding_failed", message="임베딩 실패"
        )

        response = client.post(
            "/ai/til",
            json={
                "contents": [{"input_type": "text", "content": "원문"}],
                "generate_til": True,
            },
        )

    assert response.status_code == 422
    assert response.json()["error"] == "embedding_failed"
