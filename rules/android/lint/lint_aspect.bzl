load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load(
    "@grab_bazel_common//rules/android/lint:providers.bzl",
    "AndroidLintInfo",
    "AndroidLintInspectInfo",
    "AndroidLintNodeInfo",
    "AndroidLintSourcesInfo",
    "LINT_ENABLED",
)
load(
    "@rules_android//providers:providers.bzl",
    "AndroidSdkInfo",
    "AndroidLibraryResourceClassJarProvider",
    "AndroidIdeInfo",
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

def _sdk_versions(ctx):
    """
    Return min and target sdk version inferred from manifest_values
    """
    is_binary = ctx.rule.kind == "android_binary"
    if is_binary:
        manifest_values = ctx.rule.attr.manifest_values
        return struct(
            min_sdk = manifest_values.get("minSdkVersion", None),
            target_sdk = manifest_values.get("targetSdkVersion", None),
        )
    else:
        return struct(
            min_sdk = None,
            target_sdk = None,
        )

def _res_config(ctx):
    """
    Return resource_configuration_filters if it is android_binary
    """
    is_binary = ctx.rule.kind == "android_binary"
    if is_binary:
        return ctx.rule.attr.resource_configuration_filters
    else:
        return None

def _package_name(ctx):
    """
    Return the package name of an android_binary target
    """
    is_binary = ctx.rule.kind == "android_binary"
    if is_binary:
        return ctx.rule.attr.custom_package
    else:
        return None

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
    merged_manifest = (
        [target[AndroidIdeInfo].generated_manifest] 
        if AndroidIdeInfo in target 
            and not library 
            and target[AndroidIdeInfo].generated_manifest != None 
        else []
    )

    sources = [
        struct(
            srcs = dep[AndroidLintSourcesInfo].srcs,
            resources = dep[AndroidLintSourcesInfo].resources,
            aars = dep[AndroidLintSourcesInfo].aar_deps,
            manifest = dep[AndroidLintSourcesInfo].manifest,
            merged_manifest = merged_manifest,
            baseline = dep[AndroidLintSourcesInfo].baseline,
            lint_config_xml = dep[AndroidLintSourcesInfo].lint_config[0],
            classpath = classpath,
            lint_checks = dep[AndroidLintSourcesInfo].lint_checks,
            fail_on_warning = dep[AndroidLintSourcesInfo].fail_on_warning,
            fail_on_information = dep[AndroidLintSourcesInfo].fail_on_information,
        )
        for dep in (ctx.rule.attr.deps + getattr(ctx.rule.attr, "exports", []))
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
            models_dir = lint_node_info.models_dir,
            lint_result_xml = lint_node_info.lint_result_xml,
        )
        for lint_node_info in transitive_lint_node_infos.to_list()
        if lint_node_info.enabled
    ]

def _encode_dependency(dependency_info):
    """
    Flatten the dependency details in a string to simplify arguments to Lint ClI
    """
    return "%s^%s^%s^%s^%s" % (
        dependency_info.module,
        dependency_info.android,
        dependency_info.library,
        dependency_info.partial_results_dir.path,
        dependency_info.models_dir.path,
    )

def _encode_aars(aar_info):
    """
    Flatten the dependency details in a string to simplify arguments to Lint ClI
    """
    return "%s^%s" % (
        aar_info.aar.path,
        aar_info.aar_dir.path,
    )

def _lint_common_args(
        ctx,
        args,
        android,
        library,
        package_name,
        compile_sdk_version,
        min_sdk_version,
        target_sdk_version,
        srcs,
        resources,
        res_config,
        aars,
        aar_infos,
        lint_checks,
        classpath,
        manifest,
        merged_manifest,
        baseline,
        dep_lint_node_infos,
        lint_config_xml_file,
        partial_results_dir,
        project_xml_file,
        models_dir,
        jdk_home,
        verbose):
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--name", str(ctx.label).lstrip("@"))
    if android:
        args.add("--android")
    if library:
        args.add("--library")
    if compile_sdk_version:
        args.add("--compile-sdk-version", compile_sdk_version)
    if min_sdk_version:
        args.add("--min-sdk-version", min_sdk_version)
    if target_sdk_version:
        args.add("--target-sdk-version", target_sdk_version)
    if res_config:
        args.add_joined("--res-configs", res_config, join_with = ",")
    if package_name:
        args.add("--package-name", package_name)

    args.add_joined(
        "--sources",
        srcs,
        join_with = ",",
        map_each = utils.to_path,
    )
    args.add_joined(
        "--aar_dirs",
        aar_infos,
        join_with = ",",
        map_each = _encode_aars,
    )
    args.add_joined(
        "--lint_checks",
        lint_checks,
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
    args.add("--models-dir", models_dir.path)

    if verbose:  #TODO(arun) Pass via build config
        args.add("--verbose")

    args.add("--jdk-home", jdk_home)

    args.add("--project-xml", project_xml_file.path)
    return

def _lint_analyze_action(
        ctx,
        android,
        library,
        package_name,
        compile_sdk_version,
        min_sdk_version,
        target_sdk_version,
        srcs,
        resources,
        res_config,
        aars,
        aar_infos,
        lint_checks,
        classpath,
        manifest,
        merged_manifest,
        dep_lint_node_infos,
        baseline,
        lint_config_xml_file,
        partial_results_dir,
        jdk_home,
        project_xml_file,
        models_dir,
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
        package_name = package_name,
        compile_sdk_version = compile_sdk_version,
        min_sdk_version = min_sdk_version,
        target_sdk_version = target_sdk_version,
        srcs = srcs,
        resources = resources,
        res_config = res_config,
        aars = aars,
        aar_infos = aar_infos,
        lint_checks = lint_checks,
        classpath = classpath,
        manifest = manifest,
        merged_manifest = merged_manifest,
        baseline = baseline,
        dep_lint_node_infos = dep_lint_node_infos,
        lint_config_xml_file = lint_config_xml_file,
        partial_results_dir = partial_results_dir,
        models_dir = models_dir,
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
        env = {
            "LINT_PRINT_STACKTRACE": "true",
        },
    )
    return

def _lint_report_action(
        ctx,
        android,
        library,
        package_name,
        compile_sdk_version,
        min_sdk_version,
        target_sdk_version,
        srcs,
        resources,
        res_config,
        aars,
        aar_infos,
        lint_checks,
        classpath,
        manifest,
        merged_manifest,
        dep_lint_node_infos,
        baseline,
        updated_baseline,
        lint_config_xml_file,
        fail_on_warning,
        fail_on_information,
        lint_result_xml_file,
        lint_result_junit_xml_file,
        partial_results_dir,
        jdk_home,
        project_xml_file,
        models_dir,
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
        package_name = package_name,
        compile_sdk_version = compile_sdk_version,
        min_sdk_version = min_sdk_version,
        target_sdk_version = target_sdk_version,
        srcs = srcs,
        resources = resources,
        res_config = res_config,
        aars = aars,
        aar_infos = aar_infos,
        lint_checks = lint_checks,
        classpath = classpath,
        manifest = manifest,
        merged_manifest = merged_manifest,
        baseline = baseline,
        dep_lint_node_infos = dep_lint_node_infos,
        lint_config_xml_file = lint_config_xml_file,
        partial_results_dir = partial_results_dir,
        project_xml_file = project_xml_file,
        models_dir = models_dir,
        jdk_home = jdk_home,
        verbose = verbose,
    )
    args.add("--no-create-project-xml")
    args.add("--no-create-models-dir")
    args.add("--updated-baseline", updated_baseline)
    args.add("--fail-on-warning", fail_on_warning)
    args.add("--fail-on-information", fail_on_information)

    args.add("--output-xml", lint_result_xml_file)
    args.add("--output-junit-xml", lint_result_junit_xml_file)
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
        env = {
            "LINT_PRINT_STACKTRACE": "true",
        },
    )
    return

def _aar_node_infos(aar_deps):
    return [
        aar_node_info
        for aar_node_info in aar_deps.to_list()
        if aar_node_info.aar != None
    ]

def _lint_aspect_impl(target, ctx):
    if target.label.workspace_root.startswith("external"):
        # Run lint only on internal targets
        return []
    else:
        # Current target info
        rule_kind = ctx.rule.kind
        kotlin = rule_kind == "kt_jvm_library"

        #TODO(arun) Remove name based detection and inject value into lint_sources from macro and read it later
        android = rule_kind == "android_library" or rule_kind == "android_binary" or (kotlin and target.label.name.endswith("_kt"))
        library = rule_kind != "android_binary"

        enabled = LINT_ENABLED in ctx.rule.attr.tags and (android or kotlin)

        # Dependency info
        transitive_lint_node_infos = _transitive_lint_node_infos(ctx)

        # Result
        android_lint_node_info = None  # Current target's AndroidLintNodeInfo
        if enabled:
            # Output - Start
            lint_updated_baseline_file = ctx.actions.declare_file("lint/" + target.label.name + "_updated_baseline.xml")
            lint_partial_results_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_partial_results_dir")
            lint_results_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_results_dir")

            lint_result_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result.xml")
            lint_result_junit_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result_junit.xml")
            lint_result_code_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result_code")

            # Project Xmls
            project_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_project.xml")

            # Models
            lint_models_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_models_dir")
            # Output - End

            sources = _collect_sources(target, ctx, library)

            lint_checks = [jar.files.to_list()[0] for jar in sources.lint_checks]

            compile_sdk_version = _compile_sdk_version(ctx.attr._android_sdk)
            sdk_versions = _sdk_versions(ctx)
            res_config = _res_config(ctx)
            android_package = _package_name(ctx)
            dep_lint_node_infos = _dep_lint_node_infos(target, transitive_lint_node_infos)
            dep_partial_results = [info.partial_results_dir for info in dep_lint_node_infos]
            dep_lint_models = [info.models_dir for info in dep_lint_node_infos]

            aar_node_infos = _aar_node_infos(sources.aars)
            aars = [info.aar for info in aar_node_infos]
            aars_dirs = [info.aar_dir for info in aar_node_infos]

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
            #   -baseline                 -partial-results-dir
            #   -lint_result.xml          -project-xml
            #                             -models-dir
            # Add separate actions for reporting and analyze and then return different providers in the aspect.
            # Consuming rules can decide whether to use reporting output or analysis output by using the
            # correct output file

            _lint_analyze_action(
                ctx = ctx,
                android = android,
                library = library,
                package_name = android_package,
                compile_sdk_version = compile_sdk_version,
                min_sdk_version = sdk_versions.min_sdk,
                target_sdk_version = sdk_versions.target_sdk,
                srcs = sources.srcs,
                resources = sources.resources,
                res_config = res_config,
                aars = aars,
                aar_infos = aar_node_infos,
                lint_checks = lint_checks,
                classpath = sources.classpath,
                manifest = sources.manifest,
                merged_manifest = sources.merged_manifest,
                dep_lint_node_infos = dep_lint_node_infos,
                baseline = sources.baseline,
                lint_config_xml_file = sources.lint_config_xml,
                partial_results_dir = lint_partial_results_dir,
                jdk_home = java_runtime_info.java_home,
                project_xml_file = project_xml_file,
                models_dir = lint_models_dir,
                verbose = False,
                inputs = depset(
                    sources.srcs +
                    sources.resources +
                    aars +
                    aars_dirs +
                    sources.manifest +
                    sources.merged_manifest +
                    [sources.lint_config_xml] +
                    dep_partial_results +
                    dep_lint_models +
                    baseline_inputs +
                    lint_checks,
                    transitive = [sources.classpath, java_runtime_info.files],
                ),
                outputs = [
                    lint_models_dir,
                    lint_partial_results_dir,
                    project_xml_file,
                ],
            )

            _lint_report_action(
                ctx = ctx,
                android = android,
                library = library,
                package_name = android_package,
                compile_sdk_version = compile_sdk_version,
                min_sdk_version = sdk_versions.min_sdk,
                target_sdk_version = sdk_versions.target_sdk,
                srcs = sources.srcs,
                resources = sources.resources,
                res_config = res_config,
                aars = aars,
                aar_infos = aar_node_infos,
                lint_checks = lint_checks,
                classpath = sources.classpath,
                manifest = sources.manifest,
                merged_manifest = sources.merged_manifest,
                dep_lint_node_infos = dep_lint_node_infos,
                baseline = sources.baseline,
                updated_baseline = lint_updated_baseline_file,
                lint_config_xml_file = sources.lint_config_xml,
                fail_on_warning = sources.fail_on_warning,
                fail_on_information = sources.fail_on_information,
                lint_result_xml_file = lint_result_xml_file,
                lint_result_junit_xml_file = lint_result_junit_xml_file,
                partial_results_dir = lint_results_dir,
                models_dir = lint_models_dir,
                jdk_home = java_runtime_info.java_home,
                project_xml_file = project_xml_file,
                result_code = lint_result_code_file,
                verbose = False,
                inputs = depset(
                    sources.srcs +
                    sources.resources +
                    aars +
                    aars_dirs +
                    sources.manifest +
                    sources.merged_manifest +
                    [sources.lint_config_xml] +
                    dep_partial_results +
                    dep_lint_models +
                    [lint_partial_results_dir] +  # Current module partial results from analyze action
                    [lint_models_dir] +  # Current module models from analyze action
                    [project_xml_file] +  # Reuse project xml from analyze action
                    baseline_inputs +
                    lint_checks,
                    transitive = [sources.classpath, java_runtime_info.files],
                ),
                outputs = [
                    lint_results_dir,
                    lint_result_xml_file,
                    lint_result_junit_xml_file,
                    lint_updated_baseline_file,
                    lint_result_code_file,
                ],
            )
            android_lint_inspect_info = AndroidLintInspectInfo(lint_checks = lint_checks, aars_dirs = aars_dirs)
            android_lint_node_info = AndroidLintNodeInfo(
                name = str(target.label),
                android = android,
                library = library,
                enabled = enabled,
                partial_results_dir = lint_partial_results_dir,
                models_dir = lint_models_dir,
                lint_result_xml = lint_result_xml_file,
                lint_junit_xml = lint_result_junit_xml_file,
                result_code = lint_result_code_file,
                updated_baseline = lint_updated_baseline_file,
            )
        else:
            android_lint_node_info = AndroidLintNodeInfo(
                name = str(target.label),
                android = android,
                library = library,
                enabled = enabled,
                partial_results_dir = None,
                lint_result_xml = None,
                lint_junit_xml = None,
                result_code = None,
                updated_baseline = None,
            )
            android_lint_inspect_info = AndroidLintInspectInfo(
                lint_checks = None,
                aars_dirs = None,
            )

        return AndroidLintInfo(
            info = android_lint_node_info,
            inspect_info = android_lint_inspect_info,
            transitive_nodes = depset(
                [android_lint_node_info],
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
            default = "@rules_java//toolchains:current_java_runtime",
        ),
    },
    provides = [
        #AndroidLintInfo,
    ],
)
