package io.github.ddeminii.badminton.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.ddeminii.badminton.shared.model.Side

/** 한쪽 플립 카드의 색. */
class SidePalette(
    /** 카드 바탕 */
    val card: Color,
    /** 카드 위 숫자 */
    val digit: Color,
) {
    /** 카드 위쪽은 빛을 받아 조금 더 밝다. */
    val cardHighlight: Color = lerp(card, Color.White, 0.45f)

    /** 누르고 있는 동안의 카드 바탕: 숫자 색 쪽으로 살짝 진해진다. */
    val cardPressed: Color = lerp(card, digit, 0.09f)

    /** 카드 가운데 접힌 선 */
    val fold: Color = lerp(card, digit, 0.38f)

    /** 한 자리 수일 때 앞자리에 흐리게 보이는 0 */
    val digitDimmed: Color = digit.copy(alpha = 0.16f)
}

object ScoreColors {
    /** 나: 민트 카드에 짙은 청록 숫자 */
    val Me = SidePalette(card = Color(0xFFCDEBDD), digit = Color(0xFF085041))

    /** 상대: 피치 카드에 짙은 주황 숫자 */
    val Opponent = SidePalette(card = Color(0xFFF8D9C8), digit = Color(0xFF712B13))

    /** 화면 바탕 */
    val Backdrop = Color(0xFFF3F0EA)

    fun of(side: Side): SidePalette = if (side == Side.ME) Me else Opponent
}
