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
import androidx.compose.ui.draw.rotate
import io.github.ddeminii.badminton.shared.gesture.GestureAction
import io.github.ddeminii.badminton.shared.gesture.GestureConfig
import io.github.ddeminii.badminton.shared.gesture.SplitDirection
import io.github.ddeminii.badminton.shared.gesture.twoButtonInput
import io.github.ddeminii.badminton.shared.model.GameRules
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.ui.Confetti
import io.github.ddeminii.badminton.shared.ui.FlipScore
import io.github.ddeminii.badminton.shared.ui.GameResult
import io.github.ddeminii.badminton.shared.ui.LoserEffect
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
 * - 가로: 좌/우. 왼쪽 = 나, 오른쪽 = 상대
 * - 세로: 네트 아래 바닥에 눕혀 두고 양쪽 선수가 각자 자기 점수를 보는 배치.
 *   아래 = 나 (똑바로), 위 = 상대 (180° 뒤집혀 맞은편 선수에게 똑바로 보임)
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
                split = if (landscape) SplitDirection.LEFT_RIGHT else SplitDirection.BOTTOM_TOP,
                config = PhoneGestureConfig,
                onPressedChange = { pressed = it },
                onAction = onAction,
            )
        val onFire = GameRules.onFire(state)
        val winner = GameRules.winner(state)
        val loserEffect = LoserEffect.forGame(state)
        val half = @Composable { side: Side, modifier: Modifier ->
            ScoreHalf(
                side = side,
                score = if (side == Side.ME) state.me else state.opponent,
                pressed = side in pressed,
                burning = side in onFire,
                result = resultFor(side, winner),
                loserEffect = loserEffect,
                // 세로일 때는 꽃가루도 절반마다 따로, 각 선수 쪽으로 떨어지게 한다.
                confetti = if (landscape) null else winner,
                modifier = modifier,
            )
        }

        if (landscape) {
            Row(board) {
                half(Side.ME, Modifier.weight(1f).fillMaxHeight())
                half(Side.OPPONENT, Modifier.weight(1f).fillMaxHeight())
            }
            Confetti(winner, Modifier.fillMaxSize())
        } else {
            Column(board) {
                half(Side.OPPONENT, Modifier.weight(1f).fillMaxWidth().rotate(180f))
                half(Side.ME, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

private fun resultFor(side: Side, winner: Side?): GameResult? = when (winner) {
    null -> null
    side -> GameResult.WON
    else -> GameResult.LOST
}

@Composable
private fun ScoreHalf(
    side: Side,
    score: Int,
    pressed: Boolean,
    burning: Boolean,
    result: GameResult?,
    loserEffect: LoserEffect,
    confetti: Side?,
    modifier: Modifier,
) {
    Box(modifier = modifier.fireBackground(burning, flameHeight = 0.95f), contentAlignment = Alignment.Center) {
        FlipScore(
            score = score,
            side = side,
            pressed = pressed,
            burning = burning,
            result = result,
            loserEffect = loserEffect,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.84f),
        )
        Confetti(confetti, Modifier.fillMaxSize())
    }
}
