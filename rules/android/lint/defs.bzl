load(":lint.bzl", _lint = "lint")
load(":lint_inspect.bzl", _lint_inspector = "lint_inspector")
load(":lint_sources.bzl", _lint_sources = "lint_sources")
load(":lint_test.bzl", _lint_test = "lint_test")
load(":lint_update_baseline.bzl", _lint_update_baseline = "lint_update_baseline")
load(":providers.bzl", _LINT_ENABLED = "LINT_ENABLED")
load(":utils.bzl", _baseline = "baseline")

LINT_ENABLED = _LINT_ENABLED

# Rules
lint_sources = _lint_sources
baseline = _baseline

def lint(
        name,
        linting_target,
        lint_baseline):
    """Runs android linting on the linting_target

    Run bazelisk test <name>.lint_test to run Android lint on the target and produce the result XML
    Run baselisk run <name>.lint_update_baseline to update the baseline for use with lint

    Args:
      name: The name of the lint target
      linting_target: The Bazel target to run lint on
      lint_baseline: The baseline XML file to compare results against
    """

    # Normal build target to apply lint aspect and establish AndroidLintInfo provider
    _lint(
        name = name + ".lint",
        target = linting_target,
    )

    _lint_test(
        name = name + ".lint_test",
        target = name + ".lint",
    )

    _lint_update_baseline(
        name = name + ".lint_update_baseline",
        target = name + ".lint",
        baseline = lint_baseline,
    )

    _lint_inspector(
        name = name + ".lint_inspector",
        lint_target = name + ".lint",
        target = name,
    )
