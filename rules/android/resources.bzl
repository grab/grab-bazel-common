load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")
load("@grab_bazel_common//rules/android/private:resource_merger.bzl", "resource_merger")

def _calculate_output_files(name, all_resources):
    """
    Resource merger would merge source resource files and write to a merged directory. Bazel needs to know output files in advance, so this
    method tries to predict the output files so we can register them as predeclared outputs.

    Args:
        all_resources: All resource files sorted based on priority with higher priority appearing first.
    """
    outputs = []

    # Multiple res folders root can contain same file name of resource, prevent creating same outputs by storing normalized resource paths
    # eg: `res/values/strings.xml`
    normalized_res_paths = {}

    for file in all_resources:
        res_name_and_dir = file.split("/")[-2:]  # ["values", "values.xml"] etc
        res_dir = res_name_and_dir[0]
        res_name = res_name_and_dir[1]
        if "values" in res_dir:
            # Resource merging merges all values files into single values.xml file.
            normalized_res_path = "%s/out/res/%s/values.xml" % (name, res_dir)
        else:
            normalized_res_path = "%s/out/res/%s/%s" % (name, res_dir, res_name)

        if normalized_res_path not in normalized_res_paths:
            normalized_res_paths[normalized_res_path] = normalized_res_path
            outputs.append(normalized_res_path)
    return outputs

def build_resources(
        name,
        resource_files,
        resources,
        res_values):
    """
    Calculates and returns resource_files either generated, merged or just the source ones based on parameters given. When `resources` are
    declared and it has multiple resource roots then all those roots are merged into single directory and contents of the directory are returned.
    Conversely if resource_files are used then sources are returned as is. In both cases, generated resources passed via res_values are
    accounted for.
    """
    generated_resources = []
    res_value_strings = res_values.get("strings", default = {})
    if len(res_value_strings) != 0:
        generated_resources = res_value(
            name = name + "_res_value",
            strings = res_value_strings,
        )

    if (len(resources) != 0 and len(resource_files) != 0):
        fail("Either resources or resource_files should be specified but not both")

    if (len(resources) != 0):
        # Resources are passed with the new format
        # Merge sources and return the merged result

        if (len(resources) == 1):
            resource_dir = resources.keys()[0]
            return native.glob(
                include = [resource_dir + "/**"],
                exclude = ["**/.DS_Store"],
            ) + generated_resources

        source_sets = []  # Source sets in the format res_dir::manifest
        all_resources = []
        all_manifests = []

        for resource_dir in resources.keys():
            resource_dict = resources.get(resource_dir)
            all_resources.extend(
                native.glob(
                    include = [resource_dir + "/**"],
                    exclude = ["**/.DS_Store"],
                ),
            )

            manifest = resource_dict.get("manifest", "")
            if manifest != "":
                all_manifests.append(manifest)

            source_sets.append("%s::%s" % (resource_dir, manifest))

        merge_target_name = name + "_res"
        merged_resources = _calculate_output_files(merge_target_name, all_resources)
        resource_merger(
            name = merge_target_name,
            source_sets = source_sets,
            resources = all_resources,
            manifests = all_manifests,
            merged_resources = merged_resources,
        )
        return merged_resources + generated_resources
    else:
        return resource_files + generated_resources
