load("@grab_bazel_common//tools/build_config:build_config.bzl", _build_config = "build_config")
load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")
load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")
load("@grab_bazel_common//rules/android/databinding:databinding.bzl", "DATABINDING_DEPS")
load(":resources.bzl", "build_resources")

"""Enhanced android_binary rule with support for build configs, res values, Kotlin compilation and databinding support"""

def android_binary(
        name,
        debug = True,
        build_config = {},
        custom_package = {},
        res_values = {},
        enable_data_binding = False,
        **attrs):
    """
    `android_binary` wrapper that adds Kotlin, build config, databinding and res values support. The attrs are passed to native `android_binary`
    unless otherwise stated.

    Args:
    - name: Name of the target
    - build_config: `dict` accepting various types such as `strings`, `booleans`, `ints`, `longs` and their corresponding values for generating
                    build config fields
    - res_value: `dict` accepting `string` key and their values. The value will be used to generate android resources and then adds as a
                 resource for `android_binary`.
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

    # Kotlin compilation with kt_android_library
    kotlin_target = "lib_" + name
    kt_android_library(
        name = kotlin_target,
        srcs = attrs.get("srcs", default = []),
        assets = attrs.get("assets", default = None),
        assets_dir = attrs.get("assets_dir", default = None),
        custom_package = custom_package,
        manifest = attrs.get("manifest", default = None),
        resource_files = resource_files,
        visibility = attrs.get("visibility", default = None),
        deps = attrs.get("deps", default = []) + [build_config_target],
    )

    # Build deps
    android_binary_deps = [kotlin_target]
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
        manifest = attrs.get("manifest"),
        multidex = attrs.get("multidex", default = None),
        manifest_values = attrs.get("manifest_values", default = None),
        plugins = attrs.get("plugins", default = None),
        visibility = attrs.get("visibility", default = None),
    )
