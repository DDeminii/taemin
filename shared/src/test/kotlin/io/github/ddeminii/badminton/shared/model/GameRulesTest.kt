package io.github.ddeminii.badminton.shared.model

import io.github.ddeminii.badminton.shared.model.Side.ME
import io.github.ddeminii.badminton.shared.model.Side.OPPONENT
import org.junit.Assert.assertEquals
import org.junit.Test

class GameRulesTest {

    private fun fire(me: Int, opponent: Int) = GameRules.onFire(ScoreState(me, opponent))

    @Test
    fun `nothing burns before anyone reaches 24`() {
        assertEquals(emptySet<Side>(), fire(23, 23))
        assertEquals(emptySet<Side>(), fire(23, 10))
        assertEquals(emptySet<Side>(), fire(0, 0))
    }

    @Test
    fun `match point burns only the side one point from winning`() {
        assertEquals(setOf(ME), fire(24, 22))
        assertEquals(setOf(ME), fire(24, 0))
        assertEquals(setOf(OPPONENT), fire(23, 24))
        assertEquals(setOf(ME), fire(25, 24)) // 듀스 뒤 앞선 쪽
        assertEquals(setOf(OPPONENT), fire(31, 32)) // 상한 없음
    }

    @Test
    fun `deuce burns both sides`() {
        assertEquals(setOf(ME, OPPONENT), fire(24, 24))
        assertEquals(setOf(ME, OPPONENT), fire(27, 27))
    }

    @Test
    fun `the winner needs the game point and a two point lead`() {
        assertEquals(null, GameRules.winner(ScoreState(24, 22)))
        assertEquals(null, GameRules.winner(ScoreState(25, 24)))
        assertEquals(ME, GameRules.winner(ScoreState(25, 23)))
        assertEquals(ME, GameRules.winner(ScoreState(25, 0)))
        assertEquals(OPPONENT, GameRules.winner(ScoreState(28, 30)))
        assertEquals(null, GameRules.winner(ScoreState(31, 31)))
    }

    @Test
    fun `nothing burns once the game is won`() {
        assertEquals(emptySet<Side>(), fire(25, 23))
        assertEquals(emptySet<Side>(), fire(26, 24))
        assertEquals(emptySet<Side>(), fire(30, 32))
    }
}
