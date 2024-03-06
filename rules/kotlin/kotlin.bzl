load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", _kt_jvm_library = "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", _kt_compiler_plugin = "kt_compiler_plugin")
load("@grab_bazel_common//rules/android/lint:defs.bzl", "LINT_ENABLED", "lint", "lint_sources", _lint_baseline = "baseline")

def kt_jvm_library(
        name,
        lint_options = {},
        **attrs):
    srcs = attrs.get("srcs", default = [])
    lint_sources_target = "_" + name + "_lint_sources"
    lint_baseline = _lint_baseline(lint_options.get("baseline", None))
    lint_sources(
        name = lint_sources_target,
        srcs = srcs,
        resources = [],
        manifest = None,
        baseline = lint_baseline,
        lint_config = lint_options.get("lint_config", None),
    )

    # TODO Don't run by default and only run when lintOptions.enabled = true
    if (len(srcs) != 0):
        attrs = _add_value(attrs, "deps", lint_sources_target)
        attrs = _add_value(attrs, "tags", LINT_ENABLED)
    _kt_jvm_library(
        name = name,
        **attrs
    )

    lint(
        name = name,
        linting_target = name,
        lint_baseline = lint_baseline,
    )

kt_compiler_plugin = _kt_compiler_plugin

def _add_value(attrs, name, value):
    if name in attrs and attrs[name] != None:
        attrs[name] = attrs.get(name, default = []) + [value]
    else:
        attrs[name] = [value]
    return attrs
