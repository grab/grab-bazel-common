"""
Initializes dependencies declared by bazel_common_dependencies().

Call bazel_common_deps_init() in WORKSPACE after bazel_common_dependencies()
and before rules_java_dependencies().
"""

load("@rules_android//:prereqs.bzl", "rules_android_prereqs")
load("@bazel_features//:deps.bzl", "bazel_features_deps")
load("//android/tools:defs.bzl", "android_tools")

def bazel_common_deps_init(patched_android_tools = True):
    """Downloads transitive dependencies and sets up android_tools.

    Must be called after bazel_common_dependencies() and before rules_java_dependencies().

    Args:
        patched_android_tools: If True (default), registers patched @android_tools
            with databinding 7.1.0 before rules_android_prereqs(). If False,
            rules_android_prereqs() downloads the default @android_tools.
    """
    if patched_android_tools:
        android_tools()
    rules_android_prereqs()
    bazel_features_deps()
