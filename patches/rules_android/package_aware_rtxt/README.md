# package_aware_rtxt

Custom R.txt format for databinding non-transitive R support. Standard R.txt doesn't say which package each symbol belongs to; databinding AP needs that to resolve cross-module `@{R.string.foo}` when non-transitive R mode is on.

Format: first line = package name, rest = `type name` lines. Produced as a side output of `merge_compiled` (zero extra actions) when `--experimental_use_package_aware_rtxt=true`.

- `package_aware_rtxt_tool.patch` — `PlaceholderRTxtWriter` gains a package-aware mode.
- `package_aware_rtxt_merge_action.patch` — `AndroidCompiledResourceMergingAction` accepts `--packageAwareRTxt`, re-runs merge with empty deps to get non-transitive symbols. When the flag is set, the output class jar ALSO uses the own-only `.class` files produced by that second merge (matching Bazel 7's `CompileLibraryResourcesAction.generateRFiles()`), so downstream compiles can no longer reach transitive deps' R symbols through this library's compile jar. This removes the old need for a broad binary-side transitive-R allowlist for packages whose bytecode leaked cross-module R refs.
- `busybox_package_aware_rtxt.patch` — Starlark: `merge_compiled` accepts `out_package_aware_r_txt`, passes it as `--packageAwareRTxt`.
- `provider_package_aware_rtxt.patch` — add `transitive_package_aware_r_txts` field to `StarlarkAndroidResourcesInfo`.
- `fix_exports_package_aware_rtxt.patch` — propagate package-aware R.txts through `exports`, not just `deps`.

Control surface: Bazel's native `--experimental_use_package_aware_rtxt` flag is the switch. Keep it enabled alongside non-transitive R/databinding builds that need package-qualified R metadata. `--link_library_resources=false` no longer implies this behavior.

Consumer side: see `databinding/databinding_non_transitive_r*.patch`.
