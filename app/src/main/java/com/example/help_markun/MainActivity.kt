package com.example.help_markun

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.help_markun.hardware.BleScanner
import com.example.help_markun.hardware.NfcCardReader
import com.example.help_markun.service.AppVisibility
import com.example.help_markun.service.HelpScanService
import com.example.help_markun.ui.AppTab
import com.example.help_markun.ui.HelpApp
import com.example.help_markun.ui.HelpViewModel
import com.example.help_markun.ui.theme.Help_markunTheme
import com.example.help_markun.ui.theme.LocalReduceMotion
import com.example.help_markun.ui.theme.LocalSettings
import com.example.help_markun.ui.theme.SettingsController
import com.example.help_markun.ui.theme.ThemeMode
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density

/** 振動しないダミー（操作時の振動をオフにしたとき用） */
private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

class MainActivity : ComponentActivity() {

    private val vm: HelpViewModel by viewModels()
    private lateinit var nfc: NfcCardReader

    // 通知の許可を求める（結果にかかわらず見守りは続け、許可されれば通知も出る）
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { HelpScanService.startIfAllowed(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfc = NfcCardReader(this, vm::onCard)
        handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            // 「バックグラウンドで見守る」の切り替えに合わせて常駐サービスを開始・停止
            LaunchedEffect(settings.backgroundWatch) {
                if (settings.backgroundWatch) HelpScanService.startIfAllowed(this@MainActivity)
                else HelpScanService.stop(this@MainActivity)
            }
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // アプリのテーマに合わせて、ステータスバー・ナビゲーションバーのアイコン色も切り替える
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            // 文字の大きさ：端末の設定にアプリ内の倍率を掛け合わせる
            val density = LocalDensity.current
            val scaledDensity = Density(density.density, density.fontScale * settings.textSize.scale)
            // 操作時の振動をオフにしたら、アプリ内のすべての手ごたえ振動を止める
            val haptic = LocalHapticFeedback.current
            val appHaptic = if (settings.touchFeedback) haptic else NoHaptics

            CompositionLocalProvider(
                LocalSettings provides SettingsController(settings, vm::updateSettings),
                LocalReduceMotion provides settings.reduceMotion,
                LocalDensity provides scaledDensity,
                LocalHapticFeedback provides appHaptic,
            ) {
                Help_markunTheme(darkTheme = dark) {
                    HelpApp(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** 通知から開いたときは、まずその方の詳細を出し、すぐに周囲を探し始める */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(HelpScanService.EXTRA_OPEN_NEARBY, false) != true) return
        vm.selectTab(AppTab.Nearby)
        intent.getStringExtra(HelpScanService.EXTRA_PROFILE_ID)
            ?.let { HelpScanService.present.value[it] }
            ?.let { vm.focusOn(it) }
        // 同じ通知で二度開かないよう、処理済みの印を消す
        intent.removeExtra(HelpScanService.EXTRA_OPEN_NEARBY)
        val granted = BleScanner.requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted && vm.isBluetoothEnabled()) vm.startScan()
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.visible = true
        // アプリを開いたら「見つけました」通知とアイコンのバッジを片付ける。
        // 通知を見ずにアイコンから開いた場合も、見つかっていた方の詳細を最初に出す
        val unseen = HelpScanService.clearAlerts(this)
        if (unseen.isNotEmpty() && vm.focus.value == null) vm.focusOn(unseen.first())
        HelpScanService.startIfAllowed(this)
        askNotificationPermissionOnce()
        vm.onAppVisible()
    }

    /**
     * BLE の許可が済んでいて見守りがオンなのに通知が許可されていない場合、1 回だけ許可を求める。
     * （見つけた時の通知とアイコンのバッジに必要）
     */
    private fun askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return
        if (!HelpScanService.canRun(this)) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit().putBoolean("asked_notifications", true).apply()
        notificationPermission.launch(permission)
    }

    override fun onResume() {
        super.onResume()
        // 設定画面で NFC をオンにして戻ってきた場合も反映する
        vm.onNfcStatus(nfc.status)
        nfc.start()
    }

    override fun onPause() {
        nfc.stop()
        super.onPause()
    }

    override fun onStop() {
        AppVisibility.visible = false
        vm.onAppHidden()
        super.onStop()
    }
}
