package io.github.ddeminii.badminton.phone

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import io.github.ddeminii.badminton.shared.sync.PhoneLaunch

/**
 * 워치에서 점수판을 켰다는 신호를 받아 폰 점수판을 띄운다. 앱이 꺼져 있어도 시스템이 이 서비스를 깨운다.
 *
 * 안드로이드는 백그라운드에서 화면을 마음대로 띄우는 것을 막기 때문에:
 * - "다른 앱 위에 표시" 권한이 있으면 점수판을 바로 띄우고 (폰이 잠겨 있으면 잠금화면 위에),
 * - 없거나 막히면 "점수판 열기" 알림을 띄운다. 알림을 누르면 열린다.
 */
class OpenFromWatchService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PhoneLaunch.OPEN_PATH) return
        if (canOpenDirectly(this)) {
            try {
                startActivity(scoreboardIntent(this))
                return
            } catch (e: Exception) {
                Log.d(TAG, "Direct launch blocked, showing a notification instead: ${e.message}")
            }
        }
        showOpenNotification(this)
    }

    companion object {
        private const val TAG = "OpenFromWatch"
        private const val CHANNEL_ID = "open_from_watch"
        private const val NOTIFICATION_ID = 1

        fun canOpenDirectly(context: Context): Boolean = Settings.canDrawOverlays(context)

        private fun scoreboardIntent(context: Context) = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

        private fun showOpenNotification(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Notifications not allowed; cannot open the scoreboard")
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.open_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
            val open = PendingIntent.getActivity(
                context,
                0,
                scoreboardIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.open_notification_title))
                .setContentText(context.getString(R.string.open_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                // 폰 화면이 꺼져 있거나 잠겨 있으면, 허용된 경우 알림 대신 점수판이 바로 뜬다.
                .setFullScreenIntent(open, true)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        /** 점수판이 열리면 남아 있는 "점수판 열기" 알림을 지운다. */
        fun clearOpenNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
