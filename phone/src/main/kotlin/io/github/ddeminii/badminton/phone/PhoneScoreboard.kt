package io.github.ddeminii.badminton.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.ddeminii.badminton.shared.gesture.GestureAction
import io.github.ddeminii.badminton.shared.gesture.GestureConfig
import io.github.ddeminii.badminton.shared.gesture.SplitDirection
import io.github.ddeminii.badminton.shared.gesture.twoButtonInput
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.ui.FlipScore
import io.github.ddeminii.badminton.shared.ui.ScoreColors

/** 폰의 제스처 타이밍. 오터치가 잦으면 tapLockoutMs를 올린다. */
private val PhoneGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 300,
)

/**
 * 가로 화면을 좌/우 반으로 나눈 전광판. 왼쪽 = 나, 오른쪽 = 상대.
 * 카드 바깥의 여백을 포함해 화면의 각 절반 전체가 버튼이다.
 */
@Composable
fun PhoneScoreboard(
    state: ScoreState,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(emptySet<Side>()) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ScoreColors.Backdrop)
            .twoButtonInput(
                split = SplitDirection.LEFT_RIGHT,
                config = PhoneGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            ),
    ) {
        PhoneHalf(Side.ME, state.me, Side.ME in pressed)
        PhoneHalf(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed)
    }
}

@Composable
private fun RowScope.PhoneHalf(side: Side, score: Int, pressed: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        FlipScore(
            score = score,
            side = side,
            pressed = pressed,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.84f),
        )
    }
}
