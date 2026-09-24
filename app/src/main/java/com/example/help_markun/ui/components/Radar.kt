package com.example.help_markun.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.example.help_markun.ui.theme.LocalReduceMotion

enum class RadarMode { Idle, Scanning, Alert }

/**
 * Expressive なレーダー。
 * 中心はクッキー形 → 一致するとハートへぷるんとモーフィングし、鼓動する。
 * 波紋も中心と同じ形で広がる。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveRadar(
    mode: RadarMode,
    icon: ImageVector,
    ringColor: Color,
    coreColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
) {
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Heart) }
    // 毎フレーム Path を作らないよう使い回す
    val path = remember { Path() }
    val alert = mode == RadarMode.Alert
    val reduceMotion = LocalReduceMotion.current
    // 「動きを控えめにする」では回転・波紋・鼓動を止め、形の変化（クッキー→ハート）だけ残す
    val active = mode != RadarMode.Idle && !reduceMotion

    // 形の変化はスプリングで少し行き過ぎさせ、ゼリーのように揺らす
    val progress by animateFloatAsState(if (alert) 1f else 0f, Springs.jelly(), label = "morph")
    // 一致・スキャン中は核が大きくなる
    val coreScale by animateFloatAsState(
        when (mode) {
            RadarMode.Idle -> 0.34f
            RadarMode.Scanning -> 0.38f
            RadarMode.Alert -> 0.5f
        },
        Springs.bouncy(),
        label = "coreScale",
    )

    val t = rememberInfiniteTransition(label = "radar")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val wave by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (alert) 1200 else 2400, easing = LinearEasing)),
        label = "wave",
    )
    val beat by t.animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 1000
                1f at 0
                1.14f at 120
                1f at 260
                1.08f at 380
                1f at 560
            }
        ),
        label = "beat",
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            path.rewind()
            morph.toPath(progress.coerceIn(-0.1f, 1.1f), path)
            val maxD = this.size.minDimension
            val coreD = maxD * coreScale
            // ハートはまっすぐ立たせ、クッキーの間は回し続ける
            val rot = if (active) spin * (1f - progress.coerceIn(0f, 1f)) else 0f

            // 静的なガイド
            for (i in 1..2) {
                drawFitted(path, center, coreD + (maxD - coreD) * i / 2.2f, ringColor.copy(alpha = 0.12f), rot, 1.dp.toPx())
            }
            // 同じ形の波紋
            if (active) {
                for (i in 0 until 3) {
                    val p = (wave + i / 3f) % 1f
                    drawFitted(
                        path, center, coreD + (maxD - coreD) * p,
                        ringColor.copy(alpha = (1f - p) * 0.5f),
                        rot,
                        (4.dp.toPx() * (1f - p)).coerceAtLeast(1f),
                    )
                }
            }
            val s = if (alert && !reduceMotion) beat else 1f
            drawFitted(path, center, coreD * 1.22f * s, coreColor.copy(alpha = 0.25f), rot)
            drawFitted(path, center, coreD * s, coreColor, rot)
        }
        AnimatedVisibility(!alert, enter = scaleIn(Springs.bouncy()), exit = scaleOut()) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.18f))
        }
    }
}
