# infra

Build infra tweaks. Not feature stuff, just making rules_android play nice with our setup.

- `macos_cp_reflink.patch` — default `cp --reflink=auto` is Linux-only, drop the flag so macOS doesn't fail.
- `guava_version.patch` — force guava from our maven, avoid version conflict with rules_android's pinned copy.
- `busybox_jvm_flags.patch` — bump busybox heap 3G → 8G. Big apps OOM on default. [this is temporary patch, will remove later]
- `allow_deps_without_srcs.patch` — rules_android 0.7 made "deps without srcs" a hard error. We still have legacy targets doing this, soften back to allow.
- `disable_aar_import_deps_checker.patch` — empty the rollout allowlist, checker is noisy on our AAR graph.
- `allow_resource_conflicts.patch` — allow resource conflicts repo-wide. We have overlapping drawable names across modules (prolly should clean up someday).
- `wire_strict_deps_flag.patch` — wire `--strict_java_deps` into android_library / android_binary. Default was hardcoded "DEFAULT" so the native flag got ignored.
- `compress_java_resources.patch` — Bazel 8 changed the default of `--experimental_android_compress_java_resources` from `true` to `false`, causing APK size regression (~10–15% larger). This patch forces `compress_java_resources = True` in `android_binary/impl.bzl` to restore the Bazel 7 behaviour regardless of the flag default.
