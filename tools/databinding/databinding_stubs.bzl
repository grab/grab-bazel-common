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

def _is_direct(dep, tags):
    """
    Given tags containing info about direct dependencies in format @direct//, returns true if given
    label is a direct dependency and false otherwise

    Args:
      dep: Package name to check for
      tags: Tags passed to the rule
    """
    if (len(tags) == 0):
        return True
    for tag in tags:
        if tag.startswith("@direct//"):
            if dep == (tag[9:]):
                return True
    for tag in tags:
        if tag.startswith("@direct//"):
            return False
    return True

def _list_or_depset_to_list(list_or_depset):
    if type(list_or_depset) == "list":
        return list_or_depset
    elif type(list_or_depset) == "depset":
        return list_or_depset.to_list()
    else:
        fail("Expected a list or a depset. Got %s" % type(list_or_depset))

def _databinding_stubs_impl(ctx):
    deps = ctx.attr.deps
    tags = ctx.attr.tags
    custom_package = ctx.attr.custom_package
    class_infos = {}
    r_txts = []

    for target in deps:
        if (DataBindingV2Info in target):
            for class_info in _list_or_depset_to_list(target[DataBindingV2Info].class_infos):
                if _is_direct(class_info.owner.package, tags):
                    class_infos[class_info.path] = class_info
#        if (AndroidResourcesInfo in target):
#            r_txts.append(target[AndroidResourcesInfo].compiletime_r_txt)

    # Filter duplicates
    class_infos = list(class_infos.values())

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
        progress_message = "%s %s" % (mnemonic, ctx.label),
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
        "resource_files": attr.label_list(allow_files = True),
        "deps": attr.label_list(),
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
