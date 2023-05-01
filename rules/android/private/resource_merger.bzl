"""
Rule to merge android variant specific resource folders and account for overrides.
"""

def _to_path(f):
    return f.path

def _resource_merger_impl(ctx):
    outputs = ctx.outputs.merged_resources
    label = ctx.label.name

    # Args for compiler
    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)
    args.add("RESOURCE_MERGER")
    args.add("--target", ctx.label.package)
    args.add_joined(
        "--source-sets",
        ctx.attr.source_sets,
        join_with = ",",
    )
    args.add_joined(
        "--output",
        outputs,
        join_with = ",",
        map_each = _to_path,
    )

    mnemonic = "MergeSourceSets"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(ctx.files.resources + ctx.files.manifests),
        outputs = outputs,
        executable = ctx.executable._compiler,
        arguments = [args],
        progress_message = "%s %s" % (mnemonic, ctx.label),
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
            "worker-key-mnemonic": "MergeSourceSets",
        },
    )

    return [DefaultInfo(files = depset(outputs))]

resource_merger = rule(
    implementation = _resource_merger_impl,
    attrs = {
        "source_sets": attr.string_list(),
        "resources": attr.label_list(allow_files = True, mandatory = True),
        "manifests": attr.label_list(allow_files = True, mandatory = True),
        "merged_resources": attr.output_list(mandatory = True),
        "_compiler": attr.label(
            default = Label("@grab_bazel_common//tools/aapt_lite:aapt_lite"),
            executable = True,
            cfg = "exec",
        ),
    },
)
