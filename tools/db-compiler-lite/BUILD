load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@grab_bazel_common//tools/test:test.bzl", "grab_kt_jvm_test")

kt_jvm_library(
    name = "db-compiler-lite-lib",
    srcs = glob([
        "src/main/java/**/*.kt",
    ]),
    visibility = [
        "//visibility:private",
    ],
    deps = [
        "//:dagger",
        "//tools/worker:worker_lib",
        "@bazel_common_maven//:com_github_ajalt_clikt",
        "@bazel_common_maven//:com_squareup_javapoet",
        "@bazel_common_maven//:com_squareup_moshi_moshi",
        "@bazel_common_maven//:net_sf_kxml_kxml2",
        "@bazel_common_maven//:xmlpull_xmlpull",
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
        "@maven//:androidx_databinding_databinding_common",
    ],
)

grab_kt_jvm_test(
    name = "db-compiler-lite-test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        ":db-compiler-lite-lib",
        "@bazel_common_maven//:junit_junit",
        "@com_github_jetbrains_kotlin//:kotlin-test",
    ],
)

java_binary(
    name = "db-compiler-lite",
    main_class = "com.grab.databinding.stub.MainKt",
    visibility = [
        "//visibility:public",
    ],
    runtime_deps = [
        ":db-compiler-lite-lib",
    ],
)
