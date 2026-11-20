package io.github.ddeminii.badminton.phone

import org.junit.Assert.assertEquals
import org.junit.Test

class FlatOrientationHoldTest {

    @Test
    fun `tilt is zero lying flat and ninety standing up`() {
        assertEquals(0f, FlatOrientationHold.tiltDegrees(0f, 0f, 9.8f), 0.5f)
        assertEquals(0f, FlatOrientationHold.tiltDegrees(0f, 0f, -9.8f), 0.5f) // 화면이 바닥을 향해도
        assertEquals(90f, FlatOrientationHold.tiltDegrees(0f, 9.8f, 0f), 0.5f)
        assertEquals(45f, FlatOrientationHold.tiltDegrees(0f, 6.93f, 6.93f), 0.5f)
    }
}
