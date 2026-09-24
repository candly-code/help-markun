package com.example.help_markun.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.help_markun.data.HelpProfile
import com.example.help_markun.ui.theme.HelpRed
import com.example.help_markun.ui.theme.HelpRedDeep

/** プロフィール上部：赤いコンテナ＋回る装飾＋クッキー形のアバター */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileHeader(
    profile: HelpProfile,
    caption: String,
    modifier: Modifier = Modifier,
    container: Color = HelpRed,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
    ) {
        SpinningShape(
            MaterialShapes.SoftBurst,
            Color.White.copy(alpha = 0.1f),
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp)
                .size(200.dp),
        )
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(profile.initials, color = container, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(caption, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                Text(
                    profile.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                profile.disability?.let {
                    Spacer(Modifier.size(6.dp))
                    TonalPill(Icons.AutoMirrored.Rounded.Accessible, it, Color.White, Color.White.copy(alpha = 0.2f))
                }
            }
        }
    }
}

/** プロフィール本文：お願いごと・特性・医療情報・緊急連絡先（順番に弾んで現れる） */
@Composable
fun ProfileDetails(profile: HelpProfile, modifier: Modifier = Modifier, startIndex: Int = 0) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var i = startIndex
        profile.helpRequest?.let { HelpRequestCallout(it, Modifier.appear(i++)) }

        val rows = listOfNotNull(
            profile.communication?.let { InfoItem(Icons.AutoMirrored.Rounded.Chat, "コミュニケーション", it) },
            profile.bloodType?.let { InfoItem(Icons.Rounded.Opacity, "血液型", it) },
            profile.allergies?.let { InfoItem(Icons.Rounded.NoFood, "アレルギー", it) },
            profile.medications?.let { InfoItem(Icons.Rounded.Medication, "服用中の薬", it) },
            profile.medicalNotes?.let { InfoItem(Icons.Rounded.MedicalServices, "医療メモ", it) },
        )
        if (rows.isNotEmpty()) {
            Column(
                Modifier
                    .appear(i++)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 8.dp),
            ) {
                rows.forEachIndexed { n, item -> InfoRow(item, n) }
            }
        }

        if (profile.emergencyContactPhone != null) {
            EmergencyContact(profile.emergencyContactName, profile.emergencyContactPhone, Modifier.appear(i))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HelpRequestCallout(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
    ) {
        ShapeBadge(
            Icons.Rounded.VolunteerActivism,
            MaterialShapes.Sunny,
            container = HelpRed,
            tint = Color.White,
            size = 52.dp,
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                "お手伝いしてほしいこと",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.size(4.dp))
            Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private data class InfoItem(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun InfoRow(item: InfoItem, index: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val even = index % 2 == 0
        ShapeBadge(
            item.icon,
            BadgeShapes[index % BadgeShapes.size],
            container = if (even) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            tint = if (even) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmergencyContact(name: String?, phone: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(HelpRedDeep)
            .padding(20.dp)
    ) {
        Text("緊急連絡先", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(name ?: "連絡先", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(phone, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(14.dp))
        Button(
            onClick = {
                // 発信画面を開くだけ（自動では発信しない）
                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
            },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .pressScale(source),
            interactionSource = source,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HelpRedDeep),
        ) {
            Icon(Icons.Rounded.Call, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("${name ?: "緊急連絡先"}に電話する", style = MaterialTheme.typography.labelLarge)
        }
    }
}

