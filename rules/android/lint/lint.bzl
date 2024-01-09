load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load(
    "@grab_bazel_common//rules/android/lint:providers.bzl",
    "AndroidLintInfo",
    "AndroidLintNodeInfo",
    "AndroidLintSourcesInfo",
    "LINT_ENABLED",
)

_LINT_ASPECTS_ATTR = ["deps", "runtime_deps", "exports", "associates"]  # Define attributes that aspect will propagate to

def _compile_sdk_version(sdk_target):
    """
    Detect the compile_sdk_version based on android jar path
    """
    android_jar = sdk_target[AndroidSdkInfo].android_jar.path
    if not android_jar.startswith("external/androidsdk/platforms/android-"):
        return None
    if not android_jar.endswith("/android.jar"):
        return None
    level = android_jar.removeprefix("external/androidsdk/platforms/android-")
    level = level.removesuffix("/android.jar")
    return level

def _lint_sources_classpath(target, ctx):
    """
    Collect the classpath for linting. Currently all transitive jars are passed since
    sometimes lint complains about missing class defs. Need to revisit to prune transitive
    deps for performance.

    Apart from dependency jars, add the current target's android resource jar as well.
    """
    transitive = [
        dep[JavaInfo].transitive_compile_time_jars
        for dep in ctx.rule.attr.deps
        if JavaInfo in dep
    ]
    if AndroidLibraryResourceClassJarProvider in target:
        transitive.append(target[AndroidLibraryResourceClassJarProvider].jars)
    return depset(transitive = transitive)

def _collect_sources(target, ctx, library):
    """
    Relies on lint_sources target being in the dependencies to collect sources for performing lint. This is added as as clean way
    to collect sources instead of relying on combining results in an aspect.
    """
    classpath = _lint_sources_classpath(target, ctx)
    merged_manifest = [target[AndroidManifestInfo].manifest] if AndroidManifestInfo in target and not library else []
    sources = [
        struct(
            srcs = dep[AndroidLintSourcesInfo].srcs,
            resources = dep[AndroidLintSourcesInfo].resources,
            manifest = dep[AndroidLintSourcesInfo].manifest,
            merged_manifest = merged_manifest,
            baseline = dep[AndroidLintSourcesInfo].baseline,
            lint_config_xml = dep[AndroidLintSourcesInfo].lint_config[0],
            classpath = classpath,
        )
        for dep in ctx.rule.attr.deps
        if AndroidLintSourcesInfo in dep
    ]
    if len(sources) > 1:
        fail("Only one lint_sources allowed as dependency")
    return sources[0]

def _transitive_lint_node_infos(ctx):
    return depset(
        transitive = [
            lint_info.transitive_nodes
            for lint_info in utils.collect_providers(
                AndroidLintInfo,
                getattr(ctx.rule.attr, "deps", []),
                getattr(ctx.rule.attr, "exports", []),
            )
        ],
    )

def _dep_lint_node_infos(target, transitive_lint_node_infos):
    """
    From the transitive lint node infos, only process targets which have lint enabled
    and return a struct containing all data needed for lint dependencies
    """
    return [
        struct(
            module = str(lint_node_info.name).lstrip("@"),
            android = lint_node_info.android,
            library = lint_node_info.library,
            partial_results_dir = lint_node_info.partial_results_dir,
            lint_result_xml = lint_node_info.lint_result_xml,
        )
        for lint_node_info in transitive_lint_node_infos.to_list()
        if lint_node_info.enabled
    ]

def _encode_dependency(dependency_info):
    """
    Flatten the dependency details in a string to simplify arguments to Lint ClI
    """
    return "%s^%s^%s^%s" % (
        dependency_info.module,
        dependency_info.android,
        dependency_info.library,
        dependency_info.partial_results_dir.path,
    )

def _lint_common_args(
        ctx,
        args,
        android,
        library,
        compile_sdk_version,
        srcs,
        resources,
        classpath,
        manifest,
        merged_manifest,
        baseline,
        dep_lint_node_infos,
        lint_config_xml_file,
        partial_results_dir,
        project_xml_file,
        jdk_home,
        verbose):
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--name", ctx.label.name)
    if android:
        args.add("--android")
    if library:
        args.add("--library")
    if compile_sdk_version:
        args.add("--compile-sdk-version", compile_sdk_version)

    args.add_joined(
        "--sources",
        srcs,
        join_with = ",",
        map_each = utils.to_path,
    )
    args.add_joined(
        "--resource-files",
        resources,
        join_with = ",",
        map_each = utils.to_path,
    )
    args.add_joined(
        "--classpath",
        classpath,
        join_with = ",",
        map_each = utils.to_path,
    )

    args.add_joined(
        "--dependencies",
        dep_lint_node_infos,
        join_with = ",",
        map_each = _encode_dependency,
    )

    if len(manifest) != 0:
        args.add("--manifest", manifest[0].path)
    if len(merged_manifest) != 0:
        args.add("--merged-manifest", merged_manifest[0].path)

    if baseline:
        args.add("--baseline", baseline[0].path)

    args.add("--lint-config", lint_config_xml_file.path)
    args.add("--partial-results-dir", partial_results_dir.path)

    if verbose:  #TODO(arun) Pass via build config
        args.add("--verbose")

    args.add("--jdk-home", jdk_home)

    args.add("--project-xml", project_xml_file.path)
    return

def _lint_analyze_action(
        ctx,
        android,
        library,
        compile_sdk_version,
        srcs,
        resources,
        classpath,
        manifest,
        merged_manifest,
        dep_lint_node_infos,
        baseline,
        lint_config_xml_file,
        partial_results_dir,
        jdk_home,
        project_xml_file,
        verbose,
        inputs,
        outputs):
    args = ctx.actions.args()
    args.add("ANALYZE")
    _lint_common_args(
        ctx = ctx,
        args = args,
        android = android,
        library = library,
        compile_sdk_version = compile_sdk_version,
        srcs = srcs,
        resources = resources,
        classpath = classpath,
        manifest = manifest,
        merged_manifest = merged_manifest,
        baseline = baseline,
        dep_lint_node_infos = dep_lint_node_infos,
        lint_config_xml_file = lint_config_xml_file,
        partial_results_dir = partial_results_dir,
        project_xml_file = project_xml_file,
        jdk_home = jdk_home,
        verbose = verbose,
    )

    mnemonic = "AndroidLintAnalyze"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = inputs,
        outputs = outputs,
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

def _lint_report_action(
        ctx,
        android,
        library,
        compile_sdk_version,
        srcs,
        resources,
        classpath,
        manifest,
        merged_manifest,
        dep_lint_node_infos,
        baseline,
        updated_baseline,
        lint_config_xml_file,
        lint_result_xml_file,
        partial_results_dir,
        jdk_home,
        project_xml_file,
        result_code,
        verbose,
        inputs,
        outputs):
    args = ctx.actions.args()
    args.add("REPORT")
    _lint_common_args(
        ctx = ctx,
        args = args,
        android = android,
        library = library,
        compile_sdk_version = compile_sdk_version,
        srcs = srcs,
        resources = resources,
        classpath = classpath,
        manifest = manifest,
        merged_manifest = merged_manifest,
        baseline = baseline,
        dep_lint_node_infos = dep_lint_node_infos,
        lint_config_xml_file = lint_config_xml_file,
        partial_results_dir = partial_results_dir,
        project_xml_file = project_xml_file,
        jdk_home = jdk_home,
        verbose = verbose,
    )

    args.add("--updated-baseline", updated_baseline)

    args.add("--output-xml", lint_result_xml_file.path)
    args.add("--result-code", result_code)

    mnemonic = "AndroidLint"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = inputs,
        outputs = outputs,
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
        # Current target info
        rule_kind = ctx.rule.kind
        android = rule_kind == "android_library" or rule_kind == "android_binary"
        library = rule_kind != "android_binary"

        enabled = LINT_ENABLED in ctx.rule.attr.tags and android  # Currently only android targets

        # Dependency info
        transitive_lint_node_infos = _transitive_lint_node_infos(ctx)

        # Result
        android_lint_info = None  # Current target's AndroidLintNodeInfo
        if enabled:
            # Output
            lint_updated_baseline_file = ctx.actions.declare_file("lint/" + target.label.name + "_updated_baseline.xml")
            partial_results_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_partial_results_dir")
            lint_result_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result.xml")
            lint_result_code_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result_code")
            project_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_project.xml")

            sources = _collect_sources(target, ctx, library)
            compile_sdk_version = _compile_sdk_version(ctx.attr._android_sdk)
            dep_lint_node_infos = _dep_lint_node_infos(target, transitive_lint_node_infos)
            partial_results = [info.partial_results_dir for info in dep_lint_node_infos]

            # Inputs
            baseline_inputs = []
            if sources.baseline:
                baseline_inputs.append(sources.baseline[0])

            # Pass JDK Home
            java_runtime_info = ctx.attr._javabase[java_common.JavaRuntimeInfo]

            #  +--------+                +---------+
            #  |        |                |         |
            #  | Report +--------------->| Analyze |
            #  |        |                |         |
            #  +--------+                +---------+
            #   -baseline                 -parital-results-dir
            #   -lint_result.xml          -project-xml
            # Add separate actions for reporting and analyze and then return different providers in the aspect.
            # Consuming rules can decide whether to use reporting output or analysis output by using the
            # correct output file

            _lint_analyze_action(
                ctx = ctx,
                android = android,
                library = library,
                compile_sdk_version = compile_sdk_version,
                srcs = sources.srcs,
                resources = sources.resources,
                classpath = sources.classpath,
                manifest = sources.manifest,
                merged_manifest = sources.merged_manifest,
                dep_lint_node_infos = dep_lint_node_infos,
                baseline = sources.baseline,
                lint_config_xml_file = sources.lint_config_xml,
                partial_results_dir = partial_results_dir,
                jdk_home = java_runtime_info.java_home,
                project_xml_file = project_xml_file,
                verbose = False,
                inputs = depset(
                    sources.srcs +
                    sources.resources +
                    sources.manifest +
                    sources.merged_manifest +
                    [sources.lint_config_xml] +
                    partial_results +
                    baseline_inputs,
                    transitive = [sources.classpath, java_runtime_info.files],
                ),
                outputs = [
                    partial_results_dir,
                    project_xml_file,
                ],
            )

            _lint_report_action(
                ctx = ctx,
                android = android,
                library = library,
                compile_sdk_version = compile_sdk_version,
                srcs = sources.srcs,
                resources = sources.resources,
                classpath = sources.classpath,
                manifest = sources.manifest,
                merged_manifest = sources.merged_manifest,
                dep_lint_node_infos = dep_lint_node_infos,
                baseline = sources.baseline,
                updated_baseline = lint_updated_baseline_file,
                lint_config_xml_file = sources.lint_config_xml,
                lint_result_xml_file = lint_result_xml_file,
                partial_results_dir = partial_results_dir,
                jdk_home = java_runtime_info.java_home,
                project_xml_file = project_xml_file,
                result_code = lint_result_code_file,
                verbose = False,
                inputs = depset(
                    sources.srcs +
                    sources.resources +
                    sources.manifest +
                    sources.merged_manifest +
                    [sources.lint_config_xml] +
                    partial_results +
                    [partial_results_dir] +  # Current module partial results from analyze action
                    [project_xml_file] +  # Reuse project xml from analyze action
                    baseline_inputs,
                    transitive = [sources.classpath, java_runtime_info.files],
                ),
                outputs = [
                    lint_result_xml_file,
                    lint_updated_baseline_file,
                    lint_result_code_file,
                ],
            )

            android_lint_info = AndroidLintNodeInfo(
                name = str(target.label),
                android = android,
                library = library,
                enabled = enabled,
                partial_results_dir = partial_results_dir,
                lint_result_xml = lint_result_xml_file,
                result_code = lint_result_code_file,
                updated_baseline = lint_updated_baseline_file,
            )
        else:
            android_lint_info = AndroidLintNodeInfo(
                name = str(target.label),
                android = android,
                library = library,
                enabled = enabled,
                partial_results_dir = None,
                lint_result_xml = None,
                result_code = None,
                updated_baseline = None,
            )
        return AndroidLintInfo(
            info = android_lint_info,
            transitive_nodes = depset(
                [android_lint_info],
                transitive = [depset(transitive = [transitive_lint_node_infos])],
            ),
        )

lint_aspect = aspect(
    implementation = _lint_aspect_impl,
    attr_aspects = _LINT_ASPECTS_ATTR,
    attrs = {
        "_lint_cli": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("//tools/lint:lint_cli"),
        ),
        "_android_sdk": attr.label(default = "@androidsdk//:sdk"),  # Use toolchains later
        "_javabase": attr.label(
            default = "@bazel_tools//tools/jdk:current_java_runtime",
        ),
    },
    provides = [
        #AndroidLintInfo,
    ],
)

def _lint_impl(ctx):
    target = ctx.attr.target
    lint_result_xml_file = ctx.outputs.lint_result

    # Aspect would have calculated the results already during traversal, simply symlink it
    ctx.actions.symlink(
        target_file = target[AndroidLintInfo].info.lint_result_xml,
        output = ctx.outputs.lint_result,
    )
    return [
        DefaultInfo(
            files = depset([
                ctx.outputs.lint_result,
            ]),
        ),
        target[AndroidLintInfo],  # Forward the provider
    ]

lint = rule(
    implementation = _lint_impl,
    attrs = {
        "target": attr.label(
            doc = "The android_binary or android_library that should be used for implementing linting on",
            aspects = [lint_aspect],
            providers = [
                JavaInfo,
            ],
            mandatory = True,
        ),
        "_lint_cli": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("//tools/lint:lint_cli"),
        ),
        "_android_sdk": attr.label(default = "@androidsdk//:sdk"),  # Use toolchains later
    },
    outputs = dict(
        lint_result = "%{name}_result.xml",
    ),
)
