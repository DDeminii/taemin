package io.github.ddeminii.badminton.shared.cheer

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CheerScheduleTest {

    @Test
    fun `cheers come every four to seven points, never back to back`() {
        val schedule = CheerSchedule(everyPoints = 4..7, random = Random(42))
        val cheeredAt = (1..500).filter { schedule.onPoint() }

        val gaps = (listOf(0) + cheeredAt).zipWithNext { a, b -> b - a }
        assertTrue("gaps were $gaps", gaps.all { it in 4..7 })
        assertTrue("expected a mix of gaps, got $gaps", gaps.toSet().size > 1)
    }
}
