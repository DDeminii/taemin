package io.github.ddeminii.badminton.shared.sync

import android.content.Intent
import android.net.Uri

/**
 * 워치가 폰의 점수판 앱을 여는 데 쓰는 주소.
 * 폰 앱 매니페스트의 intent-filter(scheme/host)와 반드시 같아야 한다.
 */
object PhoneLaunch {
    const val SCHEME = "badmintonscoreboard"
    const val HOST = "open"

    /** 다른 기기에서 원격으로 열 수 있는 형태(ACTION_VIEW + BROWSABLE)의 인텐트 */
    fun intent(): Intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.Builder().scheme(SCHEME).authority(HOST).build())
}
