package com.franos.main.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class VoiceRecorder(private val mCallback: Callback) {
    abstract class Callback {
        /**
         * Called when the recorder starts hearing voice.
         */
        open fun onVoiceStart() {}

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in [AudioFormat.ENCODING_PCM_16BIT].
         * @param size The size of the actual data in `data`.
         */
        open fun onVoice(data: ByteArray?, size: Int) {}

        /**
         * Called when the recorder stops hearing voice.
         */
        open fun onVoiceEnd() {}
    }

    private var mAudioRecord: AudioRecord? = null
    private var mThread: Thread? = null
    private var mBuffer: ByteArray? = null
    private val msynchronizedLock = Any()
    @Volatile
    private var mLock = true

    /** The timestamp of the last time that voice is heard.  */
    private var mLastVoiceHeardMillis = Long.MAX_VALUE

    /** The timestamp when the current voice is started.  */
    private var mVoiceStartedMillis: Long = 0

    /**
     * Starts recording audio.
     *
     *
     * The caller is responsible for calling [.stop] later.
     */
    fun start() {
        // Stop recording if it is currently ongoing.
        stop()
        // Try to create a new recording session.
        mAudioRecord = createAudioRecord()
        if (mAudioRecord == null) {
            throw RuntimeException("Cannot instantiate VoiceRecorder")
        }
        // Start recording.
        mAudioRecord!!.startRecording()
        // Start processing the captured audio.
        mThread = Thread(ProcessVoice())
        Log.d("python", "stt start")
        mThread!!.start()
    }

    /**
     * Stops recording audio.
     */
    fun stop() {
        Log.d("python", "myLog prestop(")
        mLock = false
        synchronized(msynchronizedLock) {
            Log.d("python", "myLog lock()")
            dismiss()
            if (mThread != null) {
                mThread!!.interrupt()
                mThread = null
            }
            if (mAudioRecord != null) {
                Log.d("python", "myLog stop()")
                mAudioRecord!!.stop()
                mAudioRecord!!.release()
                mAudioRecord = null
            }
            mBuffer = null
        }
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    fun dismiss() {
        Log.d("python", "myLog dismiss()")
//        mCallback.onVoiceEnd()
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE
            Log.d("python", "myLog onVoiceEnd()")
            mCallback.onVoiceEnd()
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    val sampleRate: Int
        get() = if (mAudioRecord != null) {
            mAudioRecord!!.sampleRate
        } else 0

    /**
     * Creates a new [AudioRecord].
     *
     * @return A newly created [AudioRecord], or null if it cannot be created (missing
     * permissions?).
     */
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        for (sampleRate in SAMPLE_RATE_CANDIDATES) {
            val sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue
            }
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate, CHANNEL, ENCODING, sizeInBytes
            )
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                mBuffer = ByteArray(sizeInBytes)
                return audioRecord
            } else {
                audioRecord.release()
            }
        }
        return null
    }

    /**
     * Continuously processes the captured audio and notifies [.mCallback] of corresponding
     * events.
     */
    private inner class ProcessVoice : Runnable {
        override fun run() {
            mLock = true
            while (mLock) {
                synchronized(msynchronizedLock) {
                    if (Thread.currentThread().isInterrupted) {
                        return
                    }
                    val size = mAudioRecord!!.read(mBuffer!!, 0, mBuffer!!.size)
                    val now = System.currentTimeMillis()
                    Log.d("python", "start isHearing？")
//                    Log.d("myLog run", "${mLastVoiceHeardMillis} ${now - mVoiceStartedMillis} ${now - mVoiceStartedMillis - MAX_SPEECH_LENGTH_MILLIS}")
                    if (isHearingVoice(mBuffer, size)) {
                        Log.d("python", "myLog in isHearing？")
                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                            mVoiceStartedMillis = now
                            mCallback.onVoiceStart()
                        }
                        mCallback.onVoice(mBuffer, size)
                        mLastVoiceHeardMillis = now
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                            Log.d("python", "myLog to end")
                            end()
                        }
                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                        Log.d("python", "start onvoice")
                        mCallback.onVoice(mBuffer, size)
//                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
//                            end()
//                        }
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                            Log.d("myLog to end ", "？？？？")
                            end()
                        }
                    }
                }
            }
        }

        private fun end() {
            Log.d("myLog end", "????")

            mLastVoiceHeardMillis = Long.MAX_VALUE
            mCallback.onVoiceEnd()
        }

        private fun isHearingVoice(buffer: ByteArray?, size: Int): Boolean {
            var i = 0
            while (i < size - 1) {

                // The buffer has LINEAR16 in little endian.
                var s = buffer!![i + 1].toInt()
                if (s < 0) s *= -1
                s = s shl 8
                s += Math.abs(buffer[i].toInt())
                if (s > AMPLITUDE_THRESHOLD) {
                    return true
                }
                i += 2
            }
            return false
        }
    }

    companion object {
        private val SAMPLE_RATE_CANDIDATES = intArrayOf(16000, 11025, 22050, 44100)
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val AMPLITUDE_THRESHOLD = 1500
        private const val SPEECH_TIMEOUT_MILLIS = 2000
        private const val MAX_SPEECH_LENGTH_MILLIS = 30 * 1000
    }
}
