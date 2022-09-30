load(
    "@io_bazel_rules_kotlin//kotlin:jvm.bzl",
    _kt_jvm_library = "kt_jvm_library",
)

_ANDROID_SDK_JAR = "@io_bazel_rules_kotlin//third_party:android_sdk"

def _kt_android_artifact(
        name,
        srcs = [],
        deps = [],
        resources = [],
        plugins = [],
        associates = [],
        kotlinc_opts = None,
        javac_opts = None,
        enable_data_binding = False,
        tags = [],
        exec_properties = None,
        **kwargs):
    """Delegates Android related build attributes to the native rules but uses the Kotlin builder to compile Java and
    Kotlin srcs. Returns a sequence of labels that a wrapping macro should export.
    """
    base_name = name + "_base"
    kt_name = name + "_kt"

    # TODO(bazelbuild/rules_kotlin/issues/273): This should be retrieved from a provider.
    base_deps = [_ANDROID_SDK_JAR] + deps

    # TODO(bazelbuild/rules_kotlin/issues/556): replace with starlark
    # buildifier: disable=native-android
    native.android_library(
        name = base_name,
        visibility = ["//visibility:private"],
        exports = base_deps,
        deps = deps if enable_data_binding else [],
        enable_data_binding = enable_data_binding,
        tags = tags,
        exec_properties = exec_properties,
        **kwargs
    )
    _kt_jvm_library(
        name = kt_name,
        srcs = srcs,
        deps = [base_name] + base_deps,
        resources = resources,
        plugins = plugins,
        associates = associates,
        testonly = kwargs.get("testonly", default = False),
        visibility = ["//visibility:public"],
        kotlinc_opts = kotlinc_opts,
        javac_opts = javac_opts,
        tags = tags,
        exec_properties = exec_properties,
    )
    return [base_name, kt_name]

def kt_android_library(name, exports = [], visibility = None, exec_properties = None, **kwargs):
    """Creates an Android sandwich library.
    `srcs`, `deps`, `plugins` are routed to `kt_jvm_library` the other android
    related attributes are handled by the native `android_library` rule.
    """

    # TODO(bazelbuild/rules_kotlin/issues/556): replace with starlark
    # buildifier: disable=native-android
    native.android_library(
        name = name,
        exports = exports + _kt_android_artifact(name, exec_properties = exec_properties, **kwargs),
        visibility = visibility,
        tags = kwargs.get("tags", default = None),
        testonly = kwargs.get("testonly", default = 0),
        exec_properties = exec_properties,
    )
