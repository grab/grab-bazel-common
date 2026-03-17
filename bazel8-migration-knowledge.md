# Bazel 8 Migration Knowledge Base

Complete knowledge transfer document for the grab-bazel-common Bazel 8 migration.

---

## 1. Project Overview

**Repository:** `grab-bazel-common` — shared Bazel build infrastructure for Grab's Android projects.

**Migration:** Bazel 7.4.1 → Bazel 8.5.1, while also upgrading RJE from 6.9 to 6.10.

**Branch:** `migration/bazel_8_test` — contains Bazel 8 changes, rebased onto master (which has RJE 6.10).

**Key constraint:** WORKSPACE mode only (`--enable_bzlmod=false`), no bzlmod.

---

## 2. Architecture: Consumer Macro API

The consumer (e.g., pax-android, grazel) calls these macros in their WORKSPACE:

```python
# Step 1: Declare all http_archive dependencies
load("@grab_bazel_common//rules:repositories.bzl", "bazel_common_dependencies")
bazel_common_dependencies()

# Step 2: Safe prereqs (android_tools, rules_android_prereqs, bazel_features_deps)
load("@grab_bazel_common//rules:deps_init.bzl", "bazel_common_deps_init")
bazel_common_deps_init()

# Step 3: Intermediate transitive deps (Bazel 8 requires explicit ordering)
load("@rules_cc//cc:extensions.bzl", "compatibility_proxy_repo")
compatibility_proxy_repo()

load("@rules_java//java:rules_java_deps.bzl", "rules_java_dependencies")
rules_java_dependencies()

load("@com_google_protobuf//bazel/private:proto_bazel_features.bzl", "proto_bazel_features")
proto_bazel_features(name = "proto_bazel_features")

# Step 4: Setup after transitive deps are ready
load("@grab_bazel_common//rules:deps_setup.bzl", "bazel_common_deps_setup")
bazel_common_deps_setup()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
rules_jvm_external_setup()

# Step 5: Main setup (kotlin, android SDK, maven_install)
load("@grab_bazel_common//rules:setup.bzl", "bazel_common_setup")
bazel_common_setup(buildifier_version = "6.3.3", pinned_maven_install = True)

# Step 6: Pin dependencies
load("@grab_bazel_common//rules:maven.bzl", "pin_bazel_common_dependencies")
pin_bazel_common_dependencies()

# Step 7: Consumer's own maven_install
load("@grab_bazel_common//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")
load("@rules_jvm_external//:defs.bzl", "maven_install")
maven_install(artifacts = GRAB_BAZEL_COMMON_ARTIFACTS + [...], ...)
```

**Why so many steps?** Bazel 8's WORKSPACE resolves ALL top-level `load()` statements in a .bzl file before executing any function body. This creates circular dependencies if repos aren't created in the right order. Each `load()` + call step ensures the required repo exists before the next file tries to load from it.

---

## 3. Key Dependency Versions

| Dependency | Version | Notes |
|-----------|---------|-------|
| Bazel | 8.5.1 | `.bazelversion` |
| rules_jvm_external (RJE) | 6.10 | Maven v3 lock format |
| rules_android | 0.7.1 | With 13 custom patches |
| rules_java | 9.5.0 | Creates `@compatibility_proxy` |
| rules_cc | 0.2.14 | Creates `@cc_compatibility_proxy` |
| rules_kotlin | 2.1.10 | |
| Kotlin | 2.1.0 | Compiler version |
| KSP | 2.1.0-1.0.28 | |
| bazel_features | 1.30.0 | |
| rules_license | 1.0.0 | Required by transitive deps |
| Dagger | 2.59.1 | |
| Compose compiler | From rules_kotlin jar | Not from maven anymore |

---

## 4. Files Changed from Master

### New Files
- **`rules/deps_init.bzl`** — Replaces `prereqs.bzl`. Loads `rules_android_prereqs()`, `bazel_features_deps()`, and optionally `android_tools()`.
- **`rules/deps_setup.bzl`** — Calls `rules_java_toolchains()` and `rules_jvm_external_deps()` after transitive deps are ready.

### Modified Files
- **`WORKSPACE`** — Restructured with sequential Bazel 8 dependency chain (see Section 2).
- **`rules/repositories.bzl`** — RJE bumped to 6.10, added `rules_license`, 13 patches for rules_android.
- **`rules/setup.bzl`** — Added `additional_coursier_options` parameter, `rules_jvm_external_setup()` call, kotlin 2.1.0, KSP 2.1.0-1.0.28.
- **`rules/maven.bzl`** — Simplified, removed `rules_jvm_external_setup()`.
- **`rules/test/setup.bzl`** — Added `additional_coursier_options` parameter.
- **`rules/android/android_binary.bzl`** — Re-enabled `min_sdk_version`, kept `incremental_dexing = 0`, removed `crunch_png`.
- **`workspace_defs.bzl`** — Kotlin artifacts at 2.1.0, removed compose compiler artifact.
- **`.bazelrc`** — Bazel 8 flags (see Section 8).
- **`tools/android/BUILD.bazel`** — Externalized android tools.
- **`tools/kotlin/android.bzl`** — Uses `@grab_bazel_common//tools/android:android_sdk`, conditional `exports_manifest`.
- **`tools/databinding/databinding.bzl`** — Conditional `exports_manifest`.

### Existing File (not deleted)
- **`rules/prereqs.bzl`** — Still on disk, superseded by `deps_init.bzl`. Consumer (grazel) may still reference it.

---

## 5. All rules_android Patches (13 total)

Applied in this order (order matters):

| # | Patch File | Purpose |
|---|-----------|---------|
| 1 | `guava_version.patch` | Guava version compatibility |
| 2 | `macos_cp_reflink.patch` | macOS copy compatibility |
| 3 | `use_androidx.patch` | Use AndroidX |
| 4 | `androidx_annotation_template.patch` | AndroidX annotation template |
| 5 | `databinding_deps.patch` | Databinding annotation processor fat jar includes |
| 6 | `android_resource_processor_bazel_paths.patch` | Resource processor paths |
| 7 | `allow_resource_conflicts.patch` | Allow resource conflicts |
| 8 | `skip_library_resource_linking.patch` | **Skip aapt2 link + merge_assets for libraries** |
| 9 | `fix_resource_priority_ordering.patch` | Change depset ordering preorder→postorder |
| 10 | `fix_aar_import_databinding_info.patch` | AAR import databinding provider propagation |
| 11 | `fix_databinding_providers.patch` | Add `transitive_setter_stores`, `db_package_files` fields |
| 12 | `fix_databinding_data_binding.patch` | **AAR databinding mapper + java_packages dedup** |
| 13 | `busybox_jvm_flags.patch` | **Bump busybox JVM heap 3G→8G (temporary)** |

**Patch ordering dependencies:**
- `fix_resource_priority_ordering` MUST come after `skip_library_resource_linking` (hunks were removed from ordering patch that overlap with skip_library)
- `fix_databinding_providers` MUST come before `fix_databinding_data_binding` (schema change before usage)
- `fix_aar_import_databinding_info` MUST come before `fix_databinding_data_binding` (provider propagation before consumption)

---

## 6. Memory Issues and Fixes

### Issue 1: DataBinding `java_packages` Exponential Growth

**Root Cause:** `java_packages` in `DataBindingV2Info` is a `List` not a `depset`. Each library stores `[own_package] + exported_packages` without dedup. Diamond dependencies cause exponential growth.

**Fix in `fix_databinding_data_binding.patch`:** Added `list({p: True for p in ...}.keys())` deduplication at 3 points:
1. Provider level (has databinding) — when storing `java_packages`
2. Provider level (no databinding) — same
3. Consumer level (`_get_javac_opts`) — before building javac option string

**Future optimization:** Convert `java_packages` to `depset` in the provider schema.

### Issue 2: Busybox JVM Heap Hardcoded to 3GB

**Root Cause:** `rules/java.bzl` line 467: `jvm_flags = ["-Xms3G", "-Xmx3G", "-XX:+ExitOnOutOfMemoryError"]`

**Fix in `busybox_jvm_flags.patch`:** Bumped to `-Xms8G -Xmx8G`.

**This is a temporary workaround.** The real fix is Issue 3 below.

### Issue 3: Library Intermediate Artifacts (library.ap_ and assets.zip)

**Root Cause:** Every `android_library` produces `library.ap_` (merged resources APK) and `assets.zip` (merged assets). These grow exponentially through transitive merging.

**Fix in `skip_library_resource_linking.patch`:** When `link_library_resources=False`:
- Skips `_busybox.merge_assets()` — writes empty placeholder instead
- Skips `_busybox.validate_and_link()` — writes empty placeholders for `r_java` and `r_txt`
- Does NOT produce `library.ap_`
- Does NOT produce merged `assets.zip`
- Propagates `out_class_jar` from `merge_compiled` as the compile-time R class (`JavaInfo`) — this is the complete non-transitive R class with all inner classes (layout, color, drawable, id). See Issue 4 below.
- `android_binary` is unaffected — it re-links everything from transitive compiled resources

**Consumer must set:** `build --@rules_android//rules/flags:link_library_resources=False`

### Issue 4: R class inner classes unresolved (`R.layout`, `R.color`, `R.drawable`) in consumers

**Root Cause:** The original `skip_library_resource_linking.patch` called `_busybox.generate_binary_r(r_txt=out_aapt2_r_txt)` in the `_should_link=False` branch to produce a transitive R class jar. However, `generate_binary_r`'s `--primaryRTxt` parameter expects the R.txt from the **link** step (`validate_and_link`), not from the **compile** step (`merge_compiled`). Passing `out_aapt2_r_txt` (compile-step output) produced an incomplete R class — `R.id` existed (from values resources) but `R.layout`, `R.color`, `R.drawable` were missing.

**Fix:** Replaced the `generate_binary_r` call with direct reuse of `out_class_jar` from `merge_compiled`. `merge_compiled` (MERGE_COMPILED busybox command) always runs regardless of `_should_link` and produces a complete non-transitive R class with all inner classes. This is exported via `JavaInfo` → `r_java_info` → `exports` of the `android_library`, making it visible to all consumers at compile time.

**Symptom in pax-android:** `grab-webview-gps-pax-debug_kt` failed with `unresolved reference: layout`, `unresolved reference: color`, etc. when referencing `com.grab.styles.R.layout.xxx`. The `com.grab.styles.R` class was found (via `r_java_info` export) but was incomplete.

**After this fix, `busybox_jvm_flags.patch` can likely be reverted to 3GB.**

### Issue 5: Binary R class too large (`ClassTooLargeException: Class too large: com/grabtaxi/passenger/R$id`)

**Root Cause:** When `link_library_resources=False`, the `fix_resource_priority_ordering.patch` was incorrectly zeroing out ALL dependency resources in the `merge_compiled` call: `direct_resources_nodes = depset(transitive = direct_resources_nodes if _should_link else [], ...)`. This meant `merge_compiled` only saw the binary's own resources, not deps. Then at binary packaging time, `generate_binary_r` received all transitive R.txt files but couldn't access the library R.txt files (not in `inputs`) → merged ALL IDs into the primary package's R class → `R$id` exceeded JVM's 64K class constant pool limit.

**Fix:**
1. Reverted `fix_resource_priority_ordering.patch` to pass full dependency resources to `merge_compiled` (keeping only the `preorder`→`postorder` ordering change). `merge_compiled` is lightweight (no aapt2 link) so full deps don't cause OOM.
2. Created new patch `fix_binary_r_class_generation.patch` to add `resources_nodes` file objects to the action `inputs` for `StarlarkRClassGenerator`. This allows busybox to actually access the library R.txt/manifest files and pass them as `--library` flags.

**Patch Changes:**
- **`fix_resource_priority_ordering.patch` (updated)**: Removed `if _should_link else []` conditions from `merge_compiled` call. Now passes all dependencies regardless of `link_library_resources` setting.
- **`fix_binary_r_class_generation.patch` (NEW)**: Extracts file paths from `resources_nodes` and adds them to action inputs:
  ```python
  resources_node_files = [
      f
      for node in resources_nodes.to_list()
      for f in (([node.r_txt] if node.r_txt else []) +
                ([node.manifest] if node.manifest else []))
  ]
  inputs = depset(
      [r_txt, manifest] + resources_node_files,
      transitive = transitive_r_txts + transitive_manifests,
  )
  ```

**Symptom in pax-android:** `//app:app-gps-pax-debug` binary build failed with `StarlarkRClassGenerator` unable to generate `com.grabtaxi.passenger.R$id` — class would exceed 64K constant pool when merging all transitive view IDs.

### Bazel 7 Port Reference

These optimizations were originally native Java flags in the Bazel 7 fork (see `/home/edwin/Document/Works/bazel_7_port.md`):
- Item #2: `link_library_resources` — Ported
- Item #3: `output_library_linked_resources` — Ported (via skip_library_resource_linking)
- Item #4: Reduce dexing JVM to 2GB — **NOT ported**
- Item #5: Disable bytecode verification — **NOT ported**
- Item #7: Skip desugaring for resources.jar — **NOT ported**

---

## 7. Bazel 8 Externalization Changes

### Android SDK
- `@bazel_tools//tools/android:android_jar` **no longer exists** in Bazel 8
- Must use `@androidsdk//:platforms/android-36/android.jar`
- Or use `@grab_bazel_common//tools/android:android_sdk` (java_import with neverlink)
- `tools/android/BUILD.bazel` provides both `android_tools` and `android_sdk` targets

### Transitive Dependency Chain
Bazel 8 requires explicit creation of intermediate repos:
1. `bazel_features_deps()` → creates `@bazel_features_version`
2. `compatibility_proxy_repo()` → creates `@cc_compatibility_proxy`
3. `rules_java_dependencies()` → creates `@compatibility_proxy`
4. `proto_bazel_features()` → creates `@proto_bazel_features`
5. `rules_java_toolchains()` → registers Java toolchains
6. `rules_jvm_external_deps()` → creates `@rules_jvm_external_deps`

---

## 8. Consumer `.bazelrc` Requirements

```bash
# Bazel 8 WORKSPACE mode
common --enable_bzlmod=false
common --enable_workspace=true
common --incompatible_disallow_empty_glob=false

# DataBinding — must run locally, not as worker
build --strategy=GenerateDataBindingBaseClasses=local

# Resource linking — skip at library level to avoid OOM and missing resource errors
build --@rules_android//rules/flags:link_library_resources=False

# Bazel JVM heap (for analysis phase)
startup --host_jvm_args=-Xmx16g
```

---

## 9. Consumer Migration Checklist

- [ ] Update `.bazelversion` to `8.5.1`
- [ ] Update `.bazelrc` with flags from Section 8
- [ ] Update WORKSPACE to use the multi-step dependency chain (Section 2)
- [ ] Replace `@grab_bazel_common//rules:prereqs.bzl` → `@grab_bazel_common//rules:deps_init.bzl`
- [ ] Add `deps_setup.bzl` call between `rules_java_dependencies()` and `setup.bzl`
- [ ] Replace `@bazel_tools//tools/android:android_jar` → `@grab_bazel_common//tools/android:android_sdk` or `@androidsdk//:platforms/android-36/android.jar`
- [ ] Ensure `kotlin-parcelize-compiler:2.1.0` is in maven artifacts (must match Kotlin compiler version)
- [ ] Repin maven: `REPIN=1 bazel run @maven//:pin`
- [ ] Keep `"@//:kotlin_toolchain"` (with `@` prefix) for Kotlin toolchain registration

---

## 10. Build Verification

**grab-bazel-common itself:**
- `bazel build //...` → 122 targets, 3617 actions — SUCCESS
- 12/14 tests pass, 2 pre-existing failures (lint test, binding-adapter-processor test)

**Consumer projects (pax-android):**
- Non-databinding targets: build successfully
- Databinding targets: require `java_packages` dedup patch + `link_library_resources=False`
- Parcelize targets: require matching `kotlin-parcelize-compiler` version + repin

---

## 11. Resolved Errors Reference

| Error | Root Cause | Fix |
|-------|-----------|-----|
| `@@rules_jvm_external_deps` cycle | `setup.bzl` loads deps that don't exist yet | Call `rules_jvm_external_deps()` in `deps_setup.bzl` |
| `@@compatibility_proxy` cycle | `rules_java` loads proxy that doesn't exist | Call `rules_java_dependencies()` explicitly in WORKSPACE |
| `@@bazel_features_version` cycle | `rules_java` needs `bazel_features` | Call `bazel_features_deps()` in `deps_init.bzl` |
| `@@cc_compatibility_proxy` cycle | `rules_java` needs `rules_cc` proxy | Call `compatibility_proxy_repo()` in WORKSPACE |
| `@@proto_bazel_features` cycle | Protobuf needs its own features repo | Call `proto_bazel_features()` in WORKSPACE |
| `@@rules_license` missing | Required by transitive deps | Added to `repositories.bzl` |
| `additional_coursier_options` unexpected kwarg | Missing parameter in test setup | Added to `rules/test/setup.bzl` |
| `@@bazel_tools//tools/android:android_jar` not found | Bazel 8 externalizes android tools | Use `@androidsdk//` or `@grab_bazel_common//tools/android:android_sdk` |
| OOM in `_get_javac_opts` / `ShellUtils.tokenize` | `java_packages` exponential growth | Dedup in `fix_databinding_data_binding.patch` |
| OOM in `MergeAndroidAssets` worker | Busybox JVM capped at 3GB + exponential `assets.zip` | `busybox_jvm_flags.patch` + skip `merge_assets` in `skip_library_resource_linking.patch` |
| `LinkAndroidResources` missing resources | Library-level aapt2 link can't find transitive deps | `--@rules_android//rules/flags:link_library_resources=False` |
| `NoSuchMethodError: InlineClassRepresentation` | Parcelize compiler version mismatch | Match `kotlin-parcelize-compiler` to Kotlin 2.1.0 + repin |
| `GenerateDataBindingBaseClasses` spawn strategy | Missing `.bazelrc` flag | `--strategy=GenerateDataBindingBaseClasses=local` |
| Worker unparseable response (binary protobuf) | Worker JVM OOM with `-XX:+ExitOnOutOfMemoryError` | Fix root cause (skip merge_assets) or bump heap |
| `unresolved reference: layout/color/drawable` in `*_kt` targets (e.g. `R.layout`, `R.color`) | `skip_library_resource_linking.patch` used `generate_binary_r(r_txt=out_aapt2_r_txt)` — compile-step R.txt is wrong input for link-step API, producing incomplete R class | Use `out_class_jar` from `merge_compiled` directly in `JavaInfo` (see Issue 4 in Section 6) |
| `ClassTooLargeException: Class too large: com/grabtaxi/passenger/R$id` in `StarlarkRClassGenerator` | `fix_resource_priority_ordering.patch` zeroed out deps in `merge_compiled` when `link_library_resources=False`, so binary's `generate_binary_r` received dep R.txt files but couldn't access them (not in action inputs) → merged all IDs into primary package's R class → exceeded 64K class limit | Fix 1: Restore deps to `merge_compiled` in `fix_resource_priority_ordering.patch`. Fix 2: Add `resources_nodes` files to action inputs in new `fix_binary_r_class_generation.patch` (see Issue 5 in Section 6) |

---

## 12. Patch Interdependencies

```
fix_databinding_providers.patch
    └── fix_aar_import_databinding_info.patch (uses new provider fields)
    └── fix_databinding_data_binding.patch (uses new provider fields + dedup)

skip_library_resource_linking.patch
    └── fix_resource_priority_ordering.patch (2 hunks removed, line numbers adjusted)
        (ordering patch MUST apply AFTER skip_library)
    └── fix_binary_r_class_generation.patch (adds inputs to binary R generation)

busybox_jvm_flags.patch (temporary workaround, can revert to 3GB after Issues 4+5 confirmed)
```

---

## 13. Files for Reference

| File | Location | Purpose |
|------|----------|---------|
| Bazel 7 port notes | `/home/edwin/Document/Works/bazel_7_port.md` | All Bazel 7 fork patches with porting notes |
| Memory issues doc | `docs/bazel8-memory-issues.md` | Detailed memory analysis and TODOs |
| This document | `docs/bazel8-migration-knowledge.md` | Full knowledge transfer |
| Original data_binding.bzl | rules_android v0.7.1 (326 lines) | Saved at `/tmp/data_binding_original.bzl` during analysis |

---

## 14. Outstanding Work

- [ ] Test with grazel's sample-android project (interrupted, not completed)
- [ ] Delete or update `rules/prereqs.bzl` (superseded by `deps_init.bzl`)
- [ ] Port dexing memory cap from Bazel 7 (item #4 in `bazel_7_port.md`)
- [ ] Convert `java_packages` from List to depset in DataBindingV2Info provider
- [x] Fix Issue 4: Incomplete R class from `generate_binary_r(r_txt=out_aapt2_r_txt)` — use `out_class_jar` directly
- [x] Fix Issue 5: Binary R class too large — restore deps in `merge_compiled` + add `resources_nodes` to action inputs
- [ ] Revert `busybox_jvm_flags.patch` to 3GB after confirming Issues 4+5 fixes OOM
- [ ] Consider changing `link_library_resources` default to `False`
- [ ] Verify grazel WORKSPACE uses `deps_init.bzl` instead of `prereqs.bzl`
- [ ] Complete pax-android build (currently at compilation phase, Kotlin 2.1.0 strictness issues)
