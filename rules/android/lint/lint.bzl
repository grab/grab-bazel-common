load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintInfo", "AndroidLintSourcesInfo")

_LINT_ASPECTS_ATTR = ["deps", "runtime_deps", "exports", "associates"]

def _encode_dependency(dependency_info):
    return "%s^%s^%s^%s" % (
        dependency_info.module,
        dependency_info.android,
        dependency_info.library,
        dependency_info.partial_results_dir.path,
    )

def _compile_sdk_version(sdk_target):
    android_jar = sdk_target[AndroidSdkInfo].android_jar.path
    if not android_jar.startswith("external/androidsdk/platforms/android-"):
        return None
    if not android_jar.endswith("/android.jar"):
        return None
    level = android_jar.removeprefix("external/androidsdk/platforms/android-")
    level = level.removesuffix("/android.jar")
    return level

def _lint_sources_classpath(target, ctx):
    transitive = [
        dep[JavaInfo].compile_jars
        for dep in ctx.rule.attr.deps
        if JavaInfo in dep
    ]
    if AndroidLibraryResourceClassJarProvider in target:
        transitive.append(target[AndroidLibraryResourceClassJarProvider].jars)
    return depset(transitive = transitive)

def _collect_sources(target, ctx):
    classpath = _lint_sources_classpath(target, ctx)
    merged_manifest = [target[AndroidManifestInfo].manifest] if AndroidManifestInfo in target else []
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

def _transitive_partial_results(target, ctx, partial_results_dir):
    return depset(
        [partial_results_dir],
        transitive = [
            t[AndroidLintInfo].transitive_partial_results_dirs
            for attr in _LINT_ASPECTS_ATTR
            for t in getattr(ctx.rule.attr, attr, [])
            if AndroidLintInfo in t
        ],
    )

def _lint_action(
        ctx,
        android,
        library,
        compile_sdk_version,
        srcs,
        resources,
        classpath,
        manifest,
        merged_manifest,
        dep_lint_infos,
        lint_config_xml_file,
        lint_result_xml_file,
        partial_results_dir,
        verbose,
        inputs):
    args = ctx.actions.args()
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
        dep_lint_infos,
        join_with = ",",
        map_each = _encode_dependency,
    )

    if len(manifest) != 0:
        args.add("--manifest", manifest[0].path)
    if len(merged_manifest) != 0:
        args.add("--merged-manifest", merged_manifest[0].path)

    args.add("--output-xml", lint_result_xml_file.path)
    args.add("--lint-config", lint_config_xml_file.path)
    args.add("--partial-results-dir", partial_results_dir.path)

    if verbose:
        args.add("--verbose")

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
        # Output
        partial_results_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_partial_results_dir")
        lint_result_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_result.xml")

        rule_kind = ctx.rule.kind
        android = rule_kind == "android_library" or rule_kind == "android_binary"
        library = rule_kind != "android_binary"

        enabled = "lint_enabled" in ctx.rule.attr.tags and android  # Currently only android targets

        if enabled:
            sources = _collect_sources(target, ctx)
            compile_sdk_version = _compile_sdk_version(ctx.attr._android_sdk)
            transitive_partial_results_dirs = _transitive_partial_results(target, ctx, partial_results_dir)

            _lint_action(
                ctx = ctx,
                android = android,
                library = library,
                compile_sdk_version = compile_sdk_version,
                srcs = sources.srcs,
                resources = sources.resources,
                classpath = sources.classpath,
                manifest = sources.manifest,
                merged_manifest = sources.merged_manifest,
                dep_lint_infos = [],
                lint_config_xml_file = sources.lint_config_xml,
                lint_result_xml_file = lint_result_xml_file,
                partial_results_dir = partial_results_dir,
                verbose = True,
                inputs = depset(
                    sources.srcs +
                    sources.resources +
                    sources.manifest +
                    sources.merged_manifest +
                    [sources.lint_config_xml],
                    transitive = [sources.classpath],
                ),
            )
            return [
                AndroidLintInfo(
                    name = target.label.name,
                    android = android,
                    library = library,
                    enabled = enabled,
                    partial_results_dir = partial_results_dir,
                    transitive_partial_results_dirs = transitive_partial_results_dirs,
                    lint_result_xml = lint_result_xml_file,
                ),
            ]
        else:
            # No linting to do, just propagate transitive data
            ctx.actions.run_shell(
                outputs = [partial_results_dir],
                command = ("mkdir -p %s" % (partial_results_dir.path)),
            )
            transitive_partial_results_dirs = _transitive_partial_results(target, ctx, partial_results_dir)
            ctx.actions.write(output = lint_result_xml_file, content = "")
            return [
                AndroidLintInfo(
                    name = target.label.name,
                    android = android,
                    library = library,
                    enabled = enabled,
                    partial_results_dir = None,
                    transitive_partial_results_dirs = transitive_partial_results_dirs,
                    lint_result_xml = lint_result_xml_file,
                ),
            ]

lint_aspect = aspect(
    implementation = _lint_aspect_impl,
    attr_aspects = _LINT_ASPECTS_ATTR,  # Define attributes that aspect will propagate to
    attrs = {
        "_lint_cli": attr.label(
            executable = True,
            cfg = "target",
            default = Label("//tools/lint:lint_cli"),
        ),
        "_android_sdk": attr.label(default = "@androidsdk//:sdk"),  # Use toolchains later
    },
)

def _lint_test_impl(ctx):
    target = ctx.attr.target
    lint_result_xml_file = ctx.outputs.lint_result
    executable = ctx.actions.declare_file("%s_lint.sh" % target.label.name)

    # Aspect would have calculated the results already, simply symlink it
    ctx.actions.symlink(
        target_file = ctx.attr.target[AndroidLintInfo].lint_result_xml,
        output = ctx.outputs.lint_result,
    )

    ctx.actions.write(
        output = executable,
        is_executable = False,
        content = """
    #!/bin/bash
    # TODO: Post process lint
    cat {lint_result}
            """.format(
            lint_result = lint_result_xml_file.short_path,
        ),
    )

    return [DefaultInfo(
        executable = executable,
        runfiles = ctx.runfiles(files = [lint_result_xml_file]),
        files = depset([
            ctx.outputs.lint_result,
        ]),
    )]

lint_test = rule(
    implementation = _lint_test_impl,
    attrs = {
        "target": attr.label(aspects = [lint_aspect]),
        "_lint_cli": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("//tools/lint:lint_cli"),
        ),
        "_android_sdk": attr.label(default = "@androidsdk//:sdk"),  # Use toolchains later
    },
    test = True,
    outputs = dict(
        lint_result = "%{name}_result.xml",
    ),
)
