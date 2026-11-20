package io.github.ddeminii.badminton.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.ddeminii.badminton.shared.model.Side
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 경기가 끝나면 이긴 쪽 색의 꽃가루가 화면 위에서 쏟아진다.
 * 몇 초 뒤 다 떨어지면 애니메이션이 멈춰 배터리를 쓰지 않는다. 터치는 가로채지 않는다.
 */
@Composable
fun Confetti(winner: Side?, modifier: Modifier = Modifier) {
    if (winner == null) return
    val progress = remember(winner) { Animatable(0f) }
    LaunchedEffect(winner) {
        progress.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing))
    }
    val colors = ScoreColors.confetti(winner)
    val pieces = remember(winner) {
        val random = Random(winner.ordinal * 31 + 7)
        List(CONFETTI_PIECES) {
            Piece(
                x = random.nextFloat(),
                startY = -0.1f - random.nextFloat() * 0.9f,
                speed = 0.35f + random.nextFloat() * 0.35f,
                sway = 0.015f + random.nextFloat() * 0.03f,
                swaySpeed = 2f + random.nextFloat() * 3f,
                phase = random.nextFloat() * 6.28f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                flip = 3f + random.nextFloat() * 5f,
                size = 0.8f + random.nextFloat() * 0.6f,
                color = colors[random.nextInt(colors.size)],
            )
        }
    }

    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val seconds = p * CONFETTI_MS / 1000f
        val unit = min(size.width, size.height) * 0.024f
        val fadeOut = ((1f - p) / 0.15f).coerceIn(0f, 1f)
        for (piece in pieces) {
            val y = (piece.startY + piece.speed * seconds) * size.height
            if (y < -unit * 2 || y > size.height + unit * 2) continue
            val x = (piece.x + sin(seconds * piece.swaySpeed + piece.phase) * piece.sway) * size.width
            val w = unit * 1.6f * piece.size
            // 종이가 뒤집히며 떨어지는 것처럼 높이가 납작해졌다 돌아온다.
            val h = unit * 0.7f * piece.size * (0.25f + 0.75f * abs(cos(seconds * piece.flip + piece.phase)))
            rotate(piece.spin * seconds + piece.phase * 57f, pivot = Offset(x, y)) {
                drawRect(piece.color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = fadeOut)
            }
        }
    }
}

private class Piece(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val sway: Float,
    val swaySpeed: Float,
    val phase: Float,
    val spin: Float,
    val flip: Float,
    val size: Float,
    val color: Color,
)

private const val CONFETTI_MS = 4_000
private const val CONFETTI_PIECES = 110
