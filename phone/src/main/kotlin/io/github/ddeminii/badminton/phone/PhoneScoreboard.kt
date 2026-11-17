package io.github.ddeminii.badminton.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

/** 폰의 제스처 타이밍. 오터치가 잦으면 tapLockoutMs를 올린다. */
private val PhoneGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 300,
)

private val TileGap = 10.dp
private val TileShape = RoundedCornerShape(36.dp)

/**
 * 가로 화면을 좌/우 반으로 나눈 전광판. 왼쪽 = 나, 오른쪽 = 상대.
 * 타일 사이의 틈과 바깥 여백을 포함해 화면 전체가 터치 영역이다.
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
            )
            .padding(TileGap),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        PhoneTile(Side.ME, state.me, Side.ME in pressed)
        PhoneTile(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed)
    }
}

@Composable
private fun RowScope.PhoneTile(side: Side, score: Int, pressed: Boolean) {
    ScorePanel(
        side = side,
        pressed = pressed,
        shape = TileShape,
        lightFrom = Alignment.TopCenter,
        pressedScale = 0.975f,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            ScoreDigits(
                score = score,
                color = ScoreColors.of(side).text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = maxWidth * 0.07f,
                        vertical = maxHeight * 0.10f,
                    ),
            )
        }
    }
}
