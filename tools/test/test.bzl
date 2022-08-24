load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load(":runtime_resources.bzl", "runtime_resources")

def grab_android_local_test(
        name,
        deps,
        srcs = [],
        src_sets = ["src/test/java", "src/test/kotlin"],
        associates = [],
        resources = [],
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
    """

    runtime_resources_name = name + "-runtime-resources"
    runtime_resources(
        name = runtime_resources_name,
        deps = deps,
    )

    _gen_test_targets(
        name = name,
        srcs = srcs,
        src_sets = src_sets,
        associates = associates,
        deps = deps,
        test_compile_deps = [
            "@grab_bazel_common//tools/test:mockable-android-jar",
        ],
        test_runtime_deps = [
            ":" + runtime_resources_name,
            "@grab_bazel_common//tools/test:mockable-android-jar",
            "@com_github_jetbrains_kotlin//:kotlin-reflect",
        ],
        resources = resources,
        **kwargs
    )

def grab_kt_jvm_test(
        name,
        deps,
        srcs = [],
        src_sets = ["src/test/java", "src/test/kotlin"],
        associates = [],
        **kwargs):
    """A macro that generates test targets to execute all Kotlin unit tests.

    Usage:
        The macro creates a single build target to compile all unit test classes and then creates
        multiple parallel test targets for each Test class. The name of the test class is derived from
        test class name and location of the file disk.

    Args:
        name: name for the test target,
        srcs: the test sources under test.
        src_sets: The root source set path of all test sources
        deps: the build dependencies to use for the generated the android local test target
        and all valid arguments that you want to pass to the android_local_test target
        associates: associates target to allow access to internal members from the main Kotlin target
        """
    _gen_test_targets(
        name = name,
        srcs = srcs,
        src_sets = src_sets,
        associates = associates,
        deps = deps,
        test_compile_deps = [],
        test_runtime_deps = [
            "@com_github_jetbrains_kotlin//:kotlin-reflect",
        ],
        **kwargs
    )

def _gen_test_targets(
        name,
        srcs,
        src_sets,
        deps,
        test_compile_deps,
        test_runtime_deps,
        associates = [],
        resources = [],
        **kwargs):
    """A macro that detects all test packages to be loaded onto test suite for execution

    Usage:
        The macro works under certain assumptions and only works for Kotlin files. The macro detects
        test packages within the given src_sets in the classpath and uses AllTests test suite to run
        tests.
        In order for this to function correctly, the Kotlin test file and the class name should be the
        same and package name of test class should mirror the location of the file on disk. The default
        root source set path is either src/test/java or src/test/kotlin (this can be made configurable
        via src_sets).

    Args:
    name: name of the target
    srcs: All test sources, mixed Java and Kotlin are supported during build phase but only Kotlin is
    src_sets: The root source set path of all test sources
    supported in runner phase.
    deps: All dependencies required for building test sources
    test_compile_deps: Any dependencies required for the build target.
    test_runtime_deps: Any dependencies required for the test runner target.
    associates: The list of associate targets to allow access to internal members.
    resources: A list of files that should be include in a Java jar.
    """
    test_packages = []

    if len(srcs) == 0:
        srcs = _glob_srcs_from_src_sets(src_sets)

    for src in srcs:
        for src_set in src_sets:
            if src_set[-1] != "/":
                src_set += "/"

            if src.startswith(src_set):
                path = src.split(src_set)[1]

                # com/grab/test/TestFile.kt
                path_split = path.split("/")  # [com,grab,test,TestFile.kt]

                if len(path_split) <= 1:
                    fail("\033[0;31mEmpty test package detected for {}\033[0m".format(src))

                test_package = ".".join(path_split[:-1])

                if test_package not in test_packages:
                    test_packages.append(test_package)

    test_build_target = name
    if len(test_packages) > 0:
        unique_packages = _unique_test_packages(test_packages)
        unique_packages_str = "\",\"".join(unique_packages)
        test_package_file = [test_build_target + "_package.kt"]
        native.genrule(
            name = test_build_target + "_package",
            outs = test_package_file,
            cmd = """
cat << EOF > $@
package com.grab.test
object TestPackageName {{
    @JvmField
    val PACKAGE_NAMES = listOf("{unique_base_packages}")
}}
EOF""".format(unique_base_packages = unique_packages_str),
        )

        kt_jvm_test(
            name = test_build_target,
            srcs = srcs + test_package_file,
            deps = deps + test_compile_deps + ["@grab_bazel_common//tools/test-suite:test-suite"],
            associates = associates,
            test_class = "com.grab.test.AllTests",
            jvm_flags = [
                "-Xverify:none",
                "-Djava.locale.providers=COMPAT,SPI",
            ],
            #shard_count = min(len(test_classes), 16),
            testonly = True,
            runtime_deps = test_runtime_deps,
            resources = resources,
        )

def _unique_test_packages(packages):
    """Extract unique base package names from list of provided package names

    Args:
    packages: List of package name in the format ["com.grab.test", "com.grab"]
    """
    packages = sorted(packages)
    unique_packages = []
    unique_packages.append(packages[0])

    for package in packages:
        if package not in unique_packages:
            not_in_unique_packages = True
            for unique_package in unique_packages:
                # ensure that package is not a subpackage of unique_package
                if package.startswith("{}.".format(unique_package)):
                    not_in_unique_packages = False
                    break

            if not_in_unique_packages:
                unique_packages.append(package)

    return unique_packages

def _glob_srcs_from_src_sets(src_sets):
    """Obtain a list of kt file sources contained within the given src_sets

    Args:
    src_sets: List of paths to glob the kt files
    """
    patterns = []
    for src_set in src_sets:
        if src_set[-1] != "/":
            patterns.append(src_set + "/**/*.kt")
        else:
            patterns.append(src_set + "**/*.kt")
    return native.glob(patterns)
