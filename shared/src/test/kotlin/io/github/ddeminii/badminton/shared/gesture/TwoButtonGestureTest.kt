package io.github.ddeminii.badminton.shared.gesture

import io.github.ddeminii.badminton.shared.gesture.GestureAction.Decrement
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Increment
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Reset
import io.github.ddeminii.badminton.shared.model.Side.ME
import io.github.ddeminii.badminton.shared.model.Side.OPPONENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwoButtonGestureTest {

    private val gesture = TwoButtonGesture(
        GestureConfig(longPressMs = 500, bothPressMs = 800, tapLockoutMs = 300),
    )

    @Test
    fun `short press adds one on release`() {
        gesture.onDown(ME, 0)
        assertEquals(500L, gesture.deadlineMs)
        assertEquals(Increment(ME), gesture.onUp(ME, 120))
        assertNull(gesture.deadlineMs)
    }

    @Test
    fun `holding for half a second subtracts one, and releasing afterwards does nothing`() {
        gesture.onDown(OPPONENT, 0)
        assertNull(gesture.onTime(499))
        assertEquals(Decrement(OPPONENT), gesture.onTime(500))
        assertNull(gesture.onTime(2_000))
        assertNull(gesture.onUp(OPPONENT, 2_100))
    }

    @Test
    fun `press right after a point on the same button is ignored`() {
        gesture.onDown(ME, 0)
        gesture.onUp(ME, 100)

        gesture.onDown(ME, 250) // 뗀 뒤 150ms: 무시
        assertEquals(emptySet<Any>(), gesture.activeSides)
        assertNull(gesture.deadlineMs)
        assertNull(gesture.onUp(ME, 300))

        gesture.onDown(ME, 400) // 뗀 뒤 300ms: 정상 입력
        assertEquals(Increment(ME), gesture.onUp(ME, 450))
    }

    @Test
    fun `lockout does not block the other button`() {
        gesture.onDown(ME, 0)
        gesture.onUp(ME, 100)
        gesture.onDown(OPPONENT, 150)
        assertEquals(Increment(OPPONENT), gesture.onUp(OPPONENT, 200))
    }

    @Test
    fun `holding both buttons for 800ms starts a new game exactly once`() {
        gesture.onDown(ME, 0)
        gesture.onDown(OPPONENT, 60)
        assertEquals(860L, gesture.deadlineMs)
        assertNull(gesture.onTime(500)) // 개별 −1은 취소됨
        assertEquals(Reset, gesture.onTime(860))

        assertNull(gesture.onUp(ME, 1_000))
        assertNull(gesture.onUp(OPPONENT, 1_050))
        assertNull(gesture.deadlineMs)
    }

    @Test
    fun `releasing both before 800ms does nothing at all`() {
        gesture.onDown(ME, 0)
        gesture.onDown(OPPONENT, 100)
        assertNull(gesture.onUp(ME, 700))
        assertNull(gesture.deadlineMs)
        assertNull(gesture.onTime(5_000))
        assertNull(gesture.onUp(OPPONENT, 5_100))
    }

    @Test
    fun `a finger that slips during a both-press can come back and restart the 800ms`() {
        gesture.onDown(ME, 0)
        gesture.onDown(OPPONENT, 50)
        gesture.onUp(OPPONENT, 300)
        gesture.onDown(OPPONENT, 350)
        assertEquals(1_150L, gesture.deadlineMs)
        assertEquals(Reset, gesture.onTime(1_150))
    }

    @Test
    fun `after a new game, pressing again while the other finger is still down does nothing`() {
        gesture.onDown(ME, 0)
        gesture.onDown(OPPONENT, 0)
        gesture.onTime(800)
        gesture.onUp(OPPONENT, 900)
        gesture.onDown(OPPONENT, 1_300)
        assertNull(gesture.deadlineMs)
        assertNull(gesture.onUp(OPPONENT, 1_400))
        assertNull(gesture.onUp(ME, 1_500))

        // 손을 다 떼면 다시 평소대로 동작
        gesture.onDown(ME, 2_000)
        assertEquals(Increment(ME), gesture.onUp(ME, 2_100))
    }

    @Test
    fun `second button pressed after a minus still allows a new game`() {
        gesture.onDown(ME, 0)
        assertEquals(Decrement(ME), gesture.onTime(500))
        gesture.onDown(OPPONENT, 600)
        assertEquals(Reset, gesture.onTime(1_400))
    }

    @Test
    fun `cancel drops the press without any action`() {
        gesture.onDown(ME, 0)
        gesture.cancel()
        assertNull(gesture.deadlineMs)
        assertNull(gesture.onUp(ME, 100))
        gesture.onDown(ME, 200)
        assertEquals(Increment(ME), gesture.onUp(ME, 260))
    }
}
