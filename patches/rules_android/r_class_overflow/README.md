# r_class_overflow

Big apps have >64K fields in some R inner classes (R$id, R$string). ASM's `ClassWriter.toByteArray()` throws `ClassTooLargeException` when the constant pool overflows. These patches defend at every point R classes are written.

- `binary_r_class_too_large_fix.patch` — catch ClassTooLargeException in ResourceSymbols.java, fall back to split .java files (`R_id_0`, `R_id_1`, ... with inheritance chain) and compile via javac. `R.id.foo` still works because fields inherit up the chain.
- `merge_compiled_r_class_too_large_fix.patch` — RClassGenerator.java: proactively split inner classes at >8000 fields instead of waiting for ASM to throw. Chunks `R_id_0 ← R_id_1 ← ... ← R$id` at the bytecode level.
- `binary_r_primary_only.patch` — ResourceSymbols.java: only emit "full transitive" R class for packages that actually need it at runtime (hardcoded prefix list of AARs). Rest gets filtered (own symbols only), faster and less bloat.
- `merge_primary_library_symbols.patch` — shrink that hardcoded `transitiveRPackages` set to just `com.google.android.material` and `com.grab.geo`. Add a prefix here ONLY when a runtime NoSuchFieldError proves it's needed.
