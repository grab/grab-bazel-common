load("@grab_bazel_common//rules/android/databinding:databinding.bzl", "DATABINDING_DEPS")
load("@grab_bazel_common//rules/android/lint:defs.bzl", "LINT_ENABLED", "lint", "lint_sources", _lint_baseline = "baseline")
load("@grab_bazel_common//rules/check/detekt:defs.bzl", "detekt")
load("@grab_bazel_common//tools/build_config:build_config.bzl", _build_config = "build_config")
load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")
load(":resources.bzl", "build_resources")

def android_binary(
        name,
        debug = True,
        build_config = {},
        custom_package = "",
        res_values = {},
        enable_data_binding = False,
        enable_compose = False,
        lint_options = {},
        detekt_options = {},
        **attrs):
    """
    `android_binary` wrapper that setups a native.android_binary with various customizations

    Args:
      name: Name of the target
      debug: Whether to enable debug,
      build_config: `dict` accepting various types such as `strings`, `booleans`, `ints`, `longs` and their corresponding values for generating
                    build config fields
      res_values: `dict` accepting `string` key and their values. The value will be used to generate android resources and then adds as a
                 resource for `android_binary`.
      custom_package: The package name for android_binary, must be same as one declared in AndroidManifest.xml
      lint_options: Lint options to pass to lint, typically contains baselines and config.xml
      detekt_options: detekt options to pass to detekt, typically contains baselines and config.yml
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
    merged_resources = build_resources(
        name = name,
        is_binary = True,
        namespace = attrs.get("manifest_values")["applicationId"],
        manifest = attrs.get("manifest", None),
        resource_files = attrs.get("resource_files", default = []),
        resource_sets = attrs.get("resource_sets", default = {}),
        res_values = res_values,
    )
    resource_files = merged_resources.res
    manifest = merged_resources.manifest

    # Kotlin compilation with kt_android_library
    kotlin_target = "lib_" + name
    kotlin_library_deps = attrs.get("deps", default = []) + [build_config_target]
    if enable_compose:
        kotlin_library_deps.extend(["@grab_bazel_common//rules/android/compose:compose-plugin"])

    kt_android_library(
        name = kotlin_target,
        srcs = attrs.get("srcs", default = []),
        assets = merged_resources.assets,
        assets_dir = merged_resources.asset_dir,
        custom_package = custom_package,
        manifest = manifest,
        resource_files = resource_files,
        visibility = attrs.get("visibility", default = None),
        deps = kotlin_library_deps,
    )

    lint_enabled = lint_options.get("enabled", False) and (len(attrs.get("srcs", default = [])) > 0 or len(resource_files) > 0)
    tags = []
    android_binary_deps = [kotlin_target]

    if lint_enabled:
        lint_sources_target = "_" + name + "_lint_sources"
        lint_baseline = _lint_baseline(lint_options.get("baseline", None))
        lint_sources(
            name = lint_sources_target,
            srcs = attrs.get("srcs", default = []),
            resources = [file for file in resource_files if file.endswith(".xml")],
            manifest = manifest,
            baseline = lint_baseline,
            lint_config = lint_options.get("config", None),
            deps = kotlin_library_deps,
            lint_checks = lint_options.get("lint_checks", default = []),
            fail_on_warning = lint_options.get("fail_on_warning", default = True),
            fail_on_information = lint_options.get("fail_on_information", default = True),
        )

        # Build deps
        android_binary_deps = android_binary_deps + [lint_sources_target]
        tags = [LINT_ENABLED]
        lint(
            name = name,
            linting_target = name,
            lint_baseline = lint_baseline,
        )

    if (detekt_options.get("enabled", False) and len(attrs.get("srcs", default = [])) > 0):
        detekt(
            name = name,
            baseline = detekt_options.get("baseline", None),
            cfgs = detekt_options.get("config", None),
            srcs = attrs.get("srcs", default = []),
            parallel = detekt_options.get("parallel", default = False),
            all_rules = detekt_options.get("all_rules", default = False),
            build_upon_default_config = detekt_options.get("build_upon_default_config", default = False),
            disable_default_rule_sets = detekt_options.get("disable_default_rule_sets", default = False),
            auto_correct = detekt_options.get("auto_correct", default = False),
            detekt_checks = detekt_options.get("detekt_checks", default = []),
        )

    if enable_data_binding:
        android_binary_deps.extend(DATABINDING_DEPS)

    native.android_binary(
        name = name,
        custom_package = custom_package,
        enable_data_binding = enable_data_binding,
        deps = android_binary_deps,
        crunch_png = attrs.get("crunch_png", default = True),
        debug_key = attrs.get("debug_key", default = None),
        densities = attrs.get("densities", default = None),
        dex_shards = attrs.get("dex_shards", default = None),
        dexopts = attrs.get("dexopts", default = None),
        incremental_dexing = attrs.get("incremental_dexing", default = None),
        javacopts = attrs.get("javacopts", default = None),
        manifest = manifest,
        multidex = attrs.get("multidex", default = None),
        manifest_values = attrs.get("manifest_values", default = None),
        resource_configuration_filters = attrs.get("resource_configuration_filters", default = None),
        plugins = attrs.get("plugins", default = None),
        tags = tags,
        visibility = attrs.get("visibility", default = None),
    )
