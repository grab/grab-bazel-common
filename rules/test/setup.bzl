load("@rules_jvm_external//:defs.bzl", "maven_install")

def bazel_common_test_maven(pinned_maven_install = True):
    repo_name = "bazel_common_test_maven"
    maven_install_json = "@grab_bazel_common//:%s_install.json" % repo_name if pinned_maven_install else None

    maven_install(
        name = repo_name,
        artifacts = [
            "com.google.dagger:dagger:2.52",
            "com.google.dagger:dagger-compiler:2.52",
        ],
        repositories = [
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
        strict_visibility = True,
        maven_install_json = maven_install_json,
        fetch_sources = True,
    )
