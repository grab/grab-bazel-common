# compose

## Compose UI version metadata context

Android Studio Compose preview and Layout Inspector expect APKs to contain
Compose runtime version metadata under `META-INF/*.version`. Bazel 7 carried a
native Android customization for this: `--androidx_compose_ui_version=<version>`
created `META-INF/androidx.compose.ui_ui.version` during APK assembly.

In Bazel 8, APK assembly lives in rules_android Starlark, so this behavior needs
rules_android to consume the native Android configuration fragment instead of
being implemented directly in Bazel core.

- `inject_compose_ui_version.patch` — injects
  `META-INF/androidx.compose.ui_ui.version` into the final APK when the native
  `--androidx_compose_ui_version=<version>` value is non-empty. It also keeps a
  transitional rules_android string build setting at
  `//rules/flags:androidx_compose_ui_version` as a fallback for repos that have
  not rebuilt Bazel with the native flag yet.

Consumers should use the Bazel 7 command-line spelling:

```bazelrc
common --androidx_compose_ui_version=1.8.3
```
