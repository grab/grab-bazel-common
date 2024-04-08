package com.grab.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.grab.test.lib.LayoutTestBinding
import com.grab.test.lib.R

class LibraryActivity : ComponentActivity() {

    private lateinit var binding: LayoutTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutTestBinding.inflate(R.layout.layout_test)
        setContentView(binding.view)
    }
}
