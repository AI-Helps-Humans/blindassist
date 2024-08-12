package com.franos.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import com.chaquo.myapplication.R
import com.chaquo.python.Python
import com.franos.main.html.WebViewHelper
import com.franos.main.speech.AndroidTTSTool
import com.franos.main.speech.Factory
import com.franos.main.speech.STTCallback
import com.franos.main.speech.googleimpl.GoogleTTSTool
import java.io.File
import java.io.FileOutputStream

class BlindAssistantMainActivity : Activity() {

//    private lateinit var webViewHelper: WebViewHelper
    private var button_used = false
    companion object {
        @Volatile
        public var buildSTT = Factory.buildSTT()

        @Volatile
        public var buildTTS = Factory.buildTTS()

        @Volatile
        public lateinit var webViewHelper: WebViewHelper

        @JvmStatic
        fun speakText(text: String?) {
            buildTTS.speak(text!!,false)
        }

        @JvmStatic
        fun loadurl(url: String) {
            webViewHelper.loadURLAndToFetch(url)
        }

        @JvmStatic
        fun fillin(jsscript: String) {
            webViewHelper.helpfillin(jsscript)
        }

        @JvmStatic
        fun startReadJson(text: String){
            buildTTS.speak(text!!, true)
        }

    }

    private lateinit var gestureDetector: GestureDetector
    private val py = Python.getInstance()

    val TAG = "python"
    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blind_main_layout)

        webViewHelper = WebViewHelper(this)
        val webConTainer = findViewById<FrameLayout>(R.id.webview)
        webConTainer.addView(webViewHelper.webView)

        buildSTT.init(this)
        buildTTS.init(this)
        buildSTT.setCallback(object: STTCallback {
            override fun onVoiceStart() {
                Log.i(TAG, "onVoiceStart")
            }

            override fun onVoice(data: ByteArray?, size: Int) {
                Log.i(TAG, "onVoice $data size $size")

            }

            override fun onVoiceEnd(data: ByteArray?) {
                Log.i(TAG, "onVoice $data")
            }
        })
        val btn = findViewById<ImageView>(R.id.click_btn)
        btn.setImageResource(R.mipmap.main_screen)

        // Begin handling gestures and single, double, and long presses
        // No audio playback state (excluding paused state):
        // Single tap: Decrease speech rate
        // Double tap: Increase speech rate
        // Long press: Start voice input
        // Swipe up: Scroll up the webpage (replay the main content of the previous page)
        // Swipe down: Scroll down the webpage
        // Swipe left: Previous page (navigate forward)
        // Swipe right: Next page (navigate backward)

        // Audio playback state (including paused state):
        // Single tap: Pause/Resume playback
        // Double tap: Exit audio playback state (release MediaPlayer)
        // Long press: Exit audio playback state and start voice input
        // Swipe up: Play previous section (parent node) content
        // Swipe down: Play next section (child node) content
        // Swipe left: Play content of the left neighbor
        // Swipe right: Play content of the right neighbor



        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Log.i("python", "guesture singltap")
                handleSingleClick(e)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleDoubleClick(e)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                handleSwipeGesture(e1, e2)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                handleLongPress(e)
            }
        })

         btn.setOnTouchListener { _, event ->
             Log.d(TAG, "setOnTouchListener ${event.getAction()}")
             gestureDetector.onTouchEvent(event)

             if (event.getAction() == MotionEvent.ACTION_UP) {
                 Log.d("python", "stopListening")
                 buildSTT.stopRecord()
                 button_used = false
                 return@setOnTouchListener true
             }
             true
         }
    }

    private fun handleSingleClick(e: MotionEvent?) {
        if (buildTTS.checkMediaPlayerExist()==true) {
            // Pause/Resume
            buildTTS.switch()
        } else {
            // Decrease speech rate
            buildTTS.setPlaySpeed(false)
        }
    }

    private fun handleDoubleClick(e: MotionEvent?) {
        if (buildTTS.checkMediaPlayerExist()==true) {
            // Stop and exit
            buildTTS.stopMediaPlayer()
        } else {
            // Increase speech rate
            buildTTS.setPlaySpeed(true)
        }

    }

    private fun handleLongPress(e: MotionEvent?) {
        if (buildTTS.checkMediaPlayerExist()==true) {
            buildTTS.stopMediaPlayer()
        }
        Log.d("python", "handleLongPress")
        vibrate()
        if (!buildSTT.checkHasRecordPermission()) {

        }else{
            Log.d("python", "handleLongPress, startListening")
            buildSTT.startRecord()
        }
    }

    private fun handleSwipeGesture(e1: MotionEvent?, e2: MotionEvent?) {
        if (e1 != null && e2 != null) {
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y

            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                if (deltaX > 0) {
                    handleSwipe("Right")
                } else {
                    handleSwipe("Left")
                }
            } else {
                if (deltaY > 0) {
                    handleSwipe("Down")
                } else {
                    handleSwipe("Up")
                }
            }
        }
    }


    private fun handleSwipe(direction: String) {
        if (buildTTS.checkMediaPlayerExist()==true){
            buildTTS.stopMediaPlayer()
            val module = py.getModule("agent")
            module.callAttr("do_action_read_json_end", direction)
        } else {
            webViewHelper.guestureOperation(direction)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        buildSTT.destory()
        buildTTS.destory()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}