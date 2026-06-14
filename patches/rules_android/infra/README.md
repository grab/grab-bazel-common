# infra

Build infra tweaks. Not feature stuff, just making rules_android play nice with our setup.

- `macos_cp_reflink.patch` — default `cp --reflink=auto` is Linux-only, drop the flag so macOS doesn't fail.
- `guava_version.patch` — force guava from our maven, avoid version conflict with rules_android's pinned copy.
- `busybox_jvm_flags.patch` — bump busybox heap 3G → 8G. Big apps OOM on default. [this is temporary patch, will remove later]
- `allow_deps_without_srcs.patch` — rules_android 0.7 made "deps without srcs" a hard error. We still have legacy targets doing this, soften back to allow.
- `disable_aar_import_deps_checker.patch` — empty the rollout allowlist, checker is noisy on our AAR graph.
- `allow_resource_conflicts.patch` — allow resource conflicts repo-wide. We have overlapping drawable names across modules (prolly should clean up someday).
- `suppress_resource_conflict_warnings.patch` — keep those allowed conflicts from printing noisy `CONFLICT:` warning blocks during resource merging.
- `wire_strict_deps_flag.patch` — wire `--strict_java_deps` into android_library / android_binary. Default was hardcoded "DEFAULT" so the native flag got ignored.
- `no_source_compile_classpath.patch` — restores Bazel 7's no-source Android wrapper action shape. Native Bazel 7 did not put dependency jars on the synthetic empty javac action for Android targets with no Java sources. rules_android 0.7.1 passed `deps` into `java_common.compile` anyway, so unused upstream ABI changes still invalidated the wrapper action even with `--experimental_action_input_usage_tracker=unused_classes` and `--experimental_track_class_usage=true`. The patch keeps JavaInfo propagation through the existing no-source `exports` compatibility path, but removes the compile classpath from the empty action because it cannot read those jars.
- `compress_java_resources.patch` — Bazel 8 changed the default of `--experimental_android_compress_java_resources` from `true` to `false`, causing APK size regression (~10–15% larger). This patch forces `compress_java_resources = True` in `android_binary/impl.bzl` to restore the Bazel 7 behaviour regardless of the flag default.

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
