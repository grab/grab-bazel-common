package com.grab.aapt.databinding.mapper

import com.grab.aapt.AaptScope
import com.grab.aapt.databinding.common.SrcJarPackageModule
import com.grab.aapt.databinding.common.SrcJarPackager
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