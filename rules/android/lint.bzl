def _lint_test_impl(ctx):
    classpath = depset()
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            classpath = depset(transitive = [classpath, dep[JavaInfo].transitive_runtime_jars, dep[JavaInfo].transitive_compile_time_jars])

    # TODO Extract dependent modules via a custom provider

    project_xml_file = ctx.actions.declare_file(ctx.label.name + "_project.xml")

    # Create project XML:
    project_xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
    project_xml += "<project>\n"
    project_xml += "<module name=\"{0}\" android=\"false\" library=\"true\">\n".format(ctx.label.name)
    for file in ctx.files.srcs:
        project_xml += "  <src file=\"{0}\" ".format(file.path)
        if ctx.attr.is_test_sources:
            project_xml += "test=\"true\" "
        project_xml += "/>\n"
    for file in ctx.files.resources:
        project_xml += "  <resource file=\"{0}\"/>\n".format(file.path)
    for file in classpath.to_list():
        project_xml += "  <classpath jar=\"{0}\" />\n".format(file.path)
    if ctx.file.manifest != None:
        project_xml += "  <manifest file=\"{0}\"/>\n".format(ctx.file.manifest.path)
    if ctx.file.merged_manifest != None:
        project_xml += "  <merged-manifest file=\"{0}\"/>\n".format(ctx.file.merged_manifest.path)

    project_xml += "</module>\n"
    project_xml += "</project>\n"

    ctx.actions.write(output = project_xml_file, content = project_xml)

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--project-xml", project_xml_file.path)
    args.add("--output-xml", ctx.outputs.lint_result)

    mnemonic = "AndroidLint"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(ctx.files.srcs + ctx.files.resources + [project_xml_file], transitive = [classpath]),
        outputs = [ctx.outputs.lint_result],
        executable = ctx.executable._lint_cli,
        arguments = [args],
        progress_message = "%s %s" % (mnemonic, ctx.label),
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
        },
    )

    return [DefaultInfo(files = depset([ctx.outputs.lint_result]))]

lint = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "resources": attr.label_list(allow_files = True),
        "deps": attr.label_list(allow_files = True),
        "merged_manifest": attr.label(allow_single_file = True),
        "manifest": attr.label(allow_single_file = True),
        "android": attr.bool(),
        "library": attr.bool(),
        "is_test_sources": attr.bool(),
        "_lint_cli": attr.label(
            executable = True,
            cfg = "target",
            default = Label("//tools/lint:lint_cli"),
        ),
    },
    outputs = {
        "lint_result": "%{name}_result.xml",
    },
    implementation = _lint_test_impl,
)
