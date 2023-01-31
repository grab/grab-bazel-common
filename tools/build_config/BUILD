load(":build_config.bzl", "build_config")
load("@grab_bazel_common//tools/test:test.bzl", "grab_kt_jvm_test")

java_binary(
    name = "build_config_generator",
    main_class = "com.grab.buildconfig.MainKt",
    visibility = [
        "//visibility:public",
    ],
    runtime_deps = [
        "//tools/build_config/src/main/java/com/grab/buildconfig",
    ],
)

build_config(
    name = "build-config-sample",
    package_name = "com.grab.buildconfig.sample",
    booleans = {
        "TRUE": "true",
        "FALSE": "false",
        "STRING_BOOLEAN": "test",
    },
    ints = {
        "INT": "0",
    },
    longs = {
        "LONG": "123",
    },
    strings = {
        "SIMPLE_STRING": "Hello",
        "WITH_DOLLAR": "$$$$$$$$$$$$$",
        "SPECIAL_CHARACTERS": "!@#$%^&*()$",
        "FIELD_WITH_ESCAPED_DOLLAR": "\\$ Hello",
        "REWARD_URL": "https://reward.com/hc/%1$s/test",
    },
)

grab_kt_jvm_test(
    name = "build-config-test",
    srcs = [
        "src/test/java/com/grab/buildconfig/BuildConfigTest.kt",
    ],
    deps = [
        ":build-config-sample",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@maven//:junit_junit",
    ],
)

build_config(
    name = "build-config-replace-default",
    package_name = "com.grab.buildconfig.sample",
    strings = {
        "SIMPLE_STRING": "Hello",
        "VERSION_NAME": "1.2.3",
    },
)

grab_kt_jvm_test(
    name = "build-config-replace-default-test",
    size = "small",
    srcs = [
        "src/test/java/com/grab/buildconfig/BuildConfigReplaceDefaultTest.kt",
    ],
    deps = [
        ":build-config-replace-default",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@maven//:junit_junit",
    ],
)
