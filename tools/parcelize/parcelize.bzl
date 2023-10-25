load("@grab_bazel_common//rules:defs.bzl", "kotlin_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_compiler_plugin")

def parcelize_rules():
    """Create Kotlin parcelize rules

    Usage:
    This macro exposes :parcelize target which is kt_jvm_library that exports the parcelize compiler
    plugin. Ideal usage is to declare this macro in root BUILD.bazel and invoke it via //:parcelize
    on a kt_android_library or kt_jvm_library target.

    kt_android_library(
        name = "lib",
        resource_files = glob(["src/main/res/**",]),
        custom_package = "your.package",
        manifest = "src/main/AndroidManifest.xml",
        visibility = ["//visibility:public"],
        srcs = glob(["src/main/java/**/*.kt"]),
        deps = ["//:android-extensions"],
    )

    """
    kt_compiler_plugin(
        name = "parcelize_plugin",
        id = "org.jetbrains.kotlin.parcelize",
        compile_phase = True,
        stubs_phase = False,
        target_embedded_compiler = True,
        deps = [
            "@maven//:org_jetbrains_kotlin_kotlin_parcelize_compiler",
        ],
    )

    kotlin_library(
        name = "parcelize",
        exported_compiler_plugins = [":parcelize_plugin"],
        visibility = ["//visibility:public"],
        exports = [
            "@maven//:org_jetbrains_kotlin_kotlin_parcelize_runtime",
        ],
    )
