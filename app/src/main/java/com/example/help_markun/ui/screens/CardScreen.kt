@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.help_markun.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.example.help_markun.hardware.CardInfo
import com.example.help_markun.hardware.NfcStatus
import com.example.help_markun.ui.CardState
import com.example.help_markun.ui.components.HelpMarkLogo
import com.example.help_markun.ui.components.LocalSnackbar
import com.example.help_markun.ui.components.ProfileDetails
import com.example.help_markun.ui.components.ProfileHeader
import com.example.help_markun.ui.components.ShapeBadge
import com.example.help_markun.ui.components.SpinningShape
import com.example.help_markun.ui.components.Springs
import com.example.help_markun.ui.components.TonalPill
import com.example.help_markun.ui.components.appear
import com.example.help_markun.ui.components.countUp
import com.example.help_markun.ui.components.pressScale
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.LocalReduceMotion
import com.example.help_markun.ui.theme.HelpRedDeep
import com.example.help_markun.ui.theme.HelpRedGlow
import com.example.help_markun.ui.theme.Ink
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CardScreen(
    state: CardState,
    nfcStatus: NfcStatus,
    onReset: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(eyebrow = "CARD TOUCH", title = if (state is CardState.Result) "読み取り結果" else "カードで確認")

        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                // 状態が進むときは下から、戻るときは上から。どちらも弾みながら入れ替わる
                val forward = targetState !is CardState.Waiting
                (slideInVertically(Springs.smooth()) { if (forward) it / 4 else -it / 4 } +
                    scaleIn(Springs.bouncy(), initialScale = 0.9f) + fadeIn(tween(220)))
                    .togetherWith(
                        slideOutVertically(Springs.smooth()) { if (forward) -it / 6 else it / 6 } +
                            scaleOut(targetScale = 1.04f) + fadeOut(tween(160))
                    )
            },
            label = "card",
        ) { s ->
            when (s) {
                CardState.Waiting -> TapPanel(nfcStatus)
                is CardState.Loading -> LoadingPanel(s.card.id)
                is CardState.Result -> CardResult(s, onReset)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 待ち受け
// ---------------------------------------------------------------------------

@Composable
private fun TapPanel(nfcStatus: NfcStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TapPanelHero(nfcStatus, Modifier.appear(0))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepTile(1, MaterialShapes.Cookie4Sided, "タッチ", Modifier.weight(1f).appear(1))
            StepTile(2, MaterialShapes.Clover4Leaf, "読み取り", Modifier.weight(1f).appear(2))
            StepTile(3, MaterialShapes.Sunny, "情報を表示", Modifier.weight(1f).appear(3))
        }
    }
}

@Composable
private fun TapPanelHero(nfcStatus: NfcStatus, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cs.surfaceContainerLow)
    ) {
        SpinningShape(
            MaterialShapes.Cookie9Sided,
            cs.primaryContainer,
            Modifier
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .size(230.dp),
            periodMillis = 20_000,
        )
        SpinningShape(
            MaterialShapes.Flower,
            cs.tertiaryContainer,
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = (-30).dp)
                .size(110.dp),
            periodMillis = 26_000,
            clockwise = false,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TapIllustration(
                Modifier.clearAndSetSemantics { contentDescription = "カードをスマートフォンの背面にかざすイラスト" }
            )
            Spacer(Modifier.size(8.dp))

            val (title, body) = when (nfcStatus) {
                NfcStatus.Unsupported -> "NFC に対応していません" to "この端末ではカードを読み取れません"
                NfcStatus.Disabled -> "NFC がオフになっています" to "設定から NFC をオンにしてください"
                NfcStatus.Ready -> "カードをタッチ" to "交通系 IC・社員証・NFC タグなど、どんなカードでも情報を表示します"
            }
            Text(
                title,
                color = cs.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.size(6.dp))
            Text(body, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.size(16.dp))

            when (nfcStatus) {
                NfcStatus.Disabled -> {
                    val source = remember { MutableInteractionSource() }
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = source,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .pressScale(source),
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("NFC 設定を開く", style = MaterialTheme.typography.labelLarge)
                    }
                }
                NfcStatus.Ready -> TonalPill(Icons.Rounded.Nfc, "待ち受け中", cs.primary, cs.surfaceContainerLowest)
                NfcStatus.Unsupported -> Unit
            }
        }
    }
}

/** スマホにカードが近づき、触れた瞬間に電波が広がるループアニメーション */
@Composable
private fun TapIllustration(modifier: Modifier = Modifier) {
    // 「動きを控えめにする」ではカードが触れた状態で止める
    val still = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "tap")
    // State のまま持ち、描画・配置フェーズでだけ読む（毎フレームの再コンポーズを避ける）
    val cardY = t.animateFloat(
        -70f, -70f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                -70f at 0
                10f at 800
                4f at 950
                6f at 1700
                -70f at 2600
            }
        ),
        label = "cardY",
    )
    val tilt = t.animateFloat(
        -18f, -18f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                -18f at 0
                -8f at 850
                -12f at 1000
                -12f at 1700
                -18f at 2600
            }
        ),
        label = "tilt",
    )
    val wave = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "wave")

    Box(modifier.size(width = 240.dp, height = 200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            if ((if (still) 6f else cardY.value) > 0f) {
                for (i in 0 until 2) {
                    val p = ((if (still) 0.3f else wave.value) + i / 2f) % 1f
                    drawCircle(
                        color = HelpRed.copy(alpha = (1f - p) * 0.5f),
                        radius = size.minDimension / 2f * (0.35f + p * 0.65f),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
            }
        }
        // スマホ
        Box(
            Modifier
                .size(width = 96.dp, height = 168.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Ink)
                // ダークモードでも背景に溶けないよう細い縁取り
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 34.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Icon(
                Icons.Rounded.Nfc,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
            )
            // 触れている間だけ赤いアイコンを重ねる
            Icon(
                Icons.Rounded.Nfc,
                contentDescription = null,
                tint = HelpRedGlow,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .graphicsLayer { alpha = if ((if (still) 6f else cardY.value) > 0f) 1f else 0f },
            )
        }
        // カード
        Box(
            Modifier
                .offset { IntOffset(34.dp.roundToPx(), (if (still) 6f else cardY.value).dp.roundToPx()) }
                .graphicsLayer { rotationZ = if (still) -12f else tilt.value }
                .size(width = 132.dp, height = 84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HelpRed)
                .padding(12.dp)
        ) {
            Icon(Icons.Rounded.Contactless, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(width = 60.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            )
            Icon(
                Icons.Rounded.CreditCard,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun StepTile(number: Int, polygon: RoundedPolygon, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val filled = number == 1
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainer)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(polygon.toShape())
                .background(if (filled) HelpRed else cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (filled) Color.White else cs.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// 読み取り中
// ---------------------------------------------------------------------------

@Composable
private fun LoadingPanel(id: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cs.surfaceContainerLow)
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContainedLoadingIndicator(
            Modifier.size(120.dp),
            indicatorColor = Color.White,
            containerColor = HelpRed,
        )
        Spacer(Modifier.size(20.dp))
        Text("読み取っています…", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
        Spacer(Modifier.size(6.dp))
        Text(id, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = cs.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------
// 読み取り結果（どんなカードでも表示）
// ---------------------------------------------------------------------------

@Composable
private fun CardResult(s: CardState.Result, onReset: () -> Unit) {
    val card = s.card
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var i = 0
        if (s.profile != null) {
            // 登録済みのカードは「誰か」が最重要なので、人と名前を一番上に出し、カードの絵は出さない
            ProfileHeader(
                profile = s.profile,
                caption = "このカードの登録者",
                modifier = Modifier
                    .appear(i++)
                    .semantics { liveRegion = LiveRegionMode.Assertive },
            )
            ProfileDetails(s.profile, startIndex = i)
            i += 3
        } else {
            // 未登録のカードは、読み取ったカードそのものの情報を主役にする
            DigitalCard(card, registered = false)
            i++
            UnregisteredNote(s.lookupNote, Modifier.appear(i++))
        }

        if (card.ndef.isNotEmpty()) {
            Section(Icons.AutoMirrored.Rounded.Notes, "カードに書き込まれた情報", MaterialShapes.Pill, Modifier.appear(i++)) {
                card.ndef.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        Section(Icons.Rounded.Memory, "カードの詳細", MaterialShapes.Cookie6Sided, Modifier.appear(i++)) {
            card.details.forEach { (label, value) -> DetailRow(label, value) }
            TechChips(card.techs)
        }

        NextCardButton(onReset, Modifier.appear(i))
    }
}

/** 読み取ったカード。裏返った状態からくるっと回って表を向く */
@Composable
private fun DigitalCard(card: CardInfo, registered: Boolean) {
    val flip = remember(card.id) { Animatable(-180f) }
    val lift = remember(card.id) { Animatable(0.8f) }
    LaunchedEffect(card.id) {
        lift.animateTo(1f, Springs.bouncy())
    }
    LaunchedEffect(card.id) {
        flip.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }
    val shape = RoundedCornerShape(28.dp)
    val spoken = buildString {
        append("読み取ったカード：${card.kind}。")
        card.balance?.let { append("残高 $it 円。") }
        append(if (registered) "登録済みです。" else "未登録です。")
    }
    val showFront = flip.value > -90f

    // 長押しでカード ID をコピー（DB 登録時に使える）
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val copyId = {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("カード ID", card.id))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Android 13 以降は OS 自身がコピーを知らせるので、重複しないよう 12 以下だけ表示
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            scope.launch { snackbar.showSnackbar("カード ID をコピーしました") }
        }
    }
    val source = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .pressScale(source, 0.97f)
            .graphicsLayer {
                rotationY = flip.value
                cameraDistance = 14f * density
                scaleX = lift.value
                scaleY = lift.value
            }
            .clip(shape)
            .background(if (showFront) HelpRed else HelpRedDeep)
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClick = {},
                onLongClick = copyId,
            )
            .clearAndSetSemantics {
                contentDescription = spoken
                onLongClick(label = "カード ID をコピー") { copyId(); true }
                liveRegion = LiveRegionMode.Assertive
            }
    ) {
        if (!showFront) {
            // 裏面：大きなヘルプマーク
            HelpMarkLogo(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 64.dp, height = 92.dp),
                background = Color.White.copy(alpha = 0.15f),
                foreground = Color.White.copy(alpha = 0.5f),
            )
            return@Box
        }

        SpinningShape(
            MaterialShapes.Cookie12Sided,
            Color.White.copy(alpha = 0.1f),
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .size(220.dp),
            periodMillis = 18_000,
        )
        SpinningShape(
            MaterialShapes.Clover4Leaf,
            HelpRedDeep.copy(alpha = 0.35f),
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 50.dp)
                .size(150.dp),
            periodMillis = 22_000,
            clockwise = false,
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IcChip()
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.Contactless, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    card.technology,
                    color = HelpRed,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(card.kind, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
            if (card.balance != null) {
                val shown = countUp(card.balance)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("¥", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        NumberFormat.getNumberInstance(Locale.JAPAN).format(shown),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 46.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "残高",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.id.chunked(4).joinToString("  "),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f),
                )
                if (registered) {
                    HelpMarkLogo(
                        Modifier.size(width = 22.dp, height = 32.dp),
                        background = Color.White,
                        foreground = HelpRed,
                    )
                }
            }
        }
    }
}

/** 金色の IC チップ */
@Composable
private fun IcChip() {
    Canvas(Modifier.size(width = 42.dp, height = 32.dp)) {
        val gold = Brush.linearGradient(listOf(Color(0xFFF8E3A1), Color(0xFFD4A94C), Color(0xFFF3D98B)))
        drawRoundRect(gold, cornerRadius = CornerRadius(6.dp.toPx()))
        val line = Color(0xFF9A7428).copy(alpha = 0.6f)
        val sw = 1.dp.toPx()
        drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), sw)
        drawLine(line, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), sw)
        drawLine(line, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), sw)
        drawRoundRect(
            line,
            topLeft = Offset(size.width * 0.33f, size.height * 0.25f),
            size = Size(size.width * 0.33f, size.height * 0.5f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(sw),
        )
    }
}

@Composable
private fun UnregisteredNote(note: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.tertiaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(Icons.Rounded.Info, MaterialShapes.Gem, container = cs.tertiary, tint = cs.onTertiary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("登録されていないカードです", style = MaterialTheme.typography.titleMedium, color = cs.onTertiaryContainer)
            Text(
                note ?: "カードから読み取れた情報を表示しています",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    polygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { heading() }) {
            ShapeBadge(icon, polygon, container = cs.primaryContainer, tint = cs.onPrimaryContainer, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        }
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = cs.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechChips(techs: List<String>) {
    if (techs.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        techs.forEachIndexed { n, tech ->
            Text(
                tech,
                style = MaterialTheme.typography.labelMedium,
                color = if (n % 2 == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .appear(n, key = tech)
                    .clip(CircleShape)
                    .background(
                        if (n % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun NextCardButton(onReset: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onReset()
        },
        shapes = ButtonDefaults.shapes(),
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressScale(source),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
    ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("別のカードを読む", style = MaterialTheme.typography.labelLarge)
    }
}
