package com.example.help_markun.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.example.help_markun.R
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.LocalReduceMotion

/** アイコン用のバッジで順番に使う形（単調にならないように） */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val BadgeShapes: List<RoundedPolygon> = listOf(
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Gem,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.SoftBurst,
    MaterialShapes.Pill,
)

/** 文字列から毎回同じ形を選ぶ（端末ごとに形が変わると落ち着かないため） */
fun shapeFor(key: String): RoundedPolygon = BadgeShapes[(key.hashCode() and 0x7FFFFFFF) % BadgeShapes.size]

/** MaterialShapes の形で切り抜いたアイコンバッジ */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapeBadge(
    icon: ImageVector,
    polygon: RoundedPolygon,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(polygon.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** ゆっくり回り続ける装飾用の形 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpinningShape(
    polygon: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier,
    periodMillis: Int = 24_000,
    clockwise: Boolean = true,
) {
    // 「動きを控えめにする」がオンなら回さずに置くだけ
    if (LocalReduceMotion.current) {
        Box(modifier.clip(polygon.toShape()).background(color))
        return
    }
    val t = rememberInfiniteTransition(label = "spin")
    val r by t.animateFloat(
        0f, if (clockwise) 360f else -360f,
        infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "r",
    )
    Box(
        modifier
            .graphicsLayer { rotationZ = r }
            .clip(polygon.toShape())
            .background(color)
    )
}

/**
 * 単位座標系のパスを、[center] を中心に一辺 [sizePx] の正方形へ収めて描く。
 * 形の外接矩形で正規化するので、どの MaterialShapes / Morph でもそのまま使える。
 */
fun DrawScope.drawFitted(
    path: Path,
    center: Offset,
    sizePx: Float,
    color: Color,
    rotation: Float = 0f,
    strokePx: Float? = null,
) {
    val b = path.getBounds()
    val dim = maxOf(b.width, b.height).takeIf { it > 0f } ?: return
    val k = sizePx / dim
    withTransform({
        translate(center.x, center.y)
        rotate(rotation, pivot = Offset.Zero)
        scale(k, k, pivot = Offset.Zero)
        translate(-(b.left + b.width / 2), -(b.top + b.height / 2))
    }) {
        drawPath(path, color, style = strokePx?.let { Stroke(it / k) } ?: Fill)
    }
}

/** ヘルプマーク風のロゴ（赤地に白の十字とハート） */
@Composable
fun HelpMarkLogo(modifier: Modifier = Modifier, background: Color = HelpRed, foreground: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(background, cornerRadius = CornerRadius(w * 0.18f))

        val cx = w / 2
        val arm = w * 0.17f
        val len = w * 0.56f
        val cy = h * 0.3f
        drawRect(foreground, Offset(cx - arm / 2, cy - len / 2), Size(arm, len))
        drawRect(foreground, Offset(cx - len / 2, cy - arm / 2), Size(len, arm))

        val top = h * 0.6f
        val hw = w * 0.56f
        val hh = h * 0.3f
        val left = cx - hw / 2
        val heart = Path().apply {
            moveTo(cx, top + hh)
            cubicTo(left - hw * 0.1f, top + hh * 0.45f, left + hw * 0.05f, top - hh * 0.25f, cx, top + hh * 0.18f)
            cubicTo(cx + hw * 0.45f, top - hh * 0.25f, cx + hw * 0.6f, top + hh * 0.45f, cx, top + hh)
            close()
        }
        drawPath(heart, foreground)
    }
}

/** アプリのロゴ（ランチャーアイコンと同じ絵柄）。角丸スクワークルで切り抜いて表示する */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
        // アダプティブアイコンの前景は中央 2/3 が見える範囲なので、少し拡大して収める
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.45f
                    scaleY = 1.45f
                },
        )
    }
}
