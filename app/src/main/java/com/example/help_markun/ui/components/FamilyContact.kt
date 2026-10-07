@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.example.help_markun.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.ui.LocalAccount
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.HelpRedGlow
import com.example.help_markun.ui.theme.LocalReduceMotion
import com.example.help_markun.ui.theme.Rose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** よく使う連絡の文（タップで選ぶだけで送れる） */
private val QuickMessages = listOf(
    "今、一緒にいます",
    "駅員さんに引き継ぎました",
    "迎えに来られますか？",
    "至急連絡をください",
)

private enum class SendState { Idle, Sending, Sent, Failed }

/**
 * 見つけた人（ログインなしでもよい）から、その方のご家族へ一言を送るシート。
 * 送ると紙飛行機が飛んでいき、チェックに変わってお祝いがはじける。
 */
@Composable
fun FamilyContactSheet(profile: HelpProfile, onDismiss: () -> Unit) {
    val account = LocalAccount.current ?: return
    val accountState by account.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()

    var selected by rememberSaveable { mutableStateOf<String?>(QuickMessages.first()) }
    var custom by rememberSaveable { mutableStateOf("") }
    var sender by rememberSaveable { mutableStateOf("") }
    var sendState by remember { mutableStateOf(SendState.Idle) }
    var burst by remember { mutableStateOf<Int?>(null) }
    val message = custom.trim().ifEmpty { selected.orEmpty() }

    // 紙飛行機が右上へ飛んでいく
    val fly = remember { Animatable(0f) }

    LaunchedEffect(sendState) {
        if (sendState == SendState.Sent) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            burst = (burst ?: 0) + 1
            delay(1500)
            onDismiss()
        }
        if (sendState == SendState.Failed) haptic.performHapticFeedback(HapticFeedbackType.Reject)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = cs.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.appear(0), verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(Icons.Rounded.FamilyRestroom, MaterialShapes.Heart, HelpRed, Color.White, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    "${profile.displayName}さんのご家族へ",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }

            // よく使う文：選ぶと赤く塗られて弾む
            FlowRow(
                Modifier.appear(1),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickMessages.forEach { m ->
                    val isSel = custom.isBlank() && selected == m
                    val bg by animateColorAsState(if (isSel) HelpRed else cs.surfaceContainerHigh, label = "chipBg")
                    val fg by animateColorAsState(if (isSel) Color.White else cs.onSurface, label = "chipFg")
                    val source = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .pressScale(source, 0.9f)
                            .clip(CircleShape)
                            .background(bg)
                            .then(if (isSel) Modifier else Modifier.border(1.dp, cs.outlineVariant, CircleShape))
                            .clickable(source, LocalIndication.current, role = Role.RadioButton) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                selected = m
                                custom = ""
                            }
                            .semantics { this.selected = isSel }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedContent(isSel, transitionSpec = {
                            (scaleIn(Springs.bouncy(), initialScale = 0.2f) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                                .using(SizeTransform(clip = false))
                        }, label = "chipCheck") { on ->
                            if (on) {
                                Row {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                        }
                        Text(m, color = fg, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.take(200) },
                label = { Text("自分で書く") },
                shape = MaterialTheme.shapes.large,
                minLines = 2,
                modifier = Modifier
                    .appear(2)
                    .fillMaxWidth(),
            )
            if (accountState.session == null) {
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it.take(20) },
                    label = { Text("お名前（任意）") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .appear(3)
                        .fillMaxWidth(),
                )
            }

            Box(Modifier.appear(4), contentAlignment = Alignment.Center) {
                val source = remember { MutableInteractionSource() }
                val container by animateColorAsState(
                    when (sendState) {
                        SendState.Sent -> Color(0xFF1E8E3E)
                        SendState.Failed -> cs.error
                        else -> HelpRed
                    },
                    label = "sendColor",
                )
                Button(
                    onClick = {
                        if (sendState == SendState.Sending || sendState == SendState.Sent || message.isBlank()) return@Button
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        sendState = SendState.Sending
                        scope.launch {
                            if (!reduceMotion) {
                                fly.snapTo(0f)
                                fly.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                            }
                        }
                        account.sendToFamily(profile, message, sender) { ok ->
                            sendState = if (ok) SendState.Sent else SendState.Failed
                        }
                    },
                    enabled = message.isNotBlank(),
                    shapes = ButtonDefaults.shapes(),
                    interactionSource = source,
                    colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .pressScale(source)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    AnimatedContent(
                        targetState = sendState,
                        transitionSpec = {
                            (scaleIn(Springs.jelly(), initialScale = 0.4f) + fadeIn())
                                .togetherWith(scaleOut(targetScale = 0.6f) + fadeOut())
                        },
                        label = "send",
                    ) { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (st) {
                                SendState.Idle, SendState.Failed -> {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = null,
                                        modifier = Modifier.graphicsLayer {
                                            val f = fly.value
                                            translationX = f * 220f
                                            translationY = -f * 140f
                                            rotationZ = -30f * f
                                            alpha = 1f - f
                                        },
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        if (st == SendState.Failed) "もう一度送る" else "送る",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                SendState.Sending -> {
                                    LoadingIndicator(Modifier.size(32.dp), color = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Text("送っています…", style = MaterialTheme.typography.labelLarge)
                                }
                                SendState.Sent -> {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Text("届きました", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                Burst(burst, listOf(HelpRed, HelpRedGlow, Rose, Color(0xFF1E8E3E), Color(0xFFFFC857)), Modifier.matchParentSize())
            }
        }
    }
}
