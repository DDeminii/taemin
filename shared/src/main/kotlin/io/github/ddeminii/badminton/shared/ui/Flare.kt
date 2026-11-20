package io.github.ddeminii.badminton.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 불의 세기(0~1)와, 불길이 일렁이는 데 쓰는 흐르는 시간(초) */
class Flare(val heat: State<Float>, val time: State<Float>)

/**
 * 매치포인트·듀스의 불이 타오르는 모양을 정한다. 경기에 집중할 수 있도록 계속 타지 않는다:
 * 불이 붙으면 [FLARE_IN_MS] 동안 확 타오르고, [FLARE_HOLD_MS] 동안 타다가, [FLARE_OUT_MS]에 걸쳐 서서히 꺼진다.
 * [ignite]가 바뀔 때마다(새 점수) 여전히 불이 붙을 상황이면 다시 잠깐 타오른다.
 * 불이 꺼져 있는 동안에는 시간도 멈춰 화면을 다시 그리지 않는다.
 */
@Composable
fun rememberFlare(burning: Boolean, ignite: Any?): Flare {
    val heat = remember { Animatable(0f) }
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(burning, ignite) {
        if (!burning && heat.value <= 0f) return@LaunchedEffect
        coroutineScope {
            val clock = launch {
                val start = withFrameNanos { it } - (time.floatValue * 1e9f).toLong()
                while (true) withFrameNanos { time.floatValue = (it - start) / 1e9f }
            }
            if (burning) {
                heat.animateTo(1f, tween(FLARE_IN_MS, easing = FastOutSlowInEasing))
                delay(FLARE_HOLD_MS)
                heat.animateTo(0f, tween(FLARE_OUT_MS, easing = LinearOutSlowInEasing))
            } else {
                heat.animateTo(0f, tween(FLARE_QUENCH_MS))
            }
            clock.cancel()
        }
    }
    return remember { Flare(heat.asState(), time) }
}

/** 불이 확 붙는 시간, 다 타오른 채 유지되는 시간, 서서히 꺼지는 시간, 상황이 끝나 바로 끄는 시간 */
private const val FLARE_IN_MS = 900
private const val FLARE_HOLD_MS = 2_500L
private const val FLARE_OUT_MS = 3_500
private const val FLARE_QUENCH_MS = 500
