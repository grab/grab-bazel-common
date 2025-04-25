load("@grab_bazel_common//rules:defs.bzl", "kotlin_test")
load(":build_config.bzl", "build_config")

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

config_setting(
    name = "build_config_alternative",
    define_values = {
        "alternate_values": "true",
    },
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
        "SELECT": select({
            ":build_config_alternative": ["alternate value"],
            "//conditions:default": ["default value"],
        }),
    },
)

kotlin_test(
    name = "build-config-test",
    srcs = [
        "src/test/java/com/grab/buildconfig/BuildConfigTest.kt",
    ],
    deps = [
        ":build-config-sample",
        "@maven//:junit_junit",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
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

kotlin_test(
    name = "build-config-replace-default-test",
    size = "small",
    srcs = [
        "src/test/java/com/grab/buildconfig/BuildConfigReplaceDefaultTest.kt",
    ],
    deps = [
        ":build-config-replace-default",
        "@maven//:junit_junit",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
    ],
)
