# compose

## Compose UI version metadata context

Android Studio Compose preview and Layout Inspector expect APKs to contain
Compose runtime version metadata under `META-INF/*.version`. Bazel 7 carried a
native Android customization for this: `--androidx_compose_ui_version=<version>`
created `META-INF/androidx.compose.ui_ui.version` during APK assembly.

In Bazel 8, APK assembly lives in rules_android Starlark, so this behavior needs
to live with the externalized Android rules rather than in Bazel core.

- `inject_compose_ui_version.patch` — adds a rules_android string build setting
  at `//rules/flags:androidx_compose_ui_version`, wires it into
  `android_binary`, and injects `META-INF/androidx.compose.ui_ui.version` into
  the final APK when the value is non-empty.

Consumers that want the old Bazel 7 command-line spelling can add:

```bazelrc
common --flag_alias=androidx_compose_ui_version=@rules_android//rules/flags:androidx_compose_ui_version
common --androidx_compose_ui_version=1.8.3
```

TODO: Wire an equivalent Bazel-level flag again so consumers can eventually
remove the `--flag_alias` bridge and keep only the old Bazel 7 flag spelling.
