load("@grab_bazel_common//rules/test:test.bzl", "gen_test_targets")
load("@grab_bazel_common//rules/android:runtime_resources.bzl", "runtime_resources")

def android_unit_test(
        name,
        deps,
        srcs,
        additional_src_sets = [],
        associates = [],
        resources = [],
        enable_compose = False,
        **kwargs):
    """A macro that executes all android library unit tests.

    Usage:
    The macro creates a single build target to compile all Android unit test classes and then loads
    all Test class onto a test suite for execution.

    The macro adds a mocked Android jar to compile classpath similar to Android Gradle Plugin's
    testOptions.unitTests.returnDefaultValues = true feature.

    The macro assumes Kotlin is used and will use rules_kotlin's kt_jvm_test for execution with
    mocked android.jar on the classpath.

    Executing via Robolectric is currently not supported.

    Args:
        name: name for the test target,
        srcs: the test sources under test.
        src_sets: The root source set path of all test sources
        deps: the build dependencies to use for the generated the android local test target
        and all valid arguments that you want to pass to the android_local_test target
        associates: associates target to allow access to internal members from the main Kotlin target
        resources: A list of files that should be include in a Java jar.
        enable_compose: Enable Jetpack Compose compiler on Kotlin sources
    """

    runtime_resources_name = name + "-runtime-resources"
    runtime_resources(
        name = runtime_resources_name,
        deps = deps,
    )

    if enable_compose:
        deps.extend(["@grab_bazel_common//rules/android/compose:compose-plugin"])

    gen_test_targets(
        name = name,
        srcs = srcs,
        additional_src_sets = additional_src_sets,
        associates = associates,
        deps = deps,
        test_compile_deps = [
            "@grab_bazel_common//rules/android:mock_android_jar",
        ],
        test_runtime_deps = [
            ":" + runtime_resources_name,
            "@grab_bazel_common//rules/android:mock_android_jar",
            "@com_github_jetbrains_kotlin//:kotlin-reflect",
        ],
        resources = resources,
        **kwargs
    )
