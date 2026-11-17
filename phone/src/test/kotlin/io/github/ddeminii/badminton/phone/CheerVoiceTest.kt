package io.github.ddeminii.badminton.phone

import android.speech.tts.Voice
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CheerVoiceTest {

    private fun voice(name: String, locale: Locale = Locale.KOREA, network: Boolean = false) =
        Voice(name, locale, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, network, emptySet())

    private val googleVoices = listOf(
        voice("en-us-x-sfg-local", Locale.US),
        voice("ko-kr-x-koc-local"),
        voice("ko-kr-x-kod-local"),
        voice("ko-kr-x-ism-network", network = true),
        voice("ko-kr-x-kob-local"),
        voice("ko-KR-language"),
    )

    @Test
    fun `keeps the default voice when it is not a male voice`() {
        assertNull(CheerVoice.pickFemaleVoice(googleVoices, current = voice("ko-KR-language")))
        assertNull(CheerVoice.pickFemaleVoice(googleVoices, current = voice("ko-kr-x-ism-local")))
    }

    @Test
    fun `switches to an offline female Korean voice when the default is male`() {
        val picked = CheerVoice.pickFemaleVoice(googleVoices, current = voice("ko-kr-x-koc-local"))
        assertEquals("ko-kr-x-kob-local", picked?.name)
    }

    @Test
    fun `recognises Samsung voice names`() {
        val samsung = listOf(voice("ko-KR-SMTm00"), voice("ko-KR-SMTf00"))
        assertEquals("ko-KR-SMTf00", CheerVoice.pickFemaleVoice(samsung, current = voice("ko-KR-SMTm00"))?.name)
    }
}
