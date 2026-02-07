# Rules_android setup
load("@rules_android//:defs.bzl", "rules_android_workspace")
load("@rules_android//rules:rules.bzl", "android_sdk_repository")

def rules_android_setup():
    rules_android_workspace()
    android_sdk_repository(name = "androidsdk")

    native.register_toolchains(
        "@rules_android//toolchains/android:android_default_toolchain",
        "@rules_android//toolchains/android_sdk:android_sdk_tools",
    )