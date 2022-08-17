package com.so.saba

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class Tutorial : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        val sharedPref = this.getSharedPreferences("PreviousState", Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putBoolean("TutorialDisplayed", true)
            apply()
        }
    }

    fun onFinish(view: View) {
        finish()
    }
}