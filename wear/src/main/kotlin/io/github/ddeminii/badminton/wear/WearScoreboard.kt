package io.github.ddeminii.badminton.wear

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import io.github.ddeminii.badminton.shared.gesture.GestureAction
import io.github.ddeminii.badminton.shared.gesture.GestureConfig
import io.github.ddeminii.badminton.shared.gesture.SplitDirection
import io.github.ddeminii.badminton.shared.gesture.twoButtonInput
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.ui.FlipScore
import io.github.ddeminii.badminton.shared.ui.ScoreColors
import kotlinx.coroutines.delay

/**
 * 워치의 제스처 타이밍.
 * tapLockoutMs: +1 직후 같은 버튼 입력을 무시하는 시간. 오터치가 잦으면 올린다 (300~1000 권장).
 */
private val WearGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 400,
)

/**
 * 반원 안에서 카드가 놓이는 자리. 원이 좁아지는 바깥쪽(OUTER)에 여백을 더 두어
 * 카드 모서리가 화면 가장자리에 잘리지 않게 한다.
 */
private const val OUTER_SPACE = 0.21f
private const val CARDS_SPACE = 0.73f
private const val INNER_SPACE = 0.06f
private const val CARDS_WIDTH = 0.58f

/** 워치는 반원이 낮고 넓어서 폰보다 납작한 카드를 쓴다. */
private const val CARD_ASPECT = 0.76f

/** 마지막 터치나 점수 변화 뒤 이 시간이 지나면 어두운 절전 화면으로 바뀐다. */
const val REST_AFTER_MS = 5_000L

/**
 * 원형 화면을 위/아래 반으로 나눈 점수판. 위 = 나, 아래 = 상대.
 *
 * 배터리를 아끼기 위해 [restAfterMs] 동안 입력이 없으면 검은 바탕의 절전 화면으로 바뀐다.
 * 절전 화면에서도 점수는 보이고 터치는 평소와 똑같이 한 번에 인식되며, 누르거나 점수가 바뀌면 바로 밝아진다.
 */
@Composable
fun WearScoreboard(
    state: ScoreState,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
    restAfterMs: Long = REST_AFTER_MS,
) {
    var pressed by remember { mutableStateOf(emptySet<Side>()) }

    var resting by remember { mutableStateOf(false) }
    LaunchedEffect(state.me, state.opponent, pressed) {
        resting = false
        if (pressed.isEmpty()) {
            delay(restAfterMs)
            resting = true
        }
    }
    val rest = animateFloatAsState(
        targetValue = if (resting) 1f else 0f,
        animationSpec = tween(durationMillis = if (resting) 700 else 120),
        label = "rest",
    )
    val restFraction = { rest.value }

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(lerp(ScoreColors.Backdrop, ScoreColors.RestingBackdrop, rest.value)) }
            .twoButtonInput(
                split = SplitDirection.TOP_BOTTOM,
                config = WearGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            ),
    ) {
        WearHalf(Side.ME, state.me, Side.ME in pressed, restFraction)
        WearHalf(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed, restFraction)
    }
}

@Composable
private fun ColumnScope.WearHalf(side: Side, score: Int, pressed: Boolean, rest: () -> Float) {
    val top = side == Side.ME
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(if (top) OUTER_SPACE else INNER_SPACE))
        FlipScore(
            score = score,
            side = side,
            pressed = pressed,
            cardAspect = CARD_ASPECT,
            rest = rest,
            modifier = Modifier
                .weight(CARDS_SPACE)
                .fillMaxWidth(CARDS_WIDTH),
        )
        Spacer(Modifier.weight(if (top) INNER_SPACE else OUTER_SPACE))
    }
}
