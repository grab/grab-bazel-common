def _generate_manifest_xml_impl(ctx):
    ctx.actions.expand_template(
        template = ctx.file._template,
        output = ctx.outputs.output,
        substitutions = {
            "{{.PACKAGE_NAME}}": ctx.attr.package_name,
            "{{.TARGET_PACKAGE_NAME}}": ctx.attr.target_package_name,
            "{{.INSTRUMENTATION_RUNNER}}": ctx.attr.test_instrumentation_runner,
        },
    )
    return [
        DefaultInfo(files = depset([ctx.outputs.output])),
    ]

generate_manifest_xml = rule(
    implementation = _generate_manifest_xml_impl,
    attrs = {
        "package_name": attr.string(mandatory = True),
        "target_package_name": attr.string(mandatory = True),
        "test_instrumentation_runner": attr.string(mandatory = True),
        "output": attr.output(mandatory = True),
        "_template": attr.label(
            doc = "Android Test Manifest template",
            default = ":AndroidTestManifest.xml.tpl",
            allow_single_file = True,
        ),
    },
)
