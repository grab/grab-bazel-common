"""
A rule to generate databinding stub classes like BR.java, R.java and *Binding.java to support
Kotlin compilation.
"""

def _to_short_path(f):
    return f.short_path

def _databinding_stubs_impl(ctx):
    """
    """
    deps = ctx.attr.deps
    custom_package = ctx.attr.custom_package
    databinding_metadata_prefix = "databinding-metadata"

    databinding_metadata = []
    for target in deps:
        if (DataBindingV2Info in target):
            data_binding_info = target[DataBindingV2Info]
            ci = data_binding_info.class_infos.to_list()
            for class_info in ci:
                class_info_symlink = ctx.actions.declare_file(
                    databinding_metadata_prefix + "/class_infos/%s_classInfo.zip" % target.label.name,
                )
                ctx.actions.symlink(
                    output = class_info_symlink,
                    target_file = class_info,
                )
                databinding_metadata.append(class_info_symlink)
        if (AndroidResourcesInfo in target):
            r_txt_file = target[AndroidResourcesInfo].compiletime_r_txt
            r_txt_symlink = ctx.actions.declare_file(
               databinding_metadata_prefix + "/r_txt/%s_r.txt" % target.label.name,
            )
            ctx.actions.symlink(
                output = r_txt_symlink,
                target_file = r_txt_file,
            )
            databinding_metadata.append(r_txt_symlink)


    databinding_metadata_path = ""
    if len(databinding_metadata)==0: # When no symlinks are present then create an empty dir
        databinding_metadata_dir = ctx.actions.declare_directory("databinding-metadata")
        databinding_metadata_path = databinding_metadata_dir.path
        ctx.actions.run_shell(
            mnemonic = "DatabindingMetaData",
            outputs = [databinding_metadata_dir],
            command = "mkdir -p %s" % (databinding_metadata_dir.databinding_metadata_path),
        )
    else:
        databinding_metadata_path = databinding_metadata[0].dirname

    # Args for compiler
    args = ctx.actions.args()
    args.add("--package", custom_package)
    args.add("--databinding-metadata", databinding_metadata_path)
    args.add_joined(
        "--resource-files",
        ctx.files.resource_files,
        join_with = ",",
        map_each = _to_short_path,
    )

    ctx.actions.run(
        mnemonic = "DatabindingStubs",
        inputs = depset(ctx.files.resource_files + databinding_metadata),
        outputs = [
            ctx.outputs.jar,
        ],
        executable = ctx.executable._compiler,
        arguments = [args],
        progress_message = "Generating databinding stubs",
    )

databinding_stubs = rule(
    implementation = _databinding_stubs_impl,
    attrs = {
        "custom_package": attr.string(mandatory = True),
        "resource_files": attr.label_list(
            allow_files = True,
        ),
        "deps": attr.label_list(),
        "_zipper": attr.label(
            default = Label("@bazel_tools//tools/zip:zipper"),
            cfg = "host",
            executable = True,
        ),
        "_compiler": attr.label(
            default = Label("@grab_bazel_common//tools/db-compiler-lite:db-compiler-lite"),
            executable = True,
            cfg = "exec",
        ),
    },
    outputs = dict(
        jar = "%{name}.jar",
    ),
)
