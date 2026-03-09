workspace(name = "grab_bazel_common")

# Step 1: Declare all repository dependencies
load("@grab_bazel_common//rules:repositories.bzl", "bazel_common_dependencies")

bazel_common_dependencies()

# Step 2: Initialize prereqs (android_tools, rules_android, bazel_features)
load("@grab_bazel_common//rules:deps_init.bzl", "bazel_common_deps_init")

bazel_common_deps_init()

# Step 3: Set up transitive deps required by Bazel 8
# These must be called in strict sequence due to repo creation dependencies.
load("@rules_cc//cc:extensions.bzl", "compatibility_proxy_repo")

compatibility_proxy_repo()

load("@rules_java//java:rules_java_deps.bzl", "rules_java_dependencies")

rules_java_dependencies()

load("@com_google_protobuf//bazel/private:proto_bazel_features.bzl", "proto_bazel_features")

proto_bazel_features(name = "proto_bazel_features")

load("@grab_bazel_common//rules:deps_setup.bzl", "bazel_common_deps_setup")

bazel_common_deps_setup()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

# Step 4: Main setup (kotlin, android, maven, detekt)
load("@grab_bazel_common//rules:setup.bzl", "bazel_common_setup")

bazel_common_setup(
    buildifier_version = "6.3.3",
    pinned_maven_install = True,
)

# Step 5: Pin maven dependencies
load("@grab_bazel_common//rules:maven.bzl", "pin_bazel_common_dependencies")

pin_bazel_common_dependencies()

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
