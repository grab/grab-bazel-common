load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")
load("@grab_bazel_common//rules/android/private:resource_merger.bzl", "resource_merger")

def _calculate_output_files(name, all_resources):
    """
    Resource merger would merge source resource files and write to a merged directory. Bazel needs to know output files in advance, so this
    method tries to predict the output files so we can register them as predeclared outputs.

    Args:
        all_resources: All resource files sorted based on priority with higher priority appearing first.
    """

    # Multiple res folders root can contain same file name of resource, dedup them using a dict
    outputs = {}

    # Two different resource buckets can contain different file extensions but same file name. For example icon.png and icon.webp, based on
    # what comes first, only one file will be present after merging. Track such cases and remove them from outputs.
    output_files = {}

    for file in all_resources:
        res_name_and_dir = file.split("/")[-2:]  # ["values", "values.xml"] etc
        res_dir = res_name_and_dir[0]
        res_name = res_name_and_dir[1]
        res_name_no_ext = res_name.split(".")[0]
        if "values" in res_dir:
            # Resource merging merges all values files into single values.xml file.
            normalized_res_path = "%s/out/res/%s/values.xml" % (name, res_dir)
        else:
            normalized_res_path = "%s/out/res/%s/%s" % (name, res_dir, res_name)

        if res_name_no_ext not in output_files:
            outputs[normalized_res_path] = normalized_res_path
            output_files[res_name_no_ext] = res_name_no_ext
    return list(outputs.values())

def build_resources(
        name,
        resource_files,
        resource_sets,
        res_values):
    """
    Calculates and returns resource_files either generated, merged or just the source ones based on parameters given. When `resource_sets` are
    declared and it has multiple resource roots then all those roots are merged into single directory and contents of the directory are returned.
    Conversely if resource_files are used then sources are returned as is. In both cases, generated resources passed via res_values are
    accounted for.

    Args:
        name: The name of the resource merger target
        resource_files: Default bazel expected Android resource_files format
        resource_sets: Dict of various resources, manifest and assets keyed by a source set name
            For example
            "main": {
                "res": "src/main/res",
                "manifest": "src/main/AndroidManifest.xml",
                "assets": "src/main/assets",
            }
        res_values: Dict of various resources keyed by their type to be generated during build. Uses res_value
    """
    generated_resources = []
    res_value_strings = res_values.get("strings", default = {})
    if len(res_value_strings) != 0:
        generated_resources = res_value(
            name = name + "_res_value",
            strings = res_value_strings,
        )
    if (len(resource_sets) != 0 and len(resource_files) != 0):
        fail("Either resources or resource_files should be specified but not both")

    if (len(resource_sets) != 0):
        # Resources are passed with the new format
        # Merge sources and return the merged result

        if (len(resource_sets) == 1):
            resource_set = resource_sets.keys()[0]
            resource_dir = resource_sets.get(resource_set).get("res", None)
            if resource_dir:
                return native.glob(
                    include = [resource_dir + "/**"],
                    exclude = ["**/.DS_Store"],
                ) + generated_resources

        source_sets = []  # Source sets args in the res_dir:assets:manifest format
        all_resources = []
        all_manifests = []

        for resource_set in resource_sets.keys():
            resource_dict = resource_sets.get(resource_set)
            resource_dir = resource_dict.get("res", None)
            if resource_dir:
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
