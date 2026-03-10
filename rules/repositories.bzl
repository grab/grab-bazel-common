load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def _android():
    rules_android_tag = "0.7.1"
    http_archive(
        name = "rules_android",
        sha256 = "7c45b6aaa837fb6f2f23ad11387638cb00fa9f839a04ec564caac70a543a9cd5",
        strip_prefix = "rules_android-%s" % rules_android_tag,
        url = "https://github.com/bazelbuild/rules_android/releases/download/v%s/rules_android-v%s.tar.gz" % (rules_android_tag, rules_android_tag),
        patches = [
            "@grab_bazel_common//patches/rules_android:guava_version.patch",
            "@grab_bazel_common//patches/rules_android:macos_cp_reflink.patch",
            "@grab_bazel_common//patches/rules_android:use_androidx.patch",
            "@grab_bazel_common//patches/rules_android:androidx_annotation_template.patch",
            "@grab_bazel_common//patches/rules_android:databinding_deps.patch",
            "@grab_bazel_common//patches/rules_android:android_resource_processor_bazel_paths.patch",
            "@grab_bazel_common//patches/rules_android:allow_resource_conflicts.patch",
            "@grab_bazel_common//patches/rules_android:skip_library_resource_linking.patch",
            "@grab_bazel_common//patches/rules_android:fix_resource_priority_ordering.patch",
            "@grab_bazel_common//patches/rules_android:fix_aar_import_databinding_info.patch",
            "@grab_bazel_common//patches/rules_android:fix_databinding_providers.patch",
            "@grab_bazel_common//patches/rules_android:fix_databinding_data_binding.patch",
            "@grab_bazel_common//patches/rules_android:busybox_jvm_flags.patch",
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
