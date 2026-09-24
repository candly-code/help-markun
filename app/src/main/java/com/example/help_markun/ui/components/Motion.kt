package com.example.help_markun.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.help_markun.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/** アプリ全体で使うスプリング。Expressive らしく少し弾ませる */
object Springs {
    /** 弾む：押下・ポップイン */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)

    /** ぷるんと大きく弾む：形のモーフィング */
    fun <T> jelly() = spring<T>(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)

    /** なめらか：画面遷移・位置移動 */
    fun <T> smooth() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
}

/** 押している間だけ少し縮み、離すと弾んで戻る */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.94f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) spring(stiffness = Spring.StiffnessHigh) else Springs.bouncy(),
        label = "press",
    )
    return graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/**
 * 表示時に下からふわっと弾みながら現れる。[index] ごとに少し遅らせて順番に出す。
 * [key] が変わると再生し直す。
 */
@Composable
fun Modifier.appear(index: Int = 0, key: Any? = Unit): Modifier {
    if (LocalReduceMotion.current) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(index * 70L)
        p.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 48.dp.toPx()
        val s = 0.9f + 0.1f * v
        scaleX = s
        scaleY = s
    }
}

/** 数字が変わるとき、増えたら上へ・減ったら下へ転がる */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(Springs.bouncy()) { if (up) it else -it } + fadeIn())
                .togetherWith(slideOutVertically(Springs.smooth()) { if (up) -it else it } + fadeOut())
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "rolling",
    ) { v ->
        Text("$v", style = style, color = color)
    }
}

/** 0 から目標値まで数え上げる（残高表示など） */
@Composable
fun countUp(target: Int, durationMillis: Int = 1100): Int {
    val a = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        a.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
    }
    return a.value.toInt()
}
