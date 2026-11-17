package io.github.ddeminii.badminton.shared.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.ddeminii.badminton.shared.model.Side
import kotlin.math.min

/** 한쪽 점수 버튼의 배경. 누르고 있는 동안 살짝 밝아진다. */
@Composable
fun ScorePanel(
    side: Side,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = animateColorAsState(
        targetValue = if (pressed) ScoreColors.pressedBackground(side) else ScoreColors.background(side),
        animationSpec = tween(durationMillis = if (pressed) 40 else 200),
        label = "panelBackground",
    )
    Box(
        modifier = modifier.drawBehind { drawRect(background.value) },
        content = content,
    )
}

/**
 * 주어진 영역에 들어가는 한도에서 가장 크게 점수를 그린다.
 *
 * 글자 크기는 항상 두 자리 수("00") 기준으로 맞추므로 9 → 10으로 넘어갈 때 크기가 튀지 않는다.
 * 점수가 바뀌면 숫자가 살짝 커졌다가 돌아온다.
 */
@Composable
fun ScoreDigits(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    var shownScore by remember { mutableIntStateOf(score) }
    LaunchedEffect(score) {
        if (score != shownScore) {
            shownScore = score
            scale.snapTo(POP_SCALE)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val bounds = remember { Rect() }
    val text = score.toString()

    Canvas(
        modifier = modifier
            .semantics { contentDescription = text }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        val template = "0".repeat(maxOf(2, text.length))
        paint.textSize = REFERENCE_TEXT_SIZE
        paint.getTextBounds(template, 0, template.length, bounds)
        val fit = min(size.width / bounds.width(), size.height / bounds.height())
        paint.textSize = REFERENCE_TEXT_SIZE * fit
        paint.color = color.toArgb()

        // 줄 높이가 아니라 실제 숫자 모양의 위/아래를 기준으로 세로 가운데에 맞춘다.
        paint.getTextBounds("0", 0, 1, bounds)
        val baseline = size.height / 2f - (bounds.top + bounds.bottom) / 2f
        drawIntoCanvas { it.nativeCanvas.drawText(text, size.width / 2f, baseline, paint) }
    }
}

private const val POP_SCALE = 1.14f
private const val REFERENCE_TEXT_SIZE = 200f
