load("@grab_bazel_common//rules/android/lint:defs.bzl", "lint", "lint_sources")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "LINT_ENABLED")
load("@grab_bazel_common//rules/check/detekt:defs.bzl", "detekt")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", _kt_jvm_library = "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", _kt_compiler_plugin = "kt_compiler_plugin")

kt_jvm_library = _kt_jvm_library
kt_compiler_plugin = _kt_compiler_plugin

def kt_jvm_library_interal(
        name,
        exec_properties = None,
        lint_options = {},
        detekt_options = {},
        **attrs):
    srcs = attrs.get("srcs", default = [])
    lint_sources_target = "_" + name + "_lint_sources"
    lint_baseline = lint_options.get("baseline", None)
    lint_enabled = lint_options.get("enabled", False) and len(srcs) > 0
    tags = attrs.get("tags", default = [])
    deps = attrs.get("deps", default = [])

    if lint_enabled:
        lint_sources(
            name = lint_sources_target,
            srcs = attrs.get("srcs"),
            baseline = lint_baseline,
            lint_config = lint_options.get("config", None),
            fail_on_warning = lint_options.get("fail_on_warning", default = True),
            fail_on_information = lint_options.get("fail_on_information", default = True),
        )

        lint(
            name = name,
            linting_target = name,
            lint_baseline = lint_baseline,
        )
        tags = tags + [LINT_ENABLED]
        deps = deps + [lint_sources_target]

        attrs["deps"] = deps
        attrs["tags"] = tags

    if (detekt_options.get("enabled", False) and len(srcs) > 0):
        detekt(
            name = name,
            baseline = detekt_options.get("baseline", None),
            cfgs = detekt_options.get("config", None),
            srcs = srcs,
            parallel = detekt_options.get("parallel", default = False),
            all_rules = detekt_options.get("all_rules", default = False),
            build_upon_default_config = detekt_options.get("build_upon_default_config", default = False),
            disable_default_rule_sets = detekt_options.get("disable_default_rule_sets", default = False),
            auto_correct = detekt_options.get("auto_correct", default = False),
            detekt_checks = detekt_options.get("detekt_checks", default = []),
        )

    kt_jvm_library(
        name = name,
        **attrs
    )
