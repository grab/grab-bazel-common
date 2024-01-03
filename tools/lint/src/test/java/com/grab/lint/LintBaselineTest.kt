package com.grab.lint

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertTrue

class LintBaselineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var orgBaseline: File
    private lateinit var updatedBaseline: File
    private lateinit var lintBaseline: LintBaseline

    companion object {
        private const val TMP_DIR = "/var/folders/k_/g4n9rwbj4qs2_g9_gby95zlr0000gn/T/"
        private const val PWD = "private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/33/execroot/grazel"

        private object TestEnv : Env {
            override val tmpDir = TMP_DIR
            override val pwd = PWD
        }
    }

    fun setup(orgBaselineContents: String) {
        val workingDir = temporaryFolder.newFolder()
        orgBaseline = workingDir.resolve("baseline.xml").apply {
            writeText(orgBaselineContents)
        }
        updatedBaseline = workingDir.resolve("updated.xml")
        lintBaseline = LintBaseline(
            workingDir = workingDir.toPath(),
            orgBaselineFile = orgBaseline,
            updatedBaseline = updatedBaseline,
            verbose = false,
            env = TestEnv
        )
    }

    @Test
    fun `assert generated file does not contain tmp dir in the path`() {
        setup(
            orgBaselineContents = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="5" by="lint 8.0.2" client="" dependencies="true" name="" type="baseline" variant="all" version="8.0.2">

                <issue
                    id="Typos"
                    message="Did you mean &quot;flavor!&quot; instead of &quot;flavor1&quot;?">
                    <location
                        file="/var/folders/k_/g4n9rwbj4qs2_g9_gby95zlr0000gn/T/../../../../../private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/32/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android-flavor/sample-android-flavor-flavor1-free-debug_res/out/res/values/values.xml"
                        line="5"/>
                </issue>

                <issue
                    id="MonochromeLauncherIcon"
                    message="The application adaptive icon is missing a monochrome tag">
                    <location
                        file="/var/folders/k_/g4n9rwbj4qs2_g9_gby95zlr0000gn/T/../../../../../private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/33/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android/sample-android-flavor1-free-debug_res/out/res/mipmap-anydpi-v26/ic_launcher.xml"
                        line="17"/>
                </issue>
            </issues>

        """.trimIndent()
        )
        lintBaseline.postProcess(orgBaseline)
        assertTrue("TMP Dir removed from baseline file path") {
            updatedBaseline.useLines { lines -> lines.filter { it.contains("file") }.all { TestEnv.tmpDir !in it } }
        }
    }

    @Test
    fun `assert relative path removed from baseline file output`() {
        setup(
            orgBaselineContents = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="5" by="lint 8.0.2" client="" dependencies="true" name="" type="baseline" variant="all" version="8.0.2">

                <issue
                    id="Typos"
                    message="Did you mean &quot;flavor!&quot; instead of &quot;flavor1&quot;?">
                    <location
                        file="../../../../../private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/32/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android-flavor/sample-android-flavor-flavor1-free-debug_res/out/res/values/values.xml"
                        line="5"/>
                </issue>

                <issue
                    id="MonochromeLauncherIcon"
                    message="The application adaptive icon is missing a monochrome tag">
                    <location
                        file="../../../../../private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/33/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android/sample-android-flavor1-free-debug_res/out/res/mipmap-anydpi-v26/ic_launcher.xml"
                        line="17"/>
                </issue>
            </issues>

        """.trimIndent()
        )
        lintBaseline.postProcess(orgBaseline)
        assertTrue("Relative path removed from baseline output") {
            updatedBaseline.useLines { lines -> lines.filter { it.contains("file") }.all { "../" !in it } }
        }
    }

    @Test
    fun `assert absolute pwd path removed from baseline output`() {
        setup(
            orgBaselineContents = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="5" by="lint 8.0.2" client="" dependencies="true" name="" type="baseline" variant="all" version="8.0.2">

                <issue
                    id="Typos"
                    message="Did you mean &quot;flavor!&quot; instead of &quot;flavor1&quot;?">
                    <location
                        file="private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/32/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android-flavor/sample-android-flavor-flavor1-free-debug_res/out/res/values/values.xml"
                        line="5"/>
                </issue>

                <issue
                    id="MonochromeLauncherIcon"
                    message="The application adaptive icon is missing a monochrome tag">
                    <location
                        file="private/var/tmp/_bazel_root/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/33/execroot/grazel/bazel-out/android-armeabi-v7a-fastbuild/bin/sample-android/sample-android-flavor1-free-debug_res/out/res/mipmap-anydpi-v26/ic_launcher.xml"
                        line="17"/>
                </issue>
            </issues>

        """.trimIndent()
        )
        lintBaseline.postProcess(orgBaseline)
        assertTrue("Pwd removed from baseline file path") {
            updatedBaseline.useLines { lines -> lines.filter { it.contains("file") }.all { TestEnv.pwd !in it } }
        }
    }
}