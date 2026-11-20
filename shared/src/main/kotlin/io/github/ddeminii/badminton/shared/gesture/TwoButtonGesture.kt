package io.github.ddeminii.badminton.shared.gesture

import io.github.ddeminii.badminton.shared.model.Side

/** 제스처 타이밍. 기기별로 값을 바꿔서 넘길 수 있다. */
data class GestureConfig(
    /** 이 시간 이상 한 버튼을 누르고 있으면 −1. */
    val longPressMs: Long = 500,
    /** 두 버튼을 함께 이 시간 이상 누르고 있으면 새 게임. */
    val bothPressMs: Long = 800,
    /** +1 직후 같은 버튼의 입력을 무시하는 시간 (오터치 방지). */
    val tapLockoutMs: Long = 300,
)

sealed interface GestureAction {
    data class Increment(val side: Side) : GestureAction
    data class Decrement(val side: Side) : GestureAction
    data object Reset : GestureAction
}

/**
 * 두 버튼의 눌림/뗌을 받아 +1 / −1 / 새 게임을 판정하는 상태 기계.
 * 시간은 모두 바깥에서 넘겨받으므로 안드로이드 없이 테스트할 수 있다.
 *
 * - 짧게 눌렀다 떼면 +1 (뗄 때 확정)
 * - [GestureConfig.longPressMs] 동안 누르고 있으면 그 순간 −1
 * - 두 버튼이 함께 눌리면 각각의 −1 타이머는 취소되고,
 *   함께 [GestureConfig.bothPressMs] 동안 유지되면 새 게임
 */
class TwoButtonGesture(private val config: GestureConfig = GestureConfig()) {

    private sealed interface Mode {
        data object Idle : Mode
        data class Single(val side: Side) : Mode
        data object Both : Mode

        /** 이미 처리가 끝났거나 취소된 누름. 손을 다 뗄 때까지 아무 일도 하지 않는다. */
        data class Spent(val canStartBoth: Boolean) : Mode
    }

    private var mode: Mode = Mode.Idle
    private val down = mutableSetOf<Side>()
    private val ignored = mutableSetOf<Side>()
    private val lockedUntil = mutableMapOf<Side, Long>()

    /** 다음에 [onTime]을 불러야 하는 시각. 기다릴 것이 없으면 null. */
    var deadlineMs: Long? = null
        private set

    /** 지금 유효하게 눌려 있는 버튼 (오터치 방지로 무시된 누름은 제외). */
    val activeSides: Set<Side> get() = down.toSet()

    fun onDown(side: Side, now: Long) {
        if (side in down || side in ignored) return
        if (now < (lockedUntil[side] ?: 0L)) {
            ignored += side
            return
        }
        down += side
        when (val current = mode) {
            Mode.Idle -> {
                mode = Mode.Single(side)
                deadlineMs = now + config.longPressMs
            }
            is Mode.Single -> startBoth(now)
            is Mode.Spent -> if (current.canStartBoth && down.size == 2) startBoth(now)
            Mode.Both -> Unit
        }
    }

    fun onUp(side: Side, now: Long): GestureAction? {
        if (ignored.remove(side)) return null
        if (!down.remove(side)) return null
        return when (val current = mode) {
            is Mode.Single -> {
                mode = Mode.Idle
                deadlineMs = null
                lockedUntil[side] = now + config.tapLockoutMs
                GestureAction.Increment(current.side)
            }
            Mode.Both -> {
                // 0.8초가 되기 전에 한 손가락이라도 떼면 아무 일도 일어나지 않는다.
                deadlineMs = null
                mode = if (down.isEmpty()) Mode.Idle else Mode.Spent(canStartBoth = true)
                null
            }
            is Mode.Spent -> {
                if (down.isEmpty()) mode = Mode.Idle
                null
            }
            Mode.Idle -> null
        }
    }

    /** [deadlineMs]가 지났을 때 호출한다. */
    fun onTime(now: Long): GestureAction? {
        val deadline = deadlineMs ?: return null
        if (now < deadline) return null
        deadlineMs = null
        return when (val current = mode) {
            is Mode.Single -> {
                mode = Mode.Spent(canStartBoth = true)
                GestureAction.Decrement(current.side)
            }
            Mode.Both -> {
                mode = Mode.Spent(canStartBoth = false)
                GestureAction.Reset
            }
            else -> null
        }
    }

    /** 시스템이 터치를 가로챈 경우. 진행 중이던 누름을 아무 동작 없이 버린다. */
    fun cancel() {
        down.clear()
        ignored.clear()
        mode = Mode.Idle
        deadlineMs = null
    }

    private fun startBoth(now: Long) {
        mode = Mode.Both
        deadlineMs = now + config.bothPressMs
    }
}
