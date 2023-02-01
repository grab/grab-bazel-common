load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

def _flatten_dict_to_key_value_pair(dict = {}):
    key_value_pairs = []
    for key, value in dict.items():
        key_value_pairs.append(
            "{key}={value}".format(
                key = key,
                value = value,
            )
        )

    return key_value_pairs

def _generate_final_strings(
        strings = {}):
    if (strings.get("VERSION_NAME", default = None) == None):
        # If the VERSION_NAME is not available, we auto add a default version name
        return dict(strings, VERSION_NAME = "VERSION_NAME", BUILD_TYPE = "debug")
    else:
        return dict(strings, BUILD_TYPE = "debug")

def _build_config_generator_impl(ctx):
    package_name = ctx.attr.package_name
    strings = ctx.attr.strings
    booleans = ctx.attr.booleans
    ints = ctx.attr.ints
    longs = ctx.attr.longs

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--package", package_name)
    args.add_joined(
        "--strings",
        strings,
        join_with = ","
    )
    args.add_joined(
        "--booleans",
        booleans,
        join_with = ","
    )
    args.add_joined(
        "--ints",
        ints,
        join_with = ","
    )
    args.add_joined(
        "--longs",
        longs,
        join_with = ","
    )

    output_directory = ctx.actions.declare_directory(ctx.label.name)
    args.add("--output", output_directory.path)

    output = ctx.actions.declare_file(
        "{name}/{package_name}/BuildConfig.java".format(
            name = ctx.label.name,
            package_name = package_name.replace(".", "/"),
        )
    )

    mnemonic = "BuildConfigGeneration"
    ctx.actions.run(
        mnemonic = mnemonic,
        outputs = [
            output_directory,
            output,
        ],
        executable = ctx.executable._compiler,
        arguments = [args],
        progress_message = "%s %s" % (mnemonic, ctx.label),
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
            "worker-key-mnemonic": "BuildConfigGenerationWorker",
        },
    )

    return [
        DefaultInfo(files = depset([
            output
        ]))
    ]

_build_config_generator = rule(
    implementation = _build_config_generator_impl,
    attrs = {
        "package_name": attr.string(mandatory = True),
        "strings": attr.string_list(),
        "booleans": attr.string_list(),
        "ints": attr.string_list(),
        "longs": attr.string_list(),
        "_compiler": attr.label(
            default = Label("@grab_bazel_common//tools/build_config:build_config_generator"),
            executable = True,
            cfg = "exec"
        ),
    },
)

def build_config(
        name,
        package_name,
        debug = True,
        strings = {},
        booleans = {},
        ints = {},
        longs = {}):
    """Generates a kt_jvm_library target containing build config fields just like AGP.

    Usage:
    Add the field variables in the relevant dicts like (strings, booleans etc) and add a dependency
    on this target. Values of fields are configurable (supports select())

    Args:
        name: Name for this target
        package_name: Package name of the generated build file. Same as the android_binary or android_library
        debug: Boolean to write to Build config
        strings: Build config field of type String
        booleans: Build config field of type Boolean
        ints: Build config field of type Int
        longs: Build config field of type longs
    """

    dbg = "true" if debug else "false"

    build_config_target = "_%s_gen" % name
    _build_config_generator(
        name = build_config_target,
        package_name = package_name,
        strings = _flatten_dict_to_key_value_pair(
            _generate_final_strings(strings)
        ),
        booleans = _flatten_dict_to_key_value_pair(
            dict(booleans, DEBUG = dbg)
        ),
        ints = _flatten_dict_to_key_value_pair(ints),
        longs = _flatten_dict_to_key_value_pair(longs),
    )

    kt_jvm_library(
        name = name,
        srcs = [build_config_target]
    )
