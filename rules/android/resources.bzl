load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load("@grab_bazel_common//rules/android/private:resource_merger.bzl", "resource_merger")
load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")

def _calculate_output_files(name, all_resources):
    """
    Calculates the output file paths Bazel would create from the given resources

    Resource merger would merge resources and write to a single merged directory. Bazel needs to know output files in advance, so this
    method tries to predict the output files so we can register them as predeclared outputs. We can't use `actions.declare_dir` since
    `android_binary|library` does not accept a directory as input.

    Args:
        name: Name of the target the does the merging
        all_resources: All resource_files/assets sorted based on priority with higher priority appearing first
    """

    # Multiple res folders root can contain same file name of resource, dedup them using a dict
    outputs = {}

    # Two different resource buckets can contain different file extensions but same file name. For example icon.png and icon.webp, based on
    # what comes first, only one file will be present after merging. Track such cases and remove them from outputs.
    output_file_names = {}

    for file in all_resources:
        res_name_and_dir = file.split("/")[-2:]  # ["values", "values.xml"] etc
        res_dir = res_name_and_dir[0]
        res_name = res_name_and_dir[1]
        res_name_no_ext = res_name.split(".")[0]  # Just the file name "values"

        out_res_dir = "/res" if res_dir != "assets" else ""  # Merged assets folder will not have a parent res dir
        if "values" in res_dir:
            # Resource merging merges all values files into single values.xml file.
            normalized_res_path = "%s/out%s/%s/values.xml" % (name, out_res_dir, res_dir)
            outputs[normalized_res_path] = normalized_res_path
        else:
            normalized_res_path = "%s/out%s/%s/%s" % (name, out_res_dir, res_dir, res_name)
            if res_name_no_ext not in output_file_names:
                # Dedupe with resource name for any resource that is of type `values`.
                outputs[normalized_res_path] = normalized_res_path
                output_file_names[res_name_no_ext] = res_name_no_ext

    return list(outputs.values())

def _res_glob(includes = []):
    """
    `glob` wrapper to exclude common problematic files and glob a directory as a whole using /**

    Args:
        includes: List of string directory path
    """
    return native.glob(
        include = [include + "/**" for include in includes],
        exclude = ["**/.DS_Store"],
    )

def _generate_resources(res_value_strings, name):
    if res_value_strings:
        return res_value(name = name + "_res_value", strings = res_value_strings)
    else:
        return []

def _validate_resource_parameters(resource_sets, resource_files):
    if len(resource_files) != 0:
        fail("resouce_files is deprecrated, migrate to using resources format. See resources.bzl")

def build_resources(
        name,
        is_binary,
        namespace,
        manifest,
        resource_files,
        resource_sets,
        res_values):
    """
    Merge multiple source sets and return a single merged output

    Calculates and returns resource_files, assets and manifest either generated, merged or just the source ones based on parameters given.
    When `resource_sets` are declared and it has multiple resource roots then all those roots are merged into single directory and
    contents of the directory are returned.
    Using `resource_files` is an error and not recommended.

    Args:
        name: The name of the resource merger target
        is_binary: Whether the target is android_binary
        manifest: The default primary manifest.
        namespace: The namespace of this target
        resource_files: Default bazel expected Android resource_files format (deprecated)
        resource_sets: Dict of various resources, manifest and assets keyed by a source set name
            For example
            "main": {
                "res": "src/main/res",
                "manifest": "src/main/AndroidManifest.xml",
                "assets": "src/main/assets",
            }
        res_values: Dict of various resources keyed by their type to be generated during build. Uses res_value

    Returns:
    - resources: Merged output containing resources, assets, assets_dir and manifest.
    """
    generated_resources = _generate_resources(res_values.get("strings", default = {}), name)
    _validate_resource_parameters(resource_sets, resource_files)

    if (len(resource_sets) != 0):
        # Resources are passed with the new format, merge sources and return the merged result
        if (len(resource_sets) == 1):
            # Only only source set is provided, hence glob it and return as-is.
            resource_set_name = resource_sets.keys()[0]
            resource_dict = resource_sets.get(resource_set_name)

            resource_dir = resource_dict.get("res", None)
            resources = _res_glob([resource_dir]) if resource_dir else []

            asset_dir = resource_dict.get("assets", None)
            assets = _res_glob([asset_dir]) if asset_dir else []
            has_assets = len(assets) != 0

            return struct(
                res = resources + generated_resources,
                assets = assets if has_assets else None,
                asset_dir = asset_dir if has_assets else None,
                manifest = resource_dict.get("manifest", manifest),
            )
        else:
            source_sets = []  # Source sets args in the res_dir:assets:manifest format
            all_resources = []
            all_assets = []
            all_manifests = []

            for resource_set_name in resource_sets.keys():
                resource_dict = resource_sets.get(resource_set_name)

                resource_dir = resource_dict.get("res", "")
                if resource_dir != "":
                    all_resources.extend(_res_glob([resource_dir]))

                asset_dir = resource_dict.get("assets", "")
                if asset_dir != "":
                    all_assets.extend(_res_glob([asset_dir]))

                manifest = resource_dict.get("manifest", "")
                if manifest != "":
                    all_manifests.append(manifest)

                source_sets.append("%s:%s:%s" % (resource_dir, asset_dir, manifest))

            merge_target_name = "_" + name + "_res"
            all_resources = utils.to_set(all_resources)
            all_assets = utils.to_set(all_assets)
            merged_resources = _calculate_output_files(merge_target_name, all_resources)
            merged_assets = _calculate_output_files(merge_target_name, all_assets)

            # Outputs that would be produced by merger
            merged_manifest = "%s/_merged/AndroidManifest.xml" % merge_target_name
            asset_dir = "%s/out/assets/" % merge_target_name
            resource_merger(
                name = merge_target_name,
                is_binary = is_binary,
                namespace = namespace,
                source_sets = source_sets,
                resources = all_resources + all_assets,
                manifests = all_manifests,
                merged_manifest = merged_manifest,
                merged_resources = merged_resources + merged_assets,
            )
            return struct(
                res = merged_resources + generated_resources,
                assets = merged_assets if len(merged_assets) != 0 else None,
                asset_dir = asset_dir if len(merged_assets) != 0 else None,
                manifest = merged_manifest,
            )
    else:
        return struct(
            res = resource_files + generated_resources,
            assets = None,
            asset_dir = None,
            manifest = manifest,
        )
