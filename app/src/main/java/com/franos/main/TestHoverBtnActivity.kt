package com.franos.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.chaquo.myapplication.R
import com.franos.main.hover.FloatingButtonService
import com.franos.main.permission.FloatWindowManager

class TestHoverBtnActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hover_layout)
        findViewById<Button>(R.id.launch).setOnClickListener {
            if (FloatWindowManager.getInstance().checkPermission(this@TestHoverBtnActivity)) {
                val intent = Intent(this@TestHoverBtnActivity, FloatingButtonService::class.java)
                startService(intent)
            } else {
                FloatWindowManager.getInstance().applyPermission(this@TestHoverBtnActivity)
            }
        }

    }

}