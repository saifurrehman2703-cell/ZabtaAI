package com.zabtaai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import java.util.Locale

/**
 * REAL Android Speech Recognition Engine.
 * Uses Android's built-in SpeechRecognizer (Google Speech API).
 * Supports Urdu, Hindi, and English.
 * Implements continuous listening loop.
 */
class SpeechRecognitionEngine(
    private val context: Context,
    private val onCommandReceived: (command: String, language: String) -> Unit,
    private val onError: (error: String) -> Unit
) : RecognitionListener {

    private val TAG = "ZabtaAI-Speech"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentLanguage = Language.URDU

    enum class Language(val locale: Locale, val code: String) {
        URDU(Locale("ur", "PK"), "urdu"),
        HINDI(Locale("hi", "IN"), "hindi"),
        ENGLISH(Locale.ENGLISH, "english")
    }

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
            Log.d(TAG, "SpeechRecognizer initialized")
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
            onError("Speech recognition not available")
        }
    }

    /**
     * Start continuous listening
     */
    fun startListening(language: Language = Language.URDU) {
        if (isListening) return
        if (speechRecognizer == null) initializeSpeechRecognizer()
        if (speechRecognizer == null) {
            onError("Speech recognizer initialization failed")
            return
        }

        currentLanguage = language
        isListening = true
        Log.d(TAG, "Started listening in ${language.code}")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.locale.language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.locale.language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Switch language while listening
     */
    fun switchLanguage(language: Language) {
        stopListening()
        currentLanguage = language
        startListening(language)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech input started")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio level change
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "Speech input ended")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        Log.e(TAG, "Speech recognition error: $errorMessage")
        onError(errorMessage)
        
        // Automatically restart listening
        if (isListening) {
            startListening(currentLanguage)
        }
    }

    override fun onResults(results: Bundle?) {
        if (results == null) return

        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val topResult = matches[0]
            Log.d(TAG, "Recognized: $topResult")
            onCommandReceived(topResult, currentLanguage.code)
        }

        // Restart listening after result
        if (isListening) {
            startListening(currentLanguage)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partial.isNullOrEmpty()) {
            Log.d(TAG, "Partial: ${partial[0]}")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Event: $eventType")
    }

    fun release() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "SpeechRecognizer released")
    }
}
