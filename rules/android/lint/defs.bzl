load(":lint.bzl", _lint_test = "lint_test")
load(":lint_sources.bzl", _lint_sources = "lint_sources")
load(":providers.bzl", _LINT_ENABLED = "LINT_ENABLED")

LINT_ENABLED = _LINT_ENABLED

# Rules
lint_test = _lint_test
lint_sources = _lint_sources
