def _buildifier_toolchain_impl(ctx):
  toolchain_info = platform_common.ToolchainInfo(
    binary = ctx.executable.binary,
  )
  return [toolchain_info]

_buildifier_toolchain = rule(
  implementation = _buildifier_toolchain_impl,
  attrs = {
    "binary": attr.label(
      allow_single_file = True,
      executable = True,
      mandatory = True,
      cfg = "exec",
      doc = "Buildifier binary executable"
    )
  }
)

def buildifier_toolchain(
  os, arch
):
  name = "buildifier_{os}_{arch}".format(
    os = os,
    arch = arch,
  )
  _buildifier_toolchain(
    name = name,
    binary = "@{name}//file:buildifier".format(
      name = name,
    )
  )

  if os == "darwin":
    os = "macos"
  if arch == "amd64":
    arch = "x86_64"

  native.toolchain(
    name = name + "_toolchain",
    exec_compatible_with = [
      "@platforms//os:{os}".format(
        os = os,
      ),
      "@platforms//cpu:{arch}".format(
        arch = arch,
      ),
    ],
    toolchain_type = "@grab_bazel_common//tools/buildifier:toolchain_type",
    toolchain = name,
  )
