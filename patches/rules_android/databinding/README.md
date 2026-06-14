# databinding

Core databinding fixes beyond AndroidX and AAR extraction.

- `databinding_deps.patch` — expand singlejar `include_prefixes` list so AP finds all classes it needs at runtime (androidx, kotlin, asm, gson, jcommander, javapoet, etc). Without this, AP fails with ClassNotFoundException in the sandbox.
- `fix_databinding_providers.patch` — add `transitive_setter_stores` and `db_package_files` fields to DataBindingV2Info. Needed so APP-level databinding sees all transitive setter stores (from deps of deps).
- `fix_databinding_data_binding.patch` — the big one. APP-level databinding pulls transitive AAR setter stores and generates a synthetic `com.databinding.aar.DataBinderMapperImpl` that uses reflection to load each AAR's mapper. Without this, MergedDataBinderMapper can't discover AAR mappers and you get runtime "cannot find binding" errors.
- `fix_databinding_copydir_symlink.patch` — replace N per-artifact CopyDir/CopyFile shell actions with one batched `SetupDepLibArtifacts` tree artifact action. Cuts thousands of actions in large databinding builds.
- `databinding_non_transitive_r.patch` — pass `-Aandroid.databinding.localResourceFile` / `dependenciesRFiles` to AP so it can resolve R IDs when non-transitive R mode is on. Consumes the package-aware R.txts from `package_aware_rtxt/`.
- `databinding_non_transitive_r_impl.patch` — wire local / dep package-aware R.txt collection from resources context into `_process_data_binding` in android_library impl.
- `databinding_ap_turbine_skip.patch` — set `generates_api = False` on the `compiler_annotation_processor` java_plugin. With `generates_api = True` (the original default), Bazel forces Turbine (header compilation) to run `ProcessDataBinding`. The AP uses internal Javac APIs (`Trees`, `Messager`) unavailable in Turbine's stub environment, causing an NPE and making `--java_header_compilation=true` impossible. Setting `False` tells Turbine to skip the AP entirely; full javac still runs it and generates all DataBinding classes normally.
- `filter_transitive_databinding_artifacts.patch` — restores Bazel 7's direct-dependency filtering for library-level databinding metadata when Bazel 8 Java classpath filtering is enabled. See details below.

## `filter_transitive_databinding_artifacts.patch`

Bazel 8's Java classpath filtering prunes javac inputs using Grazel-generated
`@direct//...` and `@maven//:...` tags. Before this patch, rules_android still
fed transitive databinding metadata into library-level `ProcessDataBinding`.
That made the annotation processor generate Java references to artifacts that
javac could no longer see.

The helper names in this patch are not copied from Bazel 7 because the
implementation surface changed from native Java Android rules to external
Starlark rules_android. The intended behavior is the same:

- Bazel 7's native `TransitiveArtifactsFilter.isAllowedArtifact(...)` filtered
  compile inputs from direct-dependency tags.
- Bazel 7's native `DataBindingV2Context` built library-level
  `directDependencyPkgs` from direct dependency label/package pairs.
- This patch recreates those two behaviors for rules_android's Starlark
  databinding metadata path through `_is_allowed_databinding_artifact(...)`,
  filtered `DataBindingV2Info` views, and filtered package-aware R metadata.

The first failure mode was transitive mapper generation. Generated
`DataBinderMapperImpl` classes referenced mapper packages from transitive deps,
for example:

- `com.grab.styles.DataBinderMapperImpl`
- `com.grab.paysdk.checkout.DataBinderMapperImpl`
- `com.grab.paymentnavigator.DataBinderMapperImpl`

The second failure mode was transitive resource package generation. The
annotation processor received transitive `package_aware_R.txt` files through
`dependenciesRFiles`, so generated binding implementations could reference a
transitive R class such as `com.grab.geo.utils.R.dimen.grid_0`. With
`--filter_transitive_artifacts`, javac had already removed that transitive R jar
from the compile classpath, so compilation failed.

This patch filters the library-level databinding inputs with the same direct
dependency view used by the Java classpath filter:

- `br.bin`
- `setter_store.json`
- `class-info.zip`
- package metadata files
- package-aware R files passed through `dependenciesRFiles`
- package names used for `directDependencyPkgs`

The patch also adds `direct_java_packages` to `DataBindingV2Info`. Bazel 7 kept
label/package relationships separately, but rules_android's Starlark provider
flattened package names. Without a direct package field, filtered-out
dependencies could still leak package names into `directDependencyPkgs`,
especially when a databinding-disabled dependency only carried transitive AAR
metadata.

App-level databinding intentionally keeps transitive AAR metadata because the app
uses the synthetic reflection mapper path to discover AAR mappers at runtime.
The filtering is therefore applied to library-level generation and javac
annotation processor inputs, not to the app-level synthetic mapper metadata.

The filter canonicalizes Bazel 8 main-repo artifact owners such as
`@@//pkg:target` and `@//pkg:target` back to `//pkg:target`. Without that,
internal transitive metadata can look external and survive the filter by
accident.

Bazel 8 does not currently expose the native Java filter flags to Starlark. The
patch reads those flags if they become available; otherwise, the generated
direct-dependency tags are treated as the activation signal.
