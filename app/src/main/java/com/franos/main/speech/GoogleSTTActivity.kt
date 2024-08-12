package com.franos.main.speech

//import android.speech.RecognitionListener

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.myapplication.R
import com.chaquo.python.Python

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


class GoogleSpeechTestActivity : AppCompatActivity() {
    var txt: TextView? = null
    private val py = Python.getInstance()
    private var speechClient: SpeechClient? = null
    private var mVoiceRecorder: VoiceRecorder? = null
    private var byteArray: ByteArray = byteArrayOf()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speach_test)
        txt = findViewById(R.id.result)
        Log.d("googletest", "onCreate(savedInstanceState: Bundle?")
        findViewById<Button>(R.id.record).setOnLongClickListener {
            if (!checkHasRecordPermission()) {
                return@setOnLongClickListener true
            } else {
                Log.d("googletest", "start listening")
//                startRecording()
                initializeSpeechClient()
                startVoiceRecorder()
                true
            }
        }
        findViewById<Button>(R.id.record).setOnTouchListener { v, event ->
            if (event.getAction() == MotionEvent.ACTION_UP) {
                stopVoiceRecorder()
            }
            false
        }
    }

    private fun initializeSpeechClient() {
        val credentials = GoogleCredentials.fromStream(resources.openRawResource(R.raw.real_app_cred))
        val credentialsProvider = FixedCredentialsProvider.create(credentials)
        speechClient = SpeechClient.create(
            SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()
        )
    }

    private val mVoiceCallBack: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
//            Log.e("googletest vs", "******")
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            byteArray = data?.let { byteArray.plus(it) }!!
            Log.e("googletestre", "***" + byteArray.toString())

        }

        override fun onVoiceEnd() {
            Log.e("googletestre", "len ${byteArray.toString().length}")
            transcribeRecording(byteArray)
            byteArray = ByteArray(byteArray.size)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechClient?.close()
    }

    private fun transcribeRecording(data: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = speechClient?.recognize(createRecognizeRequestFromVoice(data))
            val results = response?.resultsList
            val transcription = results?.let { processTranscriptionResults(it) }
//            Log.e("googletestre1", "Results")
            if (transcription != null) {
                Log.e("googletestre", transcription)
                val module = py.getModule("read")
                val data = module.callAttr("text_to_speech_google", transcription)
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
//        Log.e("googletest startVoice", "Ssssss")
        mVoiceRecorder = VoiceRecorder(mVoiceCallBack)
        mVoiceRecorder!!.start()
    }

    private fun stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
            mVoiceRecorder = null
        }
    }

    fun checkHasRecordPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
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

