package io.github.ddeminii.badminton.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddeminii.badminton.shared.ScoreboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 경기 중 화면이 꺼지거나 절전(ambient) 화면으로 넘어가지 않게 한다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            WearScoreboard(state = state, onAction = viewModel::onAction)
        }
    }
}
