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

BUILDIFIER_DEFAULT_VERSION = "5.1.0"
BUILDIFIER_RELEASE_URL_TEMPLATE = "https://github.com/bazelbuild/buildtools/releases/download/{version}/buildifier-{os}-{arch}"

BuildifierVersionInfo = provider(
    "Provides information about the buildifier release",
    fields = {
        "version": "version of the buildifier release",
        "supported_os": "list of supported os",
        "supported_arch": "list of supported architecture",
    },
)

BUILDIFIER_DEFAULT_TOOLCHAINS = BuildifierVersionInfo(
    version = BUILDIFIER_DEFAULT_VERSION,
    supported_os = ["darwin", "linux"],
    supported_arch = ["arm64", "amd64"],
)

BUILDIFIER_DEFAULT_TOOLCHAIN_CONFIG = {
    "name": "buildifier",
}
