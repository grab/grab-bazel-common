package com.grab.databinding.stub.mapper

import com.grab.databinding.stub.AaptScope
import com.grab.databinding.stub.common.SrcJarPackageModule
import com.grab.databinding.stub.common.SrcJarPackager
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