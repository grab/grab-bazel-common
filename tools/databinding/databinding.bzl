load("@grab_bazel_common//rules/android/lint:defs.bzl", "LINT_ENABLED")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")
load(":databinding_stubs.bzl", "databinding_stubs")

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
    """Generates Android library with Kotlin sources and Databinding support.

    This macro resolves circular dependencies that occur when using Databinding with Kotlin
    sources. It first compiles Kotlin code referenced in XML layouts using kt_jvm_library,
    then processes Databinding in android_library.

    Without this macro, a circular dependency occurs because:
    1. android_library needs Kotlin classes to process XML references
    2. Kotlin classes need generated Databinding classes
    3. Databinding classes can't be generated until android_library processes resources

    The macro breaks this cycle by:
    1. Processing resources without aapt to generate stub classes (R.java, BR.java, *Binding.java)
    2. Compiling Kotlin against these stubs
    3. Replacing stubs with actual classes in a final android_library

    Args:
        name: Name of the target.
        srcs: List of Kotlin and Java source files.
        custom_package: str, optional. Java package for generated R.java and BuildConfig files.
            If not specified, derived from the manifest's package attribute.
        manifest: Label, optional. Android manifest file.
        assets: List of files, optional. Asset files to include in the library.
        assets_dir: str, optional. Directory containing the assets.
        resource_files: List of Android resource files (layouts, drawables, etc).
            Must include any XML files that use data binding expressions.
        deps: List of targets, optional. Dependencies required by both Kotlin sources
            and Android resources.
        plugins: List of Kotlin compiler plugins, optional. Applied only to the Kotlin
            compilation phase.
        visibility: List of visibility specifications, optional.
        tags: List of tags, optional. Applied to both Kotlin and Android targets.
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
            tags = [tag for tag in tags if tag != LINT_ENABLED],
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
