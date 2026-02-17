load("@rules_android//:prereqs.bzl", "rules_android_prereqs")
load("@bazel_features//:deps.bzl", "bazel_features_deps")
load("//android/tools:defs.bzl", "android_tools")

def bazel_common_prereqs(patched_android_tools = True):
    """Downloads transitive dependencies and sets up android_tools.

    Must be called after bazel_common_dependencies() and before bazel_common_setup().

    Args:
        patched_android_tools: If True (default), registers patched @android_tools
            with databinding 7.1.0 before rules_android_prereqs(). If False,
            rules_android_prereqs() downloads the default @android_tools.
    """
    if patched_android_tools:
        # Patched android_tools MUST be before rules_android_prereqs() so maybe() in
        # prereqs.bzl sees existing @android_tools and skips the default one.
        android_tools()
    rules_android_prereqs()
    bazel_features_deps()
