# package_aware_rtxt

Custom R.txt format for databinding non-transitive R support. Standard R.txt doesn't say which package each symbol belongs to; databinding AP needs that to resolve cross-module `@{R.string.foo}` when non-transitive R mode is on.

Format: first line = package name, rest = `type name` lines. Produced as a side output of `merge_compiled` (zero extra actions).

- `package_aware_rtxt_tool.patch` — `PlaceholderRTxtWriter` gains a package-aware mode.
- `package_aware_rtxt_merge_action.patch` — `AndroidCompiledResourceMergingAction` accepts `--packageAwareRTxt`, re-runs merge with empty deps to get non-transitive symbols.
- `busybox_package_aware_rtxt.patch` — Starlark: `merge_compiled` accepts `out_package_aware_r_txt`, passes it as `--packageAwareRTxt`.
- `provider_package_aware_rtxt.patch` — add `transitive_package_aware_r_txts` field to `StarlarkAndroidResourcesInfo`.
- `fix_exports_package_aware_rtxt.patch` — propagate package-aware R.txts through `exports`, not just `deps`.

Consumer side: see `databinding/databinding_non_transitive_r*.patch`.
