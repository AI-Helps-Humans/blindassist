package com.franos.main.speech.googleimpl

import android.app.Activity
import android.util.Log
import com.chaquo.myapplication.R
import com.chaquo.python.Python
import com.franos.main.speech.STTCallback
import com.franos.main.speech.VoiceRecorder
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import com.google.cloud.speech.v1.SpeechSettings

import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class IGoogleSTTImpl : BaseSTT() {
    private val py = Python.getInstance()
    var mCallback: STTCallback? = null
    private var speechClient: SpeechClient? = null
    private var mVoiceRecorder: VoiceRecorder? = null
    private var byteArray: ByteArray = byteArrayOf()
    private var mIsRecording = false
    private var testIdx: Int = 0
    override fun init(context: Activity) {
        super.init(context)
        initializeSpeechClient()
    }

    private fun initializeSpeechClient() {
        val credentials = GoogleCredentials.fromStream(mCtx.resources.openRawResource(R.raw.real_app_cred))
        val credentialsProvider = FixedCredentialsProvider.create(credentials)
        speechClient = SpeechClient.create(
            SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()
        )
    }

    override fun setCallback(callback: STTCallback?) {
        mCallback = callback
    }

    override fun startRecord() {
        mIsRecording = true
        startVoiceRecorder()
    }

    override fun isRecording(): Boolean {
        return mIsRecording
    }

    override fun stopRecord() {
        Log.d("python", "in stopRecord")
        stopVoiceRecorder()
        mIsRecording = false
    }

    override fun destory() {
        speechClient?.close()
        mIsRecording = false
    }

    private val mVoiceCallBack: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
//            Log.e("googletest vs", "******")
            mCallback?.onVoiceStart()
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            byteArray = data?.let { byteArray.plus(it) }!!
            mCallback?.onVoice(data, size)
            Log.d("python", "onVoice" + byteArray.toString())

        }

        override fun onVoiceEnd() {
            Log.d("python", "onVoiceEnd111 len ${byteArray.toString().length}")
            transcribeRecording(byteArray)
            byteArray = ByteArray(byteArray.size)
            mCallback?.onVoiceEnd(byteArray)
            mIsRecording = false
        }
    }

    private fun transcribeRecording(data: ByteArray) {
        Log.d("python", "transcribeRecording")
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("python", "transcribeRecording io thread")
            val response = speechClient?.recognize(createRecognizeRequestFromVoice(data))
            val results = response?.resultsList

            val transcription = results?.let { processTranscriptionResults(it) }
//            Log.e("python", "Results ${transcription}"
//            Log.d("python", transcription)
//            var transcription = "aaa"
            if (transcription != null) {
                val module = py.getModule("agent")
                module.callAttr("agent", transcription)
//                Log.d("myLog trans e", data.toString())
            }
        }
    }

    private fun processTranscriptionResults(results: List<SpeechRecognitionResult>): String {
        val stringBuilder = StringBuilder()
        for (result in results) {
            val recData: SpeechRecognitionAlternative = result.alternativesList[0]
            stringBuilder.append(recData.transcript)
        }
        return stringBuilder.toString()
    }


    private fun createRecognizeRequestFromVoice(audioData: ByteArray): RecognizeRequest {
        val audioBytes =
            RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audioData)).build()
        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("en-US")
            .build()
        return RecognizeRequest.newBuilder()
            .setConfig(config)
            .setAudio(audioBytes)
            .build()
    }

    private fun startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
        }
        Log.d("python", "myLog startVoiceRr1")
        mVoiceRecorder = VoiceRecorder(mVoiceCallBack)
        mVoiceRecorder!!.start()
        Log.d("python", "myLog startVoiceRr2")
    }

    private fun stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            Log.d("myLog stopVoiceRecorder", "***")
            mVoiceRecorder!!.stop()
            mVoiceRecorder = null
        }
        mIsRecording = false
    }

}