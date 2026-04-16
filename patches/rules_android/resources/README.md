# resources

Misc resource pipeline fixes.

- `android_resource_processor_bazel_paths.patch` — databinding needs absolute path for `resInput`. Default passes relative, AP chokes on it in the Bazel sandbox.
- `fix_resource_priority_ordering.patch` — flip `preorder` → `postorder` on resource depsets and reverse dep iteration. aapt2 is last-wins for resource conflicts; this makes first-declared dep win (what BUILD authors expect — `deps = [a, b]` means a's resources beat b's).
