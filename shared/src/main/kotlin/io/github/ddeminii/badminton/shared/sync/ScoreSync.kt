package io.github.ddeminii.badminton.shared.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import io.github.ddeminii.badminton.shared.model.ScoreState

/**
 * Wearable Data Layer로 워치와 폰이 점수를 직접 주고받는다. 서버도 인터넷도 쓰지 않는다.
 *
 * 같은 상태를 두 경로로 보낸다.
 * - MessageClient: 지금 연결된 기기에 즉시 전달 (실시간 반영용)
 * - DataClient: 기기에 저장되어, 끊겨 있었거나 앱이 꺼져 있던 기기가 나중에 받아감
 *
 * 받는 쪽은 타임스탬프가 더 최신일 때만 반영하므로 같은 상태가 두 번 와도 문제없다.
 * 상대 기기가 없거나 Data Layer를 쓸 수 없으면 조용히 실패하고 앱은 단독으로 동작한다.
 */
class ScoreSync(
    context: Context,
    private val onRemoteState: (ScoreState) -> Unit,
) : DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    fun start() {
        dataClient.addListener(this).addOnFailureListener { logUnavailable("addListener", it) }
        messageClient.addListener(this)

        // 앱이 꺼져 있는 동안 다른 기기에서 바뀐 점수를 가져온다.
        dataClient.dataItems
            .addOnSuccessListener { items ->
                try {
                    items.filter { it.uri.path == PATH }.forEach { deliver(it.data) }
                } finally {
                    items.release()
                }
            }
            .addOnFailureListener { logUnavailable("getDataItems", it) }
    }

    fun stop() {
        dataClient.removeListener(this)
        messageClient.removeListener(this)
    }

    fun push(state: ScoreState) {
        val bytes = state.toBytes()

        dataClient.putDataItem(PutDataRequest.create(PATH).setData(bytes).setUrgent())
            .addOnFailureListener { logUnavailable("putDataItem", it) }

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { messageClient.sendMessage(it.id, PATH, bytes) }
            }
            .addOnFailureListener { logUnavailable("connectedNodes", it) }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == PATH) {
                deliver(event.dataItem.data)
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == PATH) deliver(event.data)
    }

    private fun deliver(bytes: ByteArray?) {
        ScoreState.fromBytes(bytes)?.let(onRemoteState)
    }

    private fun logUnavailable(what: String, error: Exception) {
        Log.d(TAG, "$what failed, running standalone: ${error.message}")
    }

    private companion object {
        const val TAG = "ScoreSync"
        const val PATH = "/score"
    }
}
