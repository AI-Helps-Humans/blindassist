package com.franos.main.hover

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: Button

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        windowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingButton = Button(this)
        floatingButton.text = "Floating Button"
        floatingButton.setBackgroundColor(Color.BLUE)
        floatingButton.setOnLongClickListener {
            vibrate()
            startRecord()
            return@setOnLongClickListener true
        }
        floatingButton.setOnClickListener {
            stopSelf()
        }

        val params = WindowManager.LayoutParams(
            200,
           200,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER or Gravity.START
        params.x = windowManager.defaultDisplay.width - params.width
        params.y = 100
        windowManager.addView(floatingButton, params)
    }

    private fun startRecord() {
        //todo START
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingButton.isAttachedToWindow) {
            windowManager.removeView(floatingButton)
        }
    }
}