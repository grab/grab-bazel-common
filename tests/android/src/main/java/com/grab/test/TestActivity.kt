package com.grab.test

import android.app.Activity
import android.os.Bundle

class TestActivity : Activity() {
    val material_res = com.google.android.material.R.color.material_blue_grey_950

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun sum(a: Int, b: Int): Int {
        return a + b
    }
}
