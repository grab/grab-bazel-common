"""
A rule to generate databinding mapper stub classes.

This rule generates the DatabindingMapperImpl class stub that is required for databinding to work
properly with Kotlin compilation.

This file exports the databinding_mapper_library macro which creates a java library with the 
generated DatabindingMapperImpl stub class.

Args:
    name: Name for the target
    custom_package: Custom package for the target.

Outputs:
    %{name}.srcjar: Stub DatabindingMapperImpl class
"""

load("@grab_bazel_common//rules/android/databinding:databinding.bzl", "DATABINDING_DEPS")

def _databinding_mapper_impl(ctx):
    custom_package = ctx.attr.custom_package

    # Databinding Mappers
    mapper_args = ctx.actions.args()
    mapper_args.set_param_file_format("multiline")
    mapper_args.use_param_file("--flagfile=%s", use_always = True)
    mapper_args.add("DATABINDING_MAPPER")
    mapper_args.add("--package", custom_package)
    mapper_args.add("--output", ctx.outputs.mapper_jar)

    mnemonic = "DatabindingMapperStubs"
    ctx.actions.run(
        mnemonic = mnemonic,
        outputs = [
            ctx.outputs.mapper_jar,
        ],
        executable = ctx.executable._compiler,
        arguments = [mapper_args],
        progress_message = "%s %s" % (mnemonic, ctx.label),
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
            "worker-key-mnemonic": "DatabindingWorker",
        },
    )

    return [
        DefaultInfo(files = depset([
            ctx.outputs.mapper_jar,
        ])),
    ]

_databinding_mapper = rule(
    implementation = _databinding_mapper_impl,
    attrs = {
        "custom_package": attr.string(mandatory = True),
        "_compiler": attr.label(
            default = Label("@grab_bazel_common//tools/aapt_lite:aapt_lite"),
            executable = True,
            cfg = "exec",
        ),
    },
    outputs = dict(
        mapper_jar = "%{name}.srcjar",
    ),
)

def databinding_mapper_library(
        name,
        custom_package,
        tags = [],
        neverlink = True,
        visibility = None):
    """Creates a java library with the generated DatabindingMapperImpl stub class.

    Args:
        name: Name for the target
        custom_package: Custom package for the target
        tags: Optional tags to apply to the targets
        neverlink: The neverlink attr for java_library
        visibility: Optional visibility for the final java_library
    """

    # Generate mapper stub
    mapper_target = "_%s_gen" % name
    _databinding_mapper(
        name = mapper_target,
        custom_package = custom_package,
        tags = tags,
    )

    # Create a java_library with the generated mapper stub
    native.java_library(
        name = name,
        srcs = [":%s.srcjar" % mapper_target],
        tags = tags,
        visibility = visibility,
        neverlink = neverlink,
        deps = DATABINDING_DEPS + [
            "@grab_bazel_common//tools/android:android_sdk",
        ],
    )
