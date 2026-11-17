package io.github.ddeminii.badminton.shared.ui

import androidx.compose.ui.graphics.Color
import io.github.ddeminii.badminton.shared.model.Side

/** 한쪽 타일의 색. 빛이 닿는 곳(light)에서 기본색(base)을 지나 가장자리(dark)로 어두워진다. */
class TilePalette(
    val light: Color,
    val base: Color,
    val dark: Color,
    val text: Color,
)

object ScoreColors {
    /** 나: 청록 */
    val Me = TilePalette(
        light = Color(0xFF128A70),
        base = Color(0xFF085041),
        dark = Color(0xFF04271F),
        text = Color(0xFFE1F5EE),
    )

    /** 상대: 주황 */
    val Opponent = TilePalette(
        light = Color(0xFFB04A22),
        base = Color(0xFF712B13),
        dark = Color(0xFF381207),
        text = Color(0xFFFAECE7),
    )

    /** 타일 사이와 바깥의 바탕 */
    val Backdrop = Color.Black

    fun of(side: Side): TilePalette = if (side == Side.ME) Me else Opponent
}
