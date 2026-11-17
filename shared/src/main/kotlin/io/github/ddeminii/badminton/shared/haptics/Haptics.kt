package io.github.ddeminii.badminton.shared.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.ddeminii.badminton.shared.model.ScoreState

/** 화면을 보지 않고도 무엇이 눌렸는지 알 수 있게 하는 진동 패턴. */
enum class HapticPattern(internal val timings: LongArray) {
    /** 내 점수 +1: 짧게 1번 */
    MY_POINT(longArrayOf(0, SHORT_MS)),

    /** 상대 점수 +1: 짧게 2번 */
    OPPONENT_POINT(longArrayOf(0, SHORT_MS, SHORT_GAP_MS, SHORT_MS)),

    /** −1: 길게 1번 */
    MINUS(longArrayOf(0, LONG_MS)),

    /** 새 게임: 길게 2번 */
    NEW_GAME(longArrayOf(0, LONG_MS, LONG_GAP_MS, LONG_MS));

    companion object {
        /**
         * 다른 기기에서 넘어온 변경을 진동으로 알릴 때 쓸 패턴.
         * 한 번의 조작으로 설명되지 않는 변화(오래 끊겼다 다시 연결된 경우 등)는 null.
         */
        fun forChange(old: ScoreState, new: ScoreState): HapticPattern? {
            val me = new.me - old.me
            val opponent = new.opponent - old.opponent
            return when {
                me == 0 && opponent == 0 -> null
                new.me == 0 && new.opponent == 0 && old.me + old.opponent > 1 -> NEW_GAME
                me == 1 && opponent == 0 -> MY_POINT
                me == 0 && opponent == 1 -> OPPONENT_POINT
                me == -1 && opponent == 0 || me == 0 && opponent == -1 -> MINUS
                else -> null
            }
        }
    }
}

private const val SHORT_MS = 70L
private const val SHORT_GAP_MS = 120L
private const val LONG_MS = 350L
private const val LONG_GAP_MS = 180L

class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(pattern: HapticPattern) {
        val vibrator = vibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
    }
}
