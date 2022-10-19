load("@bazel_tools//tools/build_defs/repo:git.bzl", "new_git_repository")

GRAB_BAZEL_COMMON_ARTIFACTS = [
    "org.jetbrains.kotlin:kotlin-parcelize-compiler:1.6.10",
    "org.jetbrains.kotlin:kotlin-parcelize-runtime:1.6.10",
    "androidx.databinding:databinding-adapters:7.1.2",
    "androidx.databinding:databinding-common:7.1.2",
    "androidx.databinding:databinding-runtime:7.1.2",
    "androidx.databinding:viewbinding:7.1.2",
    "org.json:json:20210307",
    "junit:junit:4.13",
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
    Register a @android_tools repository used for databinding that overrides the official @android_tools
    repository with precompiled android tools' library jars

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
