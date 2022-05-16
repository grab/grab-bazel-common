load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

_STRING_TYPE = "String"
_BOOLEAN_TYPE = "Boolean"
_INT_TYPE = "Int"
_LONG_TYPE = "Long"

def _raw_string(original):
    """
    Wraps the given original string to
        * Kotlin's raw string literal
        * Escape $ in Kotlin multi line string

    See: https://kotlinlang.org/docs/reference/basic-types.html#string-templates
    """
    return '"""' + original.replace("$", "${'$'}") + '"""'

def _generate_final_strings(strings = {}):
    if (strings.get("VERSION_NAME", default = None) == None):
        # If the VERSION_NAME is not available, we auto add a default version name
        return dict(strings, VERSION_NAME = "VERSION_NAME", BUILD_TYPE = "debug")
    else:
        return dict(strings, BUILD_TYPE = "debug")

def _build_statement(type, items, quote = True):
    statements = ""
    for variable_name, value in items.items():
        const = "const"
        if quote:
            value = "{}".format(_raw_string(value))
        if type == _BOOLEAN_TYPE:
            value = "{}.toBoolean()".format(value)
            const = "@JvmField"

        statements += """
    {} val {}: {} = {}""".format(
            const,
            variable_name,
            type,
            str(value)
                .replace("\\$", "$")  # Unescape existing dollar symbol
                .replace("$", "\\$$"),  # Escape dollars for Make substitution
        )
    return statements

def build_config(
        name,
        package_name,
        debug = True,
        strings = {},
        booleans = {},
        ints = {},
        longs = {}):
    """Generates a Jar file containing BuildConfig class just like AGP.

    Usage:
    Add the field variables in the relevant dicts like (strings, booleans etc) and add a dependency
    on this target.

    Args:
        name: Name for this target
        package_name: Package name of the generated build file. Same as the android_binary or android_library
        debug: Boolean to write to Build config
        strings: Build config field of type String
        booleans: Build config field of type Boolean
        ints: Build config field of type Int
        longs: Build config field of type longs
    """

    build_config = "BuildConfig"
    build_config_file_path = "$(RULEDIR)/{}.kt".format(build_config)

    # Uses srcjar so that the generated .kt file can be compiled to class file via kt_jvm_library
    build_config_jar = name + ".srcjar"

    strings_statements = _build_statement(
        _STRING_TYPE,
        _generate_final_strings(strings),
    )

    dbg = "true" if debug else "false"

    booleans_statements = _build_statement(
        _BOOLEAN_TYPE,
        dict(booleans, DEBUG = dbg),
    )
    ints_statements = _build_statement(_INT_TYPE, ints, False)
    longs_statements = _build_statement(_LONG_TYPE, longs, False)

    # Cmd for genrule. Generates BuildConfig.kt and packages it to a Java jar file in the build
    # directory
    cmd = """
cat << EOF > {build_config_file_path}
/**
 * Generated file do not modify
 */
package {package_name}
object {build_config} {{
    // Generated custom fields
    {strings_statements}
    {booleans_statements}
    {ints_statements}
    {longs_statements}
}}
EOF
$(location @bazel_tools//tools/zip:zipper) c $@ {build_config_file_path}
""".format(
        build_config = build_config,
        build_config_file_path = build_config_file_path,
        package_name = package_name,
        strings_statements = strings_statements,
        booleans_statements = booleans_statements,
        ints_statements = ints_statements,
        longs_statements = longs_statements,
    )

    native.genrule(
        name = "_" + name,
        outs = [build_config_jar],
        cmd = cmd,
        message = "Generating %s's build config class" % (native.package_name()),
        tools = ["@bazel_tools//tools/zip:zipper"],
    )

    kt_jvm_library(
        name = name,
        srcs = [build_config_jar],
    )
