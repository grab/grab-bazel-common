# List of Bazel patches

* `databinding-caching.patch` - Fixes layout-info.zip being non deterministic due to timestamp.
* `disable-resource-conflitcs.patch` - Large no of resource conflicts can spam logs with `CONFLICT` messages. This patch disables that.
* https://github.com/bazelbuild/bazel/pull/11253 - Add flag to disable output library.ap_ and reduce output size.
* `databinding-layout-info-caching.patch` - Fixes content of layout-info.zip having absolute path and breaking reproducibility. This needs to be used alongside `databinding-fixes.patch` applied to databinding jars.
* `mobile-install-crash-fix.patch` - Fix for https://github.com/bazelbuild/bazel/issues/11961#issuecomment-799579112
* `databinding-hang-fix.patch` - Fix for https://github.com/bazelbuild/bazel/issues/12780
* `android-resource-workers.patch` - Fix for enabling Android workers when databinding is used https://github.com/bazelbuild/bazel/issues/13649
* Worker support for `GenerateDatabindingBaseClasses` - https://github.com/bazelbuild/bazel/pull/16067