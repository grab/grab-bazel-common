# workers

Patches that enable Bazel persistent-worker execution for `GenerateDataBindingBaseClasses`.

## Background

`GenerateDataBindingBaseClasses` runs the `databinding_exec` binary to generate base binding
classes from layout XML. In Bazel 8 the worker strategy can be enabled for this action, but
`databinding_exec` does not natively implement the Bazel worker protocol
(`WorkRequest`/`WorkResponse` protobuf over stdin/stdout). Without a compliant binary Bazel
rejects the action with:

> Worker strategy cannot execute this action, because the command-line arguments do not contain
> exactly one @flagfile or --flagfile=

The patches below add a thin worker-protocol wrapper (`GenBaseClassesWorker`) around the existing
`AndroidDataBinding` binary and wire it into the toolchain and Starlark action.

## Patches (apply in order)

- `gen_base_classes_worker_java.patch` — adds
  `src/tools/java/com/google/devtools/build/android/GenBaseClassesWorker.java`. This class
  implements the Bazel `WorkRequestHandler` protocol: it reads `WorkRequest` protos from stdin,
  delegates each request to `AndroidDataBinding.main()`, and writes `WorkResponse` protos back to
  stdout. The wrapper does not install per-request global state such as a `SecurityManager` or
  replacement `System.out`; those are process-wide and unsafe when Bazel sends concurrent multiplex
  requests to the same worker JVM.

- `gen_base_classes_worker_lib.patch` — adds the `gen_base_classes_worker_lib` `java_library`
  target in `src/tools/java/com/google/devtools/build/android/BUILD`, wiring
  `GenBaseClassesWorker` to the Bazel worker runtime deps (`ProtoWorkerMessageProcessor`,
  `WorkRequestHandler`), the existing `android_databinding_wrapper_lib`, and
  `databinding_exec_jar`. The direct `databinding_exec_jar` dep is required because
  `GenBaseClassesWorker` calls `android.databinding.AndroidDataBinding.main(...)`; declaring it
  directly keeps strict Java deps enabled. The patch also excludes `GenBaseClassesWorker.java`
  from the broad `android_builder_lib` source glob so the worker-only dependency edge stays local
  to the worker library.

- `databinding_exec_worker_binary.patch` — adds a `databinding_exec_worker` `java_binary` target
  in `tools/android/BUILD` with `main_class = GenBaseClassesWorker` and the new lib as its
  `runtime_deps`.

- `databinding_exec_worker_toolchain.patch` — updates `toolchains/android/toolchain.bzl` to
  point `data_binding_exec` at `//tools/android:databinding_exec_worker` instead of
  `//tools/android:databinding_exec`, so every `GenerateDataBindingBaseClasses` action uses the
  worker binary.

- `generate_databinding_base_classes_worker.patch` — patches `rules/data_binding.bzl` to:
  1. Move the `GEN_BASE_CLASSES` subcommand into the `ctx.actions.args()` object (required by
     the worker protocol — all args must be in the flagfile).
  2. Add `args.use_param_file("@%s", use_always = True)` so Bazel writes args to a flagfile and
     passes `@<path>` as the sole command-line argument (the worker protocol requirement).
  3. Add `execution_requirements = {"supports-workers": "1", "supports-multiplex-workers": "1",
     "worker-key-mnemonic": "GenerateDataBindingBaseClasses"}` to the `ctx.actions.run` call.

## Companion `.bazelrc` settings

```
# Keep one worker process for the mnemonic. The action itself supports multiplex requests, so
# Bazel can still run multiple GenerateDataBindingBaseClasses actions through that process when
# --worker_multiplex is enabled.
common --worker_max_instances=GenerateDataBindingBaseClasses=1
```

The action advertises `supports-multiplex-workers = "1"` directly instead of relying only on
`--modify_execution_info=GenerateDataBindingBaseClasses=+supports-multiplex-workers`. Bazel 8's
worker strategy checks the execution-info value, and direct Starlark execution requirements make
the rules_android behavior match the old Bazel 7 native Android path more reliably.

One Bazel-side detail is important: `--modify_execution_info` can add or remove keys only; it cannot
set `supports-multiplex-workers` to `"1"`. If Bazel's
`--persistent_multiplex_android_resource_processor` expansion also applies
`--modify_execution_info=GenerateDataBindingBaseClasses=+supports-multiplex-workers`, that modifier
overwrites the Starlark value with an empty value and the action falls back to single-worker mode.
The Bazel 8 expansion should not add that modifier for `GenerateDataBindingBaseClasses`; the
rules_android action owns the worker metadata.
