load("@grab_bazel_common//rules/test:test.bzl", "gen_test_targets")

def kotlin_test(
        name,
        srcs,
        deps,
        additional_src_sets = [],
        associates = [],
        **kwargs):
    """A macro that generates test targets to execute all Kotlin unit tests.

    Usage:
        The macro creates a single build target to compile all unit test classes and then creates a test target containing each Test class.
        The name of the test target is derived from test class name and location of the file on disk.

    Args:
        name: name for the test target,
        srcs: the test sources under test.
        src_sets: The root source set path of all test sources
        deps: the build dependencies to use for the generated the android local test target
        and all valid arguments that you want to pass to the android_local_test target
        associates: associates target to allow access to internal members from the main Kotlin target
        """
    gen_test_targets(
        name = name,
        srcs = srcs,
        additional_src_sets = additional_src_sets,
        associates = associates,
        deps = deps,
        test_compile_deps = [],
        test_runtime_deps = [
            "@com_github_jetbrains_kotlin//:kotlin-reflect",
        ],
        **kwargs
    )
