package com.franos.main.speech.googleimpl

import android.content.Context
import com.franos.main.speech.ITTS

class IGoogleTTSImpl : ITTS {

    lateinit var ttsTool: GoogleTTSTool
    private var speed: Float = 1.0f

    override fun init(context: Context) {
        ttsTool = GoogleTTSTool(context)
    }

    override fun speak(text: String, readJson: Boolean) {
        ttsTool.speak(text, readJson, speed)
    }

    override fun switch() {
        ttsTool.switch()
    }

    override fun destory() {
    }

    override fun checkMediaPlayerExist(): Boolean {
        if (ttsTool.mediaPlayer != null){
            return true
        } else {
            return false
        }
    }

    override fun stopMediaPlayer(){
        ttsTool.mediaPlayer?.pause()
        ttsTool.mediaPlayer?.stop()
        ttsTool.mediaPlayer?.reset()
        ttsTool.mediaPlayer?.release()
    }

    override fun setPlaySpeed(faster: Boolean){
        if (faster == true){
            speed = speed + 0.5f
        } else {
            speed = speed - 0.5f
        }
    }
}