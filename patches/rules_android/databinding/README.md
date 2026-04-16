# databinding

Core databinding fixes beyond AndroidX and AAR extraction.

- `databinding_deps.patch` — expand singlejar `include_prefixes` list so AP finds all classes it needs at runtime (androidx, kotlin, asm, gson, jcommander, javapoet, etc). Without this, AP fails with ClassNotFoundException in the sandbox.
- `fix_databinding_providers.patch` — add `transitive_setter_stores` and `db_package_files` fields to DataBindingV2Info. Needed so APP-level databinding sees all transitive setter stores (from deps of deps).
- `fix_databinding_data_binding.patch` — the big one. APP-level databinding pulls transitive AAR setter stores and generates a synthetic `com.databinding.aar.DataBinderMapperImpl` that uses reflection to load each AAR's mapper. Without this, MergedDataBinderMapper can't discover AAR mappers and you get runtime "cannot find binding" errors.
- `fix_databinding_copydir_symlink.patch` — replace N per-artifact CopyDir/CopyFile shell actions with one batched `SetupDepLibArtifacts` tree artifact action. Cuts thousands of actions in large databinding builds.
- `databinding_non_transitive_r.patch` — pass `-Aandroid.databinding.localResourceFile` / `dependenciesRFiles` to AP so it can resolve R IDs when non-transitive R mode is on. Consumes the package-aware R.txts from `package_aware_rtxt/`.
- `databinding_non_transitive_r_impl.patch` — wire local / dep package-aware R.txt collection from resources context into `_process_data_binding` in android_library impl.
