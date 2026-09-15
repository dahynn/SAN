from unittest.mock import patch

from app.core.config import Settings
from app.llms.openai import create_openai_chat_model


@patch("app.llms.openai.ChatOpenAI")
def test_create_chat_model_can_use_responses_api(chat_openai):
    settings = Settings(
        openai_api_key="test-key",
        openai_base_url="https://example.test/v1",
        openai_model="test-model",
        openai_use_responses_api=True,
    )

    create_openai_chat_model(settings)

    chat_openai.assert_called_once_with(
        model="test-model",
        api_key="test-key",
        base_url="https://example.test/v1",
        timeout=180.0,
        use_responses_api=True,
    )
