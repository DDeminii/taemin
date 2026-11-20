package io.github.ddeminii.badminton.shared.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreStateTest {

    @Test
    fun `increment adds one to the chosen side only`() {
        val state = ScoreState().increment(Side.ME, now = 100).increment(Side.OPPONENT, now = 200)
        assertEquals(ScoreState(me = 1, opponent = 1, timestamp = 200), state)
    }

    @Test
    fun `decrement never goes below zero`() {
        val zero = ScoreState(me = 0, opponent = 3, timestamp = 50)
        assertSame(zero, zero.decrement(Side.ME, now = 100))
        assertEquals(ScoreState(0, 2, 100), zero.decrement(Side.OPPONENT, now = 100))
    }

    @Test
    fun `reset goes back to zero with a newer timestamp`() {
        val state = ScoreState(me = 15, opponent = 12, timestamp = 50).reset(now = 100)
        assertEquals(ScoreState(0, 0, 100), state)
    }

    @Test
    fun `a change is always newer than the state it was made from, even if the clock is behind`() {
        val fromOtherDevice = ScoreState(me = 3, opponent = 2, timestamp = 10_000)
        val local = fromOtherDevice.increment(Side.ME, now = 4_000)
        assertTrue(local.isNewerThan(fromOtherDevice))
        assertFalse(fromOtherDevice.isNewerThan(local))
    }

    @Test
    fun `equal timestamps still resolve to the same winner on both devices`() {
        val a = ScoreState(me = 4, opponent = 2, timestamp = 100)
        val b = ScoreState(me = 3, opponent = 3, timestamp = 100)
        assertTrue(a.isNewerThan(b) != b.isNewerThan(a))
        assertFalse(a.isNewerThan(a))
    }

    @Test
    fun `survives a round trip through bytes`() {
        val state = ScoreState(me = 21, opponent = 19, timestamp = 1_763_000_000_000)
        assertEquals(state, ScoreState.fromBytes(state.toBytes()))
    }

    @Test
    fun `rejects malformed payloads`() {
        assertNull(ScoreState.fromBytes(null))
        assertNull(ScoreState.fromBytes(ByteArray(3)))
        assertNull(ScoreState.fromBytes(ScoreState(me = -1, opponent = 0, timestamp = 1).toBytes()))
    }
}
