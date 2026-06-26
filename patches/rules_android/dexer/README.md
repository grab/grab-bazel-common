# dexer

## Clean-build determinism context

This group owns the DexBuilder part of the clean-build cache-key issue found
while validating Bazel 8 rules_android against `bazel-playground-android`.

The reproducer is two clean `//app:app-gps-pax-debug.apk` builds with:

- the same source tree, Bazel binary, SDK, NDK, and flags
- different Bazel output roots
- separate empty action disk caches
- the same repository cache
- `--execution_log_json_file` enabled for both builds

Before this fix, same-key `DexBuilder` actions could produce different dex
archive bytes. The concrete symptom was unstable
`META-INF/metadata/synthetic-contexts.map` content when duplicate class bytes
from different jars hit the persistent `CompatDexBuilder` worker cache. Which
jar sees the worker-cache miss depends on action scheduling, so the failure can
be intermittent and may disappear in a single local run.

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

Patches for the D8 dex archive builder.

- `cache_dexbuilder_synthetic_context.patch` — makes the persistent `CompatDexBuilder`
  worker cache store D8 synthetic-context metadata together with dex bytes. Without this,
  duplicate class bytes from different jars can hit the worker cache and return only the dex
  bytes, dropping `META-INF/metadata/synthetic-contexts.map` entries for that class. Which jar
  sees the cache miss depends on action scheduling, so two clean builds can produce different
  dex archive bytes and downstream cache keys even when the build inputs are identical.
- `clean_dex_splitter_output_dir.patch` — clears the declared `dexsplits/<target>` tree
  artifact before `DexFileSplitter` creates `N.shard.zip` files. The splitter opens shard zips
  with `CREATE_NEW`; if a retry or interrupted build leaves stale shard files in the output tree,
  a later run can fail near the end with `FileAlreadyExistsException`. Cleaning the declared
  output directory before writing also prevents stale higher-numbered shard files from being
  merged into the final APK.
