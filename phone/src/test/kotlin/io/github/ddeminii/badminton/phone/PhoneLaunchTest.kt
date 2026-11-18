package io.github.ddeminii.badminton.phone

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ddeminii.badminton.shared.sync.PhoneLaunch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 워치가 보내는 "점수판 열기" 메시지를 폰 앱의 서비스가 받도록 등록돼 있는지 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PhoneLaunchTest {

    @Test
    fun `the open message from the watch reaches the phone service`() {
        val app: Application = ApplicationProvider.getApplicationContext()
        val message = Intent("com.google.android.gms.wearable.MESSAGE_RECEIVED")
            .setData(Uri.parse("wear://watch-node-id${PhoneLaunch.OPEN_PATH}"))
            .setPackage(app.packageName)

        val handlers = app.packageManager.queryIntentServices(message, 0)

        assertEquals(listOf(OpenFromWatchService::class.java.name), handlers.map { it.serviceInfo.name })
    }
}
