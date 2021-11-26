"""
A rule to generate databinding stub classes like BR.java, R.java and *Binding.java to support
Kotlin compilation.

It works by parsing all resource files and dependencies' R.txt files to generate R.java class with stub
value of 0 and then parsing all layout files to generate *Binding classes. Both of these are required
to compile typical Kotlin + databinding setup (as used by Gradle).

Once stubs are generated, android_library can be used to generate the actual Binding classes

Args:
    name: Name for the target that uses the stubs
    custom_package: Custom package for the target.
    resource_files: The resource files for the target
    deps: The dependencies for the whole target.

Outputs:
    %{name}_r.srcjar: The R and BR classes
    %{name}_binding.srcjar: All the databinding *Binding classes
"""

def _to_path(f):
    return f.path

def _databinding_stubs_impl(ctx):
    """
    """
    deps = ctx.attr.deps
    custom_package = ctx.attr.custom_package
    class_infos = []
    r_txts = []

    for target in deps:
        if (DataBindingV2Info in target):
            data_binding_info = target[DataBindingV2Info]
            ci = data_binding_info.class_infos.to_list()
            if len(ci) > 0:
                class_infos.append(ci[0])
        if (AndroidResourcesInfo in target):
            r_txts.append(target[AndroidResourcesInfo].compiletime_r_txt)

    # Args for compiler
    args = ctx.actions.args()
    args.add("--package", custom_package)
    args.add_joined(
        "--resource-files",
        ctx.files.resource_files,
        join_with = ",",
        map_each = _to_path,
    )
    args.add_joined(
        "--class-infos",
        class_infos,
        join_with = ",",
        map_each = _to_path,
    )
    args.add_joined(
        "--r-txts",
        r_txts,
        join_with = ",",
        map_each = _to_path,
    )
    args.add("--r-class-output", ctx.outputs.r_class_jar)
    args.add("--stubs-output", ctx.outputs.binding_jar)

    mnemonic = "DatabindingStubs"
    ctx.actions.run(
        mnemonic = mnemonic,
        inputs = depset(ctx.files.resource_files + class_infos + r_txts),
        outputs = [
            ctx.outputs.r_class_jar,
            ctx.outputs.binding_jar,
        ],
        executable = ctx.executable._compiler,
        arguments = [args],
        progress_message = "%s Generating stubs for %s" % (mnemonic, ctx.label),
    )

    return [
        DefaultInfo(files = depset([
            ctx.outputs.r_class_jar,
            ctx.outputs.binding_jar,
        ])),
    ]

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
        r_class_jar = "%{name}_r.srcjar",
        binding_jar = "%{name}_binding.srcjar",
    ),
)
