def dagger_rules(repo_name = "@maven"):
    native.java_library(
        name = "dagger",
        exported_plugins = [":dagger-compiler"],
        visibility = ["//visibility:public"],
        exports = [
            "%s//:com_google_dagger_dagger" % repo_name,
            "%s//:javax_inject_javax_inject" % repo_name,
        ],
    )

    native.java_plugin(
        name = "dagger-compiler",
        generates_api = 1,
        processor_class = "dagger.internal.codegen.ComponentProcessor",
        deps = [
            "%s//:com_google_dagger_dagger_compiler" % repo_name,
            "%s//:com_google_dagger_dagger" % repo_name,  # FIX: Added for Bazel 8
            "%s//:com_google_dagger_dagger_spi" % repo_name,  # FIX: Added for Bazel 8
        ],
    )

    native.java_library(
        name = "dagger-producers",
        visibility = ["//visibility:public"],
        exports = [
            ":dagger",
            "%s//:com_google_dagger_dagger_producers" % repo_name,
        ],
    )

    native.java_library(
        name = "dagger-spi",
        visibility = ["//visibility:public"],
        exports = [
            "%s//:com_google_dagger_dagger_spi" % repo_name,
        ],
    )