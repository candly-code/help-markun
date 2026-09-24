package com.example.help_markun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

// 日本語は文節で改行し、行の長さをそろえる（「す」だけが次の行に落ちるような改行を防ぐ）
private val JaLineBreak = LineBreak(
    strategy = LineBreak.Strategy.Balanced,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase,
)

// 読みやすさ優先で標準より一回り大きく、行間も広めにとる
val Typography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 42.sp, lineBreak = JaLineBreak),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp, lineBreak = JaLineBreak),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, lineBreak = JaLineBreak),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, lineBreak = JaLineBreak),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp, lineBreak = JaLineBreak),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp, lineBreak = JaLineBreak),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp, lineBreak = JaLineBreak),
    // ラベルは数字の幅をそろえ（tnum）、数が変わっても文字がガタつかないようにする
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp, fontFeatureSettings = "tnum"),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.3.sp, fontFeatureSettings = "tnum"),
)
