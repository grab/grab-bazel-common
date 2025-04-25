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

load(
    "@grab_bazel_common//tools/buildifier:buildifier_binary.bzl",
    "buildifier_binary",
)
load(
    "@grab_bazel_common//tools/buildifier:defs.bzl",
    "BUILDIFIER_DEFAULT_TOOLCHAINS",
    "BUILDIFIER_DEFAULT_TOOLCHAIN_CONFIG",
)
load(
    "@grab_bazel_common//tools/buildifier:toolchains.bzl",
    "buildifier_register_toolchains",
    _buildifier_version = "buildifier_version",
)

#exports
buildifier_version = _buildifier_version

def configure_toolchains():
    native.config_setting(
        name = "non_transitive_r_class",
        values = {"define": "nontransitive_r_class=1"},
    )

    buildifier_binary(
        name = "buildifier",
    )

def register_common_toolchains(
        buildifier = BUILDIFIER_DEFAULT_TOOLCHAINS):
    buildifier_register_toolchains(
        version = buildifier.version,
        supported_os = buildifier.supported_os,
        supported_arch = buildifier.supported_arch,
    )

def configure_common_toolchains(
        buildifier = BUILDIFIER_DEFAULT_TOOLCHAIN_CONFIG):
    buildifier_binary(
        name = buildifier["name"],
    )
