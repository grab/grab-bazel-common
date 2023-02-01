load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

_STRING_TYPE = "strings"
_BOOLEAN_TYPE = "booleans"
_INT_TYPE = "ints"
_LONG_TYPE = "longs"

def _build_cmd_options(
        flag,
        value_dict = {}):
    cmd_options = ""
    if len(value_dict) > 0:
        cmd_options += " --%s " % flag
        cmd_options += "\""

        is_first = True
        for key, value in value_dict.items():
            if is_first:
                cmd_options += "%s=" % key
                is_first = False
            else:
                cmd_options += ",%s=" % key

            if type(value) == "select":
                cmd_options += value
            else:
                cmd_options += "{}".format(
                    str(value)
                        .replace("\\$", "$")  # Unescape existing dollar symbol
                        .replace("$", "\\$$"),  # Escape dollars for Make substitution
                )

        cmd_options += "\""

    return cmd_options

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
    """Generates a java_library target containing build config fields just like AGP.

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
    build_config_file_path = "%s/src/main/java/BuildConfig.java" % (name)

    build_config_generator = "@grab_bazel_common//tools/build_config:build_config_generator"
    cmd = """
    $(location {build_config_generator}) \
    --package \"{package_name}\" \
    """.format(
        build_config_generator = build_config_generator,
        package_name = package_name,
    )

    cmd += _build_cmd_options(
        _STRING_TYPE,
        _generate_final_strings(strings),
    )

    dbg = "true" if debug else "false"

    cmd += _build_cmd_options(
        _BOOLEAN_TYPE,
        dict(booleans, DEBUG = dbg),
    )
    cmd += _build_cmd_options(_INT_TYPE, ints)
    cmd += _build_cmd_options(_LONG_TYPE, longs)

    native.genrule(
        name = "_%s_gen" % name,
        outs = [build_config_file_path],
        cmd = cmd + " > $@",
        toolchains = ["@bazel_tools//tools/jdk:current_java_runtime"],
        tools = [build_config_generator],
        message = "Generating %s's build config class" % (native.package_name()),
    )

    kt_jvm_library(
        name = name,
        srcs = [build_config_file_path],
    )
