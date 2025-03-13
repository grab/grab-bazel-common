load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def _maven():
    DAGGER_TAG = "2.46.1"

    DAGGER_SHA = "bbd75275faa3186ebaa08e6779dc5410741a940146d43ef532306eb2682c13f7"

    http_archive(
        name = "bazel_common_dagger",
        sha256 = DAGGER_SHA,
        strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
        url = "https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG,
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

def bazel_common_dependencies():
    _maven()
    _detekt()
