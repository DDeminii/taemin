package io.github.ddeminii.badminton.shared

import android.app.Application
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Decrement
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Increment
import io.github.ddeminii.badminton.shared.gesture.GestureAction.Reset
import io.github.ddeminii.badminton.shared.model.Side.ME
import io.github.ddeminii.badminton.shared.model.Side.OPPONENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 워치가 없는(= Data Layer를 쓸 수 없는) 환경에서도 단독으로 동작하는지 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ScoreboardViewModelTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val vibrator = shadowOf(app.getSystemService(VibratorManager::class.java).defaultVibrator)

    @Test
    fun `counts, clamps at zero and resets without a connected device`() {
        val viewModel = ScoreboardViewModel(app)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        viewModel.onAction(Increment(ME))
        viewModel.onAction(Increment(OPPONENT))
        viewModel.onAction(Increment(OPPONENT))
        viewModel.onAction(Decrement(OPPONENT))
        assertEquals(1 to 1, viewModel.state.value.let { it.me to it.opponent })

        viewModel.onAction(Decrement(ME))
        viewModel.onAction(Decrement(ME))
        assertEquals(0 to 1, viewModel.state.value.let { it.me to it.opponent })

        viewModel.onAction(Reset)
        assertEquals(0 to 0, viewModel.state.value.let { it.me to it.opponent })
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun `score survives the app being closed`() {
        ScoreboardViewModel(app).apply {
            onAction(Increment(ME))
            onAction(Increment(ME))
            onAction(Increment(OPPONENT))
        }
        val reopened = ScoreboardViewModel(app)
        assertEquals(2 to 1, reopened.state.value.let { it.me to it.opponent })
    }

    @Test
    fun `vibrates for a change but not for minus at zero`() {
        val viewModel = ScoreboardViewModel(app)

        viewModel.onAction(Decrement(ME))
        assertFalse(vibrator.isVibrating)

        viewModel.onAction(Increment(OPPONENT))
        assertTrue(vibrator.isVibrating)
    }
}
