load("@grab_bazel_common//rules/android:android_binary.bzl", _android_binary = "android_binary")
load("@grab_bazel_common//rules/android:android_library.bzl", _android_library = "android_library")
load("@grab_bazel_common//rules/android:android_instrumentation.bzl", _android_instrumentation_binary = "android_instrumentation_binary")
load("@grab_bazel_common//rules/android:test.bzl", _android_unit_test = "android_unit_test")
load(
    "@grab_bazel_common//rules/kotlin:kotlin.bzl",
    _kt_compiler_plugin = "kt_compiler_plugin",
    _kt_jvm_library = "kt_jvm_library",
)
load("@grab_bazel_common//rules/kotlin:test.bzl", _kotlin_test = "kotlin_test")

# Android
android_binary = _android_binary
android_library = _android_library
android_instrumentation_binary = _android_instrumentation_binary
android_unit_test = _android_unit_test

# Kotlin
kt_jvm_library = _kt_jvm_library
kt_compiler_plugin = _kt_compiler_plugin
kotlin_test = _kotlin_test
