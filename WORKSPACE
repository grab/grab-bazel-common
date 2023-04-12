workspace(name = "grab_bazel_common")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@grab_bazel_common//android:repositories.bzl", "bazel_common_dependencies")

bazel_common_dependencies()

load("@grab_bazel_common//android:initialize.bzl", "bazel_common_initialize")

bazel_common_initialize(
    buildifier_version = "5.1.0",
    pinned_maven_install = True,
)

load("@grab_bazel_common//android:maven.bzl", "pin_bazel_common_artifacts")

pin_bazel_common_artifacts()

android_sdk_repository(
    name = "androidsdk",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@grab_bazel_common//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")

# Artifacts that need to be present on the consumer under @maven. They can be overriden
# by the consumer's maven_install rule.
maven_install(
    artifacts = GRAB_BAZEL_COMMON_ARTIFACTS,
    repositories = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
    ],
    strict_visibility = True,
)
