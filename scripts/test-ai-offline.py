"""Run SAN AI tests without credentials, dotenv loading, or network access."""
import os
from pathlib import Path
import socket
import sys


def deny_network(*args, **kwargs):
    raise RuntimeError("Network disabled in SAN offline tests")


def main():
    root = Path(__file__).resolve().parents[1]
    os.chdir(root / "ai")
    sys.path.insert(0, str(root / "ai"))
    for key in list(os.environ):
        if any(term in key.upper() for term in ("API_KEY", "TOKEN", "SECRET", "PASSWORD")):
            os.environ.pop(key)
    os.environ["APP_ENV"] = "test"
    os.environ["PYTHON_DOTENV_DISABLED"] = "1"
    # Explicit environment values also override Pydantic's .env settings.
    for key in ("OPENAI_API_KEY", "TAVILY_API_KEY", "GITHUB_API_TOKEN"):
        os.environ[key] = ""
    socket.socket.connect = deny_network
    socket.socket.connect_ex = deny_network
    socket.create_connection = deny_network
    import pytest
    return pytest.main([
        "-q", "tests",
        "--deselect=tests/test_preprocessor.py::test_url_real_extraction_returns_body",
        "--deselect=tests/test_preprocessor.py::test_image_real_analysis_returns_description",
        *sys.argv[1:],
    ])


if __name__ == "__main__":
    raise SystemExit(main())
