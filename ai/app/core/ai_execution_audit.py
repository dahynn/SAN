import sqlite3
from dataclasses import dataclass
from datetime import UTC, datetime
from enum import StrEnum
from functools import lru_cache
from pathlib import Path
from uuid import uuid4

from app.core.ai_release import AIRelease
from app.core.config import get_settings
from app.core.exceptions import AIProcessingError


class AIExecutionStatus(StrEnum):
    STARTED = "started"
    SUCCEEDED = "succeeded"
    FAILED = "failed"


@dataclass(frozen=True)
class AIExecutionAudit:
    execution_id: str
    release_id: str
    status: AIExecutionStatus


class SQLiteAIExecutionAuditStore:
    """Persist AI execution metadata without prompts or source contents."""

    def __init__(self, database_path: str | Path) -> None:
        self._database_path = Path(database_path)

    def start(self, release: AIRelease, *, input_count: int) -> AIExecutionAudit:
        execution_id = str(uuid4())
        started_at = datetime.now(UTC).isoformat()

        try:
            with self._connect() as connection:
                self._ensure_schema(connection)
                connection.execute(
                    """
                    INSERT INTO ai_execution_audit (
                        execution_id,
                        use_case,
                        release_id,
                        model,
                        prompt_version,
                        output_schema_version,
                        status,
                        input_count,
                        started_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        execution_id,
                        release.use_case,
                        release.release_id,
                        release.model,
                        release.prompt_version,
                        release.output_schema_version,
                        AIExecutionStatus.STARTED.value,
                        input_count,
                        started_at,
                    ),
                )
        except (OSError, sqlite3.Error) as exc:
            raise AIProcessingError(
                code="ai_audit_unavailable",
                message=f"AI execution audit start failed: {exc.__class__.__name__}",
            ) from exc

        return AIExecutionAudit(
            execution_id=execution_id,
            release_id=release.release_id,
            status=AIExecutionStatus.STARTED,
        )

    def finish(
        self,
        execution_id: str,
        status: AIExecutionStatus,
        *,
        error_code: str | None = None,
    ) -> None:
        if status is AIExecutionStatus.STARTED:
            raise ValueError("finish status must be succeeded or failed")

        try:
            with self._connect() as connection:
                self._ensure_schema(connection)
                cursor = connection.execute(
                    """
                    UPDATE ai_execution_audit
                    SET status = ?, completed_at = ?, error_code = ?
                    WHERE execution_id = ?
                    """,
                    (
                        status.value,
                        datetime.now(UTC).isoformat(),
                        error_code,
                        execution_id,
                    ),
                )
                if cursor.rowcount != 1:
                    raise sqlite3.IntegrityError("AI execution audit record not found")
        except (OSError, sqlite3.Error) as exc:
            raise AIProcessingError(
                code="ai_audit_unavailable",
                message=f"AI execution audit finish failed: {exc.__class__.__name__}",
            ) from exc

    def _connect(self) -> sqlite3.Connection:
        self._database_path.parent.mkdir(parents=True, exist_ok=True)
        return sqlite3.connect(self._database_path, timeout=5)

    @staticmethod
    def _ensure_schema(connection: sqlite3.Connection) -> None:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS ai_execution_audit (
                execution_id TEXT PRIMARY KEY,
                use_case TEXT NOT NULL,
                release_id TEXT NOT NULL,
                model TEXT NOT NULL,
                prompt_version TEXT NOT NULL,
                output_schema_version TEXT NOT NULL,
                status TEXT NOT NULL,
                input_count INTEGER NOT NULL,
                started_at TEXT NOT NULL,
                completed_at TEXT,
                error_code TEXT
            )
            """
        )


@lru_cache
def get_ai_execution_audit_store() -> SQLiteAIExecutionAuditStore:
    return SQLiteAIExecutionAuditStore(get_settings().ai_audit_db_path)
