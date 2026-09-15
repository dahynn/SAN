import pytest

from app.core.exceptions import AIProcessingError
from app.schemas.til_output import validate_til_generation_output


def test_valid_output_is_normalized() -> None:
    output = validate_til_generation_output(
        {
            "title": "  오늘의 학습  ",
            "til_markdown": "\n# TIL - 2026.09.13\n\n## 주제\n본문\n",
        }
    )

    assert output.title == "오늘의 학습"
    assert output.til_markdown == "# TIL - 2026.09.13\n\n## 주제\n본문"


@pytest.mark.parametrize(
    "raw_output",
    [
        ["title", "til_markdown"],
        {"title": "제목"},
        {"title": "제목", "til_markdown": "# TIL", "debug": "internal"},
        {"title": 123, "til_markdown": "# TIL"},
        {"title": "제목", "til_markdown": ["# TIL"]},
        {"title": "  ", "til_markdown": "# TIL"},
        {"title": "제목\n부제", "til_markdown": "# TIL"},
        {"title": "제목", "til_markdown": "일반 텍스트"},
    ],
)
def test_invalid_output_fails_with_safe_contract_error(raw_output: object) -> None:
    with pytest.raises(AIProcessingError) as error:
        validate_til_generation_output(raw_output)

    assert error.value.code == "til_output_contract_violation"
    assert "debug" not in error.value.message
    assert "internal" not in error.value.message
