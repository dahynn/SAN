from dataclasses import dataclass
from enum import StrEnum
from typing import Iterable

from app.core.exceptions import AIProcessingError


class AIReleaseStatus(StrEnum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


@dataclass(frozen=True)
class AIContract:
    use_case: str
    prompt_version: str
    output_schema_version: str


@dataclass(frozen=True)
class AIRelease:
    release_id: str
    use_case: str
    model: str
    prompt_version: str
    output_schema_version: str
    approval_status: AIReleaseStatus

    def matches(self, contract: AIContract, model: str) -> bool:
        return (
            self.use_case == contract.use_case
            and self.model == model
            and self.prompt_version == contract.prompt_version
            and self.output_schema_version == contract.output_schema_version
        )


TIL_AI_CONTRACT = AIContract(
    use_case="til-generation",
    prompt_version="til-summary-v1+til-group-v1",
    output_schema_version="til-response-v1",
)


# 모델·프롬프트·출력 스키마가 모두 일치하는 레코드만 승인 대상으로 취급합니다.
# 조합을 변경할 때는 새 release_id로 레코드를 추가하고 검토가 끝난 뒤 상태를 변경합니다.
AI_RELEASES: tuple[AIRelease, ...] = (
    AIRelease(
        release_id="til-generation-2026-09-13",
        use_case=TIL_AI_CONTRACT.use_case,
        model="gpt-5.2",
        prompt_version=TIL_AI_CONTRACT.prompt_version,
        output_schema_version=TIL_AI_CONTRACT.output_schema_version,
        approval_status=AIReleaseStatus.APPROVED,
    ),
)


def require_approved_ai_release(
    contract: AIContract,
    model: str,
    releases: Iterable[AIRelease] | None = None,
) -> AIRelease:
    registry = AI_RELEASES if releases is None else releases
    matched = next((release for release in registry if release.matches(contract, model)), None)

    if matched is None or matched.approval_status is not AIReleaseStatus.APPROVED:
        status = matched.approval_status.value if matched else "unregistered"
        raise AIProcessingError(
            code="ai_release_not_approved",
            message=(
                "AI release blocked: "
                f"use_case={contract.use_case}, model={model}, "
                f"prompt={contract.prompt_version}, schema={contract.output_schema_version}, "
                f"status={status}"
            ),
        )

    return matched
