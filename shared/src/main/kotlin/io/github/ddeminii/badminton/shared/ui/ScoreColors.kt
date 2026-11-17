package io.github.ddeminii.badminton.shared.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.ddeminii.badminton.shared.model.Side

object ScoreColors {
    val MeBackground = Color(0xFF085041)
    val MeText = Color(0xFFE1F5EE)
    val OpponentBackground = Color(0xFF712B13)
    val OpponentText = Color(0xFFFAECE7)

    /** 두 버튼 사이 경계선 */
    val Divider = Color.Black

    fun background(side: Side): Color =
        if (side == Side.ME) MeBackground else OpponentBackground

    /** 누르고 있는 동안의 살짝 밝아진 배경 */
    fun pressedBackground(side: Side): Color = lerp(background(side), Color.White, 0.15f)

    fun text(side: Side): Color = if (side == Side.ME) MeText else OpponentText
}
