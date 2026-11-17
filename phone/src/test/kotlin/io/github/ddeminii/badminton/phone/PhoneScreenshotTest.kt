package io.github.ddeminii.badminton.phone

import androidx.compose.runtime.mutableStateOf
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

        state.value = ScoreState(me = 7, opponent = 11)
        rule.mainClock.advanceTimeBy(2_000)
        rule.onRoot().captureRoboImage("build/screenshots/phone_single_digit.png")

        rule.onRoot().performTouchInput { down(Offset(width * 0.25f, centerY)) }
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureRoboImage("build/screenshots/phone_pressed.png")
    }
}
