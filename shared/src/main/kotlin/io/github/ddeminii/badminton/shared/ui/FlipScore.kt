package io.github.ddeminii.badminton.shared.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.res.ResourcesCompat
import io.github.ddeminii.badminton.shared.R
import io.github.ddeminii.badminton.shared.model.Side
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin

/**
 * 플립 시계 모양의 점수 표시. 자리마다 카드 한 장이고, 숫자가 바뀐 자리의 카드만 넘어간다.
 *
 * 항상 두 자리로 보여 주며(7 → "07", 앞의 0은 흐리게) 주어진 영역 안에서 카드를 가장 크게 놓는다.
 * 누르고 있는 동안에는 카드가 살짝 진해지고 눌려 들어간다.
 *
 * @param cardAspect 카드 한 장의 가로/세로 비율
 * @param rest 절전 화면으로 넘어간 정도 (0 = 평소 색, 1 = 어두운 절전 색). 그릴 때마다 읽는다.
 * @param burning 매치포인트·듀스일 때 true. 카드가 불에 달궈진 듯 달아오른다 (불길은 [fireBackground]).
 * @param result 경기가 끝났을 때 이쪽이 이겼는지(왕관이 내려앉음) 졌는지(회색으로 흐려지고 비구름이 뜸). 진행 중이면 null.
 *   경기가 끝나면 양쪽 카드가 똑같이 줄어들어 위에 왕관·구름 자리를 내므로 두 점수의 글자 크기가 같다.
 */
@Composable
fun FlipScore(
    score: Int,
    side: Side,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    cardAspect: Float = 0.58f,
    rest: () -> Float = { 0f },
    burning: Boolean = false,
    result: GameResult? = null,
) {
    val fade = animateFloatAsState(
        targetValue = if (result == GameResult.LOST) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "fade",
    )
    // 경기가 끝나면 양쪽 카드가 똑같이 줄어 위에 왕관·구름 자리를 낸다.
    val crownRoom = animateFloatAsState(
        targetValue = if (result != null) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "crownRoom",
    )
    // 왕관은 떨어져 통통 튀며 내려앉고, 구름은 부드럽게 내려와 떠 있는다.
    val crownDrop = remember { Animatable(0f) }
    // 구름에서 내리는 비 (몇 초 뒤 그친다)
    val rain = remember { Animatable(0f) }
    LaunchedEffect(result) {
        crownDrop.snapTo(0f)
        rain.snapTo(0f)
        when (result) {
            GameResult.WON -> {
                delay(CROWN_DELAY_MS)
                crownDrop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
            }
            GameResult.LOST -> {
                delay(CROWN_DELAY_MS)
                launch { crownDrop.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow)) }
                delay(RAIN_DELAY_MS)
                rain.animateTo(1f, tween(RAIN_MS, easing = LinearEasing))
            }
            null -> Unit
        }
    }
    val heat = animateFloatAsState(
        targetValue = if (burning) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "heat",
    )
    // 불길이 일렁이는 시간(초). 불타는 동안에만 매 프레임 흐르고, 꺼지면 멈춰 배터리를 쓰지 않는다.
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(burning) {
        if (!burning) return@LaunchedEffect
        val start = withFrameNanos { it } - (time.floatValue * 1e9f).toLong()
        while (true) withFrameNanos { time.floatValue = (it - start) / 1e9f }
    }
    val palette = {
        // 달아오른 카드는 숨 쉬듯 조금씩 밝아졌다 어두워진다.
        val glow = heat.value * (0.85f + 0.15f * sin(time.floatValue * HEAT_PULSE_SPEED))
        ScoreColors.of(side)
            .towards(ScoreColors.hot(side), glow)
            .towards(ScoreColors.Faded, fade.value)
            .towards(ScoreColors.resting(side).towards(ScoreColors.RestingFaded, fade.value), rest())
    }
    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 50 else 220),
        label = "cardPress",
    )
    val digits = score.toString().padStart(2, '0')

    BoxWithConstraints(
        modifier = modifier.semantics { contentDescription = score.toString() },
        contentAlignment = Alignment.Center,
    ) {
        val count = digits.length
        val cardWidth = minOf(maxWidth / (count + (count - 1) * CARD_GAP), maxHeight * cardAspect)
        val cardHeight = cardWidth / cardAspect
        val rowWidth = cardWidth * (count + (count - 1) * CARD_GAP)

        // 불타는 동안에는 카드를 조금 줄이고 위로 띄워, 아래와 옆에서 타오르는 불길이 보이게 한다.
        val fireLayer = Modifier.graphicsLayer {
            val scale = 1f - BURNING_SHRINK * heat.value
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, BURNING_ANCHOR_Y)
        }

        // 이기면 카드가 아래쪽을 기준으로 조금 줄어들어 위에 왕관 자리를 낸다.
        val crownLayer = Modifier.graphicsLayer {
            val scale = 1f - CROWN_ROOM * crownRoom.value
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }

        // 달궈진 카드 주위로 번지는 열기
        Canvas(crownLayer.then(fireLayer).size(rowWidth, cardHeight)) {
            drawHeatHalo(heat.value * (1f - rest()))
        }

        Row(
            modifier = crownLayer.then(fireLayer).graphicsLayer {
                val scale = 1f - PRESSED_SHRINK * press.value
                scaleX = scale
                scaleY = scale
            },
            horizontalArrangement = Arrangement.spacedBy(cardWidth * CARD_GAP),
        ) {
            digits.forEachIndexed { index, digit ->
                // 일의 자리부터 번호를 매겨, 자릿수가 늘어도 같은 자리의 카드가 유지되게 한다.
                key(count - index) {
                    FlipCard(
                        face = Face(digit, dimmed = score < 10 && index == 0),
                        palette = palette,
                        press = press,
                        modifier = Modifier.size(cardWidth, cardHeight),
                    )
                }
            }
        }

        // 이긴 쪽은 왕관, 진 쪽은 비구름이 카드 위에 자리 잡는다.
        Canvas(Modifier.size(rowWidth, cardHeight)) {
            val drop = crownDrop.value
            if (drop <= 0f || result == null) return@Canvas
            val cardsTop = size.height * CROWN_ROOM * crownRoom.value
            val width = size.width * CROWN_WIDTH * (1f - CROWN_ROOM * crownRoom.value)
            val alpha = (drop * 3f).coerceIn(0f, 1f)
            if (result == GameResult.WON) {
                val fall = (1f - drop) * size.height * 0.9f
                drawCrown(size.width / 2f, cardsTop - size.height * 0.015f - fall, width, alpha)
            } else {
                val fall = (1f - drop) * size.height * 0.25f
                val cloudBase = cardsTop - size.height * 0.03f - fall
                drawRain(size.width / 2f, cloudBase, width, rain.value)
                drawCloud(size.width / 2f, cloudBase, width, alpha)
            }
        }
    }
}

/** 경기가 끝났을 때 이쪽의 결과 */
enum class GameResult { WON, LOST }

/** 비구름 하나를 [baseY]에 바닥을 맞춰 그린다. */
private fun DrawScope.drawCloud(centerX: Float, baseY: Float, width: Float, alpha: Float) {
    val height = width * 0.5f
    fun puffs(color: Color, dy: Float, a: Float) {
        drawCircle(color, radius = height * 0.50f, center = Offset(centerX - width * 0.02f, baseY - height * 0.50f + dy), alpha = a)
        drawCircle(color, radius = height * 0.36f, center = Offset(centerX - width * 0.28f, baseY - height * 0.36f + dy), alpha = a)
        drawCircle(color, radius = height * 0.42f, center = Offset(centerX + width * 0.25f, baseY - height * 0.42f + dy), alpha = a)
        drawRoundRect(
            color = color,
            topLeft = Offset(centerX - width * 0.44f, baseY - height * 0.5f + dy),
            size = Size(width * 0.88f, height * 0.5f),
            cornerRadius = CornerRadius(height * 0.25f),
            alpha = a,
        )
    }
    puffs(Color.Black, height * 0.06f, 0.08f * alpha)
    puffs(ScoreColors.CloudShade, 0f, alpha)
    puffs(ScoreColors.Cloud, -height * 0.07f, alpha)
}

/** 구름 아래로 떨어지는 빗줄기. [progress]가 1이 되면 그친다. */
private fun DrawScope.drawRain(centerX: Float, cloudBase: Float, width: Float, progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val seconds = progress * RAIN_MS / 1000f
    val fade = min(1f, progress / 0.08f) * min(1f, (1f - progress) / 0.2f)
    val fallHeight = size.height - cloudBase
    val length = width * 0.14f
    for (i in 0 until RAIN_DROPS) {
        val x = centerX + ((i + 0.5f) / RAIN_DROPS - 0.5f) * width * 0.8f + sin(i * 7.3f) * width * 0.03f
        val phase = (i * 0.618f) % 1f
        val y = cloudBase + ((phase + seconds * (1.1f + (i % 3) * 0.15f)) % 1f) * fallHeight
        drawLine(
            color = ScoreColors.Rain,
            start = Offset(x, y),
            end = Offset(x - length * 0.15f, y + length),
            strokeWidth = width * 0.022f,
            cap = StrokeCap.Round,
            alpha = 0.85f * fade,
        )
    }
}

/** 왕관 하나를 [baseY]에 바닥을 맞춰 그린다. */
private fun DrawScope.drawCrown(centerX: Float, baseY: Float, width: Float, alpha: Float) {
    val height = width * 0.62f
    val left = centerX - width / 2f
    val right = centerX + width / 2f
    val outline = Path().apply {
        moveTo(left, baseY)
        lineTo(left, baseY - height * 0.6f)
        lineTo(centerX - width / 4f, baseY - height * 0.25f)
        lineTo(centerX, baseY - height)
        lineTo(centerX + width / 4f, baseY - height * 0.25f)
        lineTo(right, baseY - height * 0.6f)
        lineTo(right, baseY)
        close()
    }
    translate(top = height * 0.035f) { drawPath(outline, Color.Black, alpha = 0.10f * alpha) }
    drawPath(outline, ScoreColors.CrownGold, alpha = alpha)
    drawRect(
        color = ScoreColors.CrownBand,
        topLeft = Offset(left, baseY - height * 0.18f),
        size = Size(width, height * 0.18f),
        alpha = alpha,
    )
    for (tip in listOf(Offset(left, baseY - height * 0.6f), Offset(centerX, baseY - height), Offset(right, baseY - height * 0.6f))) {
        drawCircle(ScoreColors.CrownPearl, radius = height * 0.09f, center = tip, alpha = alpha)
    }
    ScoreColors.CrownGems.forEachIndexed { i, gem ->
        drawCircle(gem, radius = height * 0.065f, center = Offset(centerX + (i - 1) * width / 4f, baseY - height * 0.09f), alpha = alpha)
    }
}

/** 달궈진 카드 주위로 번지는 주황빛 열기. [heat]가 0이면 아무것도 그리지 않는다. */
private fun DrawScope.drawHeatHalo(heat: Float) {
    if (heat <= 0.01f) return
    val w = size.width
    val h = size.height
    for (step in 1..6) {
        val grow = h * 0.04f * step
        drawRoundRect(
            color = ScoreColors.Heat,
            alpha = 0.07f * heat,
            topLeft = Offset(-grow, -grow * 0.7f),
            size = Size(w + grow * 2, h + grow * 1.2f),
            cornerRadius = CornerRadius(w * 0.08f + grow),
        )
    }
}

/** 카드 한 면에 그려지는 내용 */
private data class Face(val digit: Char, val dimmed: Boolean)

/**
 * 카드 한 장. 내용이 바뀌면 실제 플립 시계처럼 넘어간다.
 *
 * 1. 위쪽 절반(이전 숫자)이 앞으로 쓰러지며 그 뒤의 새 숫자 위쪽이 드러나고,
 * 2. 이어서 새 숫자의 아래쪽 절반이 내려와 이전 숫자의 아래쪽을 덮는다.
 */
@Composable
private fun FlipCard(
    face: Face,
    palette: () -> SidePalette,
    press: State<Float>,
    modifier: Modifier = Modifier,
) {
    var from by remember { mutableStateOf(face) }
    var to by remember { mutableStateOf(face) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(face) {
        if (face != to) {
            from = to
            to = face
            progress.snapTo(0f)
            progress.animateTo(1f, tween(FLIP_MS, easing = LinearEasing))
        }
    }

    val context = LocalContext.current
    val painter = remember(context) {
        CardPainter(ResourcesCompat.getFont(context, R.font.oswald_semibold) ?: Typeface.DEFAULT_BOLD)
    }

    Box(modifier) {
        // 움직이지 않는 부분: 새 숫자의 위쪽, 그리고 덮이기 전까지는 이전 숫자의 아래쪽
        Canvas(Modifier.matchParentSize()) {
            val flipping = progress.value < 1f
            val colors = palette()
            with(painter) {
                drawShadow(colors)
                drawHalf(colors, top = true, face = to, press = press.value)
                drawHalf(colors, top = false, face = if (flipping) from else to, press = press.value)
            }
        }
        // 넘어가는 날개. 카드 가운데 가로선을 축으로 돈다.
        Canvas(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    cameraDistance = 16f * density
                    rotationX = flapAngle(progress.value)
                },
        ) {
            val p = progress.value
            val colors = palette()
            with(painter) {
                when {
                    p < 0.5f -> drawHalf(colors, top = true, face = from, press = press.value, shade = flapShade(p))
                    p < 1f -> drawHalf(colors, top = false, face = to, press = press.value, shade = flapShade(p))
                }
                drawFold(colors)
            }
        }
    }
}

/** 0 → 0.5: 위 날개가 0°에서 −90°로 가속하며 쓰러진다. 0.5 → 1: 아래 날개가 90°에서 0°로 감속하며 내려앉는다. */
private fun flapAngle(progress: Float): Float = when {
    progress >= 1f -> 0f
    progress < 0.5f -> -90f * square(progress / 0.5f)
    else -> 90f * square(1f - (progress - 0.5f) / 0.5f)
}

/** 날개가 세워질수록 빛을 덜 받아 어두워진다. */
private fun flapShade(progress: Float): Float =
    MAX_FLAP_SHADE * (if (progress < 0.5f) square(progress / 0.5f) else square(1f - (progress - 0.5f) / 0.5f))

private fun square(x: Float) = x * x

private class CardPainter(typeface: Typeface) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textAlign = Paint.Align.CENTER
    }
    private val bounds = Rect()

    private fun DrawScope.corner() = CornerRadius(size.width * 0.13f)

    /** 카드 아래에 깔리는 부드러운 그림자 */
    fun DrawScope.drawShadow(palette: SidePalette) {
        if (palette.shadow <= 0f) return
        val spread = size.width * 0.035f
        for (step in 1..4) {
            val grow = spread * step / 4f
            drawRoundRect(
                color = Color.Black,
                alpha = 0.035f * palette.shadow,
                topLeft = Offset(-grow * 0.5f, spread * 0.9f - grow * 0.2f),
                size = Size(size.width + grow, size.height + grow * 0.6f),
                cornerRadius = corner(),
            )
        }
    }

    /** 카드의 위쪽 또는 아래쪽 절반만 그린다. */
    fun DrawScope.drawHalf(palette: SidePalette, top: Boolean, face: Face, press: Float, shade: Float = 0f) {
        val middle = size.height / 2f
        clipRect(top = if (top) 0f else middle, bottom = if (top) middle else size.height) {
            val card = lerp(palette.card, palette.cardPressed, press)
            val highlight = lerp(palette.cardHighlight, palette.cardPressed, press)
            drawRoundRect(Brush.verticalGradient(listOf(highlight, card)), cornerRadius = corner())
            // 아래쪽 판은 살짝 그늘져 위쪽 판과 구분된다.
            if (!top) drawRoundRect(Color.Black, alpha = 0.03f, cornerRadius = corner())

            paint.textSize = REFERENCE_TEXT_SIZE
            paint.getTextBounds("0", 0, 1, bounds)
            val fit = min(size.height * DIGIT_HEIGHT / bounds.height(), size.width * DIGIT_WIDTH / bounds.width())
            paint.textSize = REFERENCE_TEXT_SIZE * fit
            paint.getTextBounds("0", 0, 1, bounds)
            paint.color = (if (face.dimmed) palette.digitDimmed else palette.digit).toArgb()
            val baseline = middle - (bounds.top + bounds.bottom) / 2f
            drawIntoCanvas { it.nativeCanvas.drawText(face.digit.toString(), size.width / 2f, baseline, paint) }

            if (shade > 0f) drawRoundRect(Color.Black, alpha = shade, cornerRadius = corner())
        }
    }

    /** 카드 가운데의 접힌 선 */
    fun DrawScope.drawFold(palette: SidePalette) {
        val thickness = (size.height * 0.011f).coerceAtLeast(1f)
        drawRect(
            color = palette.fold,
            topLeft = Offset(0f, size.height / 2f - thickness / 2f),
            size = Size(size.width, thickness),
        )
    }
}

/** 카드 사이 간격 (카드 너비 대비) */
private const val CARD_GAP = 0.08f

/** 카드 안에서 숫자가 차지하는 최대 높이/너비 비율 */
private const val DIGIT_HEIGHT = 0.70f
private const val DIGIT_WIDTH = 0.78f

/** 카드 한 장이 넘어가는 데 걸리는 시간 */
private const val FLIP_MS = 380

private const val MAX_FLAP_SHADE = 0.22f

/** 불타는 동안 카드가 줄어드는 비율과, 줄어들 때 붙잡는 세로 위치 (0 = 위, 1 = 아래) */
private const val BURNING_SHRINK = 0.16f
private const val BURNING_ANCHOR_Y = 0.15f

/** 이겼을 때 왕관 자리를 내려고 카드가 줄어드는 비율, 왕관 너비(카드 줄 너비 대비), 떨어지기 전 잠깐의 틈 */
private const val CROWN_ROOM = 0.2f
private const val CROWN_WIDTH = 0.42f
private const val CROWN_DELAY_MS = 250L

/** 진 쪽 구름에서 비가 내리는 시간, 구름이 뜬 뒤 비가 시작되기까지의 틈, 빗줄기 수 */
private const val RAIN_MS = 4_000
private const val RAIN_DELAY_MS = 450L
private const val RAIN_DROPS = 9

/** 달아오른 카드가 밝아졌다 어두워지는 빠르기 (라디안/초) */
private const val HEAT_PULSE_SPEED = 3f
private const val PRESSED_SHRINK = 0.03f
private const val REFERENCE_TEXT_SIZE = 200f
