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

    DAGGER_TAG = "2.46.1"
    DAGGER_SHA = "bbd75275faa3186ebaa08e6779dc5410741a940146d43ef532306eb2682c13f7"

    http_archive(
        name = "bazel_common_dagger",
        sha256 = DAGGER_SHA,
        strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
        url = "https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG,
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

def _jetifier():
    JETIFIER_SOURCE_SHA = "8ac1c5c2a8681c398883bb2cabc18f913337f165059f24e8c55714e05757761e"

    http_archive(
        name = "jetifier",
        sha256 = JETIFIER_SOURCE_SHA,
        strip_prefix = "rules_jvm_external-5.3/third_party/jetifier",
        urls = ["https://github.com/bazelbuild/rules_jvm_external/archive/refs/tags/5.3.tar.gz"],
        build_file = "@grab_bazel_common//patches/jetifier:BUILD.bazel",
    )

def _rules_java_transitive_deps():
    """Declare transitive deps of rules_java needed before rules_java_dependencies() runs."""
    http_archive(
        name = "bazel_features",
        sha256 = "2cd9e57d4c38675d321731d65c15258f3a66438ad531ae09cb8bb14217dc8572",
        strip_prefix = "bazel_features-1.11.0",
        urls = ["https://github.com/bazel-contrib/bazel_features/releases/download/v1.11.0/bazel_features-v1.11.0.tar.gz"],
    )

def bazel_common_dependencies():
    _rules_java_transitive_deps()
    _android()
    _maven()
    _kotlin()
    _detekt()
    _jetifier()
