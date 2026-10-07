package com.example.help_markun.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.help_markun.data.HelpProfile

/** 画面のどこからでも呼べる「手助け」のための操作 */
class HelpActions(
    /** その方のご家族へ連絡するシートを開く */
    val contactFamily: (HelpProfile) -> Unit = {},
)

val LocalHelpActions = staticCompositionLocalOf { HelpActions() }
