load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def _android():
    http_archive(
        name = "rules_android_ndk",
        sha256 = "07e7a2777113bb3d0a432265d1c78cfaa140a5bc4c82be4c8cd988b34382ec90",
        strip_prefix = "rules_android_ndk-0.1.5",
        url = "https://github.com/bazelbuild/rules_android_ndk/releases/download/v0.1.5/rules_android_ndk-v0.1.5.tar.gz",
    )

    rules_android_tag = "0.7.1"
    http_archive(
        name = "rules_android",
        sha256 = "7c45b6aaa837fb6f2f23ad11387638cb00fa9f839a04ec564caac70a543a9cd5",
        strip_prefix = "rules_android-%s" % rules_android_tag,
        url = "https://github.com/bazelbuild/rules_android/releases/download/v%s/rules_android-v%s.tar.gz" % (rules_android_tag, rules_android_tag),
        patches = [
            "@grab_bazel_common//patches/rules_android/infra:guava_version.patch",
            "@grab_bazel_common//patches/rules_android/infra:macos_cp_reflink.patch",
            "@grab_bazel_common//patches/rules_android/androidx:use_androidx.patch",
            "@grab_bazel_common//patches/rules_android/androidx:androidx_annotation_template.patch",
            "@grab_bazel_common//patches/rules_android/databinding:databinding_deps.patch",
            "@grab_bazel_common//patches/rules_android/resources:android_resource_processor_bazel_paths.patch",
            "@grab_bazel_common//patches/rules_android/infra:allow_resource_conflicts.patch",
            "@grab_bazel_common//patches/rules_android/infra:suppress_resource_conflict_warnings.patch",
            "@grab_bazel_common//patches/rules_android/infra:compress_java_resources.patch",
            "@grab_bazel_common//patches/rules_android/non_transitive_r:generate_binary_r_primary_only_busybox.patch",
            "@grab_bazel_common//patches/rules_android/non_transitive_r:generate_binary_r_primary_only_rclass.patch",
            "@grab_bazel_common//patches/rules_android/non_transitive_r:skip_library_resource_linking.patch",
            "@grab_bazel_common//patches/rules_android/desugar:skip_binary_r_jar_desugaring_attrs.patch",
            "@grab_bazel_common//patches/rules_android/desugar:skip_binary_r_jar_desugaring_impl.patch",
            "@grab_bazel_common//patches/rules_android/desugar:skip_binary_r_jar_desugaring_flags.patch",
            "@grab_bazel_common//patches/rules_android/resources:fix_resource_priority_ordering.patch",
            "@grab_bazel_common//patches/rules_android/aar_import:fix_aar_import_databinding_info.patch",
            "@grab_bazel_common//patches/rules_android/databinding:fix_databinding_providers.patch",
            "@grab_bazel_common//patches/rules_android/databinding:fix_databinding_data_binding.patch",
            "@grab_bazel_common//patches/rules_android/databinding:databinding_ap_turbine_skip.patch",
            "@grab_bazel_common//patches/rules_android/non_transitive_r:fix_binary_r_class_generation.patch",
            "@grab_bazel_common//patches/rules_android/infra:wire_strict_deps_flag.patch",
            "@grab_bazel_common//patches/rules_android/infra:no_source_compile_classpath.patch",
            "@grab_bazel_common//patches/rules_android/infra:disable_aar_import_deps_checker.patch",
            "@grab_bazel_common//patches/rules_android/infra:allow_deps_without_srcs.patch",
            "@grab_bazel_common//patches/rules_android/aar_import:aar_import_export_r_java.patch",
            "@grab_bazel_common//patches/rules_android/non_transitive_r:connect_native_android_flags.patch",
            "@grab_bazel_common//patches/rules_android/aar_import:native_databinding_package_info.patch",
            "@grab_bazel_common//patches/rules_android/package_aware_rtxt:package_aware_rtxt_tool.patch",
            "@grab_bazel_common//patches/rules_android/package_aware_rtxt:package_aware_rtxt_merge_action.patch",
            "@grab_bazel_common//patches/rules_android/package_aware_rtxt:busybox_package_aware_rtxt.patch",
            "@grab_bazel_common//patches/rules_android/package_aware_rtxt:provider_package_aware_rtxt.patch",
            "@grab_bazel_common//patches/rules_android/databinding:databinding_non_transitive_r.patch",
            "@grab_bazel_common//patches/rules_android/databinding:databinding_non_transitive_r_impl.patch",
            "@grab_bazel_common//patches/rules_android/r_class_overflow:binary_r_class_too_large_fix.patch",
            "@grab_bazel_common//patches/rules_android/databinding:fix_databinding_copydir_symlink.patch",
            "@grab_bazel_common//patches/rules_android/desugar:skip_r_jar_desugaring.patch",
            "@grab_bazel_common//patches/rules_android/desugar:dex_aar_import_resources_jar.patch",
            "@grab_bazel_common//patches/rules_android/r_class_overflow:merge_compiled_r_class_too_large_fix.patch",
            "@grab_bazel_common//patches/rules_android/package_aware_rtxt:fix_exports_package_aware_rtxt.patch",
            "@grab_bazel_common//patches/rules_android/aar_import:aar_import_transitive_r_txt.patch",
            "@grab_bazel_common//patches/rules_android/r_class_overflow:merge_primary_library_symbols.patch",
            "@grab_bazel_common//patches/rules_android/workers:gen_base_classes_worker_java.patch",
            "@grab_bazel_common//patches/rules_android/workers:gen_base_classes_worker_lib.patch",
            "@grab_bazel_common//patches/rules_android/workers:databinding_exec_worker_binary.patch",
            "@grab_bazel_common//patches/rules_android/workers:databinding_exec_worker_toolchain.patch",
            "@grab_bazel_common//patches/rules_android/workers:generate_databinding_base_classes_worker.patch",
            "@grab_bazel_common//patches/rules_android/databinding:filter_transitive_databinding_artifacts.patch",
            "@grab_bazel_common//patches/rules_android/desugar:isolate_metadata_desugar_worker.patch",
            "@grab_bazel_common//patches/rules_android/desugar:normalize_desugar_zip_timestamps.patch",
            "@grab_bazel_common//patches/rules_android/desugar:normalize_desugar_missing_interface_origins.patch",
            "@grab_bazel_common//patches/rules_android/dexer:cache_dexbuilder_synthetic_context.patch",
            "@grab_bazel_common//patches/rules_android/resources:canonicalize_manifest_merger_log_paths.patch",
            "@grab_bazel_common//patches/rules_android/resources:normalize_databinding_compiled_resources_timestamps.patch",
            "@grab_bazel_common//patches/rules_android/resources:sort_resource_source_table.patch",
        ],
        patch_args = ["-p1"],
    )

def _maven():
    RULES_JVM_EXTERNAL_TAG = "6.10"
    RULES_JVM_EXTERNAL_SHA = "e5f83b8f2678d2b26441e5eafefb1b061826608417b8d24e5e8e15e585eab1ba"

    http_archive(
        name = "rules_jvm_external",
        sha256 = RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG),
        patches = ["@grab_bazel_common//patches/rules_jvm_external:jetifier.patch"],
        patch_args = ["-p1"],
    )

    DAGGER_TAG = "2.59.1"

    DAGGER_SHA = "1faec1f454936fc9739a2bdf3c909528a031a8561113c3a4b350ee48c6746150"

    http_archive(
        name = "bazel_common_dagger",
        sha256 = DAGGER_SHA,
        strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
        url = "https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG,
    )

def _java():
    rules_license_tag = "1.0.0"
    http_archive(
        name = "rules_license",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_license/releases/download/%s/rules_license-%s.tar.gz" % (rules_license_tag, rules_license_tag),
            "https://github.com/bazelbuild/rules_license/releases/download/%s/rules_license-%s.tar.gz" % (rules_license_tag, rules_license_tag),
        ],
        sha256 = "26d4021f6898e23b82ef953078389dd49ac2b5618ac564ade4ef87cced147b38",
    )

    http_archive(
        name = "bazel_features",
        sha256 = "a660027f5a87f13224ab54b8dc6e191693c554f2692fcca46e8e29ee7dabc43b",
        strip_prefix = "bazel_features-1.30.0",
        url = "https://github.com/bazel-contrib/bazel_features/releases/download/v1.30.0/bazel_features-v1.30.0.tar.gz",
    )

    http_archive(
        name = "rules_java",
        urls = [
            "https://github.com/bazelbuild/rules_java/releases/download/9.5.0/rules_java-9.5.0.tar.gz",
        ],
        sha256 = "440edfa8098d00b166a5a73d215f3214a6506db01e1ec45afee356b6679c5593",
    )

    http_archive(
        name = "rules_cc",
        urls = ["https://github.com/bazelbuild/rules_cc/releases/download/0.2.14/rules_cc-0.2.14.tar.gz"],
        sha256 = "a2fdfde2ab9b2176bd6a33afca14458039023edb1dd2e73e6823810809df4027",
        strip_prefix = "rules_cc-0.2.14",
    )

def _kotlin():
    RULES_KOTLIN_VERSION = "2.1.10"

    RULES_KOTLIN_SHA = "afa951024e022f7ec565295fcf4cb74738ef7b2ff968820f1465488c06ecf0a0"

    http_archive(
        name = "io_bazel_rules_kotlin",
        sha256 = RULES_KOTLIN_SHA,
        urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin-v%s.tar.gz" % (RULES_KOTLIN_VERSION, RULES_KOTLIN_VERSION)],
    )

def _detekt():
    rules_detekt_version = "0.8.1.4"

    rules_detekt_sha = "95640b50bbb4d196ad00cce7455f6033f2a262aa56ac502b559160ca7ca84e3f"

    http_archive(
        name = "rules_detekt",
        sha256 = rules_detekt_sha,
        strip_prefix = "bazel_rules_detekt-{v}".format(v = rules_detekt_version),
        url = "https://github.com/mohammadkahelghi-grabtaxi/bazel_rules_detekt/releases/download/v{v}/bazel_rules_detekt-v{v}.tar.gz".format(v = rules_detekt_version),
    )

def _jetifier():
    JETIFIER_SOURCE_SHA = "8ac1c5c2a8681c398883bb2cabc18f913337f165059f24e8c55714e05757761e"

    http_archive(
        name = "jetifier",
        sha256 = JETIFIER_SOURCE_SHA,
        strip_prefix = "rules_jvm_external-5.3/third_party/jetifier",
        urls = ["https://github.com/bazelbuild/rules_jvm_external/archive/refs/tags/5.3.tar.gz"],
        build_file = "@grab_bazel_common//patches/jetifier:BUILD.jetifier",
    )

def bazel_common_dependencies():
    _android()
    _maven()
    _java()
    _kotlin()
    _detekt()
    _jetifier()
