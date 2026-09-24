package com.example.help_markun.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.help_markun.ui.components.DataStatus
import com.example.help_markun.ui.components.LocalDataStatus
import com.example.help_markun.ui.components.LocalSnackbar
import com.example.help_markun.ui.components.pressScale
import com.example.help_markun.ui.screens.CardScreen
import com.example.help_markun.ui.screens.NearbyScreen
import com.example.help_markun.ui.theme.HelpRed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

/** タップでページを移るときのスプリング（少しだけ行き過ぎて戻る） */
private val PageSpring = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun HelpApp(vm: HelpViewModel) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val nearby by vm.nearby.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()
    val nfc by vm.nfcStatus.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()

    val pager = rememberPagerState(initialPage = tab.ordinal) { AppTab.entries.size }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // タブごとのスクロール位置を保持（切り替えても元の位置に戻る）
    val nearbyList = rememberLazyListState()
    val cardScroll = rememberScrollState()

    // ViewModel 側からの切り替え（カードをタッチした時など）→ ページを動かす
    LaunchedEffect(tab) {
        if (pager.targetPage != tab.ordinal) pager.animateScrollToPage(tab.ordinal, animationSpec = PageSpring)
    }
    // スワイプで止まったページ → ViewModel に反映
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { vm.selectTab(AppTab.entries[it]) }
    }
    // ページが切り替わる瞬間に軽く振動
    LaunchedEffect(pager) {
        snapshotFlow { pager.targetPage }.distinctUntilChanged().drop(1).collect {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    // 新しいカードを読んだら、結果が最初から見えるよう先頭へ戻す
    LaunchedEffect(card) {
        if (card is CardState.Loading) cardScroll.scrollTo(0)
    }

    val snackbar = remember { SnackbarHostState() }

    // 戻る操作：カード結果 → 待ち受け → 近くのヘルプ の順に 1 段ずつ戻る。
    // 予測型「戻る」に対応し、端からのスワイプを始めた瞬間から画面が少し縮んで反応する
    // （途中でやめればキャンセル）。小さなジェスチャーの意図も見逃さない
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = pager.currentPage == AppTab.Card.ordinal) { events ->
        try {
            events.collect { backProgress = it.progress }
            backProgress = 0f
            if (card !is CardState.Waiting) vm.resetCard()
            else pager.animateScrollToPage(AppTab.Nearby.ordinal, animationSpec = PageSpring)
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }
    val backScale by animateFloatAsState(1f - 0.08f * backProgress, label = "backScale")

    fun select(target: AppTab) {
        // ページが動いている最中のタップも取りこぼさず、必ずそのタブへ向かう
        if (pager.targetPage == target.ordinal && !pager.isScrollInProgress) {
            // 選択中のタブをもう一度押したら先頭へ戻る
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            scope.launch {
                when (target) {
                    AppTab.Nearby -> nearbyList.animateScrollToItem(0)
                    AppTab.Card -> cardScroll.animateScrollTo(0)
                }
            }
        } else {
            scope.launch { pager.animateScrollToPage(target.ordinal, animationSpec = PageSpring) }
        }
    }

    // 文字色の既定値（LocalContentColor）をテーマの onBackground にして、
    // 色を指定していない文字もライト／ダークに追従させる
    CompositionLocalProvider(
        LocalSnackbar provides snackbar,
        LocalDataStatus provides DataStatus(
            configured = vm.supabaseConfigured,
            registeredCount = nearby.registeredCount,
            syncing = nearby.syncing,
            lastSyncedAt = nearby.lastSyncedAt,
            error = nearby.error,
            onSync = vm::refreshProfiles,
        ),
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pager,
            // 見えていないタブは破棄してアニメーションや描画を止める（スクロール位置は上で保持済み）
            beyondViewportPageCount = 0,
            // 画面幅の 15% 動かすか軽くはじくだけで次のタブへ（標準は半分）
            flingBehavior = PagerDefaults.flingBehavior(state = pager, snapPositionalThreshold = 0.15f),
            modifier = Modifier.weight(1f),
            key = { AppTab.entries[it].name },
        ) { page ->
            // タブレット・折りたたみ端末でも読みやすいよう、本文は最大幅 640dp で中央寄せ
            Box(
                Modifier
                    .fillMaxSize()
                    .pageTransition(pager, page)
                    .graphicsLayer {
                        if (page == AppTab.Card.ordinal) {
                            scaleX = backScale
                            scaleY = backScale
                        }
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    when (AppTab.entries[page]) {
                        AppTab.Nearby -> NearbyScreen(
                            state = nearby,
                            bleSupported = vm.bleSupported,
                            isBluetoothEnabled = vm::isBluetoothEnabled,
                            onStart = vm::startScan,
                            onStop = vm::stopScan,
                            onRefresh = vm::refreshProfiles,
                            listState = nearbyList,
                            focusProfile = focus,
                            onFocusConsumed = vm::consumeFocus,
                        )
                        AppTab.Card -> CardScreen(
                            state = card,
                            nfcStatus = nfc,
                            onReset = vm::resetCard,
                            scrollState = cardScroll,
                        )
                    }
                }
            }
        }

        // スナックバーはタブバーのすぐ上に出す（指の近くで気づきやすい）
        SnackbarHost(snackbar, Modifier.fillMaxWidth())

        FloatingTabBar(
            pager = pager,
            alertCount = nearby.matches.size,
            onSelect = ::select,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        )
    }
    }
}

/** スワイプ量に合わせて、離れていくページは少し縮んで薄くなる */
private fun Modifier.pageTransition(pager: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
    val d = abs(offset)
    val s = 1f - 0.08f * d
    scaleX = s
    scaleY = s
    alpha = 1f - 0.45f * d
}

private data class TabSpec(val tab: AppTab, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabSpec(AppTab.Nearby, "近くのヘルプ", Icons.Rounded.Radar),
    TabSpec(AppTab.Card, "カード", Icons.Rounded.Contactless),
)

/**
 * フローティングツールバー風のタブ。
 * 赤いインジケーターはページのスワイプ量にそのまま追従し、移動中は横に伸びる。
 */
@Composable
private fun FloatingTabBar(
    pager: PagerState,
    alertCount: Int,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    // 0.0（近くのヘルプ）〜 1.0（カード）の連続値
    val position = pager.currentPage + pager.currentPageOffsetFraction
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 端末の文字サイズを大きくしている人でもラベルが切れないよう、バーの高さも追従させる
    val barHeight = 72.dp * density.fontScale.coerceIn(1f, 1.5f)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(barHeight)
            .shadow(10.dp, CircleShape, ambientColor = HelpRed.copy(alpha = 0.4f), spotColor = HelpRed.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .padding(6.dp)
    ) {
        val itemWidth = maxWidth / Tabs.size
        val itemPx = with(density) { itemWidth.toPx() }
        val travel = position - position.toInt()
        val stretch = sin(travel * PI).toFloat() // 移動の中間で最大

        Box(
            Modifier
                .offset(x = itemWidth * position)
                .width(itemWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = 1f + 0.18f * stretch
                    scaleY = 1f - 0.1f * stretch
                }
                .clip(CircleShape)
                .background(HelpRed)
        )

        // タブバーの上を左右になぞってもタブを移動できる（指の小さな動きもそのままページに伝える）
        val dragState = rememberDraggableState { delta ->
            val pagePx = (pager.layoutInfo.pageSize + pager.layoutInfo.pageSpacing).toFloat()
            pager.dispatchRawDelta(delta * pagePx / itemPx)
        }
        Row(
            Modifier
                .fillMaxSize()
                .selectableGroup()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val pos = pager.currentPage + pager.currentPageOffsetFraction
                        // 軽くはじいた方向を優先し、ゆっくり離したら近い方のタブへ
                        val target = when {
                            velocity > 200f -> ceil(pos)
                            velocity < -200f -> floor(pos)
                            else -> round(pos)
                        }.toInt().coerceIn(0, Tabs.lastIndex)
                        scope.launch { pager.animateScrollToPage(target, animationSpec = PageSpring) }
                    },
                ),
        ) {
            Tabs.forEachIndexed { index, spec ->
                // インジケーターが重なっている割合（0〜1）で色と大きさを連続的に変える
                val sel = (1f - abs(position - index)).coerceIn(0f, 1f)
                TabItem(
                    spec = spec,
                    selection = sel,
                    selected = pager.targetPage == index,
                    badge = if (spec.tab == AppTab.Nearby) alertCount else 0,
                    onClick = { onSelect(spec.tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    spec: TabSpec,
    selection: Float,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val fg = lerp(cs.onSurfaceVariant, Color.White, selection)
    val iconScale = 1f + 0.15f * selection
    val source = remember { MutableInteractionSource() }

    Row(
        modifier
            .fillMaxHeight()
            .pressScale(source, 0.9f)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = source,
                indication = null,
            )
            .semantics {
                contentDescription = if (badge > 0) "${spec.label}、支援が必要な方 $badge 人" else spec.label
            }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(
                spec.icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
            )
            if (badge > 0) {
                Box(
                    Modifier
                        .offset(x = 12.dp, y = (-8).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(lerp(HelpRed, Color.White, selection)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$badge", color = lerp(Color.White, HelpRed, selection), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        // バッジがある時はラベルと重ならないよう間隔を広げる
        Spacer(Modifier.width(if (badge > 0) 16.dp else 8.dp))
        Text(
            spec.label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
