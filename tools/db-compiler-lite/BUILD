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
        "@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8",
        "@maven//:androidx_databinding_databinding_common",
        "@maven//:com_github_ajalt_clikt",
        "@maven//:com_squareup_javapoet",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:net_sf_kxml_kxml2",
        "@maven//:xmlpull_xmlpull",
    ],
)

grab_kt_jvm_test(
    name = "db-compiler-lite-test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        ":db-compiler-lite-lib",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@maven//:junit_junit",
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
