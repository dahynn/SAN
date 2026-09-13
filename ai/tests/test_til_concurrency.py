import asyncio

import pytest

from app.services.til import _bounded_gather


@pytest.mark.asyncio
async def test_bounded_gather_never_exceeds_requested_concurrency() -> None:
    active = 0
    peak = 0

    async def work(index: int) -> int:
        nonlocal active, peak
        active += 1
        peak = max(peak, active)
        await asyncio.sleep(0.01)
        active -= 1
        return index

    results = await _bounded_gather(2, (work(index) for index in range(6)))

    assert results == [0, 1, 2, 3, 4, 5]
    assert peak == 2
