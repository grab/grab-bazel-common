"""
A rule to collect all dependencies' Android resource jars that is made available
only on compile time and get them loaded during runtime

It works by iterating through the transitive compile time jars of all given 
target and retrieving jar files that ends with `_resources.jar` into a JavaInfo
which can then be loaded during runtime.
"""

def _runtime_resources_impl(ctx):
    deps = ctx.attr.deps

    resources_java_infos = {}
    for target in deps:
        if (JavaInfo in target):
            for jar in target[JavaInfo].transitive_compile_time_jars.to_list():
                if (jar.basename.endswith("_resources.jar")):
                    resources_java_infos[jar.path] = JavaInfo(
                        output_jar = jar,
                        compile_jar = jar,
                    )

    resources_java_infos = list(resources_java_infos.values())
    merged_java_infos = java_common.merge(resources_java_infos)

    return [
        merged_java_infos,
    ]

runtime_resources = rule(
    implementation = _runtime_resources_impl,
    attrs = {
        "deps": attr.label_list(),
    },
)
