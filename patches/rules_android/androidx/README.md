# androidx

AndroidX migration. Default rules_android still generates databinding against old `android.databinding.*` namespace.

- `use_androidx.patch` — flip `-useAndroidX` false → true for databinding AP. Without this, generated binding classes reference the removed support-library package.
- `androidx_annotation_template.patch` — update `DataBindingInfo` template to import from `androidx.databinding.*`, drop the deprecated `buildId` attr that AndroidX's annotation no longer takes.
