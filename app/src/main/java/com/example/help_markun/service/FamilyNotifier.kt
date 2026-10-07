package com.example.help_markun.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.help_markun.MainActivity
import com.example.help_markun.R
import com.example.help_markun.data.AccountStore
import com.example.help_markun.data.FamilyEvent
import com.example.help_markun.data.FamilyEventKind
import com.example.help_markun.data.FamilyRepository
import com.example.help_markun.data.HelpProfile

/**
 * 家族へのお知らせ。
 * - 送る側：支援が必要な方を見つけた・カードを読んだときに、その方の家族へ自動で知らせる（ログイン不要）
 * - 受ける側：家族グループに入っていれば、新しいお知らせを通知で受け取る
 */
object FamilyNotifier {

    const val EXTRA_OPEN_FAMILY = "open_family"
    private const val CHANNEL_FAMILY = "help_family"
    private const val FAMILY_TAG = "help_family"

    /** 設定：見つけたことをその方の家族へ自動で知らせる */
    const val KEY_AUTO_REPORT = "family_auto_report"

    private val repository = FamilyRepository()

    /** 見つけた・読み取った方に家族がいれば知らせる（設定がオフなら何もしない） */
    suspend fun reportFound(context: Context, profile: HelpProfile, kind: FamilyEventKind) {
        if (profile.familyId == null) return
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_REPORT, true)) return
        val session = AccountStore.get(context).validSession()
        runCatching { repository.report(profile.id, kind, senderName = session?.name, token = session?.accessToken) }
    }

    /**
     * 新しいお知らせを取りに行き、未通知のものを通知する。取得した一覧（新しい順）を返す。
     * 初回は過去の分をまとめて鳴らさないよう、通知済みの印だけを付ける。
     */
    suspend fun poll(context: Context, notify: Boolean): List<FamilyEvent>? {
        val store = AccountStore.get(context)
        val session = store.validSession() ?: return null
        if (!store.inFamily) return null
        val events = runCatching { repository.fetchEvents(session.accessToken) }.getOrNull() ?: return null
        val newest = events.maxOfOrNull { it.id } ?: return events
        val last = store.lastNotifiedEventId
        if (last < 0) {
            store.lastNotifiedEventId = newest
            return events
        }
        val fresh = events.filter { it.id > last && it.reporter != session.userId }
        store.lastNotifiedEventId = maxOf(last, newest)
        if (notify) fresh.sortedBy { it.id }.forEach { show(context, it) }
        return events
    }

    private fun show(context: Context, event: FamilyEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context, 2,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN_FAMILY, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FAMILY)
            .setSmallIcon(R.drawable.ic_stat_help)
            .setColor(ContextCompat.getColor(context, R.color.help_red))
            .setContentTitle(event.title)
            .setContentText(event.body ?: "近くの人が手助けしようとしています")
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.body ?: "近くの人が手助けしようとしています"))
            .setWhen(event.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis())
            .setPriority(
                if (event.kind == FamilyEventKind.Message) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(FAMILY_TAG, (event.id and 0x7FFFFFFF).toInt(), notification)
    }

    /** アプリで家族のお知らせを開いたら、通知を片付ける */
    fun clearFamilyNotifications(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.activeNotifications?.filter { it.tag == FAMILY_TAG }?.forEach { nm.cancel(it.tag, it.id) }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_FAMILY, "ご家族へのお知らせ", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "見守っている方が見つかった・カードが読み取られた・連絡が届いたときにお知らせします"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 120, 80, 120)
                setShowBadge(true)
            }
        )
    }
}
