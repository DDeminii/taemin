package io.github.ddeminii.badminton.shared.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.ddeminii.badminton.shared.model.Side

/** 화면을 어떻게 반으로 나눠 두 버튼으로 쓸지. 첫 번째 절반이 항상 "나". */
enum class SplitDirection {
    /** 워치: 위 = 나, 아래 = 상대 */
    TOP_BOTTOM,

    /** 폰: 왼쪽 = 나, 오른쪽 = 상대 */
    LEFT_RIGHT;

    fun sideAt(position: Offset, size: IntSize): Side {
        val inFirstHalf = when (this) {
            TOP_BOTTOM -> position.y < size.height / 2f
            LEFT_RIGHT -> position.x < size.width / 2f
        }
        return if (inFirstHalf) Side.ME else Side.OPPONENT
    }
}

/**
 * 이 영역 전체를 두 개의 큰 버튼으로 만든다.
 *
 * 버튼마다 따로 터치를 받지 않고 화면 전체에서 한 번에 받아 [TwoButtonGesture]로 넘기므로,
 * 두 버튼 동시 누르기를 정확히 판정할 수 있다. 손가락이 처음 닿은 위치로 버튼이 정해지고
 * 그 뒤에 미끄러져도 바뀌지 않는다 (땀 난 손 대응).
 */
@Composable
fun Modifier.twoButtonInput(
    split: SplitDirection,
    config: GestureConfig,
    onPressedChange: (Set<Side>) -> Unit,
    onAction: (GestureAction) -> Unit,
): Modifier {
    val currentOnPressedChange by rememberUpdatedState(onPressedChange)
    val currentOnAction by rememberUpdatedState(onAction)

    return pointerInput(split, config) {
        val gesture = TwoButtonGesture(config)
        val pointers = mutableMapOf<PointerId, Side>()
        var now = 0L

        awaitPointerEventScope {
            while (true) {
                val deadline = gesture.deadlineMs
                val event = if (deadline == null) {
                    awaitPointerEvent()
                } else {
                    withTimeoutOrNull((deadline - now).coerceAtLeast(1)) { awaitPointerEvent() }
                }

                if (event == null) {
                    // 누르고 있는 동안 타이머가 끝남: −1 또는 새 게임
                    now = maxOf(now, deadline ?: now)
                    gesture.onTime(now)?.let { currentOnAction(it) }
                } else {
                    var cancelled = false
                    for (change in event.changes) {
                        now = maxOf(now, change.uptimeMillis)
                        if (change.changedToDownIgnoreConsumed()) {
                            val side = split.sideAt(change.position, size)
                            val firstOnSide = side !in pointers.values
                            pointers[change.id] = side
                            if (firstOnSide) gesture.onDown(side, now)
                        } else if (change.changedToUpIgnoreConsumed()) {
                            val side = pointers.remove(change.id)
                            if (change.isConsumed) {
                                // 시스템이 터치를 취소하면 이미 소비된 "뗌" 이벤트가 온다.
                                cancelled = true
                            } else if (side != null && side !in pointers.values) {
                                gesture.onUp(side, now)?.let { currentOnAction(it) }
                            }
                        }
                        change.consume()
                    }
                    if (cancelled) {
                        pointers.clear()
                        gesture.cancel()
                    }
                }
                currentOnPressedChange(gesture.activeSides)
            }
        }
    }
}
