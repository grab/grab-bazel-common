load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def _rules_jvm_deps():
    http_archive(
        name = "rules_java",
        urls = [
            "https://github.com/bazelbuild/rules_java/releases/download/7.6.1/rules_java-7.6.1.tar.gz",
        ],
        sha256 = "f8ae9ed3887df02f40de9f4f7ac3873e6dd7a471f9cddf63952538b94b59aeb3",
    )

    rules_license_tag = "1.0.0"
    http_archive(
        name = "rules_license",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_license/releases/download/%s/rules_license-%s.tar.gz" % (rules_license_tag, rules_license_tag),
            "https://github.com/bazelbuild/rules_license/releases/download/%s/rules_license-%s.tar.gz" % (rules_license_tag, rules_license_tag),
        ],
        sha256 = "26d4021f6898e23b82ef953078389dd49ac2b5618ac564ade4ef87cced147b38",
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
    RULES_KOTLIN_VERSION = "1.9.6"

    RULES_KOTLIN_SHA = "3b772976fec7bdcda1d84b9d39b176589424c047eb2175bed09aac630e50af43"

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
    _rules_jvm_deps()
    _rules_java_transitive_deps()
    _maven()
    _kotlin()
    _detekt()
    _jetifier()
