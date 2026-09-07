.PHONY: help verify-imports frontend-install frontend-build backend-test ai-install ai-lint ai-test

help:
	@echo 'SAN: verify-imports frontend-install frontend-build backend-test ai-install ai-lint ai-test'

verify-imports:
	python3 scripts/verify-imports.py

frontend-install:
	cd frontend && pnpm install --frozen-lockfile

frontend-build:
	cd frontend && pnpm build:all

backend-test:
	cd backend && ./gradlew test --no-daemon

ai-install:
	cd ai && uv sync --frozen

ai-lint:
	cd ai && uv run --frozen ruff check .

ai-test:
	cd ai && uv run --frozen pytest
