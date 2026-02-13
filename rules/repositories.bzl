load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def android():
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
            "@grab_bazel_common//patches/rules_android:databinding_output_name.patch",
            #"@grab_bazel_common//patches/rules_android:databinding_absolute_paths.patch",
            "@grab_bazel_common//patches/rules_android:android_resource_processor_bazel_paths.patch",
        ],
        patch_args = ["-p1"],
    )

def maven_load():
    RULES_JVM_EXTERNAL_TAG = "6.9"
    RULES_JVM_EXTERNAL_SHA = "3c41eae4226a7dfdce7b213bc541557b8475c92da71e2233ec7c306630243a65"

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
    RULES_KOTLIN_VERSION = "2.1.2"

    RULES_KOTLIN_SHA = "6ea1c530261756546d0225a0b6e580eaf2f49084e28679a6c17f8ad1ccecca5d"

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

def _proto():
    http_archive(
        name = "com_google_protobuf",
        sha256 = "cf754718b0aa945b00550ed7962ddc167167bd922b842199eeb6505e6f344852",
        strip_prefix = "protobuf-%s" % "3.11.3",
        urls = [
            "https://mirror.bazel.build/github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % "3.11.3",
            "https://github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % "3.11.3",
        ],
    )

    http_archive(
        name = "bazel_skylib",
        sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
        urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (
            "1.0.3",
            "1.0.3",
        )],
    )

    http_archive(
        name = "rules_proto",
        sha256 = "e017528fd1c91c5a33f15493e3a398181a9e821a804eb7ff5acdd1d2d6c2b18d",
        strip_prefix = "rules_proto-4.0.0-3.20.0",
        urls = [
            "https://github.com/bazelbuild/rules_proto/archive/refs/tags/4.0.0-3.20.0.tar.gz",
        ],
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
    #_proto
    android()
    maven_load()
    _java()
    _kotlin()
    _detekt()
    _jetifier()
