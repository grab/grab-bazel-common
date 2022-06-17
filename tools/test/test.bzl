load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")
load(":runtime_resources.bzl", "runtime_resources")

def grab_android_local_test(
        name,
        srcs,
        deps,
        associates = [],
        custom_package = "",
        **kwargs):
    """A macro that generates test targets to execute all android library unit tests.

    Usage:
    The macro creates a single build target to compile all Android unit test classes and then creates
    multiple parallel test targets for each Test class. The name of the test class is derived from
    test class name and location of the file on disk.

    The macro adds a mocked Android jar to compile classpath similar to Android Gradle Plugin's
    testOptions.unitTests.returnDefaultValues = true feature.

    The macro assumes Kotlin is used and will use rules_kotlin's kt_jvm_library to compile test
    sources with mocked android.jar on the classpath. The test will be executed with java_test.

    Executing via Robolectric is currently not supported.

    Args:
        name: name for the test target,
        srcs: the test sources under test.
        deps: the build dependencies to use for the generated the android local test target
        and all valid arguments that you want to pass to the android_local_test target
        associates: associates target to allow access to internal members from the main Kotlin target
    """

    runtime_resources_name = name + "-runtime-resources"
    runtime_resources(
        name = runtime_resources_name,
        deps = deps,
    )

    _gen_test_targets(
        name = name,
        srcs = srcs,
        associates = associates,
        deps = deps + [":" + runtime_resources_name],
        test_compile_deps = [
            "@grab_bazel_common//tools/test:mockable-android-jar",
        ],
        test_runtime_deps = [
            "@grab_bazel_common//tools/test:mockable-android-jar",
            "@com_github_jetbrains_kotlin//:kotlin-reflect",
        ],
        **kwargs
    )

def grab_kt_jvm_test(
        name,
        srcs,
        deps,
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
        deps: the build dependencies to use for the generated the android local test target
        and all valid arguments that you want to pass to the android_local_test target
        associates: associates target to allow access to internal members from the main Kotlin target
        """
    _gen_test_targets(
        name = name,
        srcs = srcs,
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
        deps,
        test_compile_deps,
        test_runtime_deps,
        associates = [],
        **kwargs):
    """A macro to auto generate and compile target and runner targets for tests.

    Usage:
        The macro works under certain assumptions and only works for Kotlin files. The macro builds
        all test sources in a single target specified by test_compile_rule_type and then generates
        parallel runner targets with test_runner_rule_type.
        In order for this to function correctly, the Kotlin test file and the class name should be the
        same and package name of test class should mirror the location of the file on disk. The root
        source set path must be either src/main/java or src/main/kotlin (this can be made configurable
        in the future).

    Args:
    test_compile_rule_type: The rule type that will be used for compiling test sources
    test_runner_rule_type: The rule type that will be used for running test targets
    name: name of the target
    srcs: All test sources, mixed Java and Kotlin are supported during build phase but only Kotlin is
    supported in runner phase.
    deps: All dependencies required for building test sources
    test_compile_deps: Any dependencies required for the build target.
    test_runtime_deps: Any dependencies required for the test runner target.
    associates: The list of associate targets to allow access to internal members.
    """
    test_classes = []
    for src in srcs:
        if src.endswith("Test.kt") or src.endswith("Tests.kt"):
            # src/test/java/com/grab/test/TestFile.kt
            path_split = src.rpartition("/")  # [src/test/java/com/grab/test,/,TestFile.kt]

            test_file = path_split[2]  # Testfile.kt
            test_file_name = test_file.split(".")[0]  # Testfile

            # Find package name from path
            path = path_split[0]  # src/main/java/com/grab/test

            test_package = ""
            if path.find("src/test/java/") != -1 or path.find("src/test/kotlin/") != -1:  # TODO make this path configurable
                path = path.split("src/test/java/")[1] if path.find("src/test/java/") != -1 else path.split("src/test/kotlin/")[1]  # com/grab/test
                test_class = path.replace("/", ".") + "." + test_file_name  # com.grab.test.TestFile
                test_target_name = test_class.replace(".", "_")
                test_classes.append(test_class)

    test_build_target = name
    if len(test_classes) > 0:
        test_package = _common_package(test_classes)
        test_package_file = [test_build_target + "_package.kt"]
        native.genrule(
            name = test_build_target + "_package",
            outs = test_package_file,
            cmd = """
cat << EOF > $@
package com.grab.test
object TestPackageName {{
    @JvmField
    val PACKAGE_NAME = "{test_package}"
}}
EOF""".format(test_package = test_package),
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
        )

def _common_package(packages):
    """
    Extract common package name from list of provided package name
    Args:
    packages: List of package name in the format [com.grab.test], [com.grab]
    """
    packages = sorted(packages, reverse = True)
    common_path = ""

    if len(packages) == 1:
        chunks = packages[0].split(".")
        chunks.pop()
        return ".".join(chunks)

    paths = []

    for package in packages:
        paths.append(package.split("."))

    for j in range(len(paths[0])):
        path = paths[0][j]
        for i in range(len(packages)):
            if i != 0:
                if (path != paths[i][j]):
                    return common_path.strip(".")
        common_path = common_path + path + "."

    return common_path.strip(".")
