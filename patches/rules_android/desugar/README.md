# desugar

Desugaring is slow. R class jars contain only static final int constants, no Java 8+ bytecode, so desugaring them is pure overhead. These patches skip it.

- `skip_binary_r_jar_desugaring_flags.patch` — adds `//rules/flags:desugar_resources_jar` bool_flag.
- `skip_binary_r_jar_desugaring_attrs.patch` — wire the flag as private attr on android_binary.
- `skip_binary_r_jar_desugaring_impl.patch` — in android_binary impl, when flag=false and jar is `_resources.jar`, pass through untransformed.
- `skip_r_jar_desugaring.patch` — per-library: drop `AndroidIdeInfo.resource_jar` from the desugar aspect's jar set. Same idea but for every lib's R.jar (matters a lot when `link_library_resources=false` produces large transitive R jars).

Flip `common --@rules_android//rules/flags:desugar_resources_jar=false` in bazelrc to enable the binary-side skip.

- `dex_aar_import_resources_jar.patch` — restores dex processing of `AndroidIdeInfo.resource_jar.class_jar` specifically for `aar_import` targets.

  **Problem solved:** `rules_android` 0.7.1 introduced a strict check in `dex.bzl` (`_to_dexed_classpath`) that fails the build if any jar appears in `transitive_runtime_jars_for_archive` without a corresponding entry in `dex_archives_dict`. AARs like `androidx.databinding:databinding-adapters` generate a `*_resources.jar` containing real generated binding code. This jar is created inline as a `JavaInfo` dep inside `aar_import`'s rule implementation — it is never a standalone build target — so the `dex_desugar_aspect` never visits it and no dex archive is ever created for it. The result is a hard build failure.

  `skip_r_jar_desugaring.patch` removed all `AndroidIdeInfo.resource_jar` handling from the aspect (correct for `android_library` R jars which are field-only). This patch adds it back selectively for `aar_import` only, so R jar desugaring overhead is still avoided while databinding and other AAR resource jars are correctly dexed.
