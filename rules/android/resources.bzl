load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")

def build_resources(name, resource_files, res_values):
    """
    Returns list of source `resource_files` and generated `resource_files` from the `res_value` macro
    """
    return resource_files + res_value(
        name = name + "_res_value",
        strings = res_values.get("strings", default = {}),
    )
