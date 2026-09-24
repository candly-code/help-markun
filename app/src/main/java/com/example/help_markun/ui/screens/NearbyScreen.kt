@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.help_markun.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.help_markun.service.HelpScanService
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.hardware.BleScanner
import com.example.help_markun.ui.NearbyDevice
import com.example.help_markun.ui.NearbyState
import com.example.help_markun.ui.components.AppLogo
import com.example.help_markun.ui.components.ExpressiveRadar
import com.example.help_markun.ui.components.HelpMarkLogo
import com.example.help_markun.ui.components.ProfileDetails
import com.example.help_markun.ui.components.ProfileHeader
import com.example.help_markun.ui.components.RadarMode
import com.example.help_markun.ui.components.RollingNumber
import com.example.help_markun.ui.components.ShapeBadge
import com.example.help_markun.ui.components.SignalBars
import com.example.help_markun.ui.components.SpinningShape
import com.example.help_markun.ui.components.SettingsButton
import com.example.help_markun.ui.components.Springs
import com.example.help_markun.ui.components.appear
import com.example.help_markun.ui.components.distanceLabel
import com.example.help_markun.ui.components.pressScale
import com.example.help_markun.ui.components.shapeFor
import com.example.help_markun.ui.components.signalLevel
import com.example.help_markun.ui.theme.LocalReduceMotion
import com.example.help_markun.ui.theme.LocalSettings
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.HelpRedDeep
import com.example.help_markun.ui.theme.HelpRedGlow

/** リストの出入り・並び替えを弾ませる */
private fun Modifier.springItem(scope: LazyItemScope): Modifier = with(scope) {
    animateItem(
        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
        placementSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
        fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
    )
}

@Composable
fun NearbyScreen(
    state: NearbyState,
    bleSupported: Boolean,
    isBluetoothEnabled: () -> Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRefresh: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    focusProfile: HelpProfile? = null,
    onFocusConsumed: () -> Unit = {},
) {
    var localMessage by remember { mutableStateOf<String?>(null) }
    var openProfile by remember { mutableStateOf<HelpProfile?>(null) }
    // 通知から開いた時などは、その方の詳細を真っ先に表示する
    LaunchedEffect(focusProfile) {
        if (focusProfile != null) {
            openProfile = focusProfile
            onFocusConsumed()
        }
    }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val enableBt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isBluetoothEnabled()) {
            localMessage = null
            onStart()
        } else {
            localMessage = "Bluetooth をオンにするとスキャンできます"
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // スキャンに必要なのは BLE・位置情報の権限だけ。通知は断られてもスキャンは続ける
        val bleGranted = BleScanner.requiredPermissions.all {
            result[it] ?: (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED)
        }
        if (bleGranted) {
            localMessage = null
            // 一度許可されれば、以後はアプリを閉じてもバックグラウンドで見守り続ける
            HelpScanService.startIfAllowed(context)
            if (isBluetoothEnabled()) onStart()
            else enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            localMessage = "近くのデバイスを探すには、Bluetooth と位置情報の許可が必要です"
        }
    }

    val onToggle: () -> Unit = {
        haptic.performHapticFeedback(if (state.scanning) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
        when {
            state.scanning -> onStop()
            !bleSupported -> { localMessage = "この端末は Bluetooth LE に対応していません" }
            else -> permissions.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    BleScanner.requiredPermissions + Manifest.permission.POST_NOTIFICATIONS
                } else {
                    BleScanner.requiredPermissions
                }
            )
        }
    }

    val matches = state.matches
    val hideFar = LocalSettings.current.settings.hideFarDevices
    // 「遠いデバイスを表示しない」設定のときは電波が一番弱い段階を隠す（一致した方は常に表示）
    val others = state.devices.filter { !it.isMatch && !(hideFar && signalLevel(it.rssi) <= 1) }
    // 名前のない端末は数が多く並びが荒れるので、1 行にまとめて折りたたむ
    val named = others.filter { it.name != null }
    val unnamed = others.filter { it.name == null }
    var showUnnamed by remember { mutableStateOf(false) }
    val message = localMessage ?: state.error
    // エラーには「次にやること」をボタンで添えて、その場で解決できるようにする
    val bannerAction: Pair<String, () -> Unit>? = when {
        message == null -> null
        message.contains("許可") -> "設定を開く" to {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        }
        message.contains("Bluetooth") && !message.contains("対応していません") -> "オンにする" to {
            enableBt.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        !message.contains("未設定") && (message.contains("Supabase") || message.contains("同期")) -> "再試行" to onRefresh
        else -> null
    }
    val refreshWithFeedback: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        onRefresh()
    }

    // 下に引っぱって登録情報を再同期（ボタンでも同じ操作ができる）
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.syncing,
        onRefresh = refreshWithFeedback,
        state = pullState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = state.syncing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = "HELP RADAR",
                title = "近くのヘルプ",
                trailing = { SyncButton(state.syncing, onRefresh) },
            )
        }

        item(key = "hero") { ScanHero(state, onToggle, Modifier.appear(0)) }

        if (message != null) {
            item(key = "message") { MessageBanner(message, bannerAction, Modifier.springItem(this)) }
        }

        if (matches.isNotEmpty()) {
            item(key = "label-match") { SectionLabel("支援が必要な方", matches.size, HelpRed, Modifier.springItem(this)) }
            items(matches, key = { "m-" + it.address }) { device ->
                MatchCard(
                    device,
                    onOpen = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        openProfile = device.match
                    },
                    modifier = Modifier
                        .springItem(this)
                        .appear(key = device.address),
                )
            }
        }

        if (others.isNotEmpty()) {
            item(key = "label-others") {
                SectionLabel("周辺のデバイス", others.size, MaterialTheme.colorScheme.tertiary, Modifier.springItem(this))
            }
            itemsIndexedStagger(named, prefix = "d-") { device, mod -> DeviceRow(device, mod) }
            if (unnamed.isNotEmpty()) {
                item(key = "unnamed-toggle") {
                    UnnamedToggle(
                        count = unnamed.size,
                        expanded = showUnnamed,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            showUnnamed = !showUnnamed
                        },
                        modifier = Modifier.springItem(this),
                    )
                }
                if (showUnnamed) {
                    itemsIndexedStagger(unnamed, prefix = "u-") { device, mod -> DeviceRow(device, mod) }
                }
            }
        } else if (state.scanning && matches.isEmpty()) {
            item(key = "empty") { EmptyHint(Modifier.springItem(this)) }
        }
    }
    }

    openProfile?.let { profile ->
        ModalBottomSheet(
            onDismissRequest = { openProfile = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            // 内容が画面より長くても最後まで読めるよう、シート内をスクロール可能にする
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileHeader(profile = profile, caption = "近くにいる支援が必要な方", modifier = Modifier.appear(0))
                ProfileDetails(profile, startIndex = 1)
            }
        }
    }
}

/** 最初の数件だけ時間差で出し、それ以降はすぐ出す */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedStagger(
    list: List<NearbyDevice>,
    prefix: String,
    content: @Composable (NearbyDevice, Modifier) -> Unit,
) {
    items(list.size, key = { prefix + list[it].address }) { i ->
        val device = list[i]
        content(device, Modifier.springItem(this).appear(index = minOf(i, 6), key = device.address))
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(size = 26.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (slideInVertically(Springs.bouncy()) { it / 2 } + fadeIn())
                        .togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "title",
            ) { t ->
                Text(
                    t,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsButton()
            trailing()
        }
    }
}

@Composable
private fun SyncButton(syncing: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "sync")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spin")
    FilledTonalIconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier
            .size(56.dp)
            .pressScale(source, 0.85f)
            .semantics { contentDescription = "登録情報を再同期" },
    ) {
        Icon(
            Icons.Rounded.Sync,
            contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = if (syncing) -spin else 0f },
        )
    }
}

@Composable
private fun ScanHero(state: NearbyState, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val alert = state.matches.isNotEmpty()
    val mode = when {
        alert -> RadarMode.Alert
        state.scanning -> RadarMode.Scanning
        else -> RadarMode.Idle
    }
    val cs = MaterialTheme.colorScheme
    val container by animateColorAsState(if (alert) HelpRed else cs.surfaceContainerLow, Springs.smooth(), label = "c")
    val deco by animateColorAsState(if (alert) Color.White.copy(alpha = 0.12f) else cs.primaryContainer, label = "d")
    val deco2 by animateColorAsState(if (alert) Color.White.copy(alpha = 0.08f) else cs.tertiaryContainer, label = "d2")
    val text by animateColorAsState(if (alert) Color.White else cs.onSurface, label = "t")
    val sub by animateColorAsState(if (alert) Color.White.copy(alpha = 0.85f) else cs.onSurfaceVariant, label = "s")

    val headline = when (mode) {
        RadarMode.Alert -> "支援が必要な方が\n近くに ${state.matches.size} 人います"
        RadarMode.Scanning -> "周辺をさがしています"
        RadarMode.Idle -> "スキャンを開始しましょう"
    }
    val body = when (mode) {
        RadarMode.Alert -> "下のカードをタップすると、お手伝いの内容が見られます"
        RadarMode.Scanning -> "登録された BLE タグが見つかると赤くお知らせします"
        RadarMode.Idle -> "Bluetooth で近くのヘルプタグを探します"
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
    ) {
        // 大きな形がゆっくり回る装飾
        SpinningShape(
            MaterialShapes.Cookie12Sided,
            deco,
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-50).dp)
                .size(220.dp),
        )
        SpinningShape(
            MaterialShapes.Clover4Leaf,
            deco2,
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 30.dp)
                .size(140.dp),
            periodMillis = 30_000,
            clockwise = false,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                state.scanning,
                enter = expandVertically(Springs.smooth()) + fadeIn(),
                exit = shrinkVertically(Springs.smooth()) + fadeOut(),
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = if (alert) Color.White else HelpRed,
                    trackColor = if (alert) Color.White.copy(alpha = 0.25f) else cs.primaryContainer,
                )
            }
            ExpressiveRadar(
                mode = mode,
                icon = if (state.scanning) Icons.AutoMirrored.Rounded.BluetoothSearching else Icons.Rounded.Radar,
                ringColor = if (alert) Color.White else HelpRed,
                coreColor = if (alert) Color.White else HelpRed,
                iconTint = Color.White,
                size = 150.dp,
            )
            Spacer(Modifier.size(8.dp))
            AnimatedContent(
                targetState = headline,
                transitionSpec = {
                    (slideInVertically(Springs.bouncy()) { it / 2 } + fadeIn() + scaleIn(initialScale = 0.9f))
                        .togetherWith(slideOutVertically(Springs.smooth()) { -it / 2 } + fadeOut())
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                label = "headline",
            ) { h ->
                Text(h, color = text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.size(6.dp))
            AnimatedContent(targetState = body, label = "body") { b ->
                Text(b, color = sub, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.size(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    Icons.Rounded.Bluetooth, "検出", state.devices.size, alert,
                )
                StatChip(
                    if (state.error == null) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    "登録", state.registeredCount, alert,
                )
            }
            Spacer(Modifier.size(18.dp))

            val source = remember { MutableInteractionSource() }
            val btnContainer by animateColorAsState(
                when {
                    alert -> Color.White
                    state.scanning -> cs.inverseSurface
                    else -> HelpRed
                },
                Springs.smooth(),
                label = "btn",
            )
            Button(
                onClick = onToggle,
                shapes = ButtonDefaults.shapes(),
                interactionSource = source,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .pressScale(source)
                    .semantics { stateDescription = if (state.scanning) "スキャン中" else "停止中" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnContainer,
                    contentColor = when {
                        alert -> HelpRedDeep
                        state.scanning -> cs.inverseOnSurface
                        else -> Color.White
                    },
                ),
            ) {
                AnimatedContent(
                    targetState = state.scanning,
                    transitionSpec = {
                        (scaleIn(Springs.bouncy()) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                    },
                    label = "btnIcon",
                ) { scanning ->
                    Icon(if (scanning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    targetState = if (state.scanning) "スキャンを停止" else "スキャンを開始",
                    transitionSpec = {
                        (slideInVertically(Springs.bouncy()) { it } + fadeIn())
                            .togetherWith(slideOutVertically { -it } + fadeOut())
                    },
                    label = "btnText",
                ) { label ->
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Int, alert: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (alert) Color.White.copy(alpha = 0.18f) else cs.surfaceContainerLowest, label = "chipBg")
    val fg by animateColorAsState(if (alert) Color.White else cs.primary, label = "chipFg")
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        RollingNumber(value, MaterialTheme.typography.labelLarge, if (alert) Color.White else cs.onSurface)
    }
}

@Composable
private fun SectionLabel(text: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(top = 12.dp, start = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .background(color)
                .padding(horizontal = 12.dp, vertical = 2.dp),
        ) {
            RollingNumber(count, MaterialTheme.typography.labelLarge, Color.White)
        }
    }
}

/** BLE と DB が一致した端末：赤いカードが鼓動する */
@Composable
private fun MatchCard(device: NearbyDevice, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val profile = device.match ?: return
    val pulse = rememberInfiniteTransition(label = "match")
    val glow = pulse.animateFloat(
        0.2f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "glow",
    )
    val shape = MaterialTheme.shapes.extraLarge
    val source = remember { MutableInteractionSource() }
    val reduceMotion = LocalReduceMotion.current
    // 電波が途切れても一致した方はすぐには消さず、少し薄くして「弱まっている」ことを伝える
    val fadeAlpha by animateFloatAsState(
        when (device.fade) { 0 -> 1f; 1 -> 0.85f; else -> 0.6f },
        tween(900),
        label = "matchFade",
    )

    Box(
        modifier
            .graphicsLayer { alpha = fadeAlpha }
            .fillMaxWidth()
            .pressScale(source, 0.96f)
            .clip(shape)
            .background(HelpRed)
            // 光る縁は描画フェーズで読む（カード全体を毎フレーム再コンポーズしない）
            .drawWithContent {
                drawContent()
                drawOutline(
                    shape.createOutline(size, layoutDirection, this),
                    HelpRedGlow.copy(alpha = if (reduceMotion) 0.8f else glow.value),
                    style = Stroke(6.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = source,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClickLabel = "詳細を見る",
                onClick = onOpen,
            )
            .clearAndSetSemantics {
                contentDescription = "支援が必要な方、${profile.displayName}さん。${distanceLabel(device.rssi)}。" +
                    (profile.helpRequest?.let { "お願い：$it。" } ?: "") + "タップで詳細"
            }
    ) {
        SpinningShape(
            MaterialShapes.Sunny,
            Color.White.copy(alpha = 0.1f),
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .size(180.dp),
            periodMillis = 16_000,
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val beat by pulse.animateFloat(
                    1f, 1.15f,
                    infiniteRepeatable(tween(450), RepeatMode.Reverse),
                    label = "beat",
                )
                ShapeBadge(
                    Icons.Rounded.Favorite,
                    MaterialShapes.Cookie9Sided,
                    container = Color.White,
                    tint = HelpRed,
                    size = 44.dp,
                    modifier = Modifier.graphicsLayer {
                        val b = if (reduceMotion) 1f else beat
                        scaleX = b
                        scaleY = b
                    },
                )
                Spacer(Modifier.width(10.dp))
                Text("HELP 一致", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(signalStatus(device), color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                SignalBars(if (device.fade > 0) -100 else device.rssi, Color.White)
            }
            Spacer(Modifier.size(14.dp))
            Text(profile.displayName, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            profile.helpRequest?.let {
                Spacer(Modifier.size(4.dp))
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(16.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("詳細を見る", color = HelpRedDeep, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = HelpRedDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(device: NearbyDevice, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    // 電波が途切れたら、いきなり消さずにゆっくり薄くする
    val fadeAlpha by animateFloatAsState(
        when (device.fade) { 0 -> 1f; 1 -> 0.6f; else -> 0.35f },
        tween(900),
        label = "fade",
    )
    Row(
        modifier
            .graphicsLayer { alpha = fadeAlpha }
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainerLow)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 一致していない端末は赤を使わず控えめに。形は端末ごとに固定
        ShapeBadge(
            Icons.Rounded.Bluetooth,
            shapeFor(device.address),
            container = cs.surfaceContainerHighest,
            tint = cs.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                device.name ?: "名前のないデバイス",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                device.address,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = cs.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            SignalBars(if (device.fade > 0) -100 else device.rssi, cs.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            AnimatedContent(targetState = signalStatus(device), label = "dist") { d ->
                Text(d, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UnnamedToggle(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val rot by animateFloatAsState(if (expanded) 180f else 0f, Springs.bouncy(), label = "chevron")
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressScale(source, 0.97f)
            .clip(CircleShape)
            .background(cs.secondaryContainer)
            .clickable(
                interactionSource = source,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClick = onToggle,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.DevicesOther, contentDescription = null, tint = cs.onSecondaryContainer)
        Spacer(Modifier.width(12.dp))
        Text(
            "名前のないデバイス $count 台",
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text(if (expanded) "閉じる" else "表示", style = MaterialTheme.typography.labelLarge, color = HelpRed)
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = HelpRed,
            modifier = Modifier.graphicsLayer { rotationZ = rot },
        )
    }
}

@Composable
private fun MessageBanner(
    text: String,
    action: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.tertiaryContainer)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(
            Icons.Rounded.WarningAmber,
            MaterialShapes.Gem,
            container = cs.tertiary,
            tint = cs.onTertiary,
            size = 40.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            val source = remember { MutableInteractionSource() }
            Button(
                onClick = action.second,
                shapes = ButtonDefaults.shapes(),
                interactionSource = source,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .pressScale(source),
                colors = ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary),
            ) {
                Text(action.first, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator(Modifier.size(72.dp), color = HelpRed)
        Spacer(Modifier.size(12.dp))
        Text(
            "まだデバイスが見つかっていません",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 電波の状態を言葉で（途切れている時は距離ではなくその旨を出す） */
private fun signalStatus(device: NearbyDevice): String = when (device.fade) {
    0 -> distanceLabel(device.rssi)
    1 -> "受信待ち…"
    else -> "電波が途切れています"
}
