package io.github.ddeminii.badminton.wear

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ddeminii.badminton.shared.model.ScoreState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 기기 없이 화면 모양을 확인하기 위한 미리보기. 결과는 wear/build/screenshots 에 저장된다.
 * 저장되는 그림은 사각형이며, 실제 워치에서는 원 바깥이 보이지 않는다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w240dp-h240dp-round-watch-xhdpi")
class WearScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun screenshots() {
        val state = mutableStateOf(ScoreState(me = 21, opponent = 19))
        rule.setContent { WearScoreboard(state = state.value, onAction = {}) }
        rule.onRoot().captureRoboImage("build/screenshots/wear.png")

        // 카드가 넘어가는 중간 모습: 19 → 20 은 두 자리가 함께, 21 → 22 는 일의 자리만 넘어간다.
        rule.mainClock.autoAdvance = false
        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 22, opponent = 20) }
        rule.mainClock.advanceTimeBy(150)
        rule.onRoot().captureRoboImage("build/screenshots/wear_flip_1.png")
        rule.mainClock.advanceTimeBy(150)
        rule.onRoot().captureRoboImage("build/screenshots/wear_flip_2.png")
        rule.mainClock.advanceTimeBy(2_000)

        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 7, opponent = 11) }
        rule.mainClock.advanceTimeBy(2_000)
        rule.onRoot().captureRoboImage("build/screenshots/wear_single_digit.png")

        rule.onRoot().performTouchInput { down(Offset(centerX, height * 0.25f)) }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage("build/screenshots/wear_pressed.png")
    }

    /** 한동안 입력이 없으면 어두운 절전 화면으로 바뀌고, 누르면 바로 다시 밝아진다. */
    @Test
    fun resting() {
        rule.mainClock.autoAdvance = false
        rule.setContent { WearScoreboard(state = ScoreState(me = 7, opponent = 11), onAction = {}) }
        rule.mainClock.advanceTimeBy(REST_AFTER_MS + 2_000)
        rule.onRoot().captureRoboImage("build/screenshots/wear_resting.png")

        rule.onRoot().performTouchInput { down(Offset(centerX, height * 0.75f)) }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage("build/screenshots/wear_woken.png")
    }

    /** 매치포인트(24:22)와 듀스(24:24)에서 불타는 모습 */
    @Test
    fun onFire() {
        rule.mainClock.autoAdvance = false
        val state = mutableStateOf(ScoreState(me = 24, opponent = 22))
        rule.setContent { WearScoreboard(state = state.value, onAction = {}) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage("build/screenshots/wear_match_point.png")

        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 24, opponent = 24) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage("build/screenshots/wear_deuce.png")
    }
}
