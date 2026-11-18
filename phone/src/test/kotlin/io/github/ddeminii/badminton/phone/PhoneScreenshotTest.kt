package io.github.ddeminii.badminton.phone

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

/** 기기 없이 화면 모양을 확인하기 위한 미리보기. 결과는 phone/build/screenshots 에 저장된다. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w780dp-h360dp-land-xxhdpi")
class PhoneScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun screenshots() {
        val state = mutableStateOf(ScoreState(me = 21, opponent = 19))
        rule.setContent { PhoneScoreboard(state = state.value, onAction = {}) }
        rule.onRoot().captureRoboImage("build/screenshots/phone.png")

        // 카드가 넘어가는 중간 모습: 19 → 20 은 두 자리가 함께, 21 → 22 는 일의 자리만 넘어간다.
        rule.mainClock.autoAdvance = false
        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 22, opponent = 20) }
        rule.mainClock.advanceTimeBy(150)
        rule.onRoot().captureRoboImage("build/screenshots/phone_flip_1.png")
        rule.mainClock.advanceTimeBy(150)
        rule.onRoot().captureRoboImage("build/screenshots/phone_flip_2.png")
        rule.mainClock.advanceTimeBy(2_000)

        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 7, opponent = 11) }
        rule.mainClock.advanceTimeBy(2_000)
        rule.onRoot().captureRoboImage("build/screenshots/phone_single_digit.png")

        rule.onRoot().performTouchInput { down(Offset(width * 0.25f, centerY)) }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage("build/screenshots/phone_pressed.png")
    }

    /** 폰을 세로로 세웠을 때: 위 = 나, 아래 = 상대 */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi")
    fun portrait() {
        rule.setContent { PhoneScoreboard(state = ScoreState(me = 21, opponent = 19), onAction = {}) }
        rule.onRoot().captureRoboImage("build/screenshots/phone_portrait.png")
    }

    /** 매치포인트(24:22)와 듀스(24:24)에서 불타는 모습 */
    @Test
    fun onFire() {
        rule.mainClock.autoAdvance = false
        val state = mutableStateOf(ScoreState(me = 24, opponent = 22))
        rule.setContent { PhoneScoreboard(state = state.value, onAction = {}) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage("build/screenshots/phone_match_point.png")

        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 24, opponent = 24) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage("build/screenshots/phone_deuce.png")
    }

    /** 26:24로 경기가 끝났을 때: 왕관이 내려앉고 꽃가루가 쏟아지며 진 쪽은 흐려진다. */
    @Test
    fun won() {
        rule.mainClock.autoAdvance = false
        val state = mutableStateOf(ScoreState(me = 25, opponent = 24))
        rule.setContent { PhoneScoreboard(state = state.value, onAction = {}) }
        rule.mainClock.advanceTimeBy(1_000)
        Snapshot.withMutableSnapshot { state.value = ScoreState(me = 26, opponent = 24) }
        rule.mainClock.advanceTimeBy(1_400)
        rule.onRoot().captureRoboImage("build/screenshots/phone_won.png")
        rule.mainClock.advanceTimeBy(4_000)
        rule.onRoot().captureRoboImage("build/screenshots/phone_won_settled.png")
    }
}
