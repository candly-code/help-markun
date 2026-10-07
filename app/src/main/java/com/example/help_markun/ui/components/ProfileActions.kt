package com.example.help_markun.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.ui.LocalHelpActions
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch

private class ActionSpec(
    val icon: ImageVector,
    val label: String,
    val polygon: RoundedPolygon,
    /** 目立たせる操作（赤） */
    val strong: Boolean,
    val onClick: () -> Unit,
)

/**
 * 詳細の上に並ぶ「手助け」のボタン列：探す・家族に連絡。
 * 押している間はそのボタンだけが横に広がり、形が丸く変わって、離すとぷるんと戻る。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileActions(profile: HelpProfile, onFind: (() -> Unit)?, modifier: Modifier = Modifier) {
    val actions = LocalHelpActions.current
    val specs = buildList {
        if (onFind != null) add(ActionSpec(Icons.Rounded.NearMe, "探す", MaterialShapes.Cookie9Sided, true, onFind))
        if (profile.familyId != null) {
            add(ActionSpec(Icons.Rounded.FamilyRestroom, "家族に連絡", MaterialShapes.Heart, true) {
                actions.contactFamily(profile)
            })
        }
    }
    if (specs.isEmpty()) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        specs.forEach { spec -> ActionTile(spec, Modifier) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionTile(spec: ActionSpec, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // 押している間だけ横に広がる（隣は少し縮む）
    val weight by animateFloatAsState(if (pressed) 1.35f else 1f, if (pressed) Springs.smooth() else Springs.bouncy(), label = "tileWeight")
    val morphProgress by animateFloatAsState(if (pressed) 1f else 0f, Springs.jelly(), label = "tileMorph")
    val morph = remember(spec.polygon) { Morph(spec.polygon, MaterialShapes.Circle) }
    val container = if (spec.strong) HelpRed else cs.primaryContainer
    val tint = if (spec.strong) Color.White else cs.onPrimaryContainer
    val bg by animateColorAsState(if (pressed) cs.surfaceContainerHighest else cs.surfaceContainer, label = "tileBg")

    // 押した後：アイコンが首を振り、波紋が広がって「受け付けた」ことを伝える
    val wiggle = remember { Animatable(0f) }
    val ring = remember { Animatable(1f) }

    Row(
        modifier
            .weight(weight)
            .heightIn(min = 76.dp)
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = spec.label,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                if (!reduceMotion) {
                    scope.launch {
                        wiggle.animateTo(0f, keyframes {
                            durationMillis = 460
                            -16f at 80
                            12f at 180
                            -6f at 290
                            0f at 460
                        })
                    }
                    scope.launch {
                        ring.snapTo(0f)
                        ring.animateTo(1f, tween(700))
                    }
                }
                spec.onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .drawBehind {
                    val v = ring.value
                    if (v < 1f) {
                        drawCircle(container.copy(alpha = 0.4f * (1f - v)), radius = size.minDimension / 2f * (1f + 0.9f * v))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            MorphingShape(morph, progress = { morphProgress }, color = { container }, modifier = Modifier.matchParentSize())
            Icon(
                spec.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = wiggle.value },
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            spec.label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
