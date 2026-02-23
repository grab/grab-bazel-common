workspace(name = "grab_bazel_common")

load("@grab_bazel_common//rules:repositories.bzl", "bazel_common_dependencies")

bazel_common_dependencies()

# Initialize rules_java 8.x dependencies (required for rules_java 8.13.0+)
load("@rules_java//java:rules_java_deps.bzl", "rules_java_dependencies")

rules_java_dependencies()

load("@bazel_features//:deps.bzl", "bazel_features_deps")

bazel_features_deps()

load("@com_google_protobuf//bazel/private:proto_bazel_features.bzl", "proto_bazel_features")

proto_bazel_features(name = "proto_bazel_features")

# Register Java toolchains
load("@rules_java//java:repositories.bzl", "rules_java_toolchains")

rules_java_toolchains()

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

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
