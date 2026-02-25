workspace(name = "grab_bazel_common")

load("@grab_bazel_common//rules:repositories.bzl", "bazel_common_dependencies")

bazel_common_dependencies()

load("@grab_bazel_common//rules:deps_init.bzl", "bazel_common_deps_init")

bazel_common_deps_init()

load("@grab_bazel_common//rules:setup.bzl", "bazel_common_setup")

bazel_common_setup(
    buildifier_version = "6.3.3",
    pinned_maven_install = True,
)

load("@grab_bazel_common//rules:maven.bzl", "pin_bazel_common_dependencies")

pin_bazel_common_dependencies()

android_sdk_repository(
    name = "androidsdk",
)

load("@grab_bazel_common//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")
load("@rules_jvm_external//:defs.bzl", "maven_install")

# Artifacts that need to be present on the consumer under @maven. They can be overridden
# by the consumer's maven_install rule.
maven_install(
    artifacts = GRAB_BAZEL_COMMON_ARTIFACTS,
    repositories = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
    ],
    strict_visibility = True,
)
