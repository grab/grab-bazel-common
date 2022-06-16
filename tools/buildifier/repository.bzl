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

load(":defs.bzl", "BUILDIFIER_RELEASE_URL_TEMPLATE")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

def _buildifier_repository_impl(repository_ctx):
    attr = repository_ctx.attr

    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )

    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        substitutions = {
            "{{.BuildifierSupportedOS}}": "[\"{}\"]".format(
                "\",\"".join(attr.supported_os),
            ),
            "{{.BuildifierSupportedArch}}": "[\"{}\"]".format(
                "\",\"".join(attr.supported_arch),
            ),
        },
        executable = False,
    )

_buildifier_repository = repository_rule(
    implementation = _buildifier_repository_impl,
    attrs = {
        "supported_os": attr.string_list(
            allow_empty = False,
            mandatory = True,
        ),
        "supported_arch": attr.string_list(
            allow_empty = False,
            mandatory = True,
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.bazelbuild_buildtools_buildifier.bazel.tpl",
        ),
    },
)

def buildifier_repository(
        name,
        supported_os,
        supported_arch,
        version):
    for os in supported_os:
        for arch in supported_arch:
            http_file(
                name = "buildifier_{os}_{arch}".format(
                    os = os,
                    arch = arch,
                ),
                urls = [
                    BUILDIFIER_RELEASE_URL_TEMPLATE.format(
                        version = version,
                        os = os,
                        arch = arch,
                    ),
                ],
                downloaded_file_path = "buildifier",
                executable = True,
            )

    _buildifier_repository(
        name = name,
        supported_os = supported_os,
        supported_arch = supported_arch,
    )
