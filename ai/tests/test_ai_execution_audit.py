import sqlite3

import pytest

from app.core.ai_execution_audit import AIExecutionStatus, SQLiteAIExecutionAuditStore
from app.core.ai_release import AI_RELEASES
from app.core.exceptions import AIProcessingError


def test_execution_lifecycle_is_persisted_without_source_content(tmp_path) -> None:
    database_path = tmp_path / "audit.sqlite3"
    store = SQLiteAIExecutionAuditStore(database_path)

    audit = store.start(AI_RELEASES[0], input_count=3)
    store.finish(audit.execution_id, AIExecutionStatus.SUCCEEDED)

    with sqlite3.connect(database_path) as connection:
        columns = {
            row[1] for row in connection.execute("PRAGMA table_info(ai_execution_audit)")
        }
        row = connection.execute(
            """
            SELECT release_id, model, prompt_version, output_schema_version,
                   status, input_count, completed_at, error_code
            FROM ai_execution_audit
            WHERE execution_id = ?
            """,
            (audit.execution_id,),
        ).fetchone()

    assert "source_content" not in columns
    assert "prompt" not in columns
    assert row == (
        AI_RELEASES[0].release_id,
        AI_RELEASES[0].model,
        AI_RELEASES[0].prompt_version,
        AI_RELEASES[0].output_schema_version,
        AIExecutionStatus.SUCCEEDED.value,
        3,
        row[6],
        None,
    )
    assert row[6] is not None


def test_failed_execution_keeps_only_safe_error_code(tmp_path) -> None:
    database_path = tmp_path / "audit.sqlite3"
    store = SQLiteAIExecutionAuditStore(database_path)
    audit = store.start(AI_RELEASES[0], input_count=1)

    store.finish(
        audit.execution_id,
        AIExecutionStatus.FAILED,
        error_code="til_generation_failed",
    )

    with sqlite3.connect(database_path) as connection:
        row = connection.execute(
            "SELECT status, error_code FROM ai_execution_audit WHERE execution_id = ?",
            (audit.execution_id,),
        ).fetchone()

    assert row == (AIExecutionStatus.FAILED.value, "til_generation_failed")


def test_audit_write_failure_blocks_execution(tmp_path) -> None:
    directory_path = tmp_path / "not-a-database"
    directory_path.mkdir()
    store = SQLiteAIExecutionAuditStore(directory_path)

    with pytest.raises(AIProcessingError) as exc_info:
        store.start(AI_RELEASES[0], input_count=1)

    assert exc_info.value.code == "ai_audit_unavailable"
