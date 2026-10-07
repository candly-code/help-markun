package com.example.help_markun.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private const val FavoritePath =
    "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09" +
        "C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z"

/** 勢いよく広がって、端でふわりと止まる */
private val OpenEasing = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)

/**
 * 起動時のイントロ（約 1.9 秒）。
 * 1. 赤い画面に、白い線がハートの輪郭をなぞって描く
 * 2. ハートの中に白い水が波打ちながら満ちていく
 * 3. ドクン、ドクンと 2 回鼓動し、そのたびに光の輪が広がる
 * 4. ハートが「窓」になって大きく開き、その向こうにアプリが現れる
 * 「動きを控えめにする」がオン、または [skip] のときは出さない。
 */
@Composable
fun AppIntro(skip: Boolean = false) {
    val reduceMotion = LocalReduceMotion.current
    var show by rememberSaveable { mutableStateOf(!reduceMotion && !skip) }
    if (!show) return

    val trace = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val beat = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { wave.animateTo(6f, tween(1900, easing = LinearEasing)) }
        trace.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        fill.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
        repeat(2) { i ->
            launch {
                ring.snapTo(i.toFloat())
                ring.animateTo(i + 1f, tween(620, easing = FastOutSlowInEasing))
            }
            beat.animateTo(1f, tween(110))
            beat.animateTo(0f, tween(170, easing = FastOutSlowInEasing))
        }
        open.animateTo(1f, tween(640, easing = OpenEasing))
        show = false
    }

    // アプリのアイコンと同じ Material の「favorite」ハート（下の先端から輪郭をなぞり始める）
    val heart = remember { PathParser().parsePathString(FavoritePath).toPath() }

    Spacer(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            // イントロ中の誤タップを受け止める
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .drawWithCache {
                val base = size.minDimension * 0.34f
                val b = heart.getBounds()
                fun fitted(sizePx: Float): Path {
                    val k = sizePx / maxOf(b.width, b.height)
                    val m = Matrix().apply {
                        translate(size.width / 2f, size.height / 2f)
                        scale(k, k)
                        translate(-(b.left + b.width / 2), -(b.top + b.height / 2))
                    }
                    return Path().apply { addPath(heart); transform(m) }
                }
                val outline = fitted(base)
                val measure = PathMeasure().apply { setPath(outline, false) }
                val length = measure.length
                val traced = Path()
                val diag = hypot(size.width, size.height)
                val strokePx = 5.dp.toPx()

                onDrawBehind {
                    val o = open.value
                    val pulse = 1f + 0.12f * beat.value
                    val heartSize = base * pulse

                    if (o <= 0f) {
                        drawRect(HelpRed)
                        val shape = if (pulse == 1f) outline else fitted(heartSize)

                        // 2. 白い水が下から満ちる（水面は波打つ）
                        val f = fill.value
                        if (f > 0f) {
                            val top = center.y + heartSize / 2f - heartSize * 1.1f * f
                            val water = Path().apply {
                                moveTo(0f, size.height)
                                lineTo(0f, top)
                                val steps = 24
                                for (i in 0..steps) {
                                    val x = size.width * i / steps
                                    val y = top + sin((i / steps.toFloat()) * 4f * PI + wave.value * 2f * PI).toFloat() * 7.dp.toPx() * (1f - f)
                                    lineTo(x, y)
                                }
                                lineTo(size.width, size.height)
                                close()
                            }
                            clipPath(shape) { drawPath(water, Color.White) }
                        }

                        // 1. 輪郭をなぞる白い線
                        val t = trace.value
                        if (t > 0f && f < 1f) {
                            traced.rewind()
                            measure.getSegment(0f, length * t, traced, true)
                            drawPath(traced, Color.White.copy(alpha = 1f - f), style = Stroke(strokePx, cap = StrokeCap.Round))
                        }

                        // 3. 鼓動のたびに広がる光の輪
                        val r = ring.value
                        if (r > 0f) {
                            val local = r - r.toInt().toFloat()
                            if (local > 0f) {
                                drawPath(
                                    fitted(base * (1f + 1.6f * local)),
                                    Color.White.copy(alpha = 0.75f * (1f - local)),
                                    style = Stroke(strokePx * (1f - local) + 1f),
                                )
                            }
                        }
                    } else {
                        // 4. ハートの形の窓が開いて、向こうにアプリが見える
                        val window = fitted(base + (diag * 2.8f - base) * o)
                        val cover = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            addPath(window)
                        }
                        drawPath(cover, HelpRed.copy(alpha = (1f - o * o).coerceIn(0f, 1f)))
                        // 白いハートは広がりながら透けていき、その向こうにアプリが見えてくる
                        drawPath(window, Color.White.copy(alpha = (1f - o * 3.2f).coerceIn(0f, 1f)))
                    }
                }
            }
    )
}
