# aar_import

Making aar_import actually work for our SDKs. Default aar_import is too minimal, doesn't propagate everything consumers need.

- `aar_import_export_r_java.patch` — always export R java (was gated by an ACL). Consumers of AAR SDKs need transitive R access to compile.
- `aar_import_transitive_r_txt.patch` — force aar_import into the transitive merge_compiled path so its R.txt has dep symbols. Pre-compiled AAR bytecode references transitive R fields at runtime, they must exist in the lib R class.
- `fix_aar_import_databinding_info.patch` — extract `data-binding/` dir + manifest package from AAR, propagate via DataBindingV2Info. Without this, consumers with databinding can't find AAR `@BindingAdapter`s (like `android:onClick`), fails with "Cannot find a setter".
- `native_databinding_package_info.patch` — bridge for `--android_databinding_package_info` native flag so aar_import package can be overridden at build time.
