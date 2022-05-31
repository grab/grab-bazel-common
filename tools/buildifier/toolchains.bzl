# Copyright 2022 Grabtaxi Holdings PTE LTD (GRAB)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
""""""

load(":defs.bzl", "BUILDIFIER_DEFAULT_TOOLCHAINS", "BuildifierVersionInfo")
load(":repository.bzl", "buildifier_repository")

def _buildifier_toolchain_info_impl(ctx):
  toolchain_info = platform_common.ToolchainInfo(
    binary = ctx.executable.binary,
  )
  return [toolchain_info]

_buildifier_toolchain_info = rule(
  implementation = _buildifier_toolchain_info_impl,
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
  os, 
  arch,
  toolchain_type = "//:buildifier_toolchain_type",
):
  name = "buildifier_{os}_{arch}".format(
    os = os,
    arch = arch,
  )
  _buildifier_toolchain_info(
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
    toolchain_type = toolchain_type,
    toolchain = name,
  )

def buildifier_register_toolchains(
  version = BUILDIFIER_DEFAULT_TOOLCHAINS.version,
  supported_os = BUILDIFIER_DEFAULT_TOOLCHAINS.supported_os,
  supported_arch = BUILDIFIER_DEFAULT_TOOLCHAINS.supported_arch,
):
  name = "buildifier_toolchains"
  toolchain_labels = []
  for os in supported_os:
    for arch in supported_arch:
      toolchain_labels.append(
        "@{name}//:buildifier_{os}_{arch}_toolchain".format(
          name = name,
          os = os,
          arch = arch,
        )
      )
  
  buildifier_repository(
    name = name,
    supported_os = supported_os,
    supported_arch = supported_arch,
    version = version,
  )

  native.register_toolchains(*toolchain_labels)

def define_toolchains(supported_os, supported_arch):
  """
  Defines the toolchain_type and toolchains for buildifier

  Args:
    supported_os: list of supported os (e.g. ["linux", "darwin"])
    supported_arch: list of supported architecture (e.g. ["amd64", "arm64"])
  """
  for os in supported_os:
    for arch in supported_arch:
      buildifier_toolchain(
        os = os,
        arch = arch,
      )
  
  native.toolchain_type(
    name = "buildifier_toolchain_type",
    visibility = ["//visibility:public"],
  )

def buildifier_version(
  version = BUILDIFIER_DEFAULT_TOOLCHAINS.version,
  supported_os = BUILDIFIER_DEFAULT_TOOLCHAINS.supported_os,
  supported_arch = BUILDIFIER_DEFAULT_TOOLCHAINS.supported_arch,
):
  return BuildifierVersionInfo(
    version = version,
    supported_os = supported_os,
    supported_arch = supported_arch,
  )
