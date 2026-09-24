package com.example.help_markun.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** RSSI から 4 段階の強さと、おおよその距離感を返す */
fun signalLevel(rssi: Int): Int = when {
    rssi >= -60 -> 4
    rssi >= -72 -> 3
    rssi >= -84 -> 2
    else -> 1
}

fun distanceLabel(rssi: Int): String = when (signalLevel(rssi)) {
    4 -> "すぐ近く"
    3 -> "近く"
    2 -> "少し離れている"
    else -> "遠い"
}

/** 電波強度バー（段階が変わると弾みながら伸び縮みする） */
@Composable
fun SignalBars(rssi: Int, color: Color, modifier: Modifier = Modifier) {
    val level = signalLevel(rssi)
    Row(
        modifier = modifier.semantics { contentDescription = "電波の強さ 4 段階中 $level、${distanceLabel(rssi)}" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 1..4) {
            val on = i <= level
            val h by animateDpAsState(if (on) (6 + i * 5).dp else 6.dp, Springs.bouncy(), label = "bar")
            val c by animateColorAsState(if (on) color else color.copy(alpha = 0.22f), label = "barColor")
            Box(
                Modifier
                    .width(6.dp)
                    .height(h)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c)
            )
        }
    }
}

/** トーナルな小さいピル */
@Composable
fun TonalPill(
    icon: ImageVector,
    text: String,
    content: Color,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}
