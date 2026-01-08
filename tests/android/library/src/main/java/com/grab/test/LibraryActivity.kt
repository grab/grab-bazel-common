package com.grab.test

import android.app.Activity
import android.os.Bundle
import com.grab.test.di.DaggerLibraryActivityComponent

class LibraryActivity : Activity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerLibraryActivityComponent.factory().create().someDependency().doSomething()
    }

    fun x(){

    }

}
