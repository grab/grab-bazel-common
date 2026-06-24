# resources

## Clean-build determinism context

This group owns the Android resource and manifest parts of the clean-build
cache-key issue found while validating Bazel 8 rules_android against
`bazel-playground-android`.

The reproducer is two clean `//app:app-gps-pax-debug.apk` builds with:

- the same source tree, Bazel binary, SDK, NDK, and flags
- different Bazel output roots
- separate empty action disk caches
- the same repository cache
- `--execution_log_json_file` enabled for both builds

Before these fixes, `tools_android`'s cache validator reported same-key
resource actions with different outputs:

- `MergeManifests` produced byte-different `manifest_merger_log.txt` files
  because the log embedded absolute Bazel execroot paths. The merged manifest
  itself was stable, but the log is also an action output and therefore affects
  cacheability.
- `PackageAndroidResources` produced byte-different `merged.bin` files because
  resource keys were stable but resource source-table IDs/order could depend on
  set/merge insertion order.
- Databinding compiled resource zips could carry copied file mtimes.

The validation command is:

```sh
cd <workspace>/tools_android
bazelisk --output_user_root=/private/tmp/cache-validator-bazel run \
  //cache_validator:cache-validator -- \
  --color=never --root-cause \
  /private/tmp/cache-key-check/build1-exec.json \
  /private/tmp/cache-key-check/build2-exec.json
```

After applying the deterministic-output patch set, the expected result is:

```text
Total changed actions: 0
Selected root files: 0
```

Misc resource pipeline fixes.

- `android_resource_processor_bazel_paths.patch` — databinding needs absolute path for `resInput`. Default passes relative, AP chokes on it in the Bazel sandbox.
- `fix_resource_priority_ordering.patch` — flip `preorder` → `postorder` on resource depsets and reverse dep iteration. aapt2 is last-wins for resource conflicts; this makes first-declared dep win (what BUILD authors expect — `deps = [a, b]` means a's resources beat b's).
- `normalize_databinding_compiled_resources_timestamps.patch` — normalizes copied databinding processed resource file mtimes to `1980-01-01 00:00`. Without this, the compiled resources zip can differ across clean builds even when inputs are semantically identical, which then changes downstream action outputs/cache keys.
- `canonicalize_manifest_merger_log_paths.patch` — strips absolute Bazel execroot prefixes from `manifest_merger_log.txt`. The merged manifest itself is stable, but the log is also an output artifact; without this, same-key `MergeManifests` actions can produce byte-different logs across clean builds with different output roots.
- `sort_resource_source_table.patch` — sorts resource source-table entries and overwritten-source references before writing serialized resource metadata (`merged.bin`). Resource keys were already sorted, but source IDs could still depend on set/merge insertion order, causing same-key `PackageAndroidResources` actions to emit different `merged.bin` bytes.
