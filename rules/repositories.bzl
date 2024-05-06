load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def _maven():
    RULES_JVM_EXTERNAL_TAG = "5.3"
    RULES_JVM_EXTERNAL_SHA = "d31e369b854322ca5098ea12c69d7175ded971435e55c18dd9dd5f29cc5249ac"

    http_archive(
        name = "rules_jvm_external",
        sha256 = RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG),
    )

    DAGGER_TAG = "2.46.1"

    DAGGER_SHA = "bbd75275faa3186ebaa08e6779dc5410741a940146d43ef532306eb2682c13f7"

    http_archive(
        name = "bazel_common_dagger",
        sha256 = DAGGER_SHA,
        strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
        url = "https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG,
    )

def _kotlin():
    RULES_KOTLIN_VERSION = "1.8.1"

    RULES_KOTLIN_SHA = "a630cda9fdb4f56cf2dc20a4bf873765c41cf00e9379e8d59cd07b24730f4fde"

    http_archive(
        name = "io_bazel_rules_kotlin",
        sha256 = RULES_KOTLIN_SHA,
        urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin_release.tgz" % RULES_KOTLIN_VERSION],
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

def bazel_common_dependencies():
    #_proto
    _maven()
    _kotlin()
