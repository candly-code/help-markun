package com.example.help_markun.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.help_markun.MainActivity
import com.example.help_markun.R
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.data.SupabaseRepository
import com.example.help_markun.data.normalizeKey
import com.example.help_markun.hardware.BleScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** アプリが画面に出ているか（出ている間は通知を出さず、画面側で知らせる） */
object AppVisibility {
    @Volatile
    var visible: Boolean = false
}

/**
 * 近くのヘルプタグをバックグラウンドで探し続ける常駐サービス。
 *
 * - 登録済みタグのアドレスだけを対象にフィルタ付きでスキャンする（画面オフでも止まらず、電池にもやさしい）
 * - 支援が必要な方を見つけたら、優先度の高い通知を出す（ランチャーアイコンにもバッジが付く）
 * - 登録情報は一定時間ごとに取り直す
 */
class HelpScanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = SupabaseRepository()
    private lateinit var scanner: BleScanner
    private var loop: Job? = null

    /** 最後に受信した時刻（プロフィール ID → 時刻）。近くにいる方の一覧はアプリ側からも見えるよう companion に置く */
    private val lastSeen = mutableMapOf<String, Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scanner = BleScanner(this)
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // 通知の「止める」は今だけ止める。次にアプリを開けば自動で再開する（ずっと止めるのは設定から）
            stopSelf()
            return START_NOT_STICKY
        }
        if (!canRun(this) || !enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop?.isActive != true) loop = scope.launch { watch() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        _present.value = emptyMap()
        NotificationManagerCompat.from(this).cancel(ONGOING_ID)
        super.onDestroy()
    }

    /** 常駐通知を出して前面サービスになる。位置情報タイプが使えない状況（起動直後など）では外して再試行 */
    private fun enterForeground(): Boolean {
        val notification = ongoingNotification(0)
        val withLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else 0
        val deviceOnly = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else 0
        return runCatching { ServiceCompat.startForeground(this, ONGOING_ID, notification, withLocation) }
            .recoverCatching { ServiceCompat.startForeground(this, ONGOING_ID, notification, deviceOnly) }
            .isSuccess
    }

    // ---------------------------------------------------------------------------
    // 見守りループ
    // ---------------------------------------------------------------------------

    private suspend fun watch() {
        while (scope.isActive) {
            val profiles = runCatching { repository.fetchBleProfiles() }.getOrNull().orEmpty()
            // 画面オフでも動くよう、アドレス形式で登録されているタグだけを対象にする
            val targets = profiles.filterKeys { MAC.matches(it) }
            if (targets.isEmpty() || !scanner.isEnabled) {
                delay(RETRY_MS)
                continue
            }
            val filters = targets.keys.map { ScanFilter.Builder().setDeviceAddress(it).build() }

            // 一定時間スキャンしたら登録情報を取り直す
            withTimeoutOrNull(REFRESH_MS) {
                val pruner = launch {
                    while (isActive) {
                        delay(PRUNE_MS)
                        prune()
                    }
                }
                scanner.scan(filters, ScanSettings.SCAN_MODE_BALANCED)
                    .catch { delay(RETRY_MS) }
                    .collect { s -> targets[normalizeKey(s.address)]?.let { onSeen(it) } }
                pruner.cancel()
            }
        }
    }

    private fun onSeen(profile: HelpProfile) {
        val isNew = profile.id !in _present.value
        if (isNew) _present.value = _present.value + (profile.id to profile)
        lastSeen[profile.id] = System.currentTimeMillis()
        if (isNew) {
            notifyMatch(profile)
            updateOngoing()
        }
    }

    /** しばらく受信のない方は「離れた」とみなし、通知も取り下げる（次に近づいたらまた知らせる） */
    private fun prune() {
        val now = System.currentTimeMillis()
        val gone = lastSeen.filterValues { now - it > LOST_MS }.keys
        if (gone.isEmpty()) return
        _present.value = _present.value - gone
        gone.forEach { id ->
            lastSeen.remove(id)
            synchronized(alerted) { alerted.remove(id) }
            NotificationManagerCompat.from(this).cancel(ALERT_TAG, alertId(id))
        }
        updateOngoing()
    }

    // ---------------------------------------------------------------------------
    // 通知
    // ---------------------------------------------------------------------------

    private fun notifyMatch(profile: HelpProfile) {
        // アプリを開いている間は画面と振動で知らせるので、通知は重ねない
        if (AppVisibility.visible || !canNotify(this)) return
        synchronized(alerted) { alerted.add(profile.id) }
        val vibrate = prefs(this).getBoolean(KEY_ALERT_VIBRATION, true)
        val body = profile.helpRequest ?: profile.disability ?: "近くにいらっしゃいます"
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_help)
            .setColor(ContextCompat.getColor(this, R.color.help_red))
            .setContentTitle("支援が必要な方が近くにいます")
            .setContentText("${profile.displayName}さん：$body")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${profile.displayName}さん\n$body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setNumber(_present.value.size)
            .setSilent(!vibrate)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(this, profile.id))
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(this).notify(ALERT_TAG, alertId(profile.id), notification)
    }

    private fun updateOngoing() {
        if (!canNotify(this)) return
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(this).notify(ONGOING_ID, ongoingNotification(_present.value.size))
    }

    private fun ongoingNotification(count: Int): Notification {
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, HelpScanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_WATCH)
            .setSmallIcon(R.drawable.ic_stat_help)
            .setColor(ContextCompat.getColor(this, R.color.help_red))
            .setContentTitle(if (count > 0) "近くに支援が必要な方が $count 人います" else "近くのヘルプを見守っています")
            .setContentText("登録されたヘルプタグが近づくとお知らせします")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent(this))
            .addAction(0, "見守りを止める", stop)
            .build()
    }

    companion object {
        const val EXTRA_OPEN_NEARBY = "open_nearby"
        /** 通知から開いたときに、詳細を最初に見せる方のプロフィール ID */
        const val EXTRA_PROFILE_ID = "profile_id"

        /** いま近くにいる方（プロフィール ID → プロフィール） */
        private val _present = MutableStateFlow<Map<String, HelpProfile>>(emptyMap())
        val present: StateFlow<Map<String, HelpProfile>> = _present.asStateFlow()

        /** 通知を出したが、まだアプリで見ていない方 */
        private val alerted = mutableSetOf<String>()

        private const val ACTION_STOP = "com.example.help_markun.STOP_WATCH"
        private const val CHANNEL_WATCH = "help_watch"
        private const val CHANNEL_ALERT = "help_alert"
        private const val ALERT_TAG = "help_alert"
        private const val ONGOING_ID = 1

        /** 設定（アプリ本体と同じ保存先） */
        const val KEY_ENABLED = "background_watch"
        private const val KEY_ALERT_VIBRATION = "alert_vibration"

        private const val REFRESH_MS = 15 * 60_000L
        private const val RETRY_MS = 60_000L
        private const val PRUNE_MS = 15_000L
        private const val LOST_MS = 2 * 60_000L

        private val MAC = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

        private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        private fun alertId(profileId: String) = 1000 + (profileId.hashCode() and 0xFFFF)

        private fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        private fun canNotify(context: Context) =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(context, Manifest.permission.POST_NOTIFICATIONS)

        /** 見守りを始められるか：設定がオン、BLE の権限あり、DB 接続情報あり */
        fun canRun(context: Context): Boolean =
            prefs(context).getBoolean(KEY_ENABLED, true) &&
                BleScanner.requiredPermissions.all { granted(context, it) } &&
                SupabaseRepository().isConfigured

        /** 条件がそろっていれば見守りを開始する（何度呼んでもよい） */
        fun startIfAllowed(context: Context) {
            if (!canRun(context)) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, HelpScanService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HelpScanService::class.java))
        }

        /**
         * アプリを開いたら「見つけました」通知とバッジを片付ける。
         * 通知を出していた（まだ見ていない）方を返すので、アプリ側で最初に詳細を見せられる。
         */
        fun clearAlerts(context: Context): List<HelpProfile> {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.activeNotifications?.filter { it.tag == ALERT_TAG }?.forEach { nm.cancel(it.tag, it.id) }
            val ids = synchronized(alerted) { alerted.toList().also { alerted.clear() } }
            return ids.mapNotNull { _present.value[it] }
        }

        private fun openAppIntent(context: Context, profileId: String? = null): PendingIntent = PendingIntent.getActivity(
            context, profileId?.let { alertId(it) } ?: 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN_NEARBY, true)
                .putExtra(EXTRA_PROFILE_ID, profileId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_WATCH, "見守り中", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "バックグラウンドで近くのヘルプタグを探している間に表示されます"
                    // 常駐通知ではアイコンにバッジを付けない（バッジは「見つけた」時だけ）
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERT, "支援が必要な方のお知らせ", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "登録された方のヘルプタグが近くで見つかったときにお知らせします"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 180, 120, 180, 120, 360)
                    setShowBadge(true)
                }
            )
        }
    }
}
