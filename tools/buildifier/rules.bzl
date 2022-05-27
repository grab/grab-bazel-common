def _buildifier_impl(ctx):
  _buildifier_binary = ctx.toolchains["@grab_bazel_common//tools/buildifier:toolchain_type"].binary
  script = ctx.actions.declare_file("buildifier")
  ctx.actions.symlink(
    output = script,
    target_file = _buildifier_binary,
    is_executable = True,
  )

  return [
    DefaultInfo(
      runfiles = ctx.runfiles(files = [_buildifier_binary]),
      executable = script,
    ),
  ]

buildifier_binary = rule(
  implementation = _buildifier_impl,
  toolchains = ["@grab_bazel_common//tools/buildifier:toolchain_type"],
  executable = True,
)
