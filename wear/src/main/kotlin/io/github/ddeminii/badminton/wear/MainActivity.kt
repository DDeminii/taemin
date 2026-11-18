package io.github.ddeminii.badminton.wear

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddeminii.badminton.shared.ScoreboardViewModel
import io.github.ddeminii.badminton.shared.sync.PhoneLaunch

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

    override fun onStart() {
        super.onStart()
        openScoreboardOnPhone()
    }

    /**
     * 워치에서 점수판이 화면에 나타날 때마다 연결된 폰의 점수판 앱도 함께 띄운다.
     * 폰이 연결돼 있지 않거나 폰에 앱이 없으면 아무 일도 일어나지 않고 워치 단독으로 동작한다.
     */
    private fun openScoreboardOnPhone() {
        PhoneLaunch.requestOpen(this)
    }
}
