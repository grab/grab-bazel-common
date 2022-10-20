ANDROID_JARS = [
    "all_android_tools_deploy.jar",
    "ImportDepsChecker_deploy.jar",
    "desugar_jdk_libs.jar",
]

def _patched_android_tools_impl(repository_ctx):
    attr = repository_ctx.attr

    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )

    # android_tools expects jar at the root of the repository so alias it from there to actual targets
    # inside bazel common
    alias_target = """
alias(
    name = "{jar}",
    actual = "@grab_bazel_common//android/tools:{jar}",
    visibility = [
        "//visibility:public",
    ],
)
"""

    build_file_content = ""
    for jar in ANDROID_JARS:
        build_file_content += alias_target.format(jar = jar)

    repository_ctx.file(
        "BUILD",
        content = build_file_content,
    )

patched_android_tools = repository_rule(
    implementation = _patched_android_tools_impl,
)

def android_tools():
    """
    Setup an @android_tools repository with expected jars from this repo.
    """
    patched_android_tools(name = "android_tools")
