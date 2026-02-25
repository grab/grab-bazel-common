"""
Initializes dependencies declared by bazel_common_dependencies().

Call bazel_common_deps_init() in WORKSPACE after bazel_common_dependencies()
and before bazel_common_setup().
"""

load("@bazel_features//:deps.bzl", "bazel_features_deps")
load("@rules_java//java:repositories.bzl", "rules_java_dependencies")

def bazel_common_deps_init():
    rules_java_dependencies()
    bazel_features_deps()
