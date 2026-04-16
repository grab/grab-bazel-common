# desugar

Desugaring is slow. R class jars contain only static final int constants, no Java 8+ bytecode, so desugaring them is pure overhead. These patches skip it.

- `skip_binary_r_jar_desugaring_flags.patch` — adds `//rules/flags:desugar_resources_jar` bool_flag.
- `skip_binary_r_jar_desugaring_attrs.patch` — wire the flag as private attr on android_binary.
- `skip_binary_r_jar_desugaring_impl.patch` — in android_binary impl, when flag=false and jar is `_resources.jar`, pass through untransformed.
- `skip_r_jar_desugaring.patch` — per-library: drop `AndroidIdeInfo.resource_jar` from the desugar aspect's jar set. Same idea but for every lib's R.jar (matters a lot when `link_library_resources=false` produces large transitive R jars).

Flip `common --@rules_android//rules/flags:desugar_resources_jar=false` in bazelrc to enable the binary-side skip.
