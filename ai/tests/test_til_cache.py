import asyncio
from unittest.mock import AsyncMock, patch

import pytest

from app.services.til import _RESULT_CACHE, _SUMMARY_CACHE, generate_til
from app.schemas.til import TilContent, TilRequest


def _request(contents: list[str]) -> TilRequest:
    return TilRequest(
        contents=[TilContent(input_type="text", content=content) for content in contents],
        generate_til=True,
    )


@pytest.mark.asyncio
async def test_identical_til_request_reuses_final_result_and_embedding() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    request = _request(["캐시 원문 A", "캐시 원문 B"])

    try:
        with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
             patch("app.services.til.LLMClient") as llm_cls, \
             patch("app.services.til.EmbeddingClient") as embedding_cls:
            llm = llm_cls.return_value
            llm.acall = AsyncMock(side_effect=["A 요약", "B 요약"])
            llm.acall_json = AsyncMock(return_value={"title": "캐시 TIL", "til_markdown": "# TIL\n캐시"})
            embedding_cls.return_value.embed.return_value = [0.1]

            first = await generate_til(request)
            second = await generate_til(request)

        assert first == second
        assert llm.acall.await_count == 2
        assert llm.acall_json.await_count == 1
        embedding_cls.return_value.embed.assert_called_once_with("# TIL\n캐시")
    finally:
        await _SUMMARY_CACHE.clear()
        await _RESULT_CACHE.clear()


@pytest.mark.asyncio
async def test_short_multi_card_til_skips_intermediate_reduce_calls() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    request = _request(["직접 통합 원문 1", "직접 통합 원문 2", "직접 통합 원문 3", "직접 통합 원문 4"])

    try:
        with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
             patch("app.services.til.LLMClient") as llm_cls, \
             patch("app.services.til.EmbeddingClient") as embedding_cls:
            llm = llm_cls.return_value
            llm.acall = AsyncMock(return_value="짧은 요약")
            llm.acall_json = AsyncMock(return_value={"title": "직접 통합", "til_markdown": "# TIL\n본문"})
            embedding_cls.return_value.embed.return_value = [0.1]

            await generate_til(request)

        assert llm.acall.await_count == 4
        assert llm.acall_json.await_count == 1
    finally:
        await _SUMMARY_CACHE.clear()
        await _RESULT_CACHE.clear()


@pytest.mark.asyncio
async def test_concurrent_identical_requests_share_one_generation() -> None:
    await _SUMMARY_CACHE.clear()
    await _RESULT_CACHE.clear()
    request = _request(["동시 요청 원문"])

    async def delayed_summary(*_args, **_kwargs) -> str:
        await asyncio.sleep(0.01)
        return "동시 요약"

    try:
        with patch("app.services.til.preprocess", new=AsyncMock(side_effect=lambda _type, content: content)), \
             patch("app.services.til.LLMClient") as llm_cls, \
             patch("app.services.til.EmbeddingClient") as embedding_cls:
            llm = llm_cls.return_value
            llm.acall = AsyncMock(side_effect=delayed_summary)
            llm.acall_json = AsyncMock(return_value={"title": "동시", "til_markdown": "# TIL\n본문"})
            embedding_cls.return_value.embed.return_value = [0.1]

            await asyncio.gather(generate_til(request), generate_til(request))

        assert llm.acall.await_count == 1
        assert llm.acall_json.await_count == 1
        assert embedding_cls.return_value.embed.call_count == 1
    finally:
        await _SUMMARY_CACHE.clear()
        await _RESULT_CACHE.clear()
