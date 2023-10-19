load("@bazel_common_maven//:defs.bzl", _pinned_maven_install = "pinned_maven_install")

def pin_bazel_common_dependencies():
    _pinned_maven_install()
