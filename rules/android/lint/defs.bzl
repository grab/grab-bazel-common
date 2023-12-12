load(":lint.bzl", _lint = "lint")
load(":lint_test.bzl", _lint_test = "lint_test")
load(":lint_sources.bzl", _lint_sources = "lint_sources")
load(":lint_update_baseline.bzl", _lint_update_baseline = "lint_update_baseline")
load(":providers.bzl", _LINT_ENABLED = "LINT_ENABLED")

LINT_ENABLED = _LINT_ENABLED

# Rules
lint = _lint
lint_test = _lint_test
lint_sources = _lint_sources
lint_update_baseline = _lint_update_baseline
