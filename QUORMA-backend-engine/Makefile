# Python: python3 on Linux/macOS, python on Windows. Override with `make PYTHON=py ...`
PYTHON ?= $(shell command -v python3 2>/dev/null || command -v python 2>/dev/null || echo python)

.PHONY: validate engine-test

validate:
	$(PYTHON) data/validate.py data

# Unit tests exist for the engine only (the backend has none by decision)
engine-test:
	mvn -pl game-engine test
