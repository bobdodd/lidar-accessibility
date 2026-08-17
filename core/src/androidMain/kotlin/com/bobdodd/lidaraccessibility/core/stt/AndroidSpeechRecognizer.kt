package com.bobdodd.lidaraccessibility.core.stt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSystemSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android actual for [SpeechRecognizer].
 *
 * Uses `createOnDeviceSpeechRecognizer()` on API 33+ (our minSdk),
 * falling back to the cloud-backed recognizer if on-device is unavailable.
 * All recognizer operations (create, start, stop, cancel) run on the
 * main thread as required by Android's SpeechRecognizer.
 */
class AndroidSpeechRecognizer(
    private val context: Context,
    private val scope: CoroutineScope,
) : SpeechRecognizer {

    private var recognizer: AndroidSystemSpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _events = MutableSharedFlow<SttEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    override suspend fun start(config: SttConfig) {
        stop()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            if (config.preferOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        // All SpeechRecognizer operations must be on the main thread
        withContext(Dispatchers.Main.immediate) {
            val sr = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                AndroidSystemSpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                AndroidSystemSpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                AndroidSystemSpeechRecognizer.createSpeechRecognizer(context)
            }
            recognizer = sr

            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    scope.launch { _events.emit(SttEvent.EndOfSpeech) }
                }

                override fun onError(error: Int) {
                    // ERROR_NO_MATCH and ERROR_SPEECH_TIMEOUT are not really errors
                    // for our use case — surface them as NoMatch / EndOfSpeech
                    when (error) {
                        AndroidSystemSpeechRecognizer.ERROR_NO_MATCH -> {
                            scope.launch { _events.emit(SttEvent.NoMatch) }
                        }
                        AndroidSystemSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            scope.launch { _events.emit(SttEvent.EndOfSpeech) }
                        }
                        else -> {
                            val kind = when (error) {
                                AndroidSystemSpeechRecognizer.ERROR_NETWORK,
                                AndroidSystemSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SttErrorKind.NETWORK
                                AndroidSystemSpeechRecognizer.ERROR_AUDIO -> SttErrorKind.AUDIO
                                AndroidSystemSpeechRecognizer.ERROR_SERVER -> SttErrorKind.SERVER
                                AndroidSystemSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SttErrorKind.PERMISSION
                                AndroidSystemSpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                                AndroidSystemSpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> SttErrorKind.LANGUAGE_UNAVAILABLE
                                else -> SttErrorKind.UNKNOWN
                            }
                            scope.launch { _events.emit(SttEvent.Error(kind, "STT error code $error")) }
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(AndroidSystemSpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotEmpty()) {
                        scope.launch { _events.emit(SttEvent.Partial(text)) }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(AndroidSystemSpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotEmpty()) {
                        scope.launch { _events.emit(SttEvent.Final(text)) }
                    } else {
                        scope.launch { _events.emit(SttEvent.NoMatch) }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            sr.startListening(intent)
        }
    }

    override fun stop() {
        mainHandler.post {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
    }

    override fun cancel() {
        mainHandler.post {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }
}
