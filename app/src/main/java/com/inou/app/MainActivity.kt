package com.inou.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import java.util.Locale

/** 試合画面（assets/index.html）を表示し、ログの読み上げをつなぐ */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var ready = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")

        tts = TextToSpeech(this) { st ->
            if (st == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.JAPAN)
                tts?.setSpeechRate(1.1f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { done() }
                    @Deprecated("") override fun onError(id: String?) { done() }
                })
                ready = true
            }
        }
    }

    private fun done() {
        web.post { web.evaluateJavascript("window.onNativeSaid&&onNativeSaid()", null) }
    }

    inner class Bridge {
        @JavascriptInterface
        fun speak(text: String) {
            if (!ready) { done(); return }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u" + System.nanoTime())
        }
        @JavascriptInterface
        fun stop() { tts?.stop() }
    }

    override fun onPause() {
        tts?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
