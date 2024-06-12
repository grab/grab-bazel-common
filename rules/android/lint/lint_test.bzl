load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintInfo")

def _lint_test_impl(ctx):
    target = ctx.attr.target
    lint_info = ctx.attr.target[AndroidLintInfo].info

    executable = ctx.actions.declare_file("%s_lint.sh" % target.label.name)

    lint_junit_xml_file = lint_info.lint_junit_xml
    lint_result_xml_file = ctx.outputs.lint_result
    lint_result_code = lint_info.result_code
    if lint_info.enabled:
        ctx.actions.symlink(
            target_file = lint_info.lint_result_xml,
            output = lint_result_xml_file,
        )
    else:
        lint_junit_xml_file = ctx.actions.declare_file("%s_junit.xml" % target.label.name)
        ctx.actions.write(output = lint_junit_xml_file, content = "")

        ctx.actions.write(output = lint_result_xml_file, content = "")

        lint_result_code = ctx.actions.declare_file("%s_result_code" % target.label.name)
        ctx.actions.write(output = lint_result_code, content = "0")

    ctx.actions.write(
        output = executable,
        is_executable = False,
        content = """
#!/bin/bash
cat {lint_result_xml_file}
cp {lint_junit_xml_file} $XML_OUTPUT_FILE
exit $(cat {lint_result_code})
            """.format(
            lint_result_xml_file = lint_result_xml_file.short_path,
            lint_junit_xml_file = lint_junit_xml_file.short_path,
            lint_result_code = lint_result_code.short_path,
        ),
    )

    return [
        DefaultInfo(
            executable = executable,
            runfiles = ctx.runfiles(files = [
                lint_junit_xml_file,
                lint_result_xml_file,
                lint_result_code,
            ]),
            files = depset([
                ctx.outputs.lint_result,
            ]),
        ),
    ]

lint_test = rule(
    implementation = _lint_test_impl,
    attrs = {
        "target": attr.label(
            doc = "The lint target to use for calculating test result",
            providers = [AndroidLintInfo],
            mandatory = True,
        ),
    },
    test = True,
    outputs = dict(
        lint_result = "%{name}_result.xml",
    ),
)
