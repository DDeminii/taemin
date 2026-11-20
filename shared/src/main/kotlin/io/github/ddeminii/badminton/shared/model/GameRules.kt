package io.github.ddeminii.badminton.shared.model

/**
 * 경기 규칙. 점수 계산은 그대로 두고, 지금이 매치포인트·듀스인지 판단하는 데만 쓴다.
 * [GAME_POINT]점을 먼저 내되 2점 차가 나야 이기며, 점수 상한은 없다.
 */
object GameRules {
    const val GAME_POINT = 25

    /** 경기에서 이긴 쪽. [gamePoint]점 이상이면서 2점 차 이상 앞서면 이긴다. 아직 진행 중이면 null. */
    fun winner(state: ScoreState, gamePoint: Int = GAME_POINT): Side? {
        val lead = state.me - state.opponent
        return when {
            maxOf(state.me, state.opponent) < gamePoint -> null
            lead >= 2 -> Side.ME
            lead <= -2 -> Side.OPPONENT
            else -> null
        }
    }

    /** 어느 쪽이든 매치포인트에 닿을 만큼(24점) 온 경기 막판인지 */
    fun inEndgame(state: ScoreState, gamePoint: Int = GAME_POINT): Boolean =
        maxOf(state.me, state.opponent) >= gamePoint - 1

    /**
     * 불타는 효과를 보여 줄 쪽.
     * - 매치포인트 (한 점만 더 내면 이기는 쪽): 그쪽만
     * - 듀스 (둘 다 한 점만 더 내면 매치포인트인 동점): 양쪽 모두
     * - 경기가 끝났거나 그 전이면: 없음
     */
    fun onFire(state: ScoreState, gamePoint: Int = GAME_POINT): Set<Side> {
        val me = state.me
        val opponent = state.opponent
        val leader = maxOf(me, opponent)
        val lead = kotlin.math.abs(me - opponent)
        return when {
            leader >= gamePoint && lead >= 2 -> emptySet() // 경기 끝
            me == opponent && me >= gamePoint - 1 -> setOf(Side.ME, Side.OPPONENT)
            leader >= gamePoint - 1 && lead >= 1 -> setOf(if (me > opponent) Side.ME else Side.OPPONENT)
            else -> emptySet()
        }
    }
}
