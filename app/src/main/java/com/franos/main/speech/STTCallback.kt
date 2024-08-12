package com.franos.main.speech

interface STTCallback {

    fun onVoiceStart()
    fun onVoice(data: ByteArray?, size: Int)
    fun onVoiceEnd(data: ByteArray?)
}