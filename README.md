# Grab Bazel Common Android

Common rules and macros for Grab's Android projects built with Bazel. This repo provides rules and macros to support some of Android Gradle
Plugin features in Bazel.

The repo also hosts a patched Bazel Android Tools jar with fixes for build reproducibility and databinding applied.
See [/patches](https://github.com/grab/grab-bazel-common/tree/master/patches) for details.

The rules are used by [Grazel](https://github.com/grab/Grazel) - Gradle plugin to automate migration to Bazel.

# Usage

In `WORKSPACE` file,

```python
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "grab_bazel_common",
    commit = "<commit hash>",
    remote = "https://github.com/grab/grab-bazel-common.git",
)

load("@grab_bazel_common//android:repositories.bzl", "bazel_common_dependencies")

bazel_common_dependencies()

load("@grab_bazel_common//android:initialize.bzl", "bazel_common_initialize")

bazel_common_initialize(
    patched_android_tools = True, # Optionally use patched android_tools jars
    buildifier_version = "5.1.0",
)

load("@grab_bazel_common//android:maven.bzl", "pin_bazel_common_artifacts")

pin_bazel_common_artifacts()

load("@grab_bazel_common//:workspace_defs.bzl", "GRAB_BAZEL_COMMON_ARTIFACTS")

# Bazel common uses few artifacts under @maven repository
maven_install(
    artifacts = GRAB_BAZEL_COMMON_ARTIFACTS + [
      # Your project artifacts 
    ]
)    
```

# Features

### Build Config Fields

`Build Config` support for android projects

```python
load("@grab_bazel_common//tools/build_config:build_config.bzl", "build_config")

build_config(
    name = "feature-toggle-build-config",
    package_name = "com.grab.featuretoggle",
    strings = {
        "ID": "Hello",
    },
    ints = {},
    longs = {},
    strings = {},
)
```

### Res values

Gradle's `resValue` strings support for Android Projects

```python
load("@grab_bazel_common//tools/res_value:res_value.bzl", "res_value")
# Usage of defined resValues
android_library(
    resource_files = [
        ...
    ] + res_value(
        name = "app-res-value",
        strings = {
            "prefix": "app",
            "field": "debug"
        },
    ),
)
```   

### Databinding

Provides a macro which in most cases can be used as a drop-in replacement to `kt_android_library` to enable support for Kotlin code when
using databinding. [Details](https://github.com/grab/grab-bazel-common/blob/documentation/tools/databinding/databinding.bzl). Worker support
for some of the internal actions can be enabled by `build --strategy=DatabindingStubs=worker`.

```python
load("@grab_bazel_common//tools/databinding:databinding.bzl", "kt_db_android_library")

kt_db_android_library(
    name = "module",
    srcs = glob([
        "src/main/java/com/grab/module/**/*.kt",
    ]),
    assets = glob([
        "src/main/assets/empty_file.txt",
    ]),
    assets_dir = "src/main/assets",
    custom_package = "com.grab.module",
    manifest = "src/main/AndroidManifest.xml",
    resource_files = glob([
        "src/main/res/**",
    ]),
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "@maven//:io_reactivex_rxjava2_rxjava",
    ],
)
```

This requires the following flags in `.bazelrc` file.

```python
# Databinding flags
build --experimental_android_databinding_v2
build --android_databinding_use_v3_4_args
build --android_databinding_use_androidx

# Flags to enable latest android providers in rules
build --experimental_google_legacy_api
query --experimental_google_legacy_api
```

### Custom Resource Sets

Bazel expects certain Android resource folder structure (should start with `res/`) and this can conflict with Android Gradle plugin's custom
resource source set feature which does not have this validation. This macro helps to adapt the folder to Bazel expected structure so both
build systems can function.

In Gradle, if you have:

```groovy
sourceSets {
    debug {
        res.srcDirs += "src/main/res-debug"
    }
}
```

the Bazel equivalent would be:

```python
load("@grab_bazel_common//tools/custom_res:custom_res.bzl", "custom_res")

android_binary(
    name = "app",
    custom_package = "com.grab.playground",
    manifest = "src/main/AndroidManifest.xml",
    resource_files = glob([
        "src/main/res/**",
    ]) + custom_res( # Wrap the folder with custom_res macro
        dir_name = "res-debug",
        resource_files = glob([
            "src/main/res-debug/**",
        ]),
        target = "app",
    ),
)
```

### Unit Test Macros

Provides macros to simplify migrating unit tests to Bazel and ports over Android Gradle
Plugin's [return default values](https://developer.android.com/studio/test/index.html#test_options) feature. With default values, Android
Unit tests can be executed without [Robolectric](http://robolectric.org) by relying on mocked `android.jar` as an alternative
to `android_local_test` in Bazel.

The below macros makes assumptions that files containing Kotlin tests are named `*Tests.kt` or `Test.kt` and class name matches the file
name.

#### Kotlin Unit tests

```python
load("@grab_bazel_common//tools/test:test.bzl", "grab_kt_jvm_test")

grab_kt_jvm_test(
    name = "binding-adapter-processor-test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        ":binding-adapter-bridge",
        ":binding-adapter-processor",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@maven//:com_github_tschuchortdev_kotlin_compile_testing",
        "@maven//:junit_junit",
    ],
)
```

This will generate a single build target for all Kotlin files and individual `*Test` targets for each `*Test`
class. [Reference](tools/binding-adapter-bridge/BUILD.bazel).

#### Android Unit tests

Similarly for android unit tests, use `grab_android_local_test` to build and execute tests. [Reference](tools/test/android/BUILD.bazel).

```python
load("@grab_bazel_common//tools/test:test.bzl", "grab_android_local_test")

grab_android_local_test(
    name = "grab_android_local_test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    associates = [
        ":grab_android_local_test_lib_kt",
    ],
    deps = [
        "@maven//:junit_junit",
    ],
)
```

# License

```
Copyright 2021 Grabtaxi Holdings PTE LTE (GRAB)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
