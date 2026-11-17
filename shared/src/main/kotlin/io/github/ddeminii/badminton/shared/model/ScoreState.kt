package io.github.ddeminii.badminton.shared.model

import java.nio.ByteBuffer

/** 점수의 주인. 워치에서는 위/아래, 폰에서는 왼쪽/오른쪽 버튼에 대응한다. */
enum class Side { ME, OPPONENT }

/**
 * 점수판의 전체 상태. 값이 바뀔 때마다 이 객체 전체를 상대 기기로 보내고,
 * 받은 쪽은 [isNewerThan]으로 비교해 더 최신인 것만 반영한다.
 */
data class ScoreState(
    val me: Int = 0,
    val opponent: Int = 0,
    val timestamp: Long = 0L,
) {
    fun score(side: Side): Int = if (side == Side.ME) me else opponent

    fun increment(side: Side, now: Long): ScoreState = when (side) {
        Side.ME -> copy(me = me + 1, timestamp = stamp(now))
        Side.OPPONENT -> copy(opponent = opponent + 1, timestamp = stamp(now))
    }

    /** 0점에서는 내려가지 않으며, 그 경우 같은 객체를 그대로 돌려준다. */
    fun decrement(side: Side, now: Long): ScoreState = when {
        score(side) == 0 -> this
        side == Side.ME -> copy(me = me - 1, timestamp = stamp(now))
        else -> copy(opponent = opponent - 1, timestamp = stamp(now))
    }

    fun reset(now: Long): ScoreState = ScoreState(0, 0, stamp(now))

    /**
     * 두 기기의 시계가 조금 어긋나 있어도, 지금 보고 있는 상태에서 만든 변경은
     * 항상 그 상태보다 최신이 되도록 타임스탬프를 최소 +1 시킨다.
     */
    private fun stamp(now: Long): Long = maxOf(now, timestamp + 1)

    /** 타임스탬프가 같으면 점수로 순서를 정해 두 기기가 같은 결론에 도달하게 한다. */
    fun isNewerThan(other: ScoreState): Boolean = when {
        timestamp != other.timestamp -> timestamp > other.timestamp
        me != other.me -> me > other.me
        else -> opponent > other.opponent
    }

    fun toBytes(): ByteArray =
        ByteBuffer.allocate(BYTES).putInt(me).putInt(opponent).putLong(timestamp).array()

    companion object {
        private const val BYTES = Int.SIZE_BYTES * 2 + Long.SIZE_BYTES

        fun fromBytes(bytes: ByteArray?): ScoreState? {
            if (bytes == null || bytes.size != BYTES) return null
            val buffer = ByteBuffer.wrap(bytes)
            val me = buffer.getInt()
            val opponent = buffer.getInt()
            val timestamp = buffer.getLong()
            if (me < 0 || opponent < 0) return null
            return ScoreState(me, opponent, timestamp)
        }
    }
}
