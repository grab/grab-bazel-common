package com.grab.lint

import java.io.File
import java.nio.file.Path
import com.android.tools.lint.Main as LintCli

class LintAnalyzeCommand : LintBaseCommand() {

    override fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
        lintBaselineHandler: LintBaselineHandler
    ) {
        val cliArgs = (defaultLintOptions + listOf(
            "--project", projectXml.toString(),
            "--analyze-only" // Only do analyze
        )).toTypedArray()
        LintCli().run(cliArgs)
    }
}