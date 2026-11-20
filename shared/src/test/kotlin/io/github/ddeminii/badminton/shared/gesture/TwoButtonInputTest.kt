package io.github.ddeminii.badminton.shared.gesture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Decrement
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Increment
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Reset
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.model.Side.ME
import io.github.ddeminii.badminton.shared.model.Side.OPPONENT
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 실제 터치 이벤트가 Compose를 거쳐 올바른 동작으로 바뀌는지 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TwoButtonInputTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableStateListOf<GestureAction>()
    private val pressed = mutableStateOf(emptySet<Side>())

    private val TouchInjectionScope.topHalf get() = Offset(centerX, height * 0.25f)
    private val TouchInjectionScope.bottomHalf get() = Offset(centerX, height * 0.75f)

    @Before
    fun setUp() {
        rule.setContent {
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("board")
                    .twoButtonInput(
                        split = SplitDirection.TOP_BOTTOM,
                        config = GestureConfig(longPressMs = 500, bothPressMs = 800, tapLockoutMs = 300),
                        onPressedChange = { pressed.value = it },
                        onAction = { actions += it },
                    ),
            )
        }
    }

    private fun board() = rule.onNodeWithTag("board")

    @Test
    fun `tap on each half scores for that side`() {
        board().performTouchInput { down(topHalf); advanceEventTime(80); up() }
        board().performTouchInput { advanceEventTime(100); down(bottomHalf); advanceEventTime(80); up() }
        assertEquals(listOf<GestureAction>(Increment(ME), Increment(OPPONENT)), actions.toList())
    }

    @Test
    fun `long press subtracts while the finger is still down`() {
        board().performTouchInput { down(bottomHalf) }
        rule.mainClock.advanceTimeBy(450)
        assertEquals(emptyList<GestureAction>(), actions.toList())
        assertEquals(setOf(OPPONENT), pressed.value)

        rule.mainClock.advanceTimeBy(100)
        assertEquals(listOf<GestureAction>(Decrement(OPPONENT)), actions.toList())

        board().performTouchInput { up() }
        assertEquals(listOf<GestureAction>(Decrement(OPPONENT)), actions.toList())
        assertEquals(emptySet<Side>(), pressed.value)
    }

    @Test
    fun `a sliding finger keeps the button it first touched`() {
        board().performTouchInput {
            down(topHalf)
            moveTo(bottomHalf)
            advanceEventTime(60)
            up()
        }
        assertEquals(listOf<GestureAction>(Increment(ME)), actions.toList())
    }

    @Test
    fun `second tap inside the lockout is ignored`() {
        board().performTouchInput {
            down(topHalf); advanceEventTime(60); up()
            advanceEventTime(150)
            down(topHalf); advanceEventTime(60); up()
        }
        assertEquals(listOf<GestureAction>(Increment(ME)), actions.toList())
    }

    @Test
    fun `holding both halves resets once and cancels the individual long presses`() {
        board().performTouchInput {
            down(0, topHalf)
            advanceEventTime(40)
            down(1, bottomHalf)
        }
        rule.mainClock.advanceTimeBy(700)
        assertEquals(emptyList<GestureAction>(), actions.toList())
        assertEquals(setOf(ME, OPPONENT), pressed.value)

        rule.mainClock.advanceTimeBy(200)
        assertEquals(listOf<GestureAction>(Reset), actions.toList())

        board().performTouchInput { up(0); advanceEventTime(30); up(1) }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(listOf<GestureAction>(Reset), actions.toList())
    }

    @Test
    fun `letting go of both halves early does nothing`() {
        board().performTouchInput {
            down(0, topHalf)
            down(1, bottomHalf)
            advanceEventTime(600)
            up(0)
            advanceEventTime(400)
            up(1)
        }
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(emptyList<GestureAction>(), actions.toList())
    }

    @Test
    fun `a cancelled touch does not score`() {
        board().performTouchInput { down(topHalf); advanceEventTime(60); cancel() }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(emptyList<GestureAction>(), actions.toList())
        assertEquals(emptySet<Side>(), pressed.value)
    }
}
