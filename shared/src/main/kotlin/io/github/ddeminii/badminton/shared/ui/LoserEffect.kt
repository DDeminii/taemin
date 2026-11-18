package io.github.ddeminii.badminton.shared.ui

import io.github.ddeminii.badminton.shared.model.ScoreState
import kotlin.random.Random

/** 경기에서 진 쪽에 보여 줄 효과. 경기마다 무작위로 하나가 나온다. */
enum class LoserEffect {
    /** 비구름이 떠서 비가 내린다 */
    RAIN,

    /** 카드에 금이 간다 */
    CRACK,

    /** 흰 깃발이 꽂혀 펄럭인다 */
    WHITE_FLAG,

    /** 카드가 힘없이 기울어진다 */
    DROOP;

    companion object {
        /**
         * 이 경기의 효과. 폰과 워치가 똑같이 가진 점수 기록 시각으로 고르므로
         * 두 기기에서 같은 효과가 나온다.
         */
        fun forGame(state: ScoreState): LoserEffect = entries[Random(state.timestamp).nextInt(entries.size)]
    }
}
