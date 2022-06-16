load("@grab_bazel_common//tools/buildifier:toolchains.bzl", "define_toolchains")

define_toolchains(
  supported_os = {{.BuildifierSupportedOS}},
  supported_arch = {{.BuildifierSupportedArch}},
)
