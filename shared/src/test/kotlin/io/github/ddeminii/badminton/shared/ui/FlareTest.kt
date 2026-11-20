package io.github.ddeminii.badminton.shared.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ddeminii.badminton.shared.model.GameRules
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 매치포인트 불은 한 경기에서 한 번만 타오르고, 새 게임이 되면 다시 타오를 수 있다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FlareTest {

    @get:Rule
    val rule = createComposeRule()

    private val state = mutableStateOf(ScoreState(13, 23))
    private lateinit var flare: Flare

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val s = state.value
            flare = rememberFlare(burning = Side.OPPONENT in GameRules.onFire(s), rearm = !GameRules.inEndgame(s))
        }
    }

    private fun score(me: Int, opponent: Int) {
        Snapshot.withMutableSnapshot { state.value = ScoreState(me, opponent) }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun heatAfter(ms: Long): Float {
        rule.mainClock.advanceTimeBy(ms)
        return flare.heat.value
    }

    @Test
    fun `the fire burns once per game and dies down`() {
        score(13, 24) // 매치포인트
        assertTrue(heatAfter(FLARE_IN_MS + 500L) > 0.9f)
        assertEquals(0f, heatAfter(FLARE_HOLD_MS + FLARE_OUT_MS + 500L), 0f)

        score(14, 24) // 매치포인트가 이어져도 다시 켜지지 않는다
        assertEquals(0f, heatAfter(1_000), 0f)
        score(15, 24)
        assertEquals(0f, heatAfter(1_000), 0f)
    }

    @Test
    fun `a new game lets the fire burn again`() {
        score(13, 24)
        heatAfter(FLARE_IN_MS + FLARE_HOLD_MS + FLARE_OUT_MS + 500L)

        score(0, 0) // 새 게임
        score(0, 24)
        assertTrue(heatAfter(FLARE_IN_MS + 500L) > 0.9f)
    }
}
