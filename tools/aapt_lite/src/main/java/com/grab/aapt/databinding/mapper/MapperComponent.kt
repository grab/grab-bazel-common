package com.grab.aapt.databinding.mapper

import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.util.jars.SrcJarPackageModule
import com.grab.aapt.databinding.util.jars.SrcJarPackager
import dagger.Component

@AaptScope
@Component(
    modules = [
        SrcJarPackageModule::class
    ]
)
interface MapperComponent {
    val srcJarPackager: SrcJarPackager
}