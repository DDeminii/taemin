package io.github.ddeminii.badminton.shared.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import io.github.ddeminii.badminton.shared.R
import io.github.ddeminii.badminton.shared.model.Side
import kotlin.math.hypot
import kotlin.math.min

/**
 * 한쪽 점수 버튼의 바탕 타일.
 *
 * 단색 대신 [lightFrom] 지점에서 퍼지는 빛으로 칠해 입체감을 주고,
 * 누르고 있는 동안에는 전체가 살짝 밝아지며 [pressedScale]만큼 눌려 들어간다.
 */
@Composable
fun ScorePanel(
    side: Side,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    lightFrom: Alignment = Alignment.TopCenter,
    pressedScale: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = ScoreColors.of(side)
    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 50 else 220),
        label = "tilePress",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = 1f - (1f - pressedScale) * press.value
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .drawBehind {
                val origin = lightFrom.align(
                    IntSize.Zero,
                    IntSize(size.width.toInt(), size.height.toInt()),
                    LayoutDirection.Ltr,
                ).let { Offset(it.x.toFloat(), it.y.toFloat()) }

                drawRect(
                    Brush.radialGradient(
                        0f to palette.light,
                        0.5f to palette.base,
                        1f to palette.dark,
                        center = origin,
                        radius = hypot(size.width, size.height) * 0.95f,
                    ),
                )
                drawRect(Color.White, alpha = PRESSED_GLOW * press.value)
            },
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

    val context = LocalContext.current
    val paint = remember(context) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.barlow_semi_condensed_semibold)
                ?: Typeface.DEFAULT_BOLD
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
        val textSize = REFERENCE_TEXT_SIZE * fit
        paint.textSize = textSize
        paint.color = color.toArgb()
        // 타일 위에 살짝 떠 있는 느낌의 부드러운 그림자
        paint.setShadowLayer(textSize * 0.05f, 0f, textSize * 0.025f, SHADOW_COLOR)

        // 줄 높이가 아니라 실제 숫자 모양의 위/아래를 기준으로 세로 가운데에 맞춘다.
        paint.getTextBounds("0", 0, 1, bounds)
        val baseline = size.height / 2f - (bounds.top + bounds.bottom) / 2f
        drawIntoCanvas { it.nativeCanvas.drawText(text, size.width / 2f, baseline, paint) }
    }
}

private const val PRESSED_GLOW = 0.13f
private const val POP_SCALE = 1.12f
private const val REFERENCE_TEXT_SIZE = 200f
private const val SHADOW_COLOR = 0x47000000
