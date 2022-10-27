load(
    "@grab_bazel_common//toolchains:toolchains.bzl",
    "register_common_toolchains",
    _buildifier_version = "buildifier_version",
)
load("@grab_bazel_common//tools/buildifier:defs.bzl", "BUILDIFIER_DEFAULT_VERSION")
load("@grab_bazel_common//android/tools:defs.bzl", "android_tools")
load("@bazel_common_dagger//:workspace_defs.bzl", "DAGGER_ARTIFACTS", "DAGGER_REPOSITORIES")
load("@rules_jvm_external//:defs.bzl", "maven_install")

# load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

# Setup android databinding compilation and optionally used patched android tools jar
def _android(patched_android_tools):
    native.bind(
        name = "databinding_annotation_processor",
        actual = "@grab_bazel_common//tools/android:compiler_annotation_processor",
    )
    if patched_android_tools:
        android_tools()

def bazel_common_initialize(
        patched_android_tools = True,
        buildifier_version = BUILDIFIER_DEFAULT_VERSION):
    #rules_proto_dependencies()
    #rules_proto_toolchains()

    register_common_toolchains(
        buildifier = _buildifier_version(
            version = buildifier_version,
        ),
    )

    maven_install(
        name = "bazel_common_maven",
        artifacts = DAGGER_ARTIFACTS + [
            "com.google.guava:guava:29.0-jre",
            "com.google.auto:auto-common:0.10",
            "com.google.auto.service:auto-service:1.0-rc6",
            "com.google.protobuf:protobuf-java:3.6.0",
            "com.google.protobuf:protobuf-java-util:3.6.0",
            "com.squareup:javapoet:1.13.0",
            "com.github.ajalt:clikt:2.8.0",
            "org.ow2.asm:asm:6.0",
            "org.ow2.asm:asm-tree:6.0",
            "xmlpull:xmlpull:1.1.3.1",
            "net.sf.kxml:kxml2:2.3.0",
            "com.squareup.moshi:moshi:1.11.0",
            "org.jetbrains.kotlin:kotlin-parcelize-compiler:1.6.10",
            "org.jetbrains.kotlin:kotlin-parcelize-runtime:1.6.10",
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

    _android(patched_android_tools)
