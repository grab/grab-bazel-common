# non_transitive_r

Port of Bazel 7's non-transitive R class behavior. Bazel 7's native rules had a global `--android_non_transitive_r_class`; rules_android 0.7 (Starlark) lost it. We rebuild the equivalent.

Design: `android_library` gets a cheap non-transitive R from merge_compiled. `aar_import` always gets a transitive compile-time R (matches Bazel 7's hardcoded `AarImport.nonTransitiveRClass = false`). No per-target flag, no allowlist — rule-kind detected via `hasattr(ctx.attr, "aar")`.

Own-only library R class emission (own symbols, no transitive R) is wired through `package_aware_rtxt/package_aware_rtxt_merge_action.patch`: when `--experimental_use_package_aware_rtxt=true` passes `--packageAwareRTxt`, `AndroidCompiledResourceMergingAction` uses the own-only `.class` files from the primary-only re-merge as the output class jar, matching Bazel 7's `CompileLibraryResourcesAction.generateRFiles()`. This is intentionally controlled separately from `--link_library_resources`; disabling library resource linking no longer implicitly enables package-aware R.txt generation.

- `skip_library_resource_linking.patch` — adds a transitional `//rules/flags:link_library_resources` bool_flag. When false, skip aapt2 `validate_and_link` and `merge_assets` for libs (only binary links). Also adds the aar_import -> `generate_binary_r(primary_only=True)` branch, heart of the compile-time R story.
- `connect_native_android_flags.patch` — bridge for native Android flags. `ctx.fragments.android.link_library_resources` controls whether library resource linking runs, with the transitional Starlark flag still honored while repos migrate back to the Bazel 7-style native `--link_library_resources=false` spelling. `ctx.fragments.android.use_package_aware_rtxt` controls whether package-aware R.txt side outputs are generated.
- `fix_binary_r_class_generation.patch` — merge library placeholder symbols into primary R.txt in `AndroidResourceProcessor.loadPrimaryRTxt`. Replaces a former external MergeRTxts shell that did O(N²) intermediate files.
- `generate_binary_r_primary_only_busybox.patch` — Starlark: add `primary_only` kwarg, pass `--primaryOnly` to the Java tool.
- `generate_binary_r_primary_only_rclass.patch` — Java: `--primaryOnly` flag on RClassGeneratorAction. Emits R only for the primary package, skips per-library R emission. Prevents classpath shadowing between multiple aar_import compile_R.jars (was causing incomplete symbol resolution).
