package com.example.help_markun.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

/** 素早く広がり、最後はゆっくり止まる（円が画面の端に届く瞬間を柔らかくする） */
private val RevealEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/**
 * ライト／ダークの切り替えを、最後に触れた場所から新しいテーマが円形に広がる演出にする。
 * 切り替える直前の画面を写し取り、その写真に円い穴を広げて新しい画面を見せる。
 *
 * [dark] が変わると演出し、[content] には演出に合わせたテーマ（切り替え後）を渡す。
 */
@Composable
fun ThemeReveal(dark: Boolean, enabled: Boolean, content: @Composable (dark: Boolean) -> Unit) {
    val layer = rememberGraphicsLayer()
    var shownDark by remember { mutableStateOf(dark) }
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(dark) {
        if (dark == shownDark) return@LaunchedEffect
        // 古い画面を写し取ってからテーマを切り替える（写せない端末ではそのまま切り替える）
        snapshot = if (enabled) runCatching { layer.toImageBitmap() }.getOrNull() else null
        shownDark = dark
        if (snapshot != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(820, easing = RevealEasing))
        }
        snapshot = null
    }

    Box(
        Modifier
            .fillMaxSize()
            // 押した位置を覚えておく（タップ自体は下の画面にそのまま届く）
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        e.changes.firstOrNull()?.let { origin = it.position }
                    }
                }
            }
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
                val old = snapshot ?: return@drawWithContent
                val p = progress.value
                // 設定ボタン（右上）付近から広がるのが既定。触れた場所があればそこから
                val c = if (origin.isSpecified) origin else Offset(size.width * 0.8f, size.height * 0.1f)
                val maxR = hypot(maxOf(c.x, size.width - c.x), maxOf(c.y, size.height - c.y))
                val r = maxR * p
                val hole = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addOval(Rect(c, r))
                }
                clipPath(hole) { drawImage(old) }
                // 広がる縁に細い光の輪を走らせる
                if (p < 1f) {
                    drawCircle(
                        Color.White.copy(alpha = 0.35f * (1f - p)),
                        radius = r,
                        center = c,
                        style = Stroke((1f - p) * 6f * density + 1f),
                    )
                }
            },
    ) {
        content(shownDark)
    }
}
