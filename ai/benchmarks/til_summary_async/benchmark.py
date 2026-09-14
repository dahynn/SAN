"""Measure SAN card-summary latency: sequential control versus async parallelism.

This intentionally calls only the card-summary prompt. It excludes the final
TIL reduction, embeddings, application cache, Java backend and UI layers.
Raw model responses are never persisted.
"""
from __future__ import annotations

import argparse
import asyncio
import hashlib
import json
import math
import random
import statistics
import subprocess
import sys
import time
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from app.core.config import get_settings  # noqa: E402
from app.llms import LLMClient  # noqa: E402
from app.prompts import TIL_SUMMARY_PROMPT  # noqa: E402

CARDS = (
    "Java의 HashMap은 키와 값을 저장한다. 같은 키로 값을 넣으면 기존 값을 대체한다. 키의 hashCode와 equals 계약이 중요하다. 순회 순서는 보장하지 않는다.",
    "데이터베이스 트랜잭션은 여러 변경을 하나의 단위로 처리한다. 커밋하면 변경을 확정하고 롤백하면 취소한다. 테스트에서는 정상 커밋과 예외 발생 시 롤백을 각각 확인한다.",
    "비동기 작업은 요청 접수와 실제 실행 시점이 다를 수 있다. 요청 식별자와 상태를 저장하면 실행 결과를 확인할 수 있다. 재시도 전에 동일 작업의 진행 여부를 확인한다.",
)
_SOURCE_OPEN = "<source-content>"
_SOURCE_CLOSE = "</source-content>"


def _sha256(value: object) -> str:
    encoded = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(encoded).hexdigest()


def _git_sha() -> str:
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT.parent, text=True).strip()


def _safe_error(exc: BaseException) -> str:
    return str(getattr(exc, "code", exc.__class__.__name__))


async def _summarize_one(llm: LLMClient, card: str) -> dict[str, Any]:
    started = time.perf_counter()
    try:
        output = await llm.acall(
            prompt=f"{TIL_SUMMARY_PROMPT}\n\n{_SOURCE_OPEN}\n{card}\n{_SOURCE_CLOSE}",
            error_code="til_summarize_failed",
        )
        return {"ok": True, "seconds": round(time.perf_counter() - started, 6), "output_characters": len(output)}
    except BaseException as exc:
        return {"ok": False, "seconds": round(time.perf_counter() - started, 6), "error_code": _safe_error(exc)}


async def _run_once(mode: str) -> dict[str, Any]:
    llm = LLMClient()
    started_at = datetime.now(UTC).isoformat()
    started = time.perf_counter()
    if mode == "sequential":
        cards = [await _summarize_one(llm, card) for card in CARDS]
    else:
        cards = list(await asyncio.gather(*(_summarize_one(llm, card) for card in CARDS)))
    return {
        "started_at": started_at,
        "mode": mode,
        "seconds": round(time.perf_counter() - started, 6),
        "ok": all(card["ok"] for card in cards),
        "cards": cards,
    }


def _percentile_nearest_rank(values: list[float], percentile: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    return ordered[math.ceil(percentile * len(ordered)) - 1]


def _summary(records: list[dict[str, Any]]) -> dict[str, Any]:
    modes: dict[str, Any] = {}
    for mode in ("sequential", "parallel"):
        selected = [record for record in records if record["mode"] == mode]
        successes = [record["seconds"] for record in selected if record["ok"]]
        failures = [record for record in selected if not record["ok"]]
        modes[mode] = {
            "runs": len(selected),
            "successes": len(successes),
            "success_rate": len(successes) / len(selected) if selected else None,
            "mean_seconds": statistics.mean(successes) if successes else None,
            "median_seconds": statistics.median(successes) if successes else None,
            "p95_seconds_nearest_rank": _percentile_nearest_rank(successes, 0.95),
            "sample_standard_deviation_seconds": statistics.stdev(successes) if len(successes) > 1 else None,
            "failure_codes": [
                {"run": failure["run"], "errors": [card["error_code"] for card in failure["cards"] if not card["ok"]]}
                for failure in failures
            ],
        }
    sequential_mean = modes["sequential"]["mean_seconds"]
    parallel_mean = modes["parallel"]["mean_seconds"]
    modes["mean_reduction_percent"] = (
        (sequential_mean - parallel_mean) / sequential_mean * 100
        if sequential_mean and parallel_mean is not None
        else None
    )
    return modes


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs-per-mode", type=int, default=10)
    parser.add_argument("--seed", type=int, default=20260914)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.runs_per_mode < 10:
        raise ValueError("runs-per-mode must be at least 10 for career evidence")

    modes = ["sequential"] * args.runs_per_mode + ["parallel"] * args.runs_per_mode
    random.Random(args.seed).shuffle(modes)
    settings = get_settings()
    records: list[dict[str, Any]] = []
    for position, mode in enumerate(modes, start=1):
        record = await _run_once(mode)
        record["run"] = position
        records.append(record)
        print(json.dumps({"run": position, "mode": mode, "ok": record["ok"], "seconds": record["seconds"]}, ensure_ascii=False), flush=True)

    result = {
        "schema_version": "1.0",
        "started_at": records[0]["started_at"] if records else None,
        "completed_at": datetime.now(UTC).isoformat(),
        "commit_sha": _git_sha(),
        "model": settings.openai_model,
        "use_responses_api": settings.openai_use_responses_api,
        "scope": "card-summary stage only; excludes final reduction, embedding, application cache, Java backend and UI",
        "control": "sequential calls over the same three cards",
        "treatment": "asyncio.gather parallel calls over the same three cards",
        "cards_per_run": len(CARDS),
        "input_sha256": _sha256(CARDS),
        "prompt_sha256": _sha256(TIL_SUMMARY_PROMPT),
        "runs_per_mode": args.runs_per_mode,
        "randomization": {"method": "seeded shuffle", "seed": args.seed, "schedule": modes},
        "raw_model_output_persisted": False,
        "records": records,
        "summary": _summary(records),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    asyncio.run(main())
