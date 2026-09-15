from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator

from app.core.exceptions import AIProcessingError


class TilGenerationOutput(BaseModel):
    """Strict contract for the JSON returned by the TIL generation model."""

    title: str = Field(strict=True, min_length=1, max_length=200)
    til_markdown: str = Field(strict=True, min_length=1, max_length=100_000)

    model_config = ConfigDict(extra="forbid")

    @field_validator("title")
    @classmethod
    def validate_title(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized or "\n" in normalized or "\r" in normalized:
            raise ValueError("title must be a non-empty single line")
        return normalized

    @field_validator("til_markdown")
    @classmethod
    def validate_markdown(cls, value: str) -> str:
        normalized = value.strip()
        first_line = normalized.splitlines()[0] if normalized else ""
        if not (first_line == "# TIL" or first_line.startswith("# TIL ")):
            raise ValueError("til_markdown must start with a level-one TIL heading")
        return normalized


def validate_til_generation_output(raw_output: object) -> TilGenerationOutput:
    try:
        return TilGenerationOutput.model_validate(raw_output)
    except ValidationError:
        # Do not expose model output or validation details to the API or audit record.
        raise AIProcessingError(
            code="til_output_contract_violation",
            message="TIL model output did not satisfy the approved output contract",
        ) from None
