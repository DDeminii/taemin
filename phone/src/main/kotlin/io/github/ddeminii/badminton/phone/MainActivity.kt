package io.github.ddeminii.badminton.phone

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddeminii.badminton.shared.ScoreboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 카메라 구멍 영역까지 포함해 화면 전체에 그린다.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 경기 중 화면이 꺼지지 않게 한다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PhoneScoreboard(state = state, onAction = viewModel::onAction)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** 전광판처럼 화면 전체를 쓴다. 가장자리를 쓸면 시스템 바가 잠깐 나타난다. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
