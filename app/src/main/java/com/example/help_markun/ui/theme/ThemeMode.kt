package com.example.help_markun.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** 表示テーマの選び方 */
enum class ThemeMode(val label: String) {
    System("自動"),
    Light("ライト"),
    Dark("ダーク"),
}

/** 文字の大きさ（端末の設定にさらに掛け合わせる） */
enum class TextSize(val label: String, val scale: Float) {
    Normal("標準", 1f),
    Large("大きめ", 1.15f),
    Huge("特大", 1.3f),
}

/** アプリの設定（端末内に保存） */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val textSize: TextSize = TextSize.Normal,
    /** 回転・波紋などの常に動く装飾を止める */
    val reduceMotion: Boolean = false,
    /** 支援が必要な方を見つけたときに振動で知らせる */
    val alertVibration: Boolean = true,
    /** タップやスワイプの手ごたえとして振動する */
    val touchFeedback: Boolean = true,
    /** 電波が途切れてから一覧から消えるまでの秒数 */
    val fadeOutSeconds: Int = 20,
    /** 電波が弱い（遠い）デバイスを一覧に出さない（一致した方は常に表示） */
    val hideFarDevices: Boolean = false,
    /** アプリを閉じていても近くのヘルプタグを探し、見つけたら通知する */
    val backgroundWatch: Boolean = true,
)

/** どの画面からでも設定を読み書きできるようにする */
class SettingsController(val settings: AppSettings, val update: (AppSettings) -> Unit)

val LocalSettings = staticCompositionLocalOf { SettingsController(AppSettings()) {} }

/** 装飾アニメーションを控えめにするか（LocalSettings から取り出した値を配る） */
val LocalReduceMotion = staticCompositionLocalOf { false }
