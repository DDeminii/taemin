package io.github.ddeminii.badminton.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.ddeminii.badminton.shared.model.Side

/** 한쪽 플립 카드의 색. */
class SidePalette(
    /** 카드 바탕 */
    val card: Color,
    /** 카드 위쪽은 빛을 받아 조금 더 밝다. */
    val cardHighlight: Color,
    /** 누르고 있는 동안의 카드 바탕 */
    val cardPressed: Color,
    /** 카드 가운데 접힌 선 */
    val fold: Color,
    /** 카드 위 숫자 */
    val digit: Color,
    /** 한 자리 수일 때 앞자리에 흐리게 보이는 0 */
    val digitDimmed: Color,
    /** 카드 아래 그림자의 진하기 (0이면 없음) */
    val shadow: Float,
) {
    /** 이 색에서 [other] 색으로 [fraction]만큼 넘어간 중간 색 */
    fun towards(other: SidePalette, fraction: Float): SidePalette = when (fraction) {
        0f -> this
        1f -> other
        else -> SidePalette(
            card = lerp(card, other.card, fraction),
            cardHighlight = lerp(cardHighlight, other.cardHighlight, fraction),
            cardPressed = lerp(cardPressed, other.cardPressed, fraction),
            fold = lerp(fold, other.fold, fraction),
            digit = lerp(digit, other.digit, fraction),
            digitDimmed = lerp(digitDimmed, other.digitDimmed, fraction),
            shadow = shadow + (other.shadow - shadow) * fraction,
        )
    }
}

object ScoreColors {
    /** 화면 바탕 */
    val Backdrop = Color(0xFFF3F0EA)

    /** 나: 민트 카드에 짙은 청록 숫자 */
    val Me = bright(card = Color(0xFFCDEBDD), digit = Color(0xFF085041))

    /** 상대: 피치 카드에 짙은 주황 숫자 */
    val Opponent = bright(card = Color(0xFFF8D9C8), digit = Color(0xFF712B13))

    /**
     * 절전 화면. OLED는 어두운 픽셀일수록 전력을 덜 쓰므로 바탕을 검게 하고
     * 카드는 아주 어둡게, 숫자만 읽을 수 있을 만큼 밝게 남긴다.
     */
    val RestingBackdrop = Color.Black
    val RestingMe = resting(card = Color(0xFF07271F), digit = Color(0xFF9AD8C2))
    val RestingOpponent = resting(card = Color(0xFF2C1209), digit = Color(0xFFF0B79C))

    /** 매치포인트·듀스: 카드가 불에 달궈진 듯 주황빛으로 달아오른다. 숫자는 원래 색을 붉게 태운 색. */
    val HotMe = hot(digit = Me.digit)
    val HotOpponent = hot(digit = Opponent.digit)

    /** 카드 뒤에서 피어오르는 불길의 색 (바깥 불꽃 → 안쪽 불꽃) */
    val FlameOuter = listOf(Color(0xF2E23A0C), Color(0xE6FF7A1E), Color(0x00FFB347))
    val FlameInner = listOf(Color(0xF2FFC845), Color(0xCCFFF0B0), Color(0x00FFF6D0))
    val Heat = Color(0xFFFF7A22)

    /** 불타는 쪽의 어두운 바탕. 불길이 검은 배경 위에서처럼 선명하게 빛나게 한다. */
    val FireBackdrop = Color(0xFF120503)

    fun of(side: Side): SidePalette = if (side == Side.ME) Me else Opponent

    fun hot(side: Side): SidePalette = if (side == Side.ME) HotMe else HotOpponent

    fun resting(side: Side): SidePalette = if (side == Side.ME) RestingMe else RestingOpponent

    private fun bright(card: Color, digit: Color) = SidePalette(
        card = card,
        cardHighlight = lerp(card, Color.White, 0.45f),
        cardPressed = lerp(card, digit, 0.09f),
        fold = lerp(card, digit, 0.38f),
        digit = digit,
        digitDimmed = digit.copy(alpha = 0.16f),
        shadow = 1f,
    )

    private fun hot(digit: Color): SidePalette {
        val card = Color(0xFFFFAE6E)
        val burnt = lerp(digit, Color(0xFF961400), 0.5f)
        return SidePalette(
            card = card,
            cardHighlight = Color(0xFFFFDCA0),
            cardPressed = lerp(card, burnt, 0.09f),
            fold = lerp(card, burnt, 0.38f),
            digit = burnt,
            digitDimmed = burnt.copy(alpha = 0.16f),
            shadow = 1f,
        )
    }

    private fun resting(card: Color, digit: Color) = SidePalette(
        card = card,
        cardHighlight = lerp(card, Color.White, 0.04f),
        cardPressed = lerp(card, digit, 0.22f),
        fold = Color.Black,
        digit = digit,
        digitDimmed = digit.copy(alpha = 0.18f),
        shadow = 0f,
    )
}
