import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from time import monotonic
from typing import Generic, TypeVar


T = TypeVar("T")


@dataclass(frozen=True)
class _CacheEntry(Generic[T]):
    value: T
    expires_at: float


class AsyncTtlCache(Generic[T]):
    """Small process-local cache that also collapses identical in-flight work."""

    def __init__(self, max_entries: int) -> None:
        self._max_entries = max(1, max_entries)
        self._entries: dict[str, _CacheEntry[T]] = {}
        self._inflight: dict[str, asyncio.Task[T]] = {}
        self._lock = asyncio.Lock()

    async def get_or_create(
        self,
        key: str,
        ttl_seconds: float,
        factory: Callable[[], Awaitable[T]],
    ) -> T:
        now = monotonic()
        async with self._lock:
            entry = self._entries.get(key)
            if entry and entry.expires_at > now:
                return entry.value
            if entry:
                self._entries.pop(key, None)

            task = self._inflight.get(key)
            if task is None:
                task = asyncio.create_task(factory())
                self._inflight[key] = task

        try:
            value = await asyncio.shield(task)
        except BaseException:
            async with self._lock:
                if self._inflight.get(key) is task:
                    self._inflight.pop(key, None)
            raise

        async with self._lock:
            if self._inflight.get(key) is task:
                self._inflight.pop(key, None)
                if ttl_seconds > 0:
                    self._entries[key] = _CacheEntry(value=value, expires_at=monotonic() + ttl_seconds)
                    self._evict_excess_entries()
        return value

    async def clear(self) -> None:
        async with self._lock:
            self._entries.clear()
            self._inflight.clear()

    def _evict_excess_entries(self) -> None:
        excess = len(self._entries) - self._max_entries
        if excess <= 0:
            return
        for key, _entry in sorted(self._entries.items(), key=lambda item: item[1].expires_at)[:excess]:
            self._entries.pop(key, None)
