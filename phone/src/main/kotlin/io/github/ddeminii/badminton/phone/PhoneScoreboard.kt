package io.github.ddeminii.badminton.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import io.github.ddeminii.badminton.shared.R as SharedR

/** 폰의 제스처 타이밍. 오터치가 잦으면 tapLockoutMs를 올린다. */
private val PhoneGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 300,
)

/** 가로 화면을 좌/우 반으로 나눈 전광판. 왼쪽 = 나, 오른쪽 = 상대. */
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
            .background(ScoreColors.Divider)
            .twoButtonInput(
                split = SplitDirection.LEFT_RIGHT,
                config = PhoneGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            ),
    ) {
        PhonePanel(Side.ME, state.me, Side.ME in pressed)
        Spacer(Modifier.width(3.dp))
        PhonePanel(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed)
    }
}

@Composable
private fun RowScope.PhonePanel(side: Side, score: Int, pressed: Boolean) {
    ScorePanel(
        side = side,
        pressed = pressed,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val labelSize = with(LocalDensity.current) { (maxHeight * 0.075f).toSp() }

            ScoreDigits(
                score = score,
                color = ScoreColors.text(side),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = maxWidth * 0.05f,
                        end = maxWidth * 0.05f,
                        top = maxHeight * 0.13f,
                        bottom = maxHeight * 0.06f,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.02f),
            ) {
                BasicText(
                    text = stringResource(
                        if (side == Side.ME) SharedR.string.label_me else SharedR.string.label_opponent,
                    ),
                    style = TextStyle(
                        color = ScoreColors.text(side).copy(alpha = 0.75f),
                        fontSize = labelSize,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
