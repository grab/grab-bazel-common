load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintInfo")

def _lint_inspector(ctx):
    target = ctx.attr.target

    lint_checks = ctx.attr.lint_target[AndroidLintInfo].inspect_info.lint_checks
    aars_dirs = ctx.attr.lint_target[AndroidLintInfo].inspect_info.aars_dirs

    executable = ctx.actions.declare_file("lint/%s_inspect.sh" % target.label.name)
    lint_rules = ctx.actions.declare_file("lint/%s_lint_rules.json" % target.label.name)

    ctx.actions.write(
        output = executable,
        content = """
                    #!/bin/bash
                    echo "\nlint_rules: $(realpath {lint_rules})"
                            """.format(
            lint_rules = lint_rules.short_path,
        ),
    )

    mnemonic = "AndroidLintInspector"
    args = ctx.actions.args()

    args.add_joined(
        "--aars-dirs",
        aars_dirs,
        join_with = ",",
        map_each = utils.to_path,
    )
    args.add_joined(
        "--lint-checks",
        lint_checks,
        join_with = ",",
        map_each = utils.to_path,
    )
    args.add("--output", lint_rules.path)

    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(
            lint_checks + aars_dirs,
        ),
        outputs = [lint_rules],
        executable = ctx.executable._lint_inspector_cli,
        arguments = [args],
        progress_message = "%s %s" % (mnemonic, str(ctx.label).lstrip("@")),
    )

    return [
        DefaultInfo(
            executable = executable,
            runfiles = ctx.runfiles(files = [lint_rules]),
            files = depset([lint_rules]),
        ),
    ]

lint_inspector = rule(
    implementation = _lint_inspector,
    executable = True,
    attrs = {
        "_lint_inspector_cli": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("//tools/lint:lint_inspector"),
        ),
        "target": attr.label(
            doc = "name of the android/kotlin/java target",
        ),
        "lint_target": attr.label(
            doc = "The lint target to inspect",
            providers = [AndroidLintInfo],
            mandatory = True,
        ),
    },
)
