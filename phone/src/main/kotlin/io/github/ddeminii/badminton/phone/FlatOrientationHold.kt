package io.github.ddeminii.badminton.phone

import android.app.Activity
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * 폰을 바닥에 눕히면 그 순간의 화면 방향을 그대로 고정하고, 다시 세우면 자동 회전으로 돌아간다.
 *
 * 평평하게 누운 폰은 기울기 센서로 방향을 알기 어려워 화면이 세로↔가로로 멋대로 바뀔 수 있다.
 * 네트 아래 바닥에 둔 폰이 경기 중에 돌아가지 않게 하기 위한 것이다.
 */
class FlatOrientationHold(private val activity: Activity) : SensorEventListener {

    private val sensors = activity.getSystemService(SensorManager::class.java)
    private val gravity = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var held = false
    private var flatSince = 0L

    fun start() {
        gravity?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensors?.unregisterListener(this)
        release()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val tilt = tiltDegrees(event.values[0], event.values[1], event.values[2])
        val now = SystemClock.elapsedRealtime()
        when {
            tilt < FLAT_DEGREES -> {
                if (flatSince == 0L) flatSince = now
                // 내려놓는 동안 잠깐 평평해지는 것은 무시하고, 잠시 누워 있으면 고정한다.
                if (!held && now - flatSince >= SETTLE_MS) hold()
            }
            tilt > UPRIGHT_DEGREES -> {
                flatSince = 0L
                release()
            }
            else -> flatSince = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun hold() {
        held = true
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun release() {
        if (!held) return
        held = false
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    companion object {
        /** 이보다 덜 기울면 "눕혔다"고 본다 */
        private const val FLAT_DEGREES = 20f

        /** 이보다 더 기울면 "다시 세웠다"고 보고 자동 회전으로 돌아간다 (사이 구간은 그대로 유지) */
        private const val UPRIGHT_DEGREES = 40f

        /** 이 시간 동안 누워 있어야 고정한다 */
        private const val SETTLE_MS = 400L

        /** 화면이 수평면에서 기운 각도. 0 = 바닥에 평평하게 누움, 90 = 똑바로 세움. */
        fun tiltDegrees(x: Float, y: Float, z: Float): Float {
            val g = sqrt(x * x + y * y + z * z)
            if (g == 0f) return 90f
            return Math.toDegrees(acos((kotlin.math.abs(z) / g).toDouble())).toFloat()
        }
    }
}
