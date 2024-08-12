package com.franos.main.speech

import android.app.Activity

interface ISTT {

    fun init(context: Activity)

    fun setCallback(callback: STTCallback?)

    fun startRecord()

    fun isRecording(): Boolean

    fun stopRecord()

    fun destory()

    fun checkHasRecordPermission(): Boolean

}