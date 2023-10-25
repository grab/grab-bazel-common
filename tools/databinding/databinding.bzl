load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load(":databinding_stubs.bzl", "databinding_stubs")
# load(":databinding_aar.bzl", "databinding_aar")

# TODO(arun) Replace with configurable maven targets
_DATABINDING_DEPS = [
    "@maven//:androidx_databinding_databinding_adapters",
    "@maven//:androidx_databinding_databinding_common",
    "@maven//:androidx_databinding_databinding_runtime",
    "@maven//:androidx_annotation_annotation",
    "@maven//:androidx_databinding_viewbinding",
]

_zipper = "@bazel_tools//tools/zip:zipper"

def _filter_deps(deps):
    """Filters known dependency labels that are not needed for databinding compilation
    """
    results = []
    for dep in deps:
        # TODO This ideally should be filtered via rules and checking for plugin providers
        if dep != "//:dagger":
            results.append(dep)
    return results

def kt_db_android_library(
        name,
        srcs = [],
        custom_package = None,
        manifest = None,
        resource_files = [],
        assets = None,
        assets_dir = None,
        deps = [],
        plugins = [],
        visibility = None,
        tags = []):
    """Configures rules for compiling android module that uses Databinding and Kotlin.

    The macro ensures that Kotlin code referenced in any XMLs are compiled first using kt_jvm_library
    and then uses android_library's enable_data_binding to generate required Databinding classes.

    This helps in breaking circular dependency when we have android_library (databinding enabled) -> kt_jvm_library.
    In that case, Databinding classes can't be generated until resources are processed and that
    happens only in android_library target. So compiling Koltin classes becomes dependent on
    android_library and android_library depends on kt_jvm_library since it needs class files to
    process class references in XML. This macro alleviates this problem by processing resources
    without `aapt` via a custom compiler and generates stub classes like
    R.java, BR.java and *Binding.java.

    Then Kotlin code can be safely compiled without errors. In the final stage, the stub classes
    are replaced with actual classes by android_library target.

    It also supports @BindingAdapters written in Kotlin.

    Args:
        name: The name of the target.
        srcs: Kotlin and Java classes for the target.
        custom_package: Custom package for the target. Forwards to 'kt_|android_library'.
        manifest: The AndroidManifest.xml file for android library.
        assets: Assets for android_library rule
        assets_dir: Assets dir for android_library rule
        resource_files: The resource files for the target.
        deps: The dependencies for the whole target.
        plugins: Kotlin compiler plugins for internal Kotlin target
        visibility: Visibility of the target.
        tags: Tags for both Kotlin and Android resources target.
    """

    # Create R.java and stub classes for classes that Android databinding and AAPT would produce
    # so that we can compile Kotlin classes first without errors
    databinding_stubs_target = name + "-stubs"
    databinding_stubs(
        name = databinding_stubs_target,
        custom_package = custom_package,
        resource_files = resource_files,
        tags = tags,
        deps = deps + _DATABINDING_DEPS,
        non_transitive_r_class = select({
            "@grab_bazel_common//:non_transitive_r_class": True,
            "//conditions:default": False,
        }),
    )
    binding_classes_sources = databinding_stubs_target + "_binding.srcjar"

    r_classes_sources = databinding_stubs_target + "_r.srcjar"
    r_classes = name + "-r-classes"

    # R classes are not meant to be packaged into the binary, so export it as java_library but don't
    # link it.
    native.java_library(
        name = r_classes,
        srcs = [r_classes_sources],
        tags = tags,
        neverlink = 1,  # Use the R classes only for compiling and not at runtime.
    )

    # Create an intermediate target for compiling all Kotlin classes used in Databinding
    kotlin_target = name + "_kt"
    kotlin_targets = []

    # List for holding binding adapter sources
    binding_adapter_sources = []

    if len(srcs) > 0:
        # Compile all Kotlin classes first with the stubs generated earlier. The stubs are provided
        # as srcjar in binding_classes_sources. This would allow use to compile Kotlin classes successfully
        # since stubs will allow compilation to proceed without errors related to missing binding classes.
        #
        # Additionally, run our custom annotation processor "binding-adapter-bridge" that would generate
        # .java files for Kotlin @BindingAdapter.

        kt_jvm_library(
            name = kotlin_target,
            srcs = srcs + [binding_classes_sources],
            plugins = plugins,
            deps = deps + _DATABINDING_DEPS + [r_classes] + [
                "@grab_bazel_common//tools/binding-adapter-bridge:binding-adapter-bridge",
                "@grab_bazel_common//tools/android:android_sdk",
            ],
            tags = tags,
        )
        kotlin_targets.append(kotlin_target)

        # The Kotlin target would run binding-adapter annotation processor and package the Java proxy
        # classes as a jar file, BUT android_library does not run data binding annotation processor
        # if classes are present inside jar i.e deps. To overcome this, we repackage sources jar into
        # .srcjar so that we can feed it to android_library's `srcs` to force data binding processor
        # to run.
        # Additionally we clean up all extra files that might be present in the sources.jar. The
        # jar should purely contain *_Binding_Adapter_Stub.java files.
        #
        # This step can be probably be avoided when https://github.com/bazelbuild/bazel/issues/11745
        # is fixed.
        binding_adapters_source = name + "-binding-adapters"
        native.genrule(
            name = binding_adapters_source,
            srcs = [":" + kotlin_target + "-sources.jar"],
            outs = [kotlin_target + "_kt-sources.srcjar"],
            tools = [_zipper],
            tags = tags,
            message = "Generating binding adapter stubs " + name,
            cmd = """
            TEMP="adapter-sources"
            mkdir -p $$TEMP
            unzip -q -o $< '*_Binding_Adapter_Stub.java' -d $$TEMP/ 2> /dev/null || true
            touch $$TEMP/empty.txt # Package empty file to ensure jar file is always generated
            find $$TEMP/. -type f -exec $(location {zipper}) c $(OUTS) {{}} +
            rm -rf $$TEMP
            """.format(zipper = _zipper),
        )
        binding_adapter_sources.append(binding_adapters_source)

    # DatabindingMapperImpl is in the public ABI among Databinding generated classes, use a stub
    # class instead so that we can avoid running entire databinding processor for header compilation.
    databinding_mapper = "_" + name + "_mapper"
    native.java_library(
        name = databinding_mapper,
        srcs = [databinding_stubs_target + "_mapper.srcjar"],
        tags = tags,
        neverlink = 1,  # Use only in the compile classpath
        deps = _DATABINDING_DEPS + [
            "@grab_bazel_common//tools/android:android_sdk",
        ],
    )

    # Data binding target responsible for generating Databinding related classes.
    # By the time this is compiled:
    # * Kotlin/Java classes are already available via deps. So resources processing is safe.
    # * Kotlin @BindingAdapters are converted to Java via our annotation processor
    # * Our stub classes will be replaced by android_library's actual generated code.
    native.android_library(
        name = name,
        srcs = binding_adapter_sources,
        custom_package = custom_package,
        enable_data_binding = True,
        resource_files = resource_files,
        assets = assets,
        assets_dir = assets_dir,
        visibility = visibility,
        manifest = manifest,
        tags = tags,
        deps = kotlin_targets + _filter_deps(deps) + _DATABINDING_DEPS + [databinding_mapper],
        # Export the Kotlin target so that other databinding modules that depend on this module
        # can use classes defined in this module in their databinding generated classes.
        #
        # This is required since kt_android_library hides _kt target behind an android_library rule,
        # hence _kt target only appears are transitive dep instead of direct during databinding
        # generation in module A.
        # Graph:                +------+
        #                       |  kt  |
        #                       +------+
        #                          ^
        #       +--------+    +--------+
        #       |   A    +--->+   B    |
        #       +--------+    +--------+
        # A's databinding generated code can depend on B's kotlin code.
        # See: https://blog.bazel.build/2017/06/28/sjd-unused_deps.html
        # Can be also overcome by --strict_java_deps=warn
        exports = kotlin_targets,
    )

    # Package aar correctly for Gradle builds.
    # Disabled for now.
    # databinding_aar(
    #     name = name + "-databinding",
    #     android_library = name,
    #     kotlin_jar = kotlin_target + "_kt.jar",
    # )
