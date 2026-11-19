# Python: python3 on Linux/macOS, python on Windows. Override with `make PYTHON=py ...`
PYTHON ?= $(shell command -v python3 2>/dev/null || command -v python 2>/dev/null || echo python)

# The engine version is the version of the root pom (EngineVersion reads it at build time): an engine image is tagged with it.
ENGINE_VERSION := $(shell $(PYTHON) -c "import re;print(re.search(r'quorma-parent</artifactId>\s*<version>([^<]+)</version>', open('pom.xml', encoding='utf-8').read()).group(1))")
export ENGINE_VERSION

.PHONY: validate engine-test test engine-version engine-image up

validate:
	$(PYTHON) data/validate.py data

# Engine unit tests only (fast, no Docker needed)
engine-test:
	mvn -pl game-engine test

# Every test of every module. The backend's integration tests need Docker running (they are skipped without it)
test:
	mvn test

# Prints the engine version, the tag of the engine image
engine-version:
	@echo $(ENGINE_VERSION)

# Builds the engine image tagged with the engine version (the build refuses a tag that is not the version the engine reports)
engine-image:
	docker build -f engine-service/Dockerfile --build-arg ENGINE_VERSION=$(ENGINE_VERSION) -t quorma-engine:$(ENGINE_VERSION) .

# The whole stack with the engine image tagged with the engine version
up:
	docker compose up --build
