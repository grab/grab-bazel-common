package com.grab.test

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class TestActivity : Activity() {
    val material_res = com.google.android.material.R.color.material_blue_grey_950

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_test)
        android.util.Log.d("SomeReallyLongTagForLintToDetectAndWarn", "Log message")
    }

    fun sum(a: Int, b: Int): Int {
        return a + b
    }

    @Parcelize
    data class ParcelExample(val value: String) : Parcelable
}
