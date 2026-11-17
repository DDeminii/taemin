package io.github.ddeminii.badminton.phone

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ddeminii.badminton.shared.sync.PhoneLaunch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 워치가 보내는 "폰 앱 열기" 주소를 폰 앱이 실제로 받는지 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PhoneLaunchTest {

    @Test
    fun `the launch intent sent by the watch opens the phone scoreboard`() {
        val app: Application = ApplicationProvider.getApplicationContext()
        val handlers = app.packageManager.queryIntentActivities(PhoneLaunch.intent().setPackage(app.packageName), 0)

        assertEquals(listOf(MainActivity::class.java.name), handlers.map { it.activityInfo.name })
    }
}
