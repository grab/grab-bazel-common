# Custom Patches Applied to Databinding Libraries

The following fixes were applied to databinding compiler and runtime libraries on version 7.1.0.

### Error when building multiple databinding libraries in parallel

This error appears when lot of modules are built in parallel and might not occur for small projects. The patch changes the error severity to warning. The code path tries to load a file and can fail with stream closed error, see attached issue for more details.

Bazel issue: https://github.com/bazelbuild/bazel/issues/12768

```diff
--- a/compiler/src/main/java/android/databinding/tool/reflection/SdkUtil.java
+++ b/compiler/src/main/java/android/databinding/tool/reflection/SdkUtil.java
@@ -123,7 +123,7 @@ public class SdkUtil {
                 mXPath = xPathFactory.newXPath();
                 buildFullLookup();
             } catch (Throwable t) {
-                L.e(t, "cannot load api descriptions from %s", apiFile);
+                L.w(t, "cannot load api descriptions from %s", apiFile);
             } finally {
                 IOUtils.closeQuietly(inputStream);
             }
```

### Databinding artifacts are not cacheable

#### Base Classes

Databinding produces non deterministic Zip files due to time stamp embedded in Zip files. This change simply hardcodes timestamp to ensure reproducibility.

```diff
diff --git a/exec/src/main/java/android/databinding/AndroidDataBinding.kt b/exec/src/main/java/android/databinding/AndroidDataBinding.kt
index 537bc705..0ac5da71 100644
--- a/exec/src/main/java/android/databinding/AndroidDataBinding.kt
+++ b/exec/src/main/java/android/databinding/AndroidDataBinding.kt
@@ -218,7 +218,9 @@ object AndroidDataBinding {
         }
 
         private fun doWrite(entryPath: String, contents: String) {
-            val entry = ZipEntry(entryPath)
+            val entry = ZipEntry(entryPath).apply {
+                time = ZipUtil.DEFAULT_TIMESTAMP
+            }
             try {
                 zos.putNextEntry(entry)
                 zos.write(contents.toByteArray(Charsets.UTF_8))
diff --git a/exec/src/main/java/android/databinding/cli/ZipUtil.kt b/exec/src/main/java/android/databinding/cli/ZipUtil.kt
index 7d41ac3c..d611360e 100644
--- a/exec/src/main/java/android/databinding/cli/ZipUtil.kt
+++ b/exec/src/main/java/android/databinding/cli/ZipUtil.kt
@@ -21,11 +21,14 @@ import org.apache.commons.io.FileUtils
 import org.apache.commons.io.filefilter.TrueFileFilter
 import java.io.File
 import java.io.FileOutputStream
+import java.util.*
 import java.util.zip.ZipEntry
 import java.util.zip.ZipFile
 import java.util.zip.ZipOutputStream
 
 object ZipUtil {
+    val DEFAULT_TIMESTAMP = LocalDateTime.of(2010, 1, 1, 0, 0, 0)
+        .atZone(ZoneId.systemDefault())
+        .toInstant()
+        .toEpochMilli()
+
     fun unzip(file: File, outFolder: File) {
         if (!outFolder.exists() && !outFolder.mkdirs()) {
             throw RuntimeException("unable to create out folder ${outFolder.absolutePath}")
@@ -57,7 +60,9 @@ object ZipUtil {
                     .forEach { file ->
                         val entry = ZipEntry(
                             file.absolutePath.substring(inputAbsPath + 1)
-                        )
+                        ).apply {
+                            time = DEFAULT_TIMESTAMP
+                        }
                         try {
                             zos.putNextEntry(entry)
                             zos.write(file.readBytes())
@@ -68,6 +73,6 @@ object ZipUtil {
                     }
             }
         }
-
+        outFile.setLastModified(DEFAULT_TIMESTAMP)
     }
 }
```

#### Layout info zip

Layout info zip encodes absolute path in the zip. This changes fixes it by passing baseDir to `RelativizableFile` which ensures relative path can be computed. It requires a patch on Bazel binary as well, see `databinding-layout-info-caching.patch`

```diff
diff --git a/compilerCommon/src/main/java/android/databinding/tool/LayoutXmlProcessor.java b/compilerCommon/src/main/java/android/databinding/tool/LayoutXmlProcessor.java
index a9fd885c..564d15ac 100644
--- a/compilerCommon/src/main/java/android/databinding/tool/LayoutXmlProcessor.java
+++ b/compilerCommon/src/main/java/android/databinding/tool/LayoutXmlProcessor.java
@@ -182,7 +182,7 @@ public class LayoutXmlProcessor {
             public void processLayoutFile(File file)
                     throws ParserConfigurationException, SAXException, XPathExpressionException,
                     IOException {
-                processSingleFile(RelativizableFile.fromAbsoluteFile(file, null),
+                processSingleFile(RelativizableFile.fromAbsoluteFile(file, input.getRootInputFolder()),
                         convertToOutFile(file), isViewBindingEnabled);
             }
```

#### Others
* https://android.googlesource.com/platform/frameworks/data-binding/+/ae769bc6781a47e7e89a4ddfc0c4f8be9b6f76c1 Use Unix-like paths when sorting files
* https://android.googlesource.com/platform/frameworks/data-binding/+/99f2b7c1e6db27ad89f42cbe85f3bf938209e245 Ensure deterministic order of entries in GenClassInfoLog
* https://android.googlesource.com/platform/frameworks/data-binding/+/b1c2715b1be141226f9015618e0013536479c244 Sort files in directories to ensure deterministic order
* Uses 7.1.0 version of databinding compiler and related libraries