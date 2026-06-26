# infra

Build infra tweaks. Not feature stuff, just making rules_android play nice with our setup.

- `macos_cp_reflink.patch` — default `cp --reflink=auto` is Linux-only, drop the flag so macOS doesn't fail.
- `guava_version.patch` — force guava from our maven, avoid version conflict with rules_android's pinned copy.
- `pin_rules_android_maven.patch` — enable rules_android's checked-in `rules_android_maven_install.json` for both WORKSPACE and Bzlmod paths so cold builds don't perform large live Coursier resolution for rules_android tool dependencies. The WORKSPACE path uses `@rules_android//:rules_android_maven_install.json` because `//:...` resolves against the consuming workspace from `defs.bzl`. This addresses https://github.com/bazelbuild/rules_android/issues/485 for our WORKSPACE-based consumers.
- `repin_rules_android_maven_install.patch` — repin rules_android's Maven lockfile with `rules_jvm_external` 6.10. Enabling the upstream lockfile as-is fails because it has the old v2 hash format and stale resolved entries (for example protobuf 4.33.1 while `defs.bzl` requests 4.33.4).

Pinned WORKSPACE-mode `maven_install` repositories also require a follow-up call
to the generated `@repo//:defs.bzl%pinned_maven_install` macro so the per-artifact
`http_file` repositories exist. `rules/maven.bzl` wires
`@rules_android_maven//:defs.bzl%pinned_maven_install` into
`pin_bazel_common_dependencies()`, matching the existing bazel_common maven setup.

- `busybox_jvm_flags.patch` — bump busybox heap 3G → 8G. Big apps OOM on default. [this is temporary patch, will remove later]
- `allow_deps_without_srcs.patch` — rules_android 0.7 made "deps without srcs" a hard error. We still have legacy targets doing this, soften back to allow.
- `disable_aar_import_deps_checker.patch` — empty the rollout allowlist, checker is noisy on our AAR graph.
- `allow_resource_conflicts.patch` — allow resource conflicts repo-wide. We have overlapping drawable names across modules (prolly should clean up someday).
- `suppress_resource_conflict_warnings.patch` — keep those allowed conflicts from printing noisy `CONFLICT:` warning blocks during resource merging.
- `wire_strict_deps_flag.patch` — wire `--strict_java_deps` into android_library / android_binary. Default was hardcoded "DEFAULT" so the native flag got ignored.
- `no_source_compile_classpath.patch` — restores Bazel 7's no-source Android wrapper action shape. Native Bazel 7 did not put dependency jars on the synthetic empty javac action for Android targets with no Java sources. rules_android 0.7.1 passed `deps` into `java_common.compile` anyway, so unused upstream ABI changes still invalidated the wrapper action even with `--experimental_action_input_usage_tracker=unused_classes` and `--experimental_track_class_usage=true`. The patch keeps JavaInfo propagation through the existing no-source `exports` compatibility path, but removes the compile classpath from the empty action because it cannot read those jars.
- `compress_java_resources.patch` — Bazel 8 changed the default of `--experimental_android_compress_java_resources` from `true` to `false`, causing APK size regression (~10–15% larger). This patch forces `compress_java_resources = True` in `android_binary/impl.bzl` to restore the Bazel 7 behaviour regardless of the flag default.
- `resource_extractor_without_deploy_jar.patch` — restores Bazel 7's `--experimental_resource_extractor_without_deploy_jar` behavior for rules_android. When enabled, `android_binary` builds a resource-only zip from runtime jars with `shuffle_jars` (`ResourceExtractorRuntime`) and feeds that into APK Java-resource extraction instead of the large `*_deploy.jar`. This reduces cache churn, disk/remote transfer size, and APK packaging invalidation from unrelated deploy-jar changes. Instrumentation APKs keep the existing deploy-jar path, matching Bazel 7's follow-up instrumentation exception. The action uses a shell-quoted param file, matching Bazel 7, so large apps do not overflow macOS `ARG_MAX`/`sandbox-exec` with thousands of runtime `--input_jar` arguments. The patch also ports Bazel 7's `DexMapper` fallback that creates a temporary shard output when only `--output_resources` is requested; without that tool-side change, resource-only extraction fails with `Require at least one output file`. The temporary shard is created beside the declared resource output so sandboxed actions do not try to write to a host temp directory.

## `no_source_compile_classpath.patch`

This is part of compile-avoidance parity with Bazel 7.

The playground parity test used `//kotlin-android-library:kotlin-android-library-debug`
depending on `//kotlin-android-library2:kotlin-android-library2-debug`.
After changing an unused upstream Kotlin class ABI, Bazel 7 rebuilt only the
workspace status action. Bazel 8 with external rules_android rebuilt the
downstream Android wrapper jar:

- `KotlinCompile //kotlin-android-library2:...`
- `Merging Kotlin output jar //kotlin-android-library2:... (Abi)`
- `Building kotlin-android-library/libkotlin-android-library-debug.jar ()`

Action-query comparison showed both Bazel 7 and Bazel 8 passed
`--experimental_track_class_usage` when the flag was enabled. The difference was
the no-source wrapper action's inputs:

- Bazel 7 native Android wrapper action had no dependency `--classpath`.
- Bazel 8 rules_android wrapper action had a full `--classpath`, including
  upstream ABI jars such as `kotlin-android-library2-debug_kt.abi.jar`.

Because the action has no Java sources, javac cannot observe or use those
classpath jars. Passing them only gives Bazel more inputs to invalidate. The
patch therefore changes `_compile_android` so `java_common.compile` receives an
empty `deps` list when `srcs` is empty. The existing no-source compatibility
logic still adds dependency JavaInfo to `exports` when `enable_deps_without_srcs`
is true, preserving the legacy provider propagation behavior.
