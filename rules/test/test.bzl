load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

_DEFAULT_SRC_SETS = ["src/test/java", "src/test/kotlin"]

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

def gen_test_targets(
        name,
        srcs,
        additional_src_sets,
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
    src_sets = _DEFAULT_SRC_SETS + additional_src_sets

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
            deps = deps + test_compile_deps + ["@grab_bazel_common//tools/test_suite:test_suite"],
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
