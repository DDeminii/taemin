package io.github.ddeminii.badminton.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.ddeminii.badminton.shared.model.GameRules
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.ui.FlipScore
import io.github.ddeminii.badminton.shared.ui.ScoreColors
import io.github.ddeminii.badminton.shared.ui.fireBackground

/** 폰의 제스처 타이밍. 오터치가 잦으면 tapLockoutMs를 올린다. */
private val PhoneGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 300,
)

/**
 * 폰 전광판. 화면의 각 절반 전체가 버튼이다 (카드 바깥의 여백 포함).
 * - 가로로 눕히면 좌/우: 왼쪽 = 나, 오른쪽 = 상대
 * - 세로로 세우면 위/아래: 위 = 나, 아래 = 상대 (워치와 같은 배치)
 */
@Composable
fun PhoneScoreboard(
    state: ScoreState,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(emptySet<Side>()) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth >= maxHeight
        val board = Modifier
            .fillMaxSize()
            .background(ScoreColors.Backdrop)
            .twoButtonInput(
                split = if (landscape) SplitDirection.LEFT_RIGHT else SplitDirection.TOP_BOTTOM,
                config = PhoneGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            )
        val onFire = GameRules.onFire(state)
        val halves = @Composable { half: Modifier ->
            ScoreHalf(Side.ME, state.me, Side.ME in pressed, Side.ME in onFire, half)
            ScoreHalf(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed, Side.OPPONENT in onFire, half)
        }

        if (landscape) {
            Row(board) { halves(Modifier.weight(1f).fillMaxHeight()) }
        } else {
            Column(board) { halves(Modifier.weight(1f).fillMaxWidth()) }
        }
    }
}

@Composable
private fun ScoreHalf(side: Side, score: Int, pressed: Boolean, burning: Boolean, modifier: Modifier) {
    Box(modifier = modifier.fireBackground(burning, flameHeight = 0.95f), contentAlignment = Alignment.Center) {
        FlipScore(
            score = score,
            side = side,
            pressed = pressed,
            burning = burning,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.84f),
        )
    }
}
