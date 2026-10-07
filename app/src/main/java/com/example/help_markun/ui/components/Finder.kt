@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.help_markun.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.graphics.shapes.Morph
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.ui.NearbyDevice
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.HelpRedDeep
import com.example.help_markun.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

/** 近づいているか・離れているか */
private enum class Trend { Closer, Farther, Steady }

/**
 * 「近づいて探す」画面。電波の強さを手がかりに、その方のいる方へ歩いて探す。
 * 近づくほど鼓動が速く・色が濃くなり、振動も速くなる。すぐそばまで来ると中心がハートに変わる。
 */
@Composable
fun FinderDialog(profile: HelpProfile, device: NearbyDevice?, onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current

    // 近さ 0（遠い・見失った）〜 1（すぐそば）。-100dBm → 0、-50dBm → 1
    val lost = device == null || device.fade >= 2
    val raw = if (lost) 0f else ((device!!.rssi + 100) / 50f).coerceIn(0f, 1f)
    val proximity by animateFloatAsState(raw, spring(dampingRatio = 0.7f, stiffness = 60f), label = "proximity")
    val current by rememberUpdatedState(proximity)

    // 直近の電波の変化から「近づいている／離れている」を判断する
    val history = remember { mutableStateListOf<Int>() }
    LaunchedEffect(device?.rssi, lost) {
        if (lost) history.clear() else {
            history.add(device!!.rssi)
            if (history.size > 8) history.removeAt(0)
        }
    }
    val trend = if (history.size < 4) Trend.Steady else {
        val half = history.size / 2
        val diff = history.takeLast(half).average() - history.take(half).average()
        when {
            diff > 2.5 -> Trend.Closer
            diff < -2.5 -> Trend.Farther
            else -> Trend.Steady
        }
    }

    // 鼓動の位相：近いほど速く進む（毎フレーム進める。値は描画でだけ読む）
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    phase = (phase + dt * (0.6f + 2.4f * current)) % 1000f
                }
                last = now
            }
        }
    }
    // 鼓動に合わせた振動（近いほど間隔が短い）
    LaunchedEffect(Unit) {
        while (isActive) {
            val p = current
            if (p > 0.05f) haptic.performHapticFeedback(if (p > 0.8f) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
            delay((1400 - 1100 * p).toLong().coerceAtLeast(260))
        }
    }

    val level = when {
        lost -> 0
        raw < 0.25f -> 1
        raw < 0.5f -> 2
        raw < 0.8f -> 3
        else -> 4
    }
    val status = when (level) {
        0 -> "探しています…"
        1 -> "まだ遠いです"
        2 -> "近づいています"
        3 -> "近くにいます"
        else -> "すぐそばです"
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val bgFar = MaterialTheme.colorScheme.surfaceContainerLowest
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        // 近いほど背景が赤く染まる
                        drawRect(lerp(bgFar, HelpRedDeep, current.coerceIn(0f, 1f)))
                    }
                },
        ) {
            val onBg = if (proximity > 0.45f) Color.White else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("近づいて探す", style = MaterialTheme.typography.labelLarge, color = onBg.copy(alpha = 0.8f))
                        Text("${profile.displayName}さん", style = MaterialTheme.typography.headlineMedium, color = onBg)
                    }
                    val source = remember { MutableInteractionSource() }
                    Button(
                        onClick = onClose,
                        interactionSource = source,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HelpRedDeep),
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .pressScale(source),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("閉じる", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Pulse(
                        phase = { phase },
                        proximity = { current },
                        reduceMotion = reduceMotion,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clearAndSetSemantics {},
                    )
                    AnimatedContent(
                        targetState = level >= 4,
                        transitionSpec = {
                            (fadeIn() + androidx.compose.animation.scaleIn(Springs.jelly(), initialScale = 0.3f))
                                .togetherWith(fadeOut() + androidx.compose.animation.scaleOut())
                        },
                        label = "heartIcon",
                    ) { near ->
                        if (near) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HelpRed, modifier = Modifier.size(56.dp))
                        } else {
                            Spacer(Modifier.size(56.dp))
                        }
                    }
                }

                // 状態の文字は段階が変わるたびに下から入れ替わる
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        (slideInVertically(Springs.bouncy()) { it } + fadeIn())
                            .togetherWith(slideOutVertically { -it } + fadeOut())
                    },
                    label = "finderStatus",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) { s ->
                    Text(s, style = MaterialTheme.typography.displaySmall, color = onBg, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.size(10.dp))
                TrendPill(trend, lost, onBg)
                Spacer(Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TrendPill(trend: Trend, lost: Boolean, onBg: Color) {
    val (icon, label) = when {
        lost -> Icons.Rounded.SyncAlt to "見失いました"
        trend == Trend.Closer -> Icons.Rounded.NorthEast to "近づいています"
        trend == Trend.Farther -> Icons.Rounded.SouthWest to "離れています"
        else -> Icons.Rounded.SyncAlt to "そのまま"
    }
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(trend) { tilt.snapTo(if (trend == Trend.Closer) -25f else 25f); tilt.animateTo(0f, Springs.bouncy()) }
    AnimatedContent(
        targetState = icon to label,
        transitionSpec = {
            (androidx.compose.animation.scaleIn(Springs.bouncy(), initialScale = 0.7f) + fadeIn())
                .togetherWith(androidx.compose.animation.scaleOut(targetScale = 0.7f) + fadeOut())
        },
        label = "trend",
    ) { (i, l) ->
        Row(
            Modifier
                .clip(CircleShape)
                .background(onBg.copy(alpha = 0.14f))
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) { contentDescription = l },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(i, contentDescription = null, tint = onBg, modifier = Modifier.graphicsLayer { rotationZ = tilt.value })
            Spacer(Modifier.width(8.dp))
            Text(l, style = MaterialTheme.typography.labelLarge, color = onBg)
        }
    }
}

/**
 * 中心の鼓動と、外へ広がる波紋。近いほど波紋の間隔が詰まり、中心はクッキー形からハートへ変わる。
 */
@Composable
private fun Pulse(phase: () -> Float, proximity: () -> Float, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Heart) }
    Spacer(
        modifier.drawWithCache {
            val path = Path()
            val ringPath = Path().also { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Cookie12Sided).toPath(0f, it) }
            onDrawBehind {
                val p = proximity().coerceIn(0f, 1f)
                val ph = if (reduceMotion) 0f else phase()
                val maxD = size.minDimension
                val coreBase = maxD * (0.26f + 0.12f * p)
                val ringColor = lerp(HelpRed, Color.White, p)

                // 外へ広がる波紋（近いほど数が増える）
                val rings = 3
                for (i in 0 until rings) {
                    val t = ((ph + i.toFloat() / rings) % 1f)
                    val d = coreBase + (maxD - coreBase) * t
                    drawFitted(ringPath, center, d, ringColor.copy(alpha = 0.35f * (1f - t)), rotation = ph * 40f + i * 20f, strokePx = (2f + 4f * p) * density)
                }
                // 鼓動：1 拍で 2 回打つ（ドクン、ドクン）
                val beatPhase = (ph % 1f)
                val beat = if (reduceMotion) 0f else
                    (sin(beatPhase * 2f * PI).toFloat().coerceAtLeast(0f) * 0.7f + sin(beatPhase * 4f * PI).toFloat().coerceAtLeast(0f) * 0.3f)
                val core = coreBase * (1f + 0.12f * beat)
                // 近いほどハートに変わる（0.6 から変形を始めて 0.85 で完全にハート）
                val shape = ((p - 0.6f) / 0.25f).coerceIn(0f, 1f)
                path.rewind()
                morph.toPath(shape, path)
                drawFitted(path, center, core * 1.25f, ringColor.copy(alpha = 0.25f), rotation = ph * 12f * (1f - shape))
                drawFitted(path, center, core, if (p > 0.45f) Color.White else HelpRed, rotation = ph * 12f * (1f - shape))
            }
        }
    )
}
