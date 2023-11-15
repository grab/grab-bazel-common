load(
    "@grab_bazel_common//toolchains:toolchains.bzl",
    "register_common_toolchains",
    _buildifier_version = "buildifier_version",
)

# Internal
load("@grab_bazel_common//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")
load("@grab_bazel_common//tools/buildifier:defs.bzl", "BUILDIFIER_DEFAULT_VERSION")
load("@grab_bazel_common//android/tools:defs.bzl", "android_tools")

# Dagger
load("@bazel_common_dagger//:workspace_defs.bzl", "DAGGER_ARTIFACTS", "DAGGER_REPOSITORIES")

# Rules Jvm External
load("@rules_jvm_external//:defs.bzl", "maven_install")

# Kotlin
load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "kotlinc_version")

# Proto
# load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

# Setup android databinding compilation and optionally use patched android tools jar
def _android(patched_android_tools):
    native.bind(
        name = "databinding_annotation_processor",
        actual = "@grab_bazel_common//tools/android:compiler_annotation_processor",
    )
    if patched_android_tools:
        android_tools()

def _kotlin():
    kotlin_repositories(
        compiler_release = kotlinc_version(
            release = "1.8.10",
            sha256 = "4c3fa7bc1bb9ef3058a2319d8bcc3b7196079f88e92fdcd8d304a46f4b6b5787",
        ),
    )
    native.register_toolchains("//:kotlin_toolchain")

def bazel_common_setup(
        patched_android_tools = True,
        buildifier_version = BUILDIFIER_DEFAULT_VERSION,
        pinned_maven_install = True):
    #rules_proto_dependencies()
    #rules_proto_toolchains()

    register_common_toolchains(
        buildifier = _buildifier_version(
            version = buildifier_version,
        ),
    )

    native.register_toolchains("@rules_android_lint//toolchains:android_lint_default_toolchain")

    repo_name = "bazel_common_maven"
    maven_install_json = "@grab_bazel_common//:%s_install.json" % repo_name if pinned_maven_install else None

    maven_install(
        name = repo_name,
        artifacts = DAGGER_ARTIFACTS + [
            "com.android.tools.lint:lint:31.0.2",
            "com.google.guava:guava:29.0-jre",
            "com.google.auto:auto-common:0.10",
            "com.google.auto.service:auto-service:1.0-rc6",
            "com.google.protobuf:protobuf-java:3.6.0",
            "com.google.protobuf:protobuf-java-util:3.6.0",
            "io.reactivex.rxjava3:rxjava:3.0.12",
            "com.squareup.moshi:moshi-kotlin:1.14.0",
            "com.squareup.okio:okio-jvm:3.2.0",
            "com.squareup:javapoet:1.13.0",
            "com.github.ajalt:clikt:2.8.0",
            "org.ow2.asm:asm:6.0",
            "org.ow2.asm:asm-tree:6.0",
            "xmlpull:xmlpull:1.1.3.1",
            "net.sf.kxml:kxml2:2.3.0",
            "com.squareup.moshi:moshi:1.11.0",
            "org.jetbrains.kotlin:kotlin-parcelize-compiler:1.8.10",
            "org.jetbrains.kotlin:kotlin-parcelize-runtime:1.8.10",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
            "com.github.tschuchortdev:kotlin-compile-testing:1.5.0",
            "com.google.android.material:material:1.2.1",
            "javax.inject:javax.inject:1",
            "junit:junit:4.13",
            "org.json:json:20210307",
        ],
        repositories = DAGGER_REPOSITORIES + [
            "https://jcenter.bintray.com/",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
        strict_visibility = True,
        maven_install_json = maven_install_json,
        fetch_sources = True,
    )

    maven_install(
        name = "rules_android_lint_deps",
        artifacts = [
            # Testing
            "org.assertj:assertj-core:3.24.2",
            "junit:junit:4.13.2",
            # Worker Dependencies
            "com.squareup.moshi:moshi:1.15.0",
            "com.squareup.moshi:moshi-kotlin:1.15.0",
            "com.squareup.okio:okio-jvm:3.6.0",
            "io.reactivex.rxjava3:rxjava:3.1.8",
            "com.xenomachina:kotlin-argparser:2.0.7",
            # Lint Dependencies
            "com.android.tools.lint:lint:31.3.0-alpha09",
            "com.android.tools.lint:lint-api:31.3.0-alpha09",
            "com.android.tools.lint:lint-checks:31.3.0-alpha09",
            "com.android.tools.lint:lint-model:31.3.0-alpha09",
        ],
        #maven_install_json = "@rules_android_lint//:maven_install.json",
        repositories = [
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )

    _android(patched_android_tools)
    _kotlin()
