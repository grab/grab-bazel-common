load("@grab_bazel_common//rules/android:utils.bzl", "utils")

_LINT_ASPECTS_ATTR = ["deps", "runtime_deps", "exports", "associates"]

def _classpath(target):
    classpath = depset()
    if JavaInfo in target:
        classpath = depset(
            transitive = [
                classpath,
                target[JavaInfo].transitive_runtime_jars,
                target[JavaInfo].transitive_compile_time_jars,
            ],
        )
    return classpath

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

# Pass this via lint.xml from rule
def _lint_config_content():
    lint_config_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    lint_config_xml += "<lint checkTestSources=\"true\">"
    lint_config_xml += "</lint>"
    return lint_config_xml

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

def _lint_aspect_impl(aspect_ctx):
    return []

lint_aspect = aspect(
    implementation = _lint_aspect_impl,
    attr_aspects = _LINT_ASPECTS_ATTR,  # Define attributes that aspect will propagate to
    attrs = {
        "_lint_cli": attr.label(
            executable = True,
            cfg = "target",
            default = Label("//tools/lint:lint_cli"),
        ),
    },
)

def _lint_impl(ctx):
    # Collect sources and perform lint
    target = ctx.attr.target

    # Outputs
    partial_results_dir = ctx.actions.declare_directory("lint/" + target.label.name + "_partial_results_dir")
    lint_result_xml_file = ctx.outputs.lint_result
    lint_config_xml_file = ctx.actions.declare_file("lint/" + target.label.name + "_lint_config.xml")
    ctx.actions.write(output = lint_config_xml_file, content = _lint_config_content())

    # Inputs
    srcs = ctx.files.srcs
    resources = ctx.files.resources
    manifest = ctx.files.manifest
    classpath = _classpath(target)
    merged_manifest = []
    if AndroidManifestInfo in target:
        merged_manifest.append(target[AndroidManifestInfo].manifest)
    compile_sdk_version = _compile_sdk_version(ctx.attr._android_sdk)

    _lint_action(
        ctx = ctx,
        android = True,
        library = False,
        compile_sdk_version = compile_sdk_version,
        srcs = srcs,
        resources = resources,
        classpath = classpath,
        manifest = manifest,
        merged_manifest = merged_manifest,
        dep_lint_infos = [],
        lint_config_xml_file = lint_config_xml_file,
        lint_result_xml_file = lint_result_xml_file,
        partial_results_dir = partial_results_dir,
        verbose = True,
        inputs = depset(
            srcs +
            resources +
            manifest +
            merged_manifest +
            [lint_config_xml_file],
            transitive = [classpath],
        ),
    )

    ctx.actions.write(
        output = ctx.outputs.launcher_script,
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
        executable = ctx.outputs.launcher_script,
        runfiles = ctx.runfiles(files = [lint_result_xml_file]),
        files = depset([ctx.outputs.launcher_script, ctx.outputs.lint_result]),
    )]

lint_test = rule(
    implementation = _lint_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "resources": attr.label_list(allow_files = True),
        "manifest": attr.label(allow_single_file = True),
        "target": attr.label(),
        "deps": attr.label(aspects = [lint_aspect]),
        "_lint_cli": attr.label(
            executable = True,
            cfg = "exec",
            default = Label("//tools/lint:lint_cli"),
        ),
        "_android_sdk": attr.label(default = "@androidsdk//:sdk"),  # Use toolchains later
    },
    test = True,
    outputs = dict(
        launcher_script = "%{name}.sh",
        lint_result = "%{name}_result.xml",
    ),
)
