package io.github.ddeminii.badminton.shared.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

/**
 * 워치에서 점수판을 켜면 폰의 점수판도 켜지게 하는 신호.
 *
 * 워치가 연결된 폰으로 [OPEN_PATH] 메시지를 보내면, 폰 앱의 OpenFromWatchService 가
 * 앱이 꺼져 있어도 이를 받아 점수판을 띄운다. 폰이 연결돼 있지 않으면 아무 일도 일어나지 않는다.
 */
object PhoneLaunch {
    /** 폰 매니페스트의 OpenFromWatchService intent-filter(pathPrefix)와 같아야 한다. */
    const val OPEN_PATH = "/open-scoreboard"

    fun requestOpen(context: Context) {
        val messageClient = Wearable.getMessageClient(context)
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { messageClient.sendMessage(it.id, OPEN_PATH, ByteArray(0)) }
            }
            .addOnFailureListener { Log.d("PhoneLaunch", "No phone to open: ${it.message}") }
    }
}
