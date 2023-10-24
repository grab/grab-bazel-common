AndroidLintInfo = provider(
    fields = {
        "name": "Name of the target",
        "android": "True if this is an Android module",
        "library": "True if this is a library module",
        "enabled": "True if linter is supposed to run on this target",
        "partial_results_dir": "The partial results dir for library modules",
        "lint_result_xml": "The lint result XML",
        "generator_name": "The macro that generated this target",
        "srcs": "The sources of this target",
        "resource_files": "The resources of this target",
    },
)

_LINT_ASPECTS_ATTR = ["deps", "runtime_deps", "exports"]

def _get_target_outputs(target, generator_name):
    result = []
    if type(target) == "Target":
        for file in target.files.to_list():
            result.append(struct(
                file = file,
                generator_name = generator_name,
            ))
    return result

def _get_files(attr, source_attribute):
    """
    Ensure sources are files. For any of non file entry, try to extract it from output group
    """
    generator_name = attr.generator_name
    raw_values = getattr(attr, source_attribute, [])
    result = []
    if type(raw_values) == "list":
        for val in raw_values:
            if type(val) == "Target":
                result.extend(_get_target_outputs(val, generator_name))
            elif type(val) == "File":
                result.append(
                    struct(
                        file = val,
                        generator_name = generator_name,
                    ),
                )
    elif type(raw_values) == "Target":
        result.extend(_get_target_outputs(raw_values, generator_name))
    return result

def _transitive_sources(target, ctx, source_attribute):
    return depset(
        _get_files(ctx.rule.attr, source_attribute),
        transitive = [
            getattr(t[AndroidLintInfo], source_attribute)
            for attr in _LINT_ASPECTS_ATTR
            for t in getattr(ctx.rule.attr, attr, [])
            if AndroidLintInfo in t
        ],
    )

def _direct_sources(transitive_sources, generator_name):
    results = []
    for src_data in transitive_sources.to_list():
        if src_data.generator_name == generator_name:
            results.append(src_data.file)
    return results

def _classpath(target):
    classpath = depset()
    if JavaInfo in target:
        classpath = depset(transitive = [
            classpath,
            target[JavaInfo].transitive_runtime_jars,
            target[JavaInfo].transitive_compile_time_jars,
        ])
    return classpath

def _dep_lint_infos(ctx):
    result = []
    for target in ctx.rule.attr.deps:
        if AndroidLintInfo in target:
            lint_info = target[AndroidLintInfo]
            if (lint_info.enabled or True):
                result.append({
                    "module": str(target.label).lstrip("@"),
                    "android": lint_info.android,
                    "library": lint_info.library,
                    "partial_results_dir": lint_info.partial_results_dir,
                    "lint_result_xml": lint_info.lint_result_xml,
                })
    return result

def _collect_partial_results(ctx):
    return [
        t[AndroidLintInfo].partial_results_dir
        for attr in _LINT_ASPECTS_ATTR
        for t in getattr(ctx.rule.attr, attr, [])
        if AndroidLintInfo in t and t[AndroidLintInfo].enabled
    ]

def _module_xml_content(name, android, library, partial_results_dir):
    return "<module name=\"{0}\" android=\"{1}\" library=\"{2}\" partial-results-dir=\"{3}\">\n".format(
        name,
        android,
        library,
        partial_results_dir,
    )

def _project_xml_content(
        name = None,
        android = True,
        library = False,
        srcs = [],
        resources = [],
        is_test = False,
        classpath = [],
        manifest = [],
        merged_manifest = [],
        partial_results_dir = None,
        dep_lint_infos = []):
    project_xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
    project_xml += "<project>\n"
    project_xml += _module_xml_content(
        name,
        android,
        library,
        partial_results_dir.path,
    )
    for file in srcs:
        project_xml += "  <src file=\"{0}\" ".format(file.path)
        if is_test:
            project_xml += "test=\"true\" "
        project_xml += "/>\n"
    for file in resources:
        project_xml += "  <resource file=\"{0}\"/>\n".format(file.path)
    for file in classpath.to_list():
        project_xml += "  <classpath jar=\"{0}\" />\n".format(file.path)

    if len(manifest) != 0:
        project_xml += "  <manifest file=\"{0}\"/>\n".format(manifest[0].file.path)
    if len(merged_manifest) != 0:
        project_xml += "  <merged-manifest file=\"{0}\"/>\n".format(merged_manifest[0].file.path)

    for dep_info in dep_lint_infos:
        project_xml += "  <dep module=\"{0}\" />\n".format(dep_info["module"])
    project_xml += "</module>\n"

    # Dependency info
    for dep_info in dep_lint_infos:
        project_xml += _module_xml_content(
            dep_info["module"],
            dep_info["android"],
            dep_info["library"],
            dep_info["partial_results_dir"].path,
        )
        project_xml += "</module>\n"

    project_xml += "</project>\n"
    return project_xml

def _lint_config_content():
    lint_config_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    lint_config_xml += "<lint checkTestSources=\"true\">"
    lint_config_xml += "</lint>"
    return lint_config_xml

def _lint_action(
        ctx,
        project_xml_file,
        lint_result_xml_file,
        lint_config_xml_file,
        partial_results_dir,
        inputs):
    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--project-xml", project_xml_file.path)
    args.add("--output-xml", lint_result_xml_file.path)
    args.add("--lint-config", lint_config_xml_file.path)
    args.add("--partial-results", partial_results_dir.path)

    mnemonic = "AndroidLint"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = inputs,
        outputs = [
            partial_results_dir,
            lint_result_xml_file,
        ],
        executable = ctx.executable._lint_cli,
        arguments = [args],
        progress_message = "%s %s" % (mnemonic, str(ctx.label).lstrip("@")),
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
        },
    )
    return

def _lint_aspect_impl(target, ctx):
    if target.label.workspace_root.startswith("external"):
        # Run lint only on internal targets
        return []
    else:
        # Lint is enabled only for top level module target generated by the macro
        enabled = target.label.name == ctx.rule.attr.generator_name

        # Output
        partial_results_dir = ctx.actions.declare_directory(target.label.name + "_partial_results_dir")
        lint_result_xml_file = ctx.actions.declare_file(target.label.name + "_lint_result.xml")

        # Data
        transitive_srcs = _transitive_sources(target, ctx, "srcs")
        transitive_resources = _transitive_sources(target, ctx, "resource_files")
        generator_name = ctx.rule.attr.generator_name
        rule_kind = ctx.rule.kind
        android = rule_kind == "android_library" or rule_kind == "android_binary" or True
        library = rule_kind != "android_binary"
        is_test = rule_kind.endswith("_test")
        srcs = _direct_sources(transitive_srcs, generator_name)
        resources = _direct_sources(transitive_resources, generator_name)

        if enabled and len(srcs + resources) != 0:
            manifest = _get_files(ctx.rule.attr, "manifest")
            classpath = _classpath(target)
            dep_lint_infos = _dep_lint_infos(ctx)  # Collect dependencies' lint data

            project_xml_file = ctx.actions.declare_file(target.label.name + "_project.xml")
            lint_config_xml_file = ctx.actions.declare_file(target.label.name + "_lint_config.xml")

            project_xml = _project_xml_content(
                name = ctx.label.name,
                android = str(android).lower(),
                library = str(library).lower(),
                srcs = srcs,
                resources = resources,
                is_test = is_test,
                classpath = classpath,
                manifest = manifest,
                merged_manifest = manifest,
                partial_results_dir = partial_results_dir,
                dep_lint_infos = dep_lint_infos,
            )
            ctx.actions.write(output = project_xml_file, content = project_xml)
            ctx.actions.write(output = lint_config_xml_file, content = _lint_config_content())

            # Lint Action
            _lint_action(
                ctx,
                project_xml_file,
                lint_result_xml_file,
                lint_config_xml_file,
                partial_results_dir,
                inputs = depset(
                    [project_xml_file] +
                    [dep_info["partial_results_dir"] for dep_info in dep_lint_infos] +
                    [lint_config_xml_file] +
                    srcs +
                    resources,
                    transitive = [classpath],
                ),
            )

            return [
                AndroidLintInfo(
                    name = target.label.name,
                    android = android,
                    library = library,
                    enabled = enabled,
                    partial_results_dir = partial_results_dir,
                    lint_result_xml = lint_result_xml_file,
                    generator_name = generator_name,
                    srcs = transitive_srcs,
                    resource_files = transitive_resources,
                ),
            ]
        else:
            # No linting to do, just propagate transitive data
            ctx.actions.run_shell(
                outputs = [partial_results_dir],
                mnemonic = "GenLintPartialResults",
                command = ("mkdir -p %s" % (partial_results_dir.path)),
            )
            ctx.actions.write(output = lint_result_xml_file, content = "")
            return [
                AndroidLintInfo(
                    name = target.label.name,
                    android = android,
                    library = library,
                    enabled = enabled,
                    partial_results_dir = partial_results_dir,
                    lint_result_xml = lint_result_xml_file,
                    generator_name = generator_name,
                    srcs = transitive_srcs,
                    resource_files = transitive_resources,
                ),
            ]

lint_aspect = aspect(
    implementation = _lint_aspect_impl,
    attr_aspects = ["deps", "exports", "runtime_deps"],  # Define attributes that aspect will propagate to
    attrs = {
        "_lint_cli": attr.label(
            executable = True,
            cfg = "target",
            default = Label("//tools/lint:lint_cli"),
        ),
    },
)

def _lint_impl(ctx):
    ctx.actions.symlink(
        target_file = ctx.attr.target[AndroidLintInfo].lint_result_xml,
        output = ctx.outputs.lint_result,
    )
    return [DefaultInfo(files = depset([ctx.outputs.lint_result]))]

lint = rule(
    implementation = _lint_impl,
    attrs = {
        "target": attr.label(aspects = [lint_aspect]),
        "_lint_cli": attr.label(
            executable = True,
            cfg = "target",
            default = Label("//tools/lint:lint_cli"),
        ),
    },
    outputs = {
        "lint_result": "%{name}_result.xml",
    },
)
