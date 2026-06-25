# aar_import

Making aar_import actually work for our SDKs. Default aar_import is too minimal, doesn't propagate everything consumers need.

- `aar_import_export_r_java.patch` — always export R java (was gated by an ACL). Consumers of AAR SDKs need transitive R access to compile.
- `aar_import_transitive_r_txt.patch` — add R-generation-only transitive symbol aliases for aar_import. Pre-compiled AAR bytecode may reference dependency R fields at runtime, but we must not push those deps through `merge_compiled` because some Maven AARs do not have `symbols.zip`.
- `fix_aar_import_databinding_info.patch` — extracts `data-binding/` metadata and the manifest package from AARs, then propagates them through `DataBindingV2Info`. Without this, consumers with databinding cannot find AAR `@BindingAdapter`s, for example `android:onClick`, and fail with "Cannot find a setter". The extraction is folded into `AarResourcesExtractor` instead of separate unzip shell actions, so resource/assets/databinding extraction can run through the existing JSON worker path with multiplex worker metadata. The action must use `--flagfile` because Bazel's worker strategy only accepts worker actions with a single flagfile argument. The JSON worker wrapper also echoes Bazel's optional `requestId`, which is required when the action runs as a multiplex worker.
- `native_databinding_package_info.patch` — bridge for `--android_databinding_package_info` native flag so aar_import package can be overridden at build time.

## `aar_import_transitive_r_txt.patch`

### Context

In Bazel 7, native `AarImport` hardcoded:

```java
/* nonTransitiveRClass= */ false
```

That is different from normal `android_library`, which follows the global non-transitive R setting. The practical meaning is: prebuilt AARs are treated as if their bytecode was compiled against a transitive R class, because Maven/Gradle AAR bytecode often references fields from dependency R classes.

In Bazel 8 / rules_android, we ported non-transitive R by skipping library resource linking. That keeps normal libraries cheap and own-only, but it exposed two runtime failures:

- `NoSuchFieldError`, for example `com.google.android.material.R$attr.theme`, when final binary R generation did not emit dependency field names for an AAR package.
- `Resources$NotFoundException: resource ID #0x0`, when an AAR compile-time placeholder R jar leaked into runtime and won over the final binary R class.

### What the patch does

The patch creates `ResourcesNodeInfo` aliases only for `aar_import` targets when library linking is disabled. Each alias pairs:

- a dependency `R.txt`, used as the symbol-name filter
- the current AAR's processed manifest, used as the Java package for generated R classes
- `compiled_resources = None`, so resource merge/link actions ignore it

These aliases are appended to `transitive_resources_nodes`, which is already consumed by final `generate_binary_r`. That gives final binary R generation enough information to emit the AAR package's R class with transitive field names, while values still come from the final linked app `R.txt`.

### Why this shape matters

An earlier attempt made `aar_import` pass transitive dependencies into `merge_compiled`. That fixed some missing fields, but it was too broad: targets like `androidx_tracing_tracing` and `io_reactivex_rxjava2_rxandroid` can appear as AAR/resource deps without a produced `symbols.zip`, causing:

```text
invalid SerializedAndroidData: .../symbols.zip does not exist
```

The final version avoids that path entirely. Package/merge actions skip the alias nodes because they have no compiled symbols, but `generate_binary_r` still reads their `r_txt + manifest` pair. This preserves the Bazel 7 runtime behavior without reintroducing expensive transitive compiled-resource merging.

### Result

With `link_library_resources = false`, regular `android_library` targets remain own-only and cheap. `aar_import` gets Bazel-7-compatible transitive R field names in the final binary R jar, so we can remove package allowlists and avoid zero-valued runtime R classes.
