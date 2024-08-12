package com.franos.main.speech.defaultimpl

import android.app.Activity
import android.util.Log
import com.chaquo.python.Python
import com.franos.main.speech.STTCallback
import com.franos.main.speech.VoskSpeechRecognizer
import com.franos.main.speech.googleimpl.BaseSTT
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class IAndroidSTTImpl : BaseSTT() {
    private var speechService: SpeechService? = null
    private var model: Model? = null;
    private val py = Python.getInstance()
    var mCallback: STTCallback? = null
    var mIsRecording = false

    override fun init(context: Activity) {
        super.init(context)
        try {

            Log.e(1.toString(), "Model is loading")
            this.initModel()
            Log.e(1.toString(), "after Model is loading")
        } catch (e: IOException) {
            Log.e(TAG, "Model loading failed", e)
        }
    }

    override fun setCallback(callback: STTCallback?) {
        mCallback = callback
    }

    override fun startRecord() {
        mIsRecording = true
        startListening()
    }

    override fun isRecording(): Boolean {
        return mIsRecording
    }

    override fun stopRecord() {
        stopListening()
        mIsRecording = false
    }

    override fun destory() {
        mIsRecording = false
    }

    fun startListening() {
        if (model == null) {
            Log.e(1.toString(), "Model is loaded")
            Log.e(VoskSpeechRecognizer.TAG, "Model is not loaded")
            return
        }
        Log.e(VoskSpeechRecognizer.TAG, "Model is loaded")
        val recognizer = Recognizer(model, 48000.0f)
        speechService = SpeechService(recognizer, 48000.0f)
        speechService!!.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String) {
                Log.d(
                    VoskSpeechRecognizer.TAG,
                    "Partial result: $hypothesis"
                )
                val toByteArray = hypothesis.toByteArray()
                mCallback?.onVoice(toByteArray, toByteArray.size)
            }

            override fun onResult(hypothesis: String) {
                Log.d(VoskSpeechRecognizer.TAG, "Result: $hypothesis")
                mCallback?.onVoiceEnd(hypothesis.toByteArray())
            }

            override fun onFinalResult(hypothesis: String) {
                Log.d(VoskSpeechRecognizer.TAG, "Final result: $hypothesis")
                val module = py.getModule("read")
                val data = module.callAttr("text_to_speech", hypothesis)
            }

            override fun onError(e: Exception) {
                Log.e(VoskSpeechRecognizer.TAG, "Error: ", e)
            }

            override fun onTimeout() {
                Log.d(VoskSpeechRecognizer.TAG, "Timeout")
            }

        })
    }

    fun stopListening() {
        mIsRecording = false
        if (speechService != null) {
            Log.d("vosk", "speechService:stop")
            speechService!!.stop()
            speechService!!.shutdown()
            speechService = null
        }
    }

    private fun initModel() {
        val assetManager = mCtx.assets
        val modelPath = copyAssetFolder(assetManager, "vosk-model-en-us-0.22-lgraph", mCtx.filesDir.absolutePath + "/model")
        Log.d(1.toString(), "modelPath: $modelPath")
        if (modelPath != null) {
            model = Model(modelPath)
        } else {
            Log.e("VoskApp", "Failed to copy model")
        }
    }

    private fun copyAssetFolder(assetManager: android.content.res.AssetManager, srcName: String, dstName: String): String? {
        val srcDir = File(dstName)
        if (!srcDir.exists()) {
            if (!srcDir.mkdirs()) {
                return null
            }
        }
        try {
            val fileList = assetManager.list(srcName)
            if (fileList != null) {
                for (file in fileList) {
                    val srcFile = srcName + "/" + file
                    val dstFile = dstName + "/" + file
                    if (assetManager.list(srcFile)?.isNotEmpty() == true) {
                        copyAssetFolder(assetManager, srcFile, dstFile)
                    } else {
                        copyAssetFile(assetManager, srcFile, dstFile)
                    }
                }
            }
        } catch (e: IOException) {
            return null
        }
        return dstName
    }

    private fun copyAssetFile(assetManager: android.content.res.AssetManager, srcName: String, dstName: String) {
        try {
            assetManager.open(srcName).use { `in`: InputStream ->
                FileOutputStream(dstName).use { out ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
        } catch (e: IOException) {
            Log.e("VoskApp", "Failed to copy asset file: $srcName", e)
        }
    }
}