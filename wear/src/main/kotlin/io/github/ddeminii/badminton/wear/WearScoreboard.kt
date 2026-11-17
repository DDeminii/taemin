package io.github.ddeminii.badminton.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ddeminii.badminton.shared.gesture.GestureAction
import io.github.ddeminii.badminton.shared.gesture.GestureConfig
import io.github.ddeminii.badminton.shared.gesture.SplitDirection
import io.github.ddeminii.badminton.shared.gesture.twoButtonInput
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.ui.ScoreColors
import io.github.ddeminii.badminton.shared.ui.ScoreDigits
import io.github.ddeminii.badminton.shared.ui.ScorePanel

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
 * 반원 안에서 숫자가 차지하는 세로 비율.
 * 원이 좁아지는 바깥쪽에 여백을 더 두어 숫자가 화면 가장자리에 닿지 않게 한다.
 */
private const val OUTER_SPACE = 0.20f
private const val DIGITS_SPACE = 0.68f
private const val INNER_SPACE = 0.12f

/** 원형 화면을 위/아래 반으로 나눈 점수판. 위 = 나, 아래 = 상대. */
@Composable
fun WearScoreboard(
    state: ScoreState,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(emptySet<Side>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScoreColors.Backdrop)
            .twoButtonInput(
                split = SplitDirection.TOP_BOTTOM,
                config = WearGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            ),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        WearTile(Side.ME, state.me, Side.ME in pressed)
        WearTile(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed)
    }
}

@Composable
private fun ColumnScope.WearTile(side: Side, score: Int, pressed: Boolean) {
    val top = side == Side.ME
    ScorePanel(
        side = side,
        pressed = pressed,
        // 빛이 숫자 뒤에서 퍼져 나가 원의 가장자리로 갈수록 어두워진다.
        lightFrom = BiasAlignment(0f, if (top) 0.35f else -0.35f),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(if (top) OUTER_SPACE else INNER_SPACE))
            ScoreDigits(
                score = score,
                color = ScoreColors.of(side).text,
                modifier = Modifier
                    .weight(DIGITS_SPACE)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.weight(if (top) INNER_SPACE else OUTER_SPACE))
        }
    }
}
