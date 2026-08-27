package com.intercomunicador.poc.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class AudioIntercomManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AudioIntercom_PoC"
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma Português (Brasil) não suportado pelo TTS nativo.")
            } else {
                isTtsReady = true
                Log.d(TAG, "TextToSpeech inicializado com sucesso em PT-BR.")
            }
        } else {
            Log.e(TAG, "Falha ao inicializar a engine de TextToSpeech.")
        }
    }

    /**
     * Solicita foco de áudio (pausa/abaixa música) e lê o texto via TTS.
     */
    fun speakMessage(sender: String, messageText: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS ainda não está pronto para leitura.")
            return
        }

        requestAudioFocus()

        val textToSpeak = "Mensagem de $sender: $messageText"
        val utteranceId = "MSG_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Início da leitura de áudio TTS")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Leitura concluída. Liberando foco de áudio.")
                releaseAudioFocus()
                onComplete?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Erro na sintaxe de áudio TTS")
                releaseAudioFocus()
            }
        })

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Solicita o foco de áudio temporário para interromper tocadores de mídia (Spotify, YouTube Music).
     */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Mudança de AudioFocus: $focusChange")
                }
                .build()

            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    /**
     * Libera o foco de áudio permitindo que a música volte ao volume normal.
     */
    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /**
     * Ativa a conexão Bluetooth SCO (Hands-Free Profile) para utilizar o microfone do intercomunicador.
     */
    fun startBluetoothSco() {
        try {
            if (!audioManager.isBluetoothScoOn) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                Log.d(TAG, "Canal de microfone Bluetooth SCO ativado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar Bluetooth SCO", e)
        }
    }

    /**
     * Desativa a conexão Bluetooth SCO e retorna para áudio estéreo A2DP.
     */
    fun stopBluetoothSco() {
        try {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.d(TAG, "Canal Bluetooth SCO desativado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao encerrar Bluetooth SCO", e)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        stopBluetoothSco()
    }
}
