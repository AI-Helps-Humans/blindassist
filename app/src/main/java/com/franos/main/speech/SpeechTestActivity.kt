package com.franos.main.speech

//import android.speech.RecognitionListener

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.myapplication.R
import com.chaquo.python.Python
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class VoskSpeechRecognizer (private val context: Context){
    private var speechService: SpeechService? = null
    private var model: Model? = null;
    private val py = Python.getInstance()
    init {
        try {

            Log.e(1.toString(), "Model is loading")
            this.initModel()
            Log.e(1.toString(), "after Model is loading")
        } catch (e: IOException) {
            Log.e(TAG, "Model loading failed", e)
        }
    }

    fun startListening() {
        if (model == null) {
            Log.e(1.toString(), "Model is loaded")
            Log.e(TAG, "Model is not loaded")
            return
        }
        Log.e(TAG, "Model is loaded")
        val recognizer = Recognizer(model, 48000.0f)
        speechService = SpeechService(recognizer, 48000.0f)
        speechService!!.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String) {
                Log.d(
                    TAG,
                    "Partial result: $hypothesis"
                )
            }

            override fun onResult(hypothesis: String) {
                Log.d(TAG, "Result: $hypothesis")
            }

            override fun onFinalResult(hypothesis: String) {
                Log.d(TAG, "Final result: $hypothesis")
                val module = py.getModule("read")
                val data = module.callAttr("text_to_speech", hypothesis)
            }

            override fun onError(e: Exception) {
                Log.e(TAG, "Error: ", e)
            }

            override fun onTimeout() {
                Log.d(TAG, "Timeout")
            }

        })
    }

    fun stopListening() {
        if (speechService != null) {
            Log.d("vosk", "speechService:stop")
            speechService!!.stop()
            speechService!!.shutdown()
            speechService = null
        }
    }

    companion object {
        const val TAG = "VoskSpeechRecognizer"
    }


    private fun initModel() {
        val assetManager = this.context.assets
        val modelPath = copyAssetFolder(assetManager, "vosk-model-en-us-0.22-lgraph", this.context.filesDir.absolutePath + "/model")
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

class SpeechTestActivity : AppCompatActivity() {
    var txt: TextView? = null
    private var voskSpeechRecognizer: VoskSpeechRecognizer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speach_test)
        voskSpeechRecognizer = VoskSpeechRecognizer(this)
        txt = findViewById(R.id.result)
        Log.d(1.toString(), "savedInstanceState")
        findViewById<Button>(R.id.record).setOnLongClickListener {
            if (!checkHasRecordPermission()) {
                Log.d(1.toString(), "startListening")
                return@setOnLongClickListener true
            } else {
                Log.d(1.toString(), "startListening ")
                voskSpeechRecognizer?.startListening()
                Log.d(1.toString(), "afterstartListening")
            true}
        }

        findViewById<Button>(R.id.record).setOnTouchListener { v, event ->
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.d(1.toString(), "stopListening")
                voskSpeechRecognizer?.stopListening()
            }
            false
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