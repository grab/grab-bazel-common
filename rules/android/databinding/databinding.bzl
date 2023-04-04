load("@grab_bazel_common//tools/databinding:databinding.bzl", _kt_db_android_library = "kt_db_android_library")

DATABINDING_DEPS = [
    "@maven//:androidx_databinding_databinding_adapters",
    "@maven//:androidx_databinding_databinding_common",
    "@maven//:androidx_databinding_databinding_runtime",
    "@maven//:androidx_annotation_annotation",
    "@maven//:androidx_databinding_viewbinding",
]

# TODO Move tools/databinding/databinding.bzl and rule related code to this package
kt_db_android_library = _kt_db_android_library
