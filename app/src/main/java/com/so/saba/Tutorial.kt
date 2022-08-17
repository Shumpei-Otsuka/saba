package com.so.saba

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class Tutorial : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
    }

    fun onFinish(view: View) {
        finish()
    }
}