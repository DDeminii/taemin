package io.github.ddeminii.badminton.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.padding
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

/**
 * 워치의 제스처 타이밍.
 * tapLockoutMs: +1 직후 같은 버튼 입력을 무시하는 시간. 오터치가 잦으면 올린다 (300~1000 권장).
 */
private val WearGestureConfig = GestureConfig(
    longPressMs = 500,
    bothPressMs = 800,
    tapLockoutMs = 400,
)

/** 원형 화면에서 숫자가 차지하는 세로 비율. 바깥쪽 여백에는 라벨이 들어간다. */
private const val OUTER_SPACE = 0.27f
private const val DIGITS_SPACE = 0.66f
private const val INNER_SPACE = 0.07f

/** 원형 화면을 위/아래 반으로 나눈 점수판. 위 = 나, 아래 = 상대. */
@Composable
fun WearScoreboard(
    state: ScoreState,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(emptySet<Side>()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScoreColors.Divider)
            .twoButtonInput(
                split = SplitDirection.TOP_BOTTOM,
                config = WearGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            WearPanel(Side.ME, state.me, Side.ME in pressed)
            Spacer(Modifier.height(2.dp))
            WearPanel(Side.OPPONENT, state.opponent, Side.OPPONENT in pressed)
        }
        EdgeLabel(Side.ME)
        EdgeLabel(Side.OPPONENT)
    }
}

@Composable
private fun ColumnScope.WearPanel(side: Side, score: Int, pressed: Boolean) {
    val top = side == Side.ME
    ScorePanel(
        side = side,
        pressed = pressed,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        // 숫자를 화면 가운데(원이 가장 넓은 곳) 쪽으로 붙여서 최대한 크게 그린다.
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(if (top) OUTER_SPACE else INNER_SPACE))
            ScoreDigits(
                score = score,
                color = ScoreColors.text(side),
                modifier = Modifier
                    .weight(DIGITS_SPACE)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.weight(if (top) INNER_SPACE else OUTER_SPACE))
        }
    }
}

/** 원형 화면 가장자리를 따라 놓이는 작은 라벨: 위쪽 "나", 아래쪽 "상대". */
@Composable
private fun EdgeLabel(side: Side) {
    val top = side == Side.ME
    val text = stringResource(if (top) SharedR.string.label_me else SharedR.string.label_opponent)
    val color = ScoreColors.text(side).copy(alpha = 0.8f)

    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
        anchor = if (top) 270f else 90f,
        angularDirection = if (top) CurvedDirection.Angular.Normal else CurvedDirection.Angular.Reversed,
    ) {
        basicCurvedText(
            text = text,
            modifier = CurvedModifier.padding(8.dp),
            style = {
                CurvedTextStyle(
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Medium,
                )
            },
        )
    }
}
