@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.help_markun.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.SignalCellularAlt1Bar
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.example.help_markun.BuildConfig
import com.example.help_markun.ui.theme.LocalSettings
import com.example.help_markun.ui.theme.TextSize
import com.example.help_markun.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 設定画面に出すデータベースの状態 */
class DataStatus(
    val configured: Boolean,
    val registeredCount: Int,
    val syncing: Boolean,
    val lastSyncedAt: Long?,
    val error: String?,
    val onSync: () -> Unit,
)

val LocalDataStatus = staticCompositionLocalOf { DataStatus(false, 0, false, null, null) {} }

/** ヘッダー右上の設定ボタン。押すと設定シートが開く */
@Composable
fun SettingsButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    // 開いている間は歯車が少し回って「開いた」ことを示す
    val rotation by animateFloatAsState(if (open) 60f else 0f, Springs.bouncy(), label = "gear")

    FilledTonalIconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            open = true
        },
        interactionSource = source,
        modifier = modifier
            .size(56.dp)
            .pressScale(source, 0.85f)
            .semantics { contentDescription = "設定" },
    ) {
        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.graphicsLayer { rotationZ = rotation })
    }

    if (open) SettingsSheet(onDismiss = { open = false })
}

@Composable
private fun SettingsSheet(onDismiss: () -> Unit) {
    val controller = LocalSettings.current
    val s = controller.settings
    val data = LocalDataStatus.current
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "設定",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { heading() },
            )

            // ---- 表示 ----
            SettingsGroup("表示", Modifier.appear(0)) {
                PickerRow(
                    icon = Icons.Rounded.Palette,
                    polygon = MaterialShapes.Cookie4Sided,
                    title = "テーマ",
                    subtitle = "「自動」は端末のダークモード設定に合わせます",
                    options = ThemeMode.entries.map { it.label },
                    icons = listOf(Icons.Rounded.BrightnessAuto, Icons.Rounded.LightMode, Icons.Rounded.DarkMode),
                    selected = s.themeMode.ordinal,
                    onSelect = { controller.update(s.copy(themeMode = ThemeMode.entries[it])) },
                )
                PickerRow(
                    icon = Icons.Rounded.FormatSize,
                    polygon = MaterialShapes.Clover4Leaf,
                    title = "文字の大きさ",
                    subtitle = "端末の文字サイズに上乗せされます",
                    options = TextSize.entries.map { it.label },
                    selected = s.textSize.ordinal,
                    onSelect = { controller.update(s.copy(textSize = TextSize.entries[it])) },
                )
                SwitchRow(
                    icon = Icons.Rounded.Animation,
                    polygon = MaterialShapes.Sunny,
                    title = "動きを控えめにする",
                    subtitle = "回転や波紋などの装飾アニメーションを止めます",
                    checked = s.reduceMotion,
                    onCheckedChange = { controller.update(s.copy(reduceMotion = it)) },
                )
            }

            // ---- お知らせ ----
            SettingsGroup("お知らせ", Modifier.appear(1)) {
                SwitchRow(
                    icon = Icons.Rounded.Radar,
                    polygon = MaterialShapes.Cookie12Sided,
                    title = "バックグラウンドで見守る",
                    subtitle = "アプリを閉じていても近くのヘルプタグを探し、見つけたら通知とアイコンのバッジでお知らせします",
                    checked = s.backgroundWatch,
                    onCheckedChange = { controller.update(s.copy(backgroundWatch = it)) },
                )
                SwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    polygon = MaterialShapes.Gem,
                    title = "支援が必要な方を振動で知らせる",
                    subtitle = "音が聞こえにくい場合も気づけるよう、強めに振動します",
                    checked = s.alertVibration,
                    onCheckedChange = { controller.update(s.copy(alertVibration = it)) },
                )
                SwitchRow(
                    icon = Icons.Rounded.TouchApp,
                    polygon = MaterialShapes.Cookie6Sided,
                    title = "操作時の振動",
                    subtitle = "タップやスワイプの手ごたえを振動で返します",
                    checked = s.touchFeedback,
                    onCheckedChange = { controller.update(s.copy(touchFeedback = it)) },
                )
            }

            // ---- 周辺のデバイス ----
            SettingsGroup("周辺のデバイス", Modifier.appear(2)) {
                val fadeOptions = listOf(10, 20, 30)
                PickerRow(
                    icon = Icons.Rounded.Timer,
                    polygon = MaterialShapes.Pentagon,
                    title = "電波が途切れてから消えるまで",
                    subtitle = "受信がなくなると少しずつ薄くなり、この時間で一覧から消えます",
                    options = fadeOptions.map { "${it}秒" },
                    selected = fadeOptions.indexOf(s.fadeOutSeconds).coerceAtLeast(0),
                    onSelect = { controller.update(s.copy(fadeOutSeconds = fadeOptions[it])) },
                )
                SwitchRow(
                    icon = Icons.Rounded.SignalCellularAlt1Bar,
                    polygon = MaterialShapes.SoftBurst,
                    title = "遠いデバイスを表示しない",
                    subtitle = "電波の弱いデバイスを一覧から隠します（支援が必要な方は常に表示）",
                    checked = s.hideFarDevices,
                    onCheckedChange = { controller.update(s.copy(hideFarDevices = it)) },
                )
            }

            // ---- データ ----
            SettingsGroup("データ", Modifier.appear(3)) {
                DataRow(data)
            }

            // ---- アプリについて ----
            SettingsGroup("アプリについて", Modifier.appear(4)) {
                InfoRow(
                    icon = Icons.Rounded.Info,
                    polygon = MaterialShapes.Pill,
                    title = "バージョン",
                    value = "${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 部品
// ---------------------------------------------------------------------------

@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 6.dp)
                .semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            content()
        }
    }
}

@Composable
private fun RowLead(icon: ImageVector, polygon: RoundedPolygon) {
    ShapeBadge(
        icon,
        polygon,
        container = MaterialTheme.colorScheme.primaryContainer,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        size = 42.dp,
    )
    Spacer(Modifier.width(14.dp))
}

@Composable
private fun RowTexts(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 行のどこを押してもスイッチが切り替わる（小さなスイッチを狙わなくてよい） */
@Composable
private fun SwitchRow(
    icon: ImageVector,
    polygon: RoundedPolygon,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    onCheckedChange(it)
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLead(icon, polygon)
        RowTexts(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        // 行全体が toggleable なので、スイッチ自体はタップを受けない
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 選択肢を横並びで選ぶ行。選択中の印がスプリングで滑って移動する */
@Composable
private fun PickerRow(
    icon: ImageVector,
    polygon: RoundedPolygon,
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    subtitle: String? = null,
    icons: List<ImageVector>? = null,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowLead(icon, polygon)
            RowTexts(title, subtitle, Modifier.weight(1f))
        }
        Spacer(Modifier.size(10.dp))
        SegmentedPicker(options, selected, onSelect, icons)
    }
}

@Composable
fun SegmentedPicker(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    icons: List<ImageVector>? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerHighest)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / options.size
        val x by animateDpAsState(itemWidth * selected, Springs.bouncy(), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(itemWidth)
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .background(cs.primary)
        )
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { i, label ->
                val isSel = i == selected
                val fg by animateColorAsState(if (isSel) cs.onPrimary else cs.onSurfaceVariant, label = "segFg")
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .selectable(
                            selected = isSel,
                            role = Role.RadioButton,
                            onClick = {
                                if (!isSel) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(i)
                            },
                        )
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icons?.getOrNull(i)?.let {
                        Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, polygon: RoundedPolygon, title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLead(icon, polygon)
        RowTexts(title, null, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataRow(data: DataStatus) {
    val cs = MaterialTheme.colorScheme
    val ok = data.configured && data.error == null
    val time = data.lastSyncedAt?.let { SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(it)) }
    val status = when {
        !data.configured -> "接続情報が未設定です"
        data.error != null -> data.error
        else -> "登録 ${data.registeredCount} 件" + (time?.let { "・最終同期 $it" } ?: "")
    }
    val source = remember { MutableInteractionSource() }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(
                if (ok) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                MaterialShapes.Cookie9Sided,
                container = if (ok) cs.primaryContainer else cs.tertiaryContainer,
                tint = if (ok) cs.onPrimaryContainer else cs.onTertiaryContainer,
                size = 42.dp,
            )
            Spacer(Modifier.width(14.dp))
            RowTexts(if (ok) "データベースに接続中" else "データベースに接続できません", status, Modifier.weight(1f))
        }
        Spacer(Modifier.size(10.dp))
        FilledTonalButton(
            onClick = data.onSync,
            enabled = data.configured && !data.syncing,
            interactionSource = source,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .pressScale(source),
        ) {
            if (data.syncing) {
                LoadingIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("同期しています…", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Rounded.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("今すぐ同期する", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

