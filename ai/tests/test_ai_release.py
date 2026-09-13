import pytest

from app.core.ai_release import (
    AIContract,
    AIRelease,
    AIReleaseStatus,
    TIL_AI_CONTRACT,
    require_approved_ai_release,
)
from app.core.exceptions import AIProcessingError


def _release(
    *,
    model: str = "gpt-5.2",
    prompt_version: str = TIL_AI_CONTRACT.prompt_version,
    output_schema_version: str = TIL_AI_CONTRACT.output_schema_version,
    status: AIReleaseStatus = AIReleaseStatus.APPROVED,
) -> AIRelease:
    return AIRelease(
        release_id="test-release",
        use_case=TIL_AI_CONTRACT.use_case,
        model=model,
        prompt_version=prompt_version,
        output_schema_version=output_schema_version,
        approval_status=status,
    )


def test_exact_approved_release_is_allowed() -> None:
    release = _release()

    selected = require_approved_ai_release(TIL_AI_CONTRACT, "gpt-5.2", [release])

    assert selected == release


@pytest.mark.parametrize("status", [AIReleaseStatus.PENDING, AIReleaseStatus.REJECTED])
def test_non_approved_release_is_blocked(status: AIReleaseStatus) -> None:
    with pytest.raises(AIProcessingError) as exc_info:
        require_approved_ai_release(TIL_AI_CONTRACT, "gpt-5.2", [_release(status=status)])

    assert exc_info.value.code == "ai_release_not_approved"
    assert f"status={status.value}" in exc_info.value.message


@pytest.mark.parametrize(
    ("contract", "model"),
    [
        (TIL_AI_CONTRACT, "unapproved-model"),
        (
            AIContract(
                use_case=TIL_AI_CONTRACT.use_case,
                prompt_version="til-pipeline-v2",
                output_schema_version=TIL_AI_CONTRACT.output_schema_version,
            ),
            "gpt-5.2",
        ),
        (
            AIContract(
                use_case=TIL_AI_CONTRACT.use_case,
                prompt_version=TIL_AI_CONTRACT.prompt_version,
                output_schema_version="til-response-v2",
            ),
            "gpt-5.2",
        ),
    ],
)
def test_unregistered_model_prompt_or_schema_combination_is_blocked(
    contract: AIContract,
    model: str,
) -> None:
    with pytest.raises(AIProcessingError) as exc_info:
        require_approved_ai_release(contract, model, [_release()])

    assert exc_info.value.code == "ai_release_not_approved"
    assert "status=unregistered" in exc_info.value.message
