package io.github.ddeminii.badminton.phone

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ddeminii.badminton.shared.ScoreboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreboardViewModel by viewModels()
    private val flatHold by lazy { FlatOrientationHold(this) }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 카메라 구멍 영역까지 포함해 화면 전체에 그린다.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 경기 중 화면이 꺼지지 않게 한다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showOverLockScreen()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PhoneScoreboard(state = state, onAction = viewModel::onAction)
        }
        hideSystemBars()


        if (savedInstanceState == null) askForWatchLaunchPermissions()

        // ── 응원 소리 (지금은 꺼 둠) ──────────────────────────────────────────────
        // 나중에 녹음된 응원 목소리("태민 화이팅!")로 켜려면:
        //  1. 녹음 파일을 phone/src/main/res/raw/cheer.mp3 로 넣는다 (m4a, ogg, wav 도 됨. 파일 이름은 소문자·숫자·_ 만).
        //  2. 아래 주석을 푼다. 그러면 4~7점마다 한 번씩 (shared 의 CheerSchedule) 녹음이 재생된다.
        //     소리 크기는 폰의 미디어 음량을 따른다.
        //  3. onDestroy() 에서 cheerSound?.release() 를 부른다.
        //
        // cheerSound = MediaPlayer.create(this, R.raw.cheer)
        // lifecycleScope.launch {
        //     repeatOnLifecycle(Lifecycle.State.STARTED) {
        //         viewModel.cheers.collect {
        //             cheerSound?.seekTo(0)
        //             cheerSound?.start()
        //         }
        //     }
        // }
        //
        // (필요한 것: 클래스에 `private var cheerSound: MediaPlayer? = null`,
        //  import android.media.MediaPlayer, androidx.lifecycle.Lifecycle,
        //  androidx.lifecycle.lifecycleScope, androidx.lifecycle.repeatOnLifecycle, kotlinx.coroutines.launch)
        // ─────────────────────────────────────────────────────────────────────────
    }

    /**
     * 워치에서 점수판을 켜면 폰 점수판도 자동으로 켜지려면 두 가지 허용이 필요하다.
     * - 알림: 바로 띄울 수 없을 때 "점수판 열기" 알림을 보여 주기 위해 (시스템 창으로 한 번 물어봄)
     * - 다른 앱 위에 표시: 앱이 꺼져 있어도 점수판을 바로 띄우기 위해 (설정 화면에서 직접 켜야 함)
     * 두 번 이상 귀찮게 하지 않도록 다른 앱 위에 표시 안내는 한 번만 보여 준다.
     */
    private fun askForWatchLaunchPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences("launch", MODE_PRIVATE)
        if (OpenFromWatchService.canOpenDirectly(this) || prefs.getBoolean(KEY_ASKED_OVERLAY, false)) return
        prefs.edit { putBoolean(KEY_ASKED_OVERLAY, true) }

        AlertDialog.Builder(this)
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
                )
            }
            .setNegativeButton(R.string.permission_later, null)
            .show()
    }

    /**
     * 워치에서 점수판을 켰을 때 폰이 잠겨 있거나 화면이 꺼져 있어도 바로 보이게 한다.
     * 잠금은 풀리지 않으며, 점수판 밖으로 나가려면 평소처럼 잠금을 풀어야 한다.
     */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        OpenFromWatchService.clearOpenNotification(this)
        flatHold.start()
    }

    override fun onPause() {
        flatHold.stop()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /**
     * 상단바(상태 표시줄)와 하단 내비게이션 바를 숨겨 화면 전체를 쓴다.
     * 가장자리를 쓸면 잠깐 나타났다가 다시 사라지고, 알림창을 내렸다 돌아와도 다시 숨긴다.
     */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        const val KEY_ASKED_OVERLAY = "asked_overlay"
    }
}
