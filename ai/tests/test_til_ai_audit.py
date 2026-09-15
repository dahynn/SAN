from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.core.ai_execution_audit import AIExecutionAudit, AIExecutionStatus
from app.core.ai_release import AI_RELEASES
from app.core.exceptions import AIProcessingError
from app.schemas.til import TilContent, TilRequest
from app.services.til import _RESULT_CACHE, _SUMMARY_CACHE, generate_til


def _request(source: str) -> TilRequest:
    return TilRequest(
        contents=[TilContent(input_type="text", content=source)],
        generate_til=True,
    )


@pytest.mark.asyncio
async def test_successful_uncached_generation_records_release_and_outcome() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    audit_store = MagicMock()
    audit_store.start.return_value = AIExecutionAudit(
        execution_id="execution-1",
        release_id=AI_RELEASES[0].release_id,
        status=AIExecutionStatus.STARTED,
    )

    with patch("app.services.til.get_ai_execution_audit_store", return_value=audit_store), \
         patch("app.services.til.preprocess", new=AsyncMock(return_value="민감 원문")), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as embedding_cls:
        llm_cls.return_value.acall = AsyncMock(return_value="요약")
        llm_cls.return_value.acall_json = AsyncMock(
            return_value={"title": "제목", "til_markdown": "# TIL"}
        )
        embedding_cls.return_value.embed.return_value = [0.1]

        await generate_til(_request("민감 원문"))

    audit_store.start.assert_called_once_with(AI_RELEASES[0], input_count=1)
    audit_store.finish.assert_called_once_with(
        "execution-1",
        AIExecutionStatus.SUCCEEDED,
    )


@pytest.mark.asyncio
async def test_failed_generation_records_error_code_without_error_message() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    audit_store = MagicMock()
    audit_store.start.return_value = AIExecutionAudit(
        execution_id="execution-2",
        release_id=AI_RELEASES[0].release_id,
        status=AIExecutionStatus.STARTED,
    )

    with patch("app.services.til.get_ai_execution_audit_store", return_value=audit_store), \
         patch("app.services.til.preprocess", new=AsyncMock(return_value="원문")), \
         patch("app.services.til.LLMClient") as llm_cls:
        llm_cls.return_value.acall = AsyncMock(
            side_effect=AIProcessingError(
                code="til_generation_failed",
                message="provider secret detail",
            )
        )

        with pytest.raises(AIProcessingError):
            await generate_til(_request("원문"))

    audit_store.finish.assert_called_once_with(
        "execution-2",
        AIExecutionStatus.FAILED,
        error_code="til_generation_failed",
    )


@pytest.mark.asyncio
async def test_invalid_output_is_audited_as_failure_and_not_cached() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    audit_store = MagicMock()
    audit_store.start.side_effect = [
        AIExecutionAudit(
            execution_id=f"execution-{index}",
            release_id=AI_RELEASES[0].release_id,
            status=AIExecutionStatus.STARTED,
        )
        for index in (3, 4)
    ]

    with patch("app.services.til.get_ai_execution_audit_store", return_value=audit_store), \
         patch("app.services.til.preprocess", new=AsyncMock(return_value="원문")), \
         patch("app.services.til.LLMClient") as llm_cls, \
         patch("app.services.til.EmbeddingClient") as embedding_cls:
        llm_cls.return_value.acall = AsyncMock(return_value="요약")
        llm_cls.return_value.acall_json = AsyncMock(
            return_value={"title": "제목", "til_markdown": 123}
        )

        for _ in range(2):
            with pytest.raises(AIProcessingError) as error:
                await generate_til(_request("동일 원문"))
            assert error.value.code == "til_output_contract_violation"

    assert llm_cls.return_value.acall_json.await_count == 2
    embedding_cls.return_value.embed.assert_not_called()
    assert audit_store.finish.call_args_list == [
        (("execution-3", AIExecutionStatus.FAILED), {"error_code": "til_output_contract_violation"}),
        (("execution-4", AIExecutionStatus.FAILED), {"error_code": "til_output_contract_violation"}),
    ]
