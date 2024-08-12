package com.franos.main.speech.googleimpl

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.franos.main.speech.ISTT

abstract class BaseSTT : ISTT {

    protected lateinit var mCtx: Activity
    protected val TAG = "VoskSpeechRecognizer"

    override fun init(context: Activity) {
        mCtx = context
    }

    override fun checkHasRecordPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    mCtx,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    mCtx,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    10023
                )
                return false
            } else {
                return true
            }
        }
        return true
    }


}