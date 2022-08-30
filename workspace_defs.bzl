load("@bazel_tools//tools/build_defs/repo:git.bzl", "new_git_repository")

GRAB_BAZEL_COMMON_ARTIFACTS = [
    "com.google.guava:guava:29.0-jre",
    "com.google.auto:auto-common:0.10",
    "com.google.auto.service:auto-service:1.0-rc6",
    "com.google.protobuf:protobuf-java:3.6.0",
    "com.google.protobuf:protobuf-java-util:3.6.0",
    "com.squareup:javapoet:1.13.0",
    "com.github.ajalt:clikt:2.8.0",
    "org.ow2.asm:asm:6.0",
    "org.ow2.asm:asm-tree:6.0",
    "xmlpull:xmlpull:1.1.3.1",
    "net.sf.kxml:kxml2:2.3.0",
    "com.squareup.moshi:moshi:1.11.0",
    "org.jetbrains.kotlin:kotlin-parcelize-compiler:1.6.10",
    "org.jetbrains.kotlin:kotlin-parcelize-runtime:1.6.10",
]

ANDROID_TOOLS_BUILD_FILE = """
exports_files([
  "all_android_tools_deploy.jar",
  "ImportDepsChecker_deploy.jar",
  "desugar_jdk_libs.jar",
])
"""

def android_tools(commit, remote, **kwargs):
    """
    Register a @android_tools repository used for databinding that overrides the official @android_tools repository with precompiled
    android tools' library jars

    Args:
      Provide typical arguments that would be provided to new_git_repository.
    """
    native.bind(
        name = "databinding_annotation_processor",
        actual = "@grab_bazel_common//tools/android:compiler_annotation_processor",
    )

    new_git_repository(
        name = "android_tools",
        commit = commit,
        remote = remote,
        build_file_content = ANDROID_TOOLS_BUILD_FILE,
        **kwargs
    )
