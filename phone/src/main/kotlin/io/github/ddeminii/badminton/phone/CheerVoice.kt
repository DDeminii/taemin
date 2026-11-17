package io.github.ddeminii.badminton.phone

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * 응원 문구를 폰의 글자 읽어주기(TTS)로, 여자 목소리로 소리 내어 읽는다.
 * 소리 크기는 폰의 미디어 음량을 따른다. 한국어 음성이 없는 폰에서는 조용히 아무것도 하지 않는다.
 */
class CheerVoice(context: Context) : TextToSpeech.OnInitListener {

    private val text = context.getString(R.string.cheer_text)
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.d(TAG, "No text-to-speech engine available")
            return
        }
        val language = tts.setLanguage(Locale.KOREAN)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.d(TAG, "No Korean voice installed")
            return
        }
        pickFemaleVoice(tts.voices.orEmpty(), tts.voice)?.let { tts.voice = it }
        tts.setPitch(CHEER_PITCH)
        tts.setSpeechRate(CHEER_RATE)
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        ready = true
    }

    fun cheer() {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cheer")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "CheerVoice"

        /** 응원답게 조금 높고 빠르게 */
        private const val CHEER_PITCH = 1.15f
        private const val CHEER_RATE = 1.05f

        /**
         * 한국어 음성 중 여자 목소리를 고른다. 안드로이드는 음성의 성별을 따로 알려 주지 않으므로
         * 음성 이름으로 판단한다 (Google: ism/kob = 여성, koc/kod = 남성. Samsung: f = 여성, m = 남성).
         *
         * 기본 음성이 남성으로 보이지 않으면 그대로 두고(두 엔진 모두 기본 한국어 음성은 여성),
         * 남성으로 보일 때만 여성 음성으로 바꾼다. 바꿀 필요가 없으면 null.
         */
        internal fun pickFemaleVoice(voices: Collection<Voice>, current: Voice?): Voice? {
            if (current != null && !looksMale(current)) return null
            val korean = voices.filter { it.locale.language == Locale.KOREAN.language && !it.isNetworkConnectionRequired }
            return korean.firstOrNull(::looksFemale) ?: korean.firstOrNull { !looksMale(it) }
        }

        private fun looksFemale(voice: Voice): Boolean {
            val name = voice.name.lowercase(Locale.ROOT)
            return "female" in name || voice.features.orEmpty().any { it.equals("female", ignoreCase = true) } ||
                FEMALE_MARKERS.any { it in name }
        }

        private fun looksMale(voice: Voice): Boolean {
            val name = voice.name.lowercase(Locale.ROOT)
            if (looksFemale(voice)) return false
            return "male" in name || voice.features.orEmpty().any { it.equals("male", ignoreCase = true) } ||
                MALE_MARKERS.any { it in name }
        }

        private val FEMALE_MARKERS = listOf("-x-ism-", "-x-kob-", "smtf")
        private val MALE_MARKERS = listOf("-x-koc-", "-x-kod-", "smtm")
    }
}
