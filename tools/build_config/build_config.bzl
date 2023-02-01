load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

def _flatten_key_value_pair(keys = [], values = []):
    result = []
    for index, key in enumerate(keys):
        value = values[index]
        result.append(
            "{key}={value}".format(
                key = key,
                value = value,
            ),
        )

    return result

def _build_config_generator_impl(ctx):
    package_name = ctx.attr.package_name
    string_keys = ctx.attr.string_keys
    string_values = ctx.attr.string_values
    boolean_keys = ctx.attr.boolean_keys
    boolean_values = ctx.attr.boolean_values
    int_keys = ctx.attr.int_keys
    int_values = ctx.attr.int_values
    long_keys = ctx.attr.long_keys
    long_values = ctx.attr.long_values

    strings = _flatten_key_value_pair(string_keys, string_values)
    booleans = _flatten_key_value_pair(boolean_keys, boolean_values)
    ints = _flatten_key_value_pair(int_keys, int_values)
    longs = _flatten_key_value_pair(long_keys, long_values)

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("--flagfile=%s", use_always = True)

    args.add("--package", package_name)
    args.add_joined(
        "--strings",
        strings,
        join_with = ",",
    )
    args.add_joined(
        "--booleans",
        booleans,
        join_with = ",",
    )
    args.add_joined(
        "--ints",
        ints,
        join_with = ",",
    )
    args.add_joined(
        "--longs",
        longs,
        join_with = ",",
    )

    output_directory = ctx.actions.declare_directory(ctx.label.name)
    args.add("--output", output_directory.path)

    output = ctx.actions.declare_file(
        "{name}/{package_name}/BuildConfig.java".format(
            name = ctx.label.name,
            package_name = package_name.replace(".", "/"),
        ),
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
            output,
        ])),
    ]

_build_config_generator = rule(
    implementation = _build_config_generator_impl,
    attrs = {
        "package_name": attr.string(mandatory = True),
        "string_keys": attr.string_list(),
        "string_values": attr.string_list(),
        "boolean_keys": attr.string_list(),
        "boolean_values": attr.string_list(),
        "int_keys": attr.string_list(),
        "int_values": attr.string_list(),
        "long_keys": attr.string_list(),
        "long_values": attr.string_list(),
        "_compiler": attr.label(
            default = Label("@grab_bazel_common//tools/build_config:build_config_generator"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _convert_to_keys_values_tuple(dict = {}):
    """Converts the given dict into a tuple with a list of keys and a list of values.

    Static values and dynamic (select()) values are being separated and finally
    combined with dynamic keys and values getting pushed to the back of the list
    """
    statics = {}
    dynamics = {}
    for key, value in dict.items():
        if type(value) == "select":
            dynamics[key] = value
        else:
            statics[key] = str(value)

    keys = statics.keys() + dynamics.keys()
    values = statics.values()
    for dynamic_value in dynamics.values():
        values += dynamic_value
    return (keys, values)

def _generate_final_strings(
        strings = {}):
    if (strings.get("VERSION_NAME", default = None) == None):
        # If the VERSION_NAME is not available, we auto add a default version name
        return dict(strings, VERSION_NAME = "VERSION_NAME", BUILD_TYPE = "debug")
    else:
        return dict(strings, BUILD_TYPE = "debug")

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

    (string_keys, string_values) = _convert_to_keys_values_tuple(
        _generate_final_strings(strings),
    )

    (boolean_keys, boolean_values) = _convert_to_keys_values_tuple(
        dict(booleans, DEBUG = dbg),
    )

    (int_keys, int_values) = _convert_to_keys_values_tuple(ints)
    (long_keys, long_values) = _convert_to_keys_values_tuple(longs)

    build_config_target = "_%s_gen" % name
    _build_config_generator(
        name = build_config_target,
        package_name = package_name,
        string_keys = string_keys,
        string_values = string_values,
        boolean_keys = boolean_keys,
        boolean_values = boolean_values,
        int_keys = int_keys,
        int_values = int_values,
        long_keys = long_keys,
        long_values = long_values,
    )

    kt_jvm_library(
        name = name,
        srcs = [build_config_target],
    )
