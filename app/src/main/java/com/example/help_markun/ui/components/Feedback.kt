package com.example.help_markun.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/** 画面のどこからでも操作結果を知らせるためのスナックバー */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }
