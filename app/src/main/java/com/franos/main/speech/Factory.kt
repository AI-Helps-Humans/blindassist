package com.franos.main.speech

import com.franos.main.speech.googleimpl.IGoogleSTTImpl
import com.franos.main.speech.googleimpl.IGoogleTTSImpl

class Factory {

    companion object {

        fun buildTTS(): ITTS {
            return IGoogleTTSImpl()
        }

        fun buildSTT(): ISTT {
            return IGoogleSTTImpl()
        }

    }

}