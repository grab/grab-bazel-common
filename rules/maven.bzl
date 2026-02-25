load("@bazel_common_maven//:defs.bzl", _pinned_maven_install = "pinned_maven_install")
load("@bazel_common_test_maven//:defs.bzl", _pinned_test_maven_install = "pinned_maven_install")
load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

def pin_bazel_common_dependencies():
    rules_jvm_external_setup()
    _pinned_maven_install()
    _pinned_test_maven_install()
