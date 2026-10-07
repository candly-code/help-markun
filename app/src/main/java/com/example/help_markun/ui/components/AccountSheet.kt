@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.help_markun.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Diversity1
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.example.help_markun.data.FamilyEvent
import com.example.help_markun.data.FamilyEventKind
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.ui.AccountState
import com.example.help_markun.ui.AccountViewModel
import com.example.help_markun.ui.LocalAccount
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.HelpRedDeep
import com.example.help_markun.ui.theme.HelpRedGlow
import com.example.help_markun.ui.theme.LocalReduceMotion
import com.example.help_markun.ui.theme.Plum
import com.example.help_markun.ui.theme.Rose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** お祝いのはじけに使う色 */
private val BurstColors = listOf(HelpRed, HelpRedGlow, Rose, Plum, Color(0xFFFFC857))

/** アバターの色（メンバーごとに変えて見分けやすく） */
// 赤い背景の上にも置くので、赤そのものは使わない
private val AvatarColors = listOf(Plum, HelpRedDeep, Color(0xFFB4533C), Color(0xFF8A3FA0), Color(0xFF5B2A86))

private fun avatarColor(key: String) = AvatarColors[(key.hashCode() and 0x7FFFFFFF) % AvatarColors.size]

private fun initialOf(name: String) = name.trim().take(1).uppercase().ifEmpty { "?" }

// ---------------------------------------------------------------------------
// ヘッダーのアカウントボタン
// ---------------------------------------------------------------------------

/**
 * ヘッダー右上のアカウントボタン。
 * ログインなし：人のアイコンの丸。ログインすると赤いクッキー形に変形してイニシャルが出る。
 * 家族からの未読のお知らせがあると、波紋が広がり続けて数のバッジが弾む。
 */
@Composable
fun AccountButton(modifier: Modifier = Modifier) {
    val account = LocalAccount.current ?: return
    val state by account.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val cs = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current

    val signedIn = state.signedIn
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val progress by animateFloatAsState(if (signedIn) 1f else 0f, Springs.jelly(), label = "accountMorph")
    val container by animateColorAsState(if (signedIn) HelpRed else cs.secondaryContainer, label = "accountColor")
    val unread = state.unread

    val t = rememberInfiniteTransition(label = "account")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600)), label = "ripple")

    Box(
        modifier
            .size(56.dp)
            .pressScale(source, 0.85f)
            .drawBehind {
                // 未読があるときは外へ広がる波紋で気づかせる
                if (unread > 0 && !reduceMotion) {
                    val r = size.minDimension / 2f
                    drawCircle(HelpRed.copy(alpha = 0.35f * (1f - ripple)), radius = r * (1f + 0.45f * ripple))
                }
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                account.openSheet()
            }
            .semantics {
                contentDescription = when {
                    !signedIn -> "アカウント（ログインなしで使用中）"
                    unread > 0 -> "アカウントと家族、新しいお知らせ $unread 件"
                    else -> "アカウントと家族、${state.session?.name}"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MorphingShape(
            morph,
            progress = { progress },
            color = { container },
            rotation = { if (signedIn && !reduceMotion) spin else 0f },
            modifier = Modifier.matchParentSize(),
        )
        AnimatedContent(
            targetState = state.session?.name,
            transitionSpec = {
                (scaleIn(Springs.bouncy(), initialScale = 0.3f) + fadeIn())
                    .togetherWith(scaleOut(targetScale = 0.3f) + fadeOut())
            },
            label = "accountFace",
        ) { name ->
            if (name == null) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = cs.onSecondaryContainer)
            } else {
                Text(initialOf(name), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        // 未読数のバッジ（数が変わると転がる）
        AnimatedVisibility(
            visible = unread > 0,
            enter = scaleIn(Springs.bouncy()),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(HelpRedDeep),
                contentAlignment = Alignment.Center,
            ) {
                RollingNumber(minOf(unread, 9), MaterialTheme.typography.labelMedium, Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// アカウントのシート
// ---------------------------------------------------------------------------

@Composable
fun AccountSheetHost() {
    val account = LocalAccount.current ?: return
    val open by account.sheetOpen.collectAsStateWithLifecycle()
    if (open) AccountSheet(account)
}

private enum class Stage { Guest, NoFamily, Family }

@Composable
private fun AccountSheet(account: AccountViewModel) {
    val state by account.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val stage = when {
        !state.signedIn -> Stage.Guest
        state.family == null -> Stage.NoFamily
        else -> Stage.Family
    }
    // シートを開いてお知らせが見えたら既読にする
    LaunchedEffect(state.events.firstOrNull()?.id) { account.markRead() }
    // ログインや参加で画面が切り替わったら、キーボードを閉じて先頭から見せる
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    LaunchedEffect(stage) {
        focus.clearFocus()
        scroll.animateScrollTo(0)
    }

    ModalBottomSheet(
        onDismissRequest = account::closeSheet,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = cs.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (stage == Stage.Guest) "アカウント" else "アカウントと家族",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { heading() },
                )
                AnimatedVisibility(state.loading, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    LoadingIndicator(Modifier.size(36.dp))
                }
            }

            MessageLine(state)

            // 段階（ログインなし → ログイン済み → 家族グループ）が進むときは下から弾んで入れ替わる
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInVertically(Springs.smooth()) { if (forward) it / 3 else -it / 3 } +
                        scaleIn(Springs.bouncy(), initialScale = 0.92f) + fadeIn(tween(240)))
                        .togetherWith(
                            slideOutVertically(Springs.smooth()) { if (forward) -it / 5 else it / 5 } +
                                scaleOut(targetScale = 1.04f) + fadeOut(tween(160))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "accountStage",
            ) { s ->
                when (s) {
                    Stage.Guest -> GuestContent(state, account)
                    Stage.NoFamily -> NoFamilyContent(state, account)
                    Stage.Family -> FamilyContent(state, account)
                }
            }
        }
    }
}

/** 成功・エラーの 1 行。エラーのときは左右に小さく震えて知らせる */
@Composable
private fun MessageLine(state: AccountState) {
    val text = state.error ?: state.notice
    val isError = state.error != null
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.error) {
        if (state.error != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            shake.animateTo(0f, keyframes {
                durationMillis = 420
                -14f at 60
                12f at 130
                -8f at 200
                5f at 270
                0f at 420
            })
        }
    }
    AnimatedVisibility(
        visible = text != null,
        enter = expandVertically(Springs.smooth()) + fadeIn() + scaleIn(Springs.bouncy(), initialScale = 0.9f),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(if (isError) cs.errorContainer else cs.primaryContainer)
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isError) Icons.Rounded.PersonOff else Icons.Rounded.Check,
                contentDescription = null,
                tint = if (isError) cs.onErrorContainer else cs.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) cs.onErrorContainer else cs.onPrimaryContainer,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ログインなし
// ---------------------------------------------------------------------------

@Composable
private fun GuestContent(state: AccountState, account: AccountViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GuestHero(Modifier.appear(0))
        AuthForm(state, account, Modifier.appear(1))
    }
}

/** 「ログインなしでも使える」ことを最初に伝える赤いヒーロー。形がゆっくり漂う */
@Composable
private fun GuestHero(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(HelpRed)
    ) {
        FloatingShapes(Modifier.matchParentSize())
        Column(Modifier.padding(24.dp)) {
            Text("ログインなしでも使えます", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(12.dp))
            BenefitRow(Icons.Rounded.Diversity1, "家族で見守る", 0)
            BenefitRow(Icons.Rounded.NotificationsActive, "見つかったら家族に通知", 1)
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, index: Int) {
    Row(
        Modifier
            .appear(index + 1)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(icon, BadgeShapes[index + 2], container = Color.White, tint = HelpRed, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

/** 背景でゆっくり漂いながら回る形（装飾） */
@Composable
private fun FloatingShapes(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "float")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "b")
    Box(modifier) {
        SpinningShape(
            MaterialShapes.Flower,
            Color.White.copy(alpha = 0.09f),
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(60, if (reduceMotion) -40 else (-60 + 70 * a).toInt()) }
                .size(170.dp),
            periodMillis = 30_000,
        )
        SpinningShape(
            MaterialShapes.Heart,
            Color.White.copy(alpha = 0.08f),
            Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(if (reduceMotion) 0 else (-40 * b).toInt(), 50) }
                .size(110.dp),
            periodMillis = 22_000,
            clockwise = false,
        )
    }
}

@Composable
private fun AuthForm(state: AccountState, account: AccountViewModel, modifier: Modifier = Modifier) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = ログイン、1 = 新規登録
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val signUp = mode == 1
    val canSubmit = email.contains('@') && password.length >= 6 && (!signUp || name.isNotBlank()) && !state.busy
    val submit = {
        if (canSubmit) {
            if (signUp) account.signUp(email, password, name) else account.signIn(email, password)
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SegmentedPicker(listOf("ログイン", "新規登録"), mode, { mode = it })

        AnimatedVisibility(
            visible = signUp,
            enter = expandVertically(Springs.smooth()) + fadeIn() + slideInVertically(Springs.bouncy()) { -it / 2 },
            exit = shrinkVertically(Springs.smooth()) + fadeOut(),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                label = { Text("お名前") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("メールアドレス") },
            leadingIcon = { Icon(Icons.Rounded.Mail, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if (signUp) "パスワード（6 文字以上）" else "パスワード") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    AnimatedContent(showPassword, transitionSpec = {
                        (scaleIn(Springs.bouncy()) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                    }, label = "eye") { shown ->
                        Icon(
                            if (shown) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (shown) "パスワードを隠す" else "パスワードを表示",
                        )
                    }
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        BusyButton(
            label = if (signUp) "アカウントを作る" else "ログイン",
            busy = state.busy,
            enabled = canSubmit,
            onClick = submit,
        )
    }
}

/** 押すと中身がくるっと入れ替わって、処理中は形の変わるローディングになるボタン */
@Composable
private fun BusyButton(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressScale(source),
    ) {
        AnimatedContent(
            targetState = busy,
            transitionSpec = {
                (slideInVertically(Springs.bouncy()) { it } + fadeIn())
                    .togetherWith(slideOutVertically { -it } + fadeOut())
            },
            label = "busy",
        ) { b ->
            if (b) {
                LoadingIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let {
                        Icon(it, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ログイン済み・家族グループなし
// ---------------------------------------------------------------------------

@Composable
private fun NoFamilyContent(state: AccountState, account: AccountViewModel) {
    val session = state.session ?: return
    var familyName by rememberSaveable { mutableStateOf("${session.name}の家族") }
    var code by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileBanner(session.name, session.email, Modifier.appear(0))

        ActionCard(Icons.Rounded.FamilyRestroom, MaterialShapes.Cookie9Sided, "家族グループを作る", Modifier.appear(1)) {
            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it.take(20) },
                label = { Text("グループの名前") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            BusyButton("作る", state.busy, familyName.isNotBlank() && !state.busy, { account.createFamily(familyName) }, Icons.Rounded.FamilyRestroom)
        }

        ActionCard(Icons.Rounded.GroupAdd, MaterialShapes.Clover4Leaf, "招待コードで参加", Modifier.appear(2)) {
            CodeInput(code, { code = it }, onDone = { if (code.length == 6) account.joinFamily(code) })
            BusyButton("参加する", state.busy, code.length == 6 && !state.busy, { account.joinFamily(code) }, Icons.Rounded.GroupAdd)
        }

        SignOutButton(account, Modifier.appear(3))
    }
}

@Composable
private fun ProfileBanner(name: String, email: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    // ログインした瞬間、アバターからお祝いがはじける
    var burst by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { burst = 1 }
    Box(modifier) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopAvatar(name, name, 64.dp, 0)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("ログイン中", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
            Text(name, style = MaterialTheme.typography.titleLarge, color = cs.onPrimaryContainer)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Burst(burst, BurstColors, Modifier.matchParentSize())
    }
}

/** 回転しながら弾んで現れる、形付きのアバター */
@Composable
private fun PopAvatar(key: String, name: String, size: androidx.compose.ui.unit.Dp, index: Int, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val spin = remember { Animatable(if (reduceMotion) 0f else -120f) }
    LaunchedEffect(Unit) {
        delay(index * 90L)
        launch { spin.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        pop.animateTo(1f, Springs.bouncy())
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                rotationZ = spin.value
            }
            .clip(shapeFor(key).toShape())
            .background(avatarColor(key)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialOf(name),
            color = Color.White,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    polygon: androidx.graphics.shapes.RoundedPolygon,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(icon, polygon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        content()
    }
}

/**
 * 6 マスの招待コード入力。入力した文字はマスの中でぽんと弾み、次に入れるマスの枠が脈打つ。
 * 小文字で入れても大文字になる。
 */
@Composable
private fun CodeInput(value: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "caret")
    val caret by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "caretAlpha")
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            onChange(raw.uppercase().filter { it.isLetterOrDigit() && it.code < 128 }.take(6))
        },
        singleLine = true,
        // 予測変換が入ると大文字への置き換えと食い違って文字が消えるので、変換なしの入力にする
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "招待コード、6 文字中 ${value.length} 文字入力済み" },
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        val ch = value.getOrNull(i)
                        val active = i == value.length
                        val border by animateColorAsState(
                            when {
                                active -> cs.primary
                                ch != null -> cs.primary.copy(alpha = 0.5f)
                                else -> cs.outlineVariant
                            },
                            label = "codeBorder",
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (ch != null) cs.primaryContainer else cs.surfaceContainerHighest)
                                .border(
                                    if (active) 2.5.dp else 1.5.dp,
                                    if (active) border.copy(alpha = caret) else border,
                                    RoundedCornerShape(14.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = ch,
                                transitionSpec = {
                                    (slideInVertically(Springs.bouncy()) { -it } + scaleIn(Springs.bouncy(), initialScale = 0.4f) + fadeIn())
                                        .togetherWith(scaleOut(targetScale = 0.4f) + fadeOut())
                                },
                                label = "codeChar",
                            ) { c ->
                                Text(
                                    c?.toString() ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = cs.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
                // 実際の入力欄は見えないように重ねる（タップでキーボードが出る）
                Box(Modifier.matchParentSize().graphicsLayer { alpha = 0f }) { inner() }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// 家族グループ
// ---------------------------------------------------------------------------

@Composable
private fun FamilyContent(state: AccountState, account: AccountViewModel) {
    val family = state.family ?: return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FamilyHero(state, Modifier.appear(0))
        InviteCard(family.name, family.inviteCode, Modifier.appear(1))
        WatchedCard(state, account, Modifier.appear(2))
        EventsCard(state, Modifier.appear(3))
        FamilyFooter(account, Modifier.appear(4))
    }
}

@Composable
private fun FamilyHero(state: AccountState, modifier: Modifier = Modifier) {
    val family = state.family ?: return
    var burst by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(family.id) { burst = family.id.hashCode() }
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(HelpRed)
    ) {
        FloatingShapes(Modifier.matchParentSize())
        Burst(burst, listOf(Color.White, Rose, Color(0xFFFFC857)), Modifier.matchParentSize())
        Column(Modifier.padding(24.dp)) {
            Text("家族グループ", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Text(family.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.size(16.dp))
            // メンバーは少しずつ重なって並び、順番に回転しながら弾んで現れる
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                family.members.take(6).forEachIndexed { i, m ->
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HelpRed)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PopAvatar(m.userId, m.name, 46.dp, i)
                    }
                }
                Spacer(Modifier.width(26.dp))
                Text(
                    "${family.members.size} 人",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** 招待コード。1 文字ずつ裏返りながら現れ、コピーするとボタンがチェックに変わる */
@Composable
private fun InviteCard(familyName: String, code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("招待コード", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Row(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = "招待コード " + code.toList().joinToString("、") },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            code.forEachIndexed { i, c -> FlipChar(c, i, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val copySource = remember { MutableInteractionSource() }
            FilledTonalButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("招待コード", code))
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    copied = true
                },
                interactionSource = copySource,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .pressScale(copySource),
            ) {
                AnimatedContent(copied, transitionSpec = {
                    (scaleIn(Springs.bouncy(), initialScale = 0.5f) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                }, label = "copy") { done ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (done) "コピーしました" else "コピー", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            val shareSource = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    val text = "ヘルプマーくんの家族グループ「$familyName」に参加してください。\n" +
                        "アプリの「アカウント」→「招待コードで参加」に、次のコードを入力します。\n招待コード：$code"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, "招待コードを送る"))
                },
                interactionSource = shareSource,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .pressScale(shareSource),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("送る", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FlipChar(c: Char, index: Int, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val flip = remember { Animatable(if (reduceMotion) 0f else 90f) }
    LaunchedEffect(Unit) {
        delay(250L + index * 80L)
        flip.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier
            .aspectRatio(0.82f)
            .graphicsLayer {
                rotationX = flip.value
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(14.dp))
            .background(HelpRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(c.toString(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun WatchedCard(state: AccountState, account: AccountViewModel, modifier: Modifier = Modifier) {
    val family = state.family ?: return
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainer)
            .padding(vertical = 12.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(Icons.Rounded.Radar, MaterialShapes.Cookie12Sided, cs.primaryContainer, cs.onPrimaryContainer, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text("見守っている方", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        }
        if (family.watched.isEmpty()) {
            Text(
                "まだいません",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        family.watched.forEachIndexed { i, p ->
            var confirm by remember(p.profileId) { mutableStateOf(false) }
            Row(
                Modifier
                    .appear(i, key = p.profileId)
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopAvatar(p.profileId, p.name, 48.dp, i)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    p.disability?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
                }
                // 外すときは 2 段階（誤タップで外れないように）
                AnimatedContent(confirm, transitionSpec = {
                    (scaleIn(Springs.bouncy(), initialScale = 0.7f) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                        .using(SizeTransform(clip = false))
                }, label = "unwatch") { c ->
                    if (c) {
                        // アプリからは追加できないので、外すと元に戻せないことを伝える
                        Button(
                            onClick = { account.unwatch(p.profileId, p.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
                        ) { Text("外す（戻せません）", style = MaterialTheme.typography.labelLarge) }
                    } else {
                        TextButton(onClick = { confirm = true }) { Text("見守りを外す", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}

/**
 * 家族へのお知らせの一覧。縦の線が上から伸びて、各お知らせの点がぽんと付く。
 * 未読のお知らせは赤く縁取りして、点が脈打つ。
 */
@Composable
private fun EventsCard(state: AccountState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val line = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(state.events.size) { line.animateTo(1f, tween(900)) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(Icons.Rounded.NotificationsActive, MaterialShapes.Sunny, cs.primaryContainer, cs.onPrimaryContainer, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text("家族へのお知らせ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.size(6.dp))
        if (state.events.isEmpty()) {
            Text(
                "まだありません",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            return@Column
        }
        val lineColor = cs.outlineVariant
        Column(
            Modifier.drawBehind {
                // 左の時間軸の線（上から下へ伸びる）
                val x = 22.dp.toPx()
                drawLine(lineColor, Offset(x, 24.dp.toPx()), Offset(x, 24.dp.toPx() + (size.height - 48.dp.toPx()) * line.value), strokeWidth = 3.dp.toPx())
            },
        ) {
            state.events.take(20).forEachIndexed { i, e ->
                EventRow(e, unread = e.id > state.lastReadId && e.reporter != state.session?.userId, index = i)
            }
        }
    }
}

@Composable
private fun EventRow(event: FamilyEvent, unread: Boolean, index: Int) {
    val cs = MaterialTheme.colorScheme
    val (icon, polygon) = when (event.kind) {
        FamilyEventKind.Nearby -> Icons.Rounded.Radar to MaterialShapes.Cookie9Sided
        FamilyEventKind.Card -> Icons.Rounded.Contactless to MaterialShapes.Pill
        FamilyEventKind.Message -> Icons.AutoMirrored.Rounded.Chat to MaterialShapes.Heart
    }
    val t = rememberInfiniteTransition(label = "unread")
    val beat by t.animateFloat(1f, 1.14f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "beat")
    val reduceMotion = LocalReduceMotion.current
    Row(
        Modifier
            .appear(minOf(index, 6), key = event.id)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        ShapeBadge(
            icon,
            polygon,
            container = if (event.kind == FamilyEventKind.Message || unread) HelpRed else cs.secondaryContainer,
            tint = if (event.kind == FamilyEventKind.Message || unread) Color.White else cs.onSecondaryContainer,
            size = 44.dp,
            modifier = Modifier.graphicsLayer {
                val s = if (unread && !reduceMotion) beat else 1f
                scaleX = s
                scaleY = s
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(if (unread) cs.primaryContainer else cs.surfaceContainerHigh)
                .then(if (unread) Modifier.border(2.dp, HelpRed, MaterialTheme.shapes.medium) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    relativeTime(event.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) HelpRed else cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (unread) Text("NEW", style = MaterialTheme.typography.labelMedium, color = HelpRed)
            }
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            event.body?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

private fun relativeTime(time: Long): String {
    if (time <= 0) return ""
    val diff = (System.currentTimeMillis() - time).coerceAtLeast(0)
    val min = diff / 60_000
    return when {
        min < 1 -> "たった今"
        min < 60 -> "$min 分前"
        min < 24 * 60 -> "${min / 60} 時間前"
        else -> SimpleDateFormat("M月d日 H:mm", Locale.JAPAN).format(Date(time))
    }
}

@Composable
private fun FamilyFooter(account: AccountViewModel, modifier: Modifier = Modifier) {
    var confirmLeave by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedContent(confirmLeave, transitionSpec = {
            (expandVertically(Springs.smooth()) + fadeIn() + scaleIn(Springs.bouncy(), initialScale = 0.9f))
                .togetherWith(fadeOut() + scaleOut(targetScale = 0.95f))
                .using(SizeTransform(clip = false))
        }, label = "leave") { c ->
            if (c) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(cs.errorContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "お知らせが届かなくなります",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onErrorContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { confirmLeave = false }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                            Text("やめる", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = {
                                confirmLeave = false
                                account.leaveFamily()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) { Text("抜ける", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { confirmLeave = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("家族グループから抜ける", style = MaterialTheme.typography.labelLarge) }
            }
        }
        SignOutButton(account)
    }
}

@Composable
private fun SignOutButton(account: AccountViewModel, modifier: Modifier = Modifier) {
    TextButton(
        onClick = account::signOut,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("ログアウト", style = MaterialTheme.typography.labelLarge)
    }
}
