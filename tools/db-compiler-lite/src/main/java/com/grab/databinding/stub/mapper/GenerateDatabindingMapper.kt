package com.grab.databinding.stub.mapper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.grab.databinding.stub.binding.generator.generateDataBindingComponentInterface
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

private val MAPPER_FILE_TEMPLATE = """
    package %s;
    
    import android.util.SparseArray;
    import android.util.SparseIntArray;
    import android.view.View;

    import androidx.databinding.DataBinderMapper;
    import androidx.databinding.DataBindingComponent;
    import androidx.databinding.ViewDataBinding;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;

    public class DataBinderMapperImpl extends DataBinderMapper {

        @Override
        public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
            return null;
        }

        @Override
        public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
            return null;
        }

        @Override
        public int getLayoutId(String tag) {
            return 0;
        }

        @Override
        public String convertBrIdToString(int localId) {
            return null;
        }
    }
""".trimIndent()


class GenerateMapperCommand : CliktCommand() {

    private val packageName by option(
        "-p",
        "--package",
        help = "Package name of R class"
    ).required()

    private val output by option(
        "-o",
        "--output",
        help = "The mapper.srcjar file containing generated DatabindingMapper file"
    ).convert { File(it) }.required()

    private fun generateDatabindingMapper(dir: Path) {
        val packageDir = dir.resolve(packageName.replace(".", File.separator))
        Files.createDirectories(packageDir)
        val outputPath = packageDir.resolve("DataBinderMapperImpl.java")
        OutputStreamWriter(
            Files.newOutputStream(outputPath),
            StandardCharsets.UTF_8
        ).use { writer ->
            writer.write(MAPPER_FILE_TEMPLATE.format(packageName))
        }
    }

    override fun run() {
        val tmpDir = Files.createTempDirectory("tmp")
        generateDatabindingMapper(tmpDir)
        generateDataBindingComponentInterface(tmpDir.toFile())
        val packager = DaggerMapperComponent.create().srcJarPackager
        packager.packageSrcJar(
            inputDir = tmpDir.toFile(),
            outputFile = output
        )
    }
}