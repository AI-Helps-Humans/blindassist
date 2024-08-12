package com.franos.main.speech

import android.content.Context

interface ITTS {
    fun init(context: Context)
    fun speak(text: String,readJson: Boolean)
    fun switch()
    fun destory()
    fun checkMediaPlayerExist(): Boolean
    fun setPlaySpeed(faster: Boolean)
    fun stopMediaPlayer()
}