package io.github.ddeminii.badminton.shared.haptics

import io.github.ddeminii.badminton.shared.model.ScoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HapticPatternTest {

    private fun change(from: Pair<Int, Int>, to: Pair<Int, Int>) =
        HapticPattern.forChange(ScoreState(from.first, from.second, 1), ScoreState(to.first, to.second, 2))

    @Test
    fun `remote changes map to the same patterns as local presses`() {
        assertEquals(HapticPattern.MY_POINT, change(3 to 2, 4 to 2))
        assertEquals(HapticPattern.OPPONENT_POINT, change(3 to 2, 3 to 3))
        assertEquals(HapticPattern.MINUS, change(3 to 2, 2 to 2))
        assertEquals(HapticPattern.MINUS, change(3 to 2, 3 to 1))
        assertEquals(HapticPattern.NEW_GAME, change(11 to 9, 0 to 0))
    }

    @Test
    fun `no vibration when nothing changed or the jump cannot be one press`() {
        assertNull(change(3 to 2, 3 to 2))
        assertNull(change(0 to 0, 0 to 0))
        assertNull(change(3 to 2, 7 to 5))
    }
}
