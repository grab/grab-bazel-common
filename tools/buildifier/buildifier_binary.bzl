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
"""
Rule for running buildifier via toolchain config
"""

def _buildifier_impl(ctx):
    _buildifier_binary = ctx.toolchains["@buildifier_toolchains//:buildifier_toolchain_type"].binary
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
    toolchains = ["@buildifier_toolchains//:buildifier_toolchain_type"],
    executable = True,
)
