load("@grab_bazel_common//tools/build_config:build_config.bzl", _build_config = "build_config")
load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")
load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")
load("@grab_bazel_common//rules/android/databinding:databinding.bzl", "kt_db_android_library")
load(":resources.bzl", "build_resources")

"""Enhanced android_library rule with support for build configs, res values, Kotlin compilation and databinding support"""

def android_library(
        name,
        debug = True,
        srcs = [],
        build_config = {},
        custom_package = {},
        res_values = {},
        enable_data_binding = False,
        **attrs):
    """
    `android_library` wrapper that adds Kotlin, build config, databinding and res values support.

    Args:
    - name: Name of the target
    - build_config: `dict` accepting various types such as `strings`, `booleans`, `ints`, `longs` and their corresponding values for generating
                    build config fields
    - res_value: `dict` accepting `string` key and their values. The value will be used to generate android resources and then adds as a
                 resource for `android_library`.
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
        res_values = res_values,
    )

    # For now we delegate to existing macros to build the modules, as android library implementation matures, we can remove this and just
    # have one implementation of android_library that does all esp databinding and Kotlin support.
    # Databinding -> kt_db_android_library
    # Android with Kotlin -> kt_android_library
    # Android with resources alone -> native.android_library

    rule_type = native.android_library

    if enable_data_binding:
        rule_type = kt_db_android_library
    elif len(srcs) == 0 and len(resource_files) != 0:
        rules_type = android_library
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
        deps = attrs.get("deps", default = []) + [build_config_target],
        tags = attrs.get("tags", default = None),
        plugins = attrs.get("plugins", default = None),
    )
