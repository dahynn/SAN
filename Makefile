.PHONY: help verify-imports frontend-install frontend-build backend-test ai-install ai-lint ai-test

UV ?= $(if $(wildcard $(CURDIR)/.local/tools/bin/uv),$(CURDIR)/.local/tools/bin/uv,uv)
export UV_CACHE_DIR ?= $(CURDIR)/.local/uv-cache
export GRADLE_USER_HOME ?= $(CURDIR)/.local/gradle

help:
	@echo 'SAN: verify-imports frontend-install frontend-build backend-test ai-install ai-lint ai-test'

verify-imports:
	python3 scripts/verify-imports.py

frontend-install:
	cd frontend && pnpm install --frozen-lockfile

frontend-build:
	cd frontend && pnpm build:all

backend-test:
	cd backend && ./gradlew test --no-daemon --no-watch-fs --max-workers=2

ai-install:
	cd ai && $(UV) sync --frozen

ai-lint:
	cd ai && $(UV) run --frozen ruff check .

ai-test:
	cd ai && $(UV) run --frozen python ../scripts/test-ai-offline.py
