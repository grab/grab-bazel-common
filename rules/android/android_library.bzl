load("@grab_bazel_common//tools/build_config:build_config.bzl", _build_config = "build_config")
load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")
load("@grab_bazel_common//rules/android/databinding:databinding.bzl", "kt_db_android_library")
load(":resources.bzl", "build_resources")
load("@grab_bazel_common//rules/android/lint:defs.bzl", "LINT_ENABLED", "lint", "lint_sources")

def android_library(
        name,
        debug = True,
        srcs = [],
        build_config = {},
        custom_package = {},
        res_values = {},
        enable_data_binding = False,
        enable_compose = False,
        lint_options = {},
        **attrs):
    """
    `android_binary` wrapper that setups a native.android_binary with various customizations

    Args:
      name: Name of the target
      debug: Whether to enable debug,
      srcs: Source files for the library, can contain mix of java and kotlin files.
      build_config: `dict` accepting various types such as `strings`, `booleans`, `ints`, `longs` and their corresponding values for generating
                    build config fields
      res_values: `dict` accepting `string` key and their values. The value will be used to generate android resources and then adds as a
                 resource for `android_binary`.
      custom_package: The package name for android_binary, must be same as one declared in AndroidManifest.xml
      lint_options: Lint options to pass to lint, typically contains baselines and config.xml
      enable_data_binding: Enable android databinding support for this target
      enable_compose: Enable Jetpack Compose support for this target
      **attrs: Additional attrs to pass to generated android_binary.
    """

    build_config_target = name + "_build_cfg"
    _build_config(
        name = build_config_target,
        package_name = custom_package,
        debug = debug,
        booleans = build_config.get("booleans", default = {}),
        ints = build_config.get("ints", default = {}),
        longs = build_config.get("longs", default = {}),
        strings = build_config.get("strings", default = {}),
    )

    resource_files = build_resources(
        name = name,
        resource_files = attrs.get("resource_files", default = []),
        resources = attrs.get("resources", default = {}),
        res_values = res_values,
    )

    lint_enabled = lint_options.get("enabled", False) and (len(srcs) > 0 or len(resource_files) > 0)
    android_library_deps = attrs.get("deps", default = []) + [build_config_target]
    tags = attrs.get("tags", default = [])
    if lint_enabled:
        lint_sources_target = "_" + name + "_lint_sources"
        lint_baseline = lint_options.get("baseline", None)
        lint_sources(
            name = lint_sources_target,
            srcs = srcs,
            resources = [file for file in resource_files if file.endswith(".xml")],
            manifest = attrs.get("manifest"),
            baseline = lint_baseline,
            lint_config = lint_options.get("config", None),
            deps = android_library_deps,
            lint_checks = lint_options.get("lint_checks", default = []),
            fail_on_warning = lint_options.get("fail_on_warning", default = True),
            fail_on_information = lint_options.get("fail_on_information", default = True),
        )
        android_library_deps = android_library_deps + [lint_sources_target]
        lint(
            name = name,
            linting_target = name,
            lint_baseline = lint_baseline,
        )
        tags = tags + [LINT_ENABLED]

    if enable_compose:
        android_library_deps.extend(["@grab_bazel_common//rules/android/compose:compose-plugin"])

    # For now we delegate to existing macros to build the modules, as android library implementation matures, we can remove this and just
    # have one implementation of android_library that does all esp databinding and Kotlin support.
    # Databinding -> kt_db_android_library
    # Android with Kotlin -> kt_android_library
    # Android with resources alone -> native.android_library

    rule_type = native.android_library

    if enable_data_binding:
        rule_type = kt_db_android_library
    elif len(srcs) == 0 and len(resource_files) != 0:
        rule_type = native.android_library
    elif len(srcs) != 0:
        rule_type = kt_android_library

    rule_type(
        name = name,
        srcs = srcs,
        custom_package = custom_package,
        manifest = attrs.get("manifest"),
        resource_files = resource_files,
        assets = attrs.get("assets", default = None),
        assets_dir = attrs.get("assets_dir", default = None),
        visibility = attrs.get("visibility", default = None),
        tags = tags,
        deps = android_library_deps,
        plugins = attrs.get("plugins", default = None),
    )
