load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

TYPES = [
  struct(
    os = "linux",
    arch = "amd64",
  ),
  struct(
    os = "linux",
    arch = "arm64",
  ),
  struct(
    os = "darwin",
    arch = "amd64",
  ),
  struct(
    os = "darwin",
    arch = "arm64",
  ),
]

BUILDIFIER_NAME = "buildifier"

def _buildifier_repository_impl(repository_ctx):
  content = 'load("@grab_bazel_common//tools/buildifier:toolchain.bzl", "buildifier_toolchain")'
  for type in TYPES:
    content += """
buildifier_toolchain(
  os = "{os}",
  arch = "{arch}",
)
""".format(
  os = type.os,
  arch = type.arch,
)
  repository_ctx.file(
    "BUILD.bazel", 
    content = content,
  )

_buildifier_repository = repository_rule(
  implementation = _buildifier_repository_impl,
)

def buildifier_register_toolchains(
  name = "buildifier_toolchains",
  version = "5.1.0",
):
  toolchain_labels = []
  for type in TYPES:
    http_file(
      name = "{name}_{os}_{arch}".format(
        name = BUILDIFIER_NAME, 
        os = type.os, 
        arch = type.arch
      ),
      urls = [
        "https://github.com/bazelbuild/buildtools/releases/download/{version}/{name}-{os}-{arch}".format(
          version = version,
          name = BUILDIFIER_NAME,
          os = type.os,
          arch = type.arch,
        )
      ],
      downloaded_file_path = BUILDIFIER_NAME,
      executable = True,
    )
    toolchain_labels.append(
      "@buildifier_toolchains//:buildifier_{os}_{arch}_toolchain".format(
        os = type.os,
        arch = type.arch,
      )
    )

  _buildifier_repository(
    name = name,
  )

  native.register_toolchains(*toolchain_labels)
