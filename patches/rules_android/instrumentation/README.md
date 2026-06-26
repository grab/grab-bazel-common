# rules_android instrumentation patches

Fixes for the instrumentation APK build path.

- `preserve_test_class_overrides.patch` — restore Bazel 7 `WARN` behavior in
  the deploy-jar class filter so test sources that shadow a production class
  by sharing its fully-qualified name (different bytecode, same FQN) survive
