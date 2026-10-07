package com.example.help_markun.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.example.help_markun.ui.theme.LocalReduceMotion
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2 つの形の間を [progress] で連続的に変形させて描く（0 = 始めの形、1 = 終わりの形）。
 * 値は描画フェーズで読むので、アニメーション中も再コンポーズは起きない。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingShape(
    morph: Morph,
    progress: () -> Float,
    color: () -> Color,
    modifier: Modifier = Modifier,
    rotation: () -> Float = { 0f },
    scale: () -> Float = { 1f },
) {
    Spacer(
        modifier.drawWithCache {
            val path = Path()
            onDrawBehind {
                path.rewind()
                morph.toPath(progress().coerceIn(-0.15f, 1.15f), path)
                drawFitted(path, center, size.minDimension * scale(), color(), rotation())
            }
        }
    )
}

private class Particle(val angle: Float, val distance: Float, val size: Float, val spin: Float, val shape: Int, val delay: Float)

/**
 * お祝いのはじけ。[trigger] が変わるたびに、中心から小さな形がぱっと飛び散る
 * （ログインできた・家族に届いた、などの「できた！」の瞬間に使う）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Burst(
    trigger: Any?,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    count: Int = 18,
) {
    if (LocalReduceMotion.current) return
    val t = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val r = Random(trigger.hashCode())
        List(count) { i ->
            Particle(
                angle = (i.toFloat() / count) * 360f + r.nextFloat() * 18f,
                distance = 0.55f + r.nextFloat() * 0.45f,
                size = 0.05f + r.nextFloat() * 0.05f,
                spin = (r.nextFloat() - 0.5f) * 540f,
                shape = r.nextInt(3),
                delay = r.nextFloat() * 0.12f,
            )
        }
    }
    val shapes = remember {
        listOf(MaterialShapes.Heart, MaterialShapes.Cookie4Sided, MaterialShapes.Circle).map { p ->
            // 同じ形どうしの Morph にすると、Compose の Path として取り出せる
            Path().also { Morph(p, p).toPath(0f, it) }
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        t.snapTo(0f)
        t.animateTo(1f, tween(900, easing = LinearEasing))
    }
    Spacer(
        modifier.drawWithCache {
            onDrawBehind {
                val v = t.value
                if (v >= 1f) return@onDrawBehind
                // 置いた場所（ボタンやカード）の大きさに合わせて広がる。粒の大きさは画面上で一定
                val radius = size.maxDimension / 2f * 0.95f
                particles.forEachIndexed { i, p ->
                    val local = ((v - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEachIndexed
                    // 勢いよく飛び出して、ふわっと止まりながら消える
                    val travel = FastOutSlowInEasing.transform(local)
                    val a = Math.toRadians(p.angle.toDouble())
                    val d = radius * p.distance * travel
                    val c = Offset(center.x + (cos(a) * d).toFloat(), center.y + (sin(a) * d).toFloat() + radius * 0.15f * local * local)
                    val alpha = (1f - local).coerceIn(0f, 1f)
                    val s = (p.size * 220f).dp.toPx() * (0.6f + 0.6f * (1f - local))
                    drawFitted(shapes[p.shape], c, s, colors[i % colors.size].copy(alpha = alpha), p.spin * local)
                }
            }
        }
    )
}
