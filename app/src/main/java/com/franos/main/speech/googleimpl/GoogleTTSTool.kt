package com.franos.main.speech.googleimpl

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import com.chaquo.myapplication.R
import com.chaquo.python.Python
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.texttospeech.v1.AudioConfig
import com.google.cloud.texttospeech.v1.AudioEncoding
import com.google.cloud.texttospeech.v1.SsmlVoiceGender
import com.google.cloud.texttospeech.v1.SynthesisInput
import com.google.cloud.texttospeech.v1.TextToSpeechClient
import com.google.cloud.texttospeech.v1.TextToSpeechSettings
import com.google.cloud.texttospeech.v1.VoiceSelectionParams
import com.google.protobuf.ByteString
import java.io.File
import java.io.FileOutputStream


class GoogleTTSTool constructor(context: Context) {
    private var tts: TextToSpeechClient? = null
    private val filesDir = context.filesDir
    private var isPause = false
    private val outputFileName = "output.wav"
    public var mediaPlayer: MediaPlayer? = null
    init {
        val credentials = GoogleCredentials.fromStream(context.resources.openRawResource(R.raw.real_app_cred))
        val credentialsProvider = FixedCredentialsProvider.create(credentials)
        tts = TextToSpeechClient.create(
            TextToSpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()
        )

    }

    fun speak(text: String, readJson: Boolean = false, speed: Float = 1.0f) {
        // 配置合成请求
        val input = SynthesisInput.newBuilder()
            .setText(text)
            .build()

        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode("en-US")
            .setSsmlGender(SsmlVoiceGender.NEUTRAL)
            .build()

        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.LINEAR16)
            .build()

        // 执行合成
        val response = tts!!.synthesizeSpeech(input, voice, audioConfig)
        val audioContents: ByteString = response!!.audioContent

        // 将音频内容写入文件
        val outputFile = File(filesDir,outputFileName)
        FileOutputStream(outputFile).use { it.write(audioContents.toByteArray()) }

        // 播放音频文件
        playAudio(outputFile, readJson, speed)
    }

    private fun playAudio(outputFile: File, readJson: Boolean, speed: Float) {
        this.mediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val params = mediaPlayer!!.playbackParams
            params.setSpeed(speed)
            mediaPlayer!!.playbackParams = params
        }
        mediaPlayer!!.setDataSource(outputFile.path)
        mediaPlayer!!.prepare()
        mediaPlayer!!.start()
        mediaPlayer!!.setOnCompletionListener {
            it.release()
            mediaPlayer = null
            if (readJson == true){
                var py = Python.getInstance()
                val module = py.getModule("agent")
                module.callAttr("do_action_read_json_end")
            }

        }
    }

    public fun switch(){
        if (isPause == true){
            resumeAudio()
        }else{
            pauseAudio()
        }

    }


    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPause = true
    }

    private fun resumeAudio() {
        mediaPlayer?.start()
        isPause = false
    }

}

