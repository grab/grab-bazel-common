"""
Sets up transitive dependencies required by Bazel 8.

Call bazel_common_deps_setup() in WORKSPACE after rules_java_dependencies(),
rules_java_toolchains(), rules_jvm_external_deps(), and rules_jvm_external_setup()
have been called.

Due to Bazel 8's stricter transitive dependency requirements, the WORKSPACE
needs explicit sequential load() and call steps between deps_init and setup.
See WORKSPACE for the full required sequence.
"""

load("@rules_java//java:repositories.bzl", "rules_java_toolchains")
load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

def bazel_common_deps_setup():
    """Sets up rules_java toolchains and rules_jvm_external deps for Bazel 8.

    Must be called after bazel_common_deps_init() -> rules_java_dependencies()
    and before bazel_common_setup().
    """
    rules_java_toolchains()
    rules_jvm_external_deps()
