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
  delegates each request to `AndroidDataBinding.main()`, captures stdout/stderr per request, and
  writes `WorkResponse` protos back to stdout. A custom `SecurityManager` intercepts
  `System.exit()` calls from the wrapped binary so the worker JVM stays alive between requests.

- `gen_base_classes_worker_lib.patch` — adds the `gen_base_classes_worker_lib` `java_library`
  target in `src/tools/java/com/google/devtools/build/android/BUILD`, wiring
  `GenBaseClassesWorker` to the Bazel worker runtime deps (`ProtoWorkerMessageProcessor`,
  `WorkRequestHandler`) and the existing `android_databinding_wrapper_lib`.

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
  3. Add `execution_requirements = {"supports-workers": "1", "worker-key-mnemonic":
     "GenerateDataBindingBaseClasses"}` to the `ctx.actions.run` call.

## Companion `.bazelrc` settings

```
# Disable multiplex for GenerateDataBindingBaseClasses:
# AndroidDataBinding has mutable static state unsafe to share across concurrent
# requests within one JVM, so multiplex workers are disabled.
common --modify_execution_info=GenerateDataBindingBaseClasses=+supports-multiplex-workers=0
common --worker_max_instances=GenerateDataBindingBaseClasses=1
```
