package com.google.devtools.build.android

import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException
import kotlin.io.path.name

class SourceSetTest {

    @Test
    fun `assert sourceset is parsed as expected from input sourceset string`() {
        assertThrows("Invalid format, should be resources:assets:manifest", IllegalArgumentException::class.java) {
            SourceSet.from("target", ":::")
        }
        assertThrows("Invalid format, should be resources:assets:manifest", IllegalArgumentException::class.java) {
            SourceSet.from("target", "res:assets")
        }
        SourceSet.from("target", "res::manifest").let { sourceSet ->
            assertTrue("Res is parsed", sourceSet.resourceDirs.all { it.name == "res" })
            assertTrue("Empty paths are skipped", sourceSet.assetDirs.isEmpty())
            assertTrue("Manifest is parsed", sourceSet.manifest.name == "manifest")
        }
    }
}