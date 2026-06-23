# desugar

## Clean-build determinism and min-SDK context

This group owns the Desugar part of the clean-build cache-key issue found while
validating Bazel 8 rules_android against `bazel-playground-android`.

The reproducer is two clean `//app:app-gps-pax-debug.apk` builds with:

- the same source tree, Bazel binary, SDK, NDK, and flags
- different Bazel output roots
- separate empty action disk caches
- the same repository cache
- `--execution_log_json_file` enabled for both builds

The root Desugar parity gap was that rules_android was not propagating the
Android binary min SDK into Desugar actions. Bazel 7 passed
`android_binary.min_sdk_version` into both the binary's own jar desugar action
and the transitive dex/desugar aspect. In rules_android, the equivalent Starlark
code existed, but `min_sdk_version.clamp` returned `0` and `min_sdk_version.get`
ignored the propagated build setting, so `--min_sdk_version` was omitted or fell
back to the depot floor. Our macro now passes the derived min SDK value to native
`android_binary`, and the patches below restore the rules_android propagation
path.

The min-SDK propagation validation is an `aquery` over Desugar actions:

```sh
bazel aquery 'mnemonic("Desugar", deps(//app:app-gps-pax-debug))' \
  --include_commandline --output=textproto
```

Before this fix, `bazel-playground-android` showed 1 Desugar action at min SDK
24 and 1158 actions at min SDK 23. After this fix, all 1159 Desugar actions
carry `--min_sdk_version 24`.

The validation command is:

```sh
cd <workspace>/tools_android
bazelisk --output_user_root=/private/tmp/cache-validator-bazel run \
  //cache_validator:cache-validator -- \
  --color=never --root-cause \
  /private/tmp/cache-key-check/build1-exec.json \
  /private/tmp/cache-key-check/build2-exec.json
```

After applying the min-SDK propagation patches, the expected clean-build
comparison result is:

```text
Total changed actions: 0
Selected root files: 0
```

We also validated two clean playground APK builds with the earlier Desugar
output-normalization experiments disabled together. The comparison still
reported `Total changed actions: 0`, so this group keeps the min-SDK propagation
fixes and does not carry additional Desugar output-normalization patches.

## Multiplex Desugar worker context

Bazel 7's native Android `DexArchiveAspect` used the same dex/desugar execution
requirements helper for both `DexBuilder` and `Desugar`. With
`--persistent_multiplex_android_dex_desugar`, both actions advertised
`supports-multiplex-workers`.

In rules_android 0.7.1, `rules/dex.bzl` already mirrors that behavior for
`DexBuilder`, but `rules/desugar.bzl` only advertised `supports-workers`. In our
bazelrc we cap `--worker_max_instances=Desugar=1` to avoid worker-pool CPU
thrashing. Without the multiplex execution requirement, that cap means only one
Desugar request can run at a time and the build UI shows many queued
`[Sched] Desugaring ...` actions.

- `multiplex_desugar_worker.patch` — adds
  `supports-multiplex-workers` to `Desugar` actions when
  `ctx.fragments.android.persistent_multiplex_android_dex_desugar` is enabled,
  matching Bazel 7's flag-gated behavior and rules_android's current
  `DexBuilder` behavior.

Desugaring is slow. R class jars contain only static final int constants, no Java 8+ bytecode, so desugaring them is pure overhead. These patches skip it.

- `skip_binary_r_jar_desugaring_flags.patch` — adds `//rules/flags:desugar_resources_jar` bool_flag.
- `skip_binary_r_jar_desugaring_attrs.patch` — wire the flag as private attr on android_binary.
- `skip_binary_r_jar_desugaring_impl.patch` — in android_binary impl, when flag=false and jar is `_resources.jar`, pass through untransformed.
- `skip_r_jar_desugaring.patch` — per-library: drop `AndroidIdeInfo.resource_jar` from the desugar aspect's jar set. Same idea but for every lib's R.jar (matters a lot when `link_library_resources=false` produces large transitive R jars).

Flip `common --@rules_android//rules/flags:desugar_resources_jar=false` in bazelrc to enable the binary-side skip.

- `propagate_min_sdk_to_desugar.patch` — restores Bazel 7 min SDK propagation for
  desugar/dex actions. It allows main-repo `android_binary.min_sdk_version`,
  enables rules_android's intended min-SDK clamp, and makes dex/desugar actions
  read the propagated `//rules/flags:min_sdk_version` setting instead of always
  using the depot floor.

- `propagate_min_sdk_through_split_transition.patch` — carries the propagated
  min-SDK build setting through the Android split transition used by
  `android_binary.deps`, so the dex/desugar aspect on transitive dependencies
  sees the same min SDK as the binary.

- `dex_aar_import_resources_jar.patch` — restores dex processing of `AndroidIdeInfo.resource_jar.class_jar` for `aar_import` and `android_library` targets.

  **Problem solved:** `rules_android` 0.7.1 introduced a strict check in `dex.bzl` (`_to_dexed_classpath`) that fails the build if any jar appears in `transitive_runtime_jars_for_archive` without a corresponding entry in `dex_archives_dict`. Two target kinds are affected:

  - **`aar_import`**: AARs like `androidx.databinding:databinding-adapters` generate a `*_resources.jar` containing real generated binding code. This jar is created inline as a `JavaInfo` dep inside `aar_import`'s rule implementation — it is never a standalone build target — so the `dex_desugar_aspect` never visits it and no dex archive is ever created for it.

  - **`android_library` (including `kt_android_library` wrappers)**: The R class jar is provided via `AndroidIdeInfo.resource_jar`. `_get_library_r_jars` in `impl.bzl` is supposed to filter it from `transitive_runtime_jars_for_archive` via `AndroidLibraryResourceClassJarProvider`, but `kt_android_library` wrappers do not propagate this provider, so the jar slips through the filter and reaches `_to_dexed_classpath` without a dex archive entry.

  `skip_r_jar_desugaring.patch` removed all `AndroidIdeInfo.resource_jar` handling from the aspect. This patch adds it back selectively for `aar_import` and `android_library`, so the vast majority of R jar desugaring overhead is still avoided while the affected jars are correctly dexed.
