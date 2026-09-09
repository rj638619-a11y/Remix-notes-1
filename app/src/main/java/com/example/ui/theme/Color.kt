package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base Soft Minimalist Colors
val BgLight = Color(0xFFF8F8F6)
val BgLightGradientEnd = Color(0xFFEFF0EC)
val CardLight = Color(0xFFFFFFFF)
val TxLight = Color(0xFF1C1E21)
val Tx2Light = Color(0xFF70757A)
val Tx3Light = Color(0xFFA0A5AA)
val HairLight = Color(0x18000000)

// Pastel Accents
val AccentBlue = Color(0xFF3872E0)
val AccentBlueBg = Color(0xFFEBF2FD)
val AccentMint = Color(0xFF2EB85C)
val AccentMintBg = Color(0xFFE8F7EC)
val AccentLavender = Color(0xFF9652DE)
val AccentLavenderBg = Color(0xFFF4ECFC)
val AccentOrange = Color(0xFFFF8B19)
val AccentOrangeBg = Color(0xFFFFF2E6)
val AccentPdfRed = Color(0xFFEB4444)
val AccentPdfRedBg = Color(0xFFFDEBED)
val AccentFavYellow = Color(0xFFF0A500)
val AccentFavYellowBg = Color(0xFFFFF7E6)

val ChipOnBgLight = Color(0x1A3872E0)
val ChipOnTxLight = Color(0xFF1E5BBF)
val GlassLight = Color(0xCCFFFFFF) // 80% translucent white
val GlassBrdLight = Color(0x66FFFFFF) // 40% crisp white highlight border
val GlassHiLight = Color(0xE6FFFFFF)
val FieldLight = Color(0x0D000000)
val MarkLight = Color(0xFFFFF1C2)
val DangerColorLight = Color(0xFFEB4444)
val ShadowLight = Color(0x0D000000)

// Dark Charcoal Base
val BgDark = Color(0xFF0F1012)
val BgDarkGradientEnd = Color(0xFF181A1D)
val CardDark = Color(0xFF1C1E22)
val TxDark = Color(0xFFF2F4F7)
val Tx2Dark = Color(0xFFA0A5AA)
val Tx3Dark = Color(0xFF686D74)
val HairDark = Color(0x22FFFFFF)

val GlassDark = Color(0xB81C1E22)
val GlassBrdDark = Color(0x26FFFFFF)
val GlassHiDark = Color(0x33FFFFFF)
val FieldDark = Color(0x1AFFFFFF)
val MarkDark = Color(0xFF5C4708)
val DangerColorDark = Color(0xFFFF5C5C)
val ShadowDark = Color(0x80000000)

@Immutable
data class GlassCustomColors(
    val bg: Color,
    val bgGradientEnd: Color,
    val card: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val hairline: Color,
    val accent: Color,
    val accentSecondary: Color,
    val chipOnBg: Color,
    val chipOnTx: Color,
    val glass: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val field: Color,
    val mark: Color,
    val danger: Color,
    val shadow: Color,
    val isDark: Boolean,
    val isReduced: Boolean,
    // Pastel Accents
    val pastelBlue: Color = AccentBlue,
    val pastelBlueBg: Color = AccentBlueBg,
    val pastelMint: Color = AccentMint,
    val pastelMintBg: Color = AccentMintBg,
    val pastelLavender: Color = AccentLavender,
    val pastelLavenderBg: Color = AccentLavenderBg,
    val pastelOrange: Color = AccentOrange,
    val pastelOrangeBg: Color = AccentOrangeBg,
    val pastelPdfRed: Color = AccentPdfRed,
    val pastelPdfRedBg: Color = AccentPdfRedBg,
    val pastelFavYellow: Color = AccentFavYellow,
    val pastelFavYellowBg: Color = AccentFavYellowBg
)

val LightGlassColors = GlassCustomColors(
    bg = BgLight,
    bgGradientEnd = BgLightGradientEnd,
    card = CardLight,
    text = TxLight,
    textSecondary = Tx2Light,
    textTertiary = Tx3Light,
    hairline = HairLight,
    accent = AccentBlue,
    accentSecondary = AccentFavYellow,
    chipOnBg = ChipOnBgLight,
    chipOnTx = ChipOnTxLight,
    glass = GlassLight,
    glassBorder = GlassBrdLight,
    glassHighlight = GlassHiLight,
    field = FieldLight,
    mark = MarkLight,
    danger = DangerColorLight,
    shadow = ShadowLight,
    isDark = false,
    isReduced = false
)

val DarkGlassColors = GlassCustomColors(
    bg = BgDark,
    bgGradientEnd = BgDarkGradientEnd,
    card = CardDark,
    text = TxDark,
    textSecondary = Tx2Dark,
    textTertiary = Tx3Dark,
    hairline = HairDark,
    accent = Color(0xFF6BA0FA),
    accentSecondary = AccentFavYellow,
    chipOnBg = Color(0x336BA0FA),
    chipOnTx = Color(0xFF92BBFF),
    glass = GlassDark,
    glassBorder = GlassBrdDark,
    glassHighlight = GlassHiDark,
    field = FieldDark,
    mark = MarkDark,
    danger = DangerColorDark,
    shadow = ShadowDark,
    isDark = true,
    isReduced = false,
    pastelBlueBg = Color(0x2E3872E0),
    pastelMintBg = Color(0x2E2EB85C),
    pastelLavenderBg = Color(0x2E9652DE),
    pastelOrangeBg = Color(0x2EFF8B19),
    pastelPdfRedBg = Color(0x2EEB4444),
    pastelFavYellowBg = Color(0x2EF0A500)
)

// Matcha Mint Theme
val MatchaGlassColors = GlassCustomColors(
    bg = Color(0xFFF1F6F2),
    bgGradientEnd = Color(0xFFE4EEE6),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF1B281E),
    textSecondary = Color(0xFF5E7262),
    textTertiary = Color(0xFF91A495),
    hairline = Color(0x1A2E7D32),
    accent = Color(0xFF2E7D32),
    accentSecondary = Color(0xFF81C784),
    chipOnBg = Color(0x262E7D32),
    chipOnTx = Color(0xFF1B5E20),
    glass = Color(0xD9FFFFFF),
    glassBorder = Color(0x80FFFFFF),
    glassHighlight = Color(0xF2FFFFFF),
    field = Color(0x0D2E7D32),
    mark = Color(0xFFE8F5E9),
    danger = DangerColorLight,
    shadow = ShadowLight,
    isDark = false,
    isReduced = false,
    pastelBlue = Color(0xFF2E7D32),
    pastelBlueBg = Color(0xFFE2F0E5)
)

// Lavender Lilac Theme
val LavenderGlassColors = GlassCustomColors(
    bg = Color(0xFFF7F4FB),
    bgGradientEnd = Color(0xFFEFE8F7),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF241A30),
    textSecondary = Color(0xFF726187),
    textTertiary = Color(0xFFA597B8),
    hairline = Color(0x1A7E39C4),
    accent = Color(0xFF7E39C4),
    accentSecondary = Color(0xFFBA68C8),
    chipOnBg = Color(0x267E39C4),
    chipOnTx = Color(0xFF5A189A),
    glass = Color(0xD9FFFFFF),
    glassBorder = Color(0x80FFFFFF),
    glassHighlight = Color(0xF2FFFFFF),
    field = Color(0x0D7E39C4),
    mark = Color(0xFFF3E5F5),
    danger = DangerColorLight,
    shadow = ShadowLight,
    isDark = false,
    isReduced = false,
    pastelLavender = Color(0xFF7E39C4),
    pastelLavenderBg = Color(0xFFEDE4F8)
)

// Sepia Warm Paper Theme
val SepiaGlassColors = GlassCustomColors(
    bg = Color(0xFFFAF5EB),
    bgGradientEnd = Color(0xFFF0E7D5),
    card = Color(0xFFFFFDF9),
    text = Color(0xFF2C2416),
    textSecondary = Color(0xFF786950),
    textTertiary = Color(0xFFAAA08E),
    hairline = Color(0x1FA27632),
    accent = Color(0xFFB86B14),
    accentSecondary = Color(0xFFD49B45),
    chipOnBg = Color(0x26B86B14),
    chipOnTx = Color(0xFF804505),
    glass = Color(0xDCFFFDF9),
    glassBorder = Color(0x80FFFFFF),
    glassHighlight = Color(0xF2FFFFFF),
    field = Color(0x0FA27632),
    mark = Color(0xFFFFF1C2),
    danger = DangerColorLight,
    shadow = ShadowLight,
    isDark = false,
    isReduced = false,
    pastelOrange = Color(0xFFB86B14),
    pastelOrangeBg = Color(0xFFFCECDA)
)

// Ocean Deep Midnight Theme
val OceanGlassColors = GlassCustomColors(
    bg = Color(0xFF080F1A),
    bgGradientEnd = Color(0xFF0F1C30),
    card = Color(0xFF101E33),
    text = Color(0xFFE8F1FA),
    textSecondary = Color(0xFF8FA9C8),
    textTertiary = Color(0xFF546E8F),
    hairline = Color(0x2638BDF8),
    accent = Color(0xFF38BDF8),
    accentSecondary = Color(0xFF0284C7),
    chipOnBg = Color(0x3338BDF8),
    chipOnTx = Color(0xFF7DD3FC),
    glass = Color(0xB8101E33),
    glassBorder = Color(0x3338BDF8),
    glassHighlight = Color(0x4D38BDF8),
    field = Color(0x1F38BDF8),
    mark = Color(0xFF075985),
    danger = DangerColorDark,
    shadow = ShadowDark,
    isDark = true,
    isReduced = false,
    pastelBlue = Color(0xFF38BDF8),
    pastelBlueBg = Color(0x2E38BDF8)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }
