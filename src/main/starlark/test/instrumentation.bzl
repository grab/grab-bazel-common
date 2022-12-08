load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")

def android_instrumentation_binary(
        name,
        associates = [],
        custom_package = None,
        debug_key = None,
        deps = [],
        instruments = None,
        manifest_values = {},
        resources = [],
        resource_strip_prefix = "",
        resource_files = [],
        srcs = [],
        test_instrumentation_runner = "androidx.test.runner.AndroidJUnitRunner",
        **kwargs):
    """A macro that creates an Android instumentation binary

    Assumptions:
      - The build config flag [--experimental_disable_instrumentation_manifest_merge] is enabled
    """

    generate_manifest_name = name + "_manifest"
    _generate_manifest_xml(
        name = generate_manifest_name,
        output = name + ".AndroidTestManifest.xml",
        package_name = custom_package,
        test_instrumentation_runner = test_instrumentation_runner,
    )

    android_library_name = name + "_lib"
    kt_android_library(
        name = android_library_name,
        srcs = srcs,
        manifest = generate_manifest_name,
        exports_manifest = 0,
        custom_package = custom_package,
        associates = associates,
        testonly = True,
        resources = resources,
        resource_strip_prefix = resource_strip_prefix,
        resource_files = resource_files,
        visibility = [
            "//visibility:public",
        ],
        deps = deps,
    )

    _manifest_values = {}
    if "applicationId" in manifest_values:
        application_id = manifest_values["applicationId"]
        if application_id.rpartition(".")[2] != "test":
            _manifest_values["applicationId"] = application_id + ".test"
    else:
        _manifest_values["applicationId"] = custom_package + ".test"

    native.android_binary(
        name = name,
        instruments = instruments,
        custom_package = custom_package,
        debug_key = debug_key,
        testonly = True,
        manifest = generate_manifest_name,
        manifest_values = _manifest_values,
        resource_files = resource_files,
        visibility = [
            "//visibility:public",
        ],
        deps = [android_library_name],
    )

def _generate_manifest_xml_impl(ctx):
    ctx.actions.expand_template(
        template = ctx.file._template,
        output = ctx.outputs.output,
        substitutions = {
            "{{.PACKAGE_NAME}}": ctx.attr.package_name,
            "{{.INSTRUMENTATION_RUNNER}}": ctx.attr.test_instrumentation_runner,
        },
    )

    return [
        DefaultInfo(files = depset([ctx.outputs.output])),
    ]

_generate_manifest_xml = rule(
    implementation = _generate_manifest_xml_impl,
    attrs = {
        "package_name": attr.string(
            mandatory = True,
        ),
        "test_instrumentation_runner": attr.string(
            mandatory = True,
        ),
        "output": attr.output(
            mandatory = True,
        ),
        "_template": attr.label(
            doc = "Android Test Manifest template",
            default = ":AndroidTestManifest.xml.tpl",
            allow_single_file = True,
        ),
    },
)
