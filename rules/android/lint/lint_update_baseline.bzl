load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintInfo")

def _lint_update_baseline(ctx):
    target = ctx.attr.target
    executable = ctx.actions.declare_file("lint/%s_update_baseline.sh" % target.label.name)

    updated_internal_baseline = ctx.attr.target[AndroidLintInfo].info.updated_baseline
    source_baseline = ctx.files.baseline[0]

    ctx.actions.write(
        output = executable,
        is_executable = False,
        content = """
        #!/bin/bash
        cp -rf {source} $BUILD_WORKING_DIRECTORY/{target}
        echo "$(tput setaf 2)Updated {target} $(tput sgr0)"
                """.format(
            source = updated_internal_baseline.short_path,
            target = source_baseline.short_path,
        ),
    )
    return [
        DefaultInfo(
            executable = executable,
            runfiles = ctx.runfiles(files = [updated_internal_baseline]),
        ),
    ]

lint_update_baseline = rule(
    implementation = _lint_update_baseline,
    executable = True,
    attrs = {
        "baseline": attr.label(allow_single_file = True, mandatory = False),
        "target": attr.label(
            doc = "The lint target to use for getting the updated baseline",
            providers = [AndroidLintInfo],
            mandatory = True,
        ),
    },
)
