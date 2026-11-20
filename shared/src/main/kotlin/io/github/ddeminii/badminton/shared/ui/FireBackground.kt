package io.github.ddeminii.badminton.shared.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * 매치포인트·듀스일 때 이 영역(화면의 한쪽 절반)을 어둡게 하고 아래에서 불길이 타오르게 한다.
 * 큰 불길은 카드 뒤에서 타오르고, 낮은 불길은 카드 앞에서 반투명하게 카드 아래쪽을 핥고 올라온다.
 *
 * 안드로이드 13 이상(S25 Ultra, Galaxy Watch7)에서는 GPU 셰이더로 실제 불처럼 소용돌이치는 불길을 그리고,
 * 그보다 오래된 기기에서는 단순한 불꽃 모양으로 대신한다.
 * 불타지 않는 동안에는 아무것도 그리지 않고 애니메이션도 멈춰 배터리를 쓰지 않는다.
 *
 * 불은 계속 타지 않고 몇 초 뒤 서서히 꺼진다 ([rememberFlare]). 새 점수([ignite])가 나면 다시 잠깐 타오른다.
 *
 * @param burning 불을 붙일 상황인지 (매치포인트·듀스)
 * @param ignite 바뀔 때마다 불을 다시 붙이는 값 (보통 현재 점수)
 * @param flameHeight 이 영역 높이 대비 뒤쪽 불길이 닿는 높이
 * @param frontHeight 이 영역 높이 대비 카드 앞쪽 불길이 닿는 높이 (0이면 앞쪽 불길 없음)
 */
@Composable
fun Modifier.fireBackground(
    burning: Boolean,
    ignite: Any? = null,
    flameHeight: Float = 1f,
    frontHeight: Float = 0.2f,
): Modifier {
    val flare = rememberFlare(burning, ignite)
    val heat = flare.heat
    val time = flare.time
    val shaders = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) FireShader() to FireShader() else null
    }

    return drawWithContent {
        val h = heat.value
        if (h <= 0.01f) {
            drawContent()
            return@drawWithContent
        }
        val t = time.value
        drawRect(ScoreColors.FireBackdrop, alpha = h)
        if (shaders != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shaders.first.draw(this, t, h, flameHeight, strength = 1f, seed = 0f)
        } else {
            drawSimpleFlames(t, h, flameHeight)
        }

        drawContent()

        if (frontHeight > 0f && shaders != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shaders.second.draw(this, t + 37f, h, frontHeight, strength = FRONT_STRENGTH, seed = 11.3f)
        }
    }
}

/** 카드 앞 불길의 진하기. 숫자가 비쳐 보일 만큼만. */
private const val FRONT_STRENGTH = 0.9f

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class FireShader {
    private val shader = RuntimeShader(FIRE_AGSL)
    private val brush = ShaderBrush(shader)

    fun draw(scope: DrawScope, time: Float, heat: Float, reach: Float, strength: Float, seed: Float) = with(scope) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("heat", heat)
        shader.setFloatUniform("reach", reach)
        shader.setFloatUniform("strength", strength)
        shader.setFloatUniform("seed", seed)
        drawRect(brush)
    }
}

/**
 * 불길 셰이더 (AGSL). 위로 흘러가는 노이즈를 다른 노이즈로 비틀어(domain warping)
 * 실제 불처럼 갈라지고 소용돌이치는 불길을 만든 뒤, 온도에 따라 검붉은색 → 주황 → 노랑으로 칠한다.
 */
private const val FIRE_AGSL = """
uniform float2 size;
uniform float time;
uniform float heat;
uniform float reach;
uniform float strength;
uniform float seed;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + float2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / size;
    float height = 1.0 - uv.y;               // 0 = 바닥, 1 = 맨 위
    float y = height / reach;                 // 0 = 바닥, 1 = 불길이 닿는 높이
    float aspect = size.x / size.y;
    // 세로로 길쭉한 불꽃이 되도록 가로를 더 촘촘하게 본다. 불길 높이와 상관없이 같은 결을 쓴다.
    float2 p = float2(uv.x * aspect * 3.2 + seed, height * 1.6);

    float2 warp = float2(
        fbm(p * 0.8 + float2(0.0, -time * 0.9)),
        fbm(p * 0.8 + float2(5.2, -time * 1.2))
    );
    float n = fbm(p + warp * 2.2 + float2(0.0, -time * 2.0));
    // 갈라진 불꽃 혀 사이로 어두운 틈이 생기게 대비를 높인다.
    n = smoothstep(0.25, 0.8, n);

    float t = n * 1.55 - y * 1.05 + 0.02;
    // 바닥은 언제나 타오르게 한다.
    t += (1.0 - smoothstep(0.0, 0.35, y)) * 0.45;
    t *= smoothstep(0.0, 0.12, uv.x) * smoothstep(1.0, 0.88, uv.x);
    t = clamp(t * (0.55 + 0.45 * heat), 0.0, 1.0);

    float3 c = mix(float3(0.35, 0.02, 0.0), float3(0.85, 0.12, 0.01), smoothstep(0.05, 0.30, t));
    c = mix(c, float3(1.0, 0.40, 0.03), smoothstep(0.25, 0.55, t));
    c = mix(c, float3(1.0, 0.68, 0.12), smoothstep(0.50, 0.78, t));
    c = mix(c, float3(1.0, 0.88, 0.45), smoothstep(0.78, 1.0, t));

    float alpha = smoothstep(0.06, 0.30, t) * heat * strength;
    return half4(half3(c * alpha), half(alpha));
}
"""

/** 셰이더를 쓸 수 없는 오래된 기기용: 일렁이는 불꽃 혀를 겹쳐 그린다. */
private fun DrawScope.drawSimpleFlames(time: Float, heat: Float, flameHeight: Float) {
    val w = size.width
    val h = size.height * flameHeight
    val base = size.height
    for (inner in listOf(false, true)) {
        for (i in 0 until SIMPLE_TONGUES) {
            val seed = i * 1.37f + if (inner) 0.71f else 0f
            val flicker = 0.5f +
                0.3f * sin(time * (2.1f + seed % 1.3f) + seed * 5f) +
                0.2f * sin(time * (5.3f + seed % 0.9f) + seed * 11f)
            val outer = h * (0.55f + 0.35f * flicker)
            val height = (if (inner) outer * 0.65f else outer) * heat
            val halfWidth = w / SIMPLE_TONGUES * if (inner) 0.55f else 0.85f
            val x = w * (0.03f + 0.94f * (i + 0.5f) / SIMPLE_TONGUES)
            val sway = sin(time * (1.7f + seed % 0.7f) + seed * 3f) * halfWidth * 0.6f
            val tip = base - height
            val tongue = Path().apply {
                moveTo(x - halfWidth, base)
                cubicTo(x - halfWidth, base - height * 0.45f, x + sway - halfWidth * 0.15f, base - height * 0.75f, x + sway, tip)
                cubicTo(x + sway + halfWidth * 0.15f, base - height * 0.75f, x + halfWidth, base - height * 0.45f, x + halfWidth, base)
                close()
            }
            drawPath(
                path = tongue,
                brush = Brush.verticalGradient(
                    colors = if (inner) ScoreColors.FlameInner else ScoreColors.FlameOuter,
                    startY = base,
                    endY = tip,
                ),
                alpha = heat,
            )
        }
    }
}

private const val SIMPLE_TONGUES = 9
