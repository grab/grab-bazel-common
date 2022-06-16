workspace(name = "grab_bazel_common")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "3.3"

RULES_JVM_EXTERNAL_SHA = "d85951a92c0908c80bd8551002d66cb23c3434409c814179c0ff026b53544dab"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

DAGGER_TAG = "2.37"

DAGGER_SHA = "0f001ed38ed4ebc6f5c501c20bd35a68daf01c8dbd7541b33b7591a84fcc7b1c"

http_archive(
    name = "dagger",
    sha256 = DAGGER_SHA,
    strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
    url = "https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG,
)

load("@dagger//:workspace_defs.bzl", "DAGGER_ARTIFACTS", "DAGGER_REPOSITORIES")
load("@//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = DAGGER_ARTIFACTS + GRAB_BAZEL_COMMON_ARTIFACTS + [
        "androidx.databinding:databinding-adapters:3.4.2",
        "androidx.databinding:databinding-common:3.4.2",
        "androidx.databinding:databinding-runtime:3.4.2",
        "androidx.annotation:annotation:1.1.0",
        "com.github.tschuchortdev:kotlin-compile-testing:1.3.1",
        "com.google.android.material:material:1.2.1",
        "javax.inject:javax.inject:1",
        "junit:junit:4.13",
        "org.json:json:20210307",
    ],
    repositories = DAGGER_REPOSITORIES + [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
    ],
    strict_visibility = True,
)

RULES_KOTLIN_VERSION = "1.6.0-RC-2"

RULES_KOTLIN_SHA = "88d19c92a1fb63fb64ddb278cd867349c3b0d648b6fe9ef9a200b9abcacd489d"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = RULES_KOTLIN_SHA,
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin_release.tgz" % RULES_KOTLIN_VERSION],
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")

kotlin_repositories()

load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")

kt_register_toolchains()

android_sdk_repository(
    name = "androidsdk",
)

load("@grab_bazel_common//:workspace_defs.bzl", "ANDROID_TOOLS_BUILD_FILE")

new_local_repository(
    name = "android_tools",
    build_file_content = ANDROID_TOOLS_BUILD_FILE,
    path = ".",
)

load("@grab_bazel_common//toolchains:toolchains.bzl", "register_common_toolchains")

register_common_toolchains()
