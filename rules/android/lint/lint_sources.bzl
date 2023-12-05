load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintSourcesInfo")

def _target_outputs(targets):
    results = []
    for target in targets:
        if type(target) == "Target":
            for file in target.files.to_list():
                results.append(file)
    return results

def _lint_sources_impl(ctx):
    empty_jar = ctx.files._empty_jar[0]
    return [
        JavaInfo(
            output_jar = empty_jar,
            compile_jar = empty_jar,
            neverlink = True,
        ),
        AndroidLintSourcesInfo(
            name = ctx.attr.name,
            srcs = _target_outputs(ctx.attr.srcs),
            resources = _target_outputs(ctx.attr.resources),
            manifest = _target_outputs([ctx.attr.manifest]),
            baseline = None,  # TODO(arun) Pass existing baseline
            lint_config = _target_outputs([ctx.attr.lint_config]),
        ),
    ]

lint_sources = rule(
    implementation = _lint_sources_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "resources": attr.label_list(allow_files = True),
        "manifest": attr.label(allow_single_file = True),
        "target": attr.string(),
        "lint_config": attr.label(
            doc = "Lint config xml",
            allow_single_file = True,
            cfg = "target",
            default = Label(":lint_config.xml"),
        ),
        "_empty_jar": attr.label(
            doc = """Empty jar for exporting JavaInfos.""",
            allow_single_file = True,
            cfg = "target",
            default = Label("//third_party:empty.jar"),
        ),
        # TODO(arun) add assets
    },
    provides = [
        JavaInfo,
        AndroidLintSourcesInfo,
    ],
)
