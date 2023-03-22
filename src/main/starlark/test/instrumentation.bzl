load("@grab_bazel_common//tools/kotlin:android.bzl", "kt_android_library")

def android_instrumentation_binary(
        name,
        srcs = [],
        custom_package = None,
        debug_key = None,
        deps = [],
        instruments = None,
        target_package = None,
        manifest_values = {},
        resources = [],
        resource_strip_prefix = "",
        resource_files = [],
        associates = [],
        test_instrumentation_runner = "androidx.test.runner.AndroidJUnitRunner",
        **kwargs):
    """A macro that creates an Android instrumentation binary.

    - A test AndroidManifest.xml is created with targetPackage, package, instrumentation runner and activities declared
    - A library target is created for compiling test classes
    - A android_binary target with instruments set to `instruments` target.

    Assumptions:
      - The build config flag [--experimental_disable_instrumentation_manifest_merge] is enabled
    """

    package_name = custom_package
    if "applicationId" in manifest_values:
        package_name = manifest_values["applicationId"]

    test_manifest = name + "_manifest"
    _generate_manifest_xml(
        name = test_manifest,
        output = name + ".AndroidTestManifest.xml",
        package_name = package_name,
        target_package_name = target_package,
        test_instrumentation_runner = test_instrumentation_runner,
    )

    android_library_name = name + "_lib"
    kt_android_library(
        name = android_library_name,
        srcs = srcs,
        manifest = test_manifest,
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

    native.android_binary(
        name = name,
        instruments = instruments,
        custom_package = custom_package,
        debug_key = debug_key,
        testonly = True,
        manifest = test_manifest,
        manifest_values = manifest_values,
        resource_files = resource_files,
        visibility = [
            "//visibility:public",
        ],
        deps = [android_library_name],
        **kwargs
    )

def _generate_manifest_xml_impl(ctx):
    ctx.actions.expand_template(
        template = ctx.file._template,
        output = ctx.outputs.output,
        substitutions = {
            "{{.PACKAGE_NAME}}": ctx.attr.package_name,
            "{{.TARGET_PACKAGE_NAME}}": ctx.attr.target_package_name,
            "{{.INSTRUMENTATION_RUNNER}}": ctx.attr.test_instrumentation_runner,
        },
    )
    return [
        DefaultInfo(files = depset([ctx.outputs.output])),
    ]

_generate_manifest_xml = rule(
    implementation = _generate_manifest_xml_impl,
    attrs = {
        "package_name": attr.string(mandatory = True),
        "target_package_name": attr.string(mandatory = True),
        "test_instrumentation_runner": attr.string(mandatory = True),
        "output": attr.output(mandatory = True),
        "_template": attr.label(
            doc = "Android Test Manifest template",
            default = ":AndroidTestManifest.xml.tpl",
            allow_single_file = True,
        ),
    },
)
