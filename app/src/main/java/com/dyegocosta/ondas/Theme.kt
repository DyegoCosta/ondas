package com.dyegocosta.ondas

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class Palette(
    val ground: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val field: Color,
    val hot: Color,
    val onHot: Color,
    val hotRing1: Color,
    val hotRing2: Color,
    val cool: Color,
    val onCool: Color,
    val coolGround: Color,
    val danger: Color,
    val onDanger: Color,
    val tab: Color,
)

private val Light = Palette(
    ground = Color(0xFFF5F7F7), surface = Color(0xFFFFFFFF), ink = Color(0xFF12242B), muted = Color(0xFF526369),
    line = Color(0xFFD9E1E3), field = Color(0xFF8A9BA1), hot = Color(0xFFCF3D10), onHot = Color(0xFFFFFFFF),
    hotRing1 = Color(0x24CF3D10), hotRing2 = Color(0x12CF3D10), cool = Color(0xFF0F6E78), onCool = Color(0xFFFFFFFF),
    coolGround = Color(0xFFE3F1F2), danger = Color(0xFFB3300A), onDanger = Color(0xFFFFFFFF), tab = Color(0xFFE6ECED),
)

private val Dark = Palette(
    ground = Color(0xFF0B1418), surface = Color(0xFF15232A), ink = Color(0xFFEAF2F3), muted = Color(0xFFA3B5BA),
    line = Color(0xFF25363D), field = Color(0xFF3C5059), hot = Color(0xFFE2562A), onHot = Color(0xFF1A0A04),
    hotRing1 = Color(0x33E2562A), hotRing2 = Color(0x17E2562A), cool = Color(0xFF62CBD5), onCool = Color(0xFF06181B),
    coolGround = Color(0xFF0C2026), danger = Color(0xFFFF9C80), onDanger = Color(0xFF1A0A04), tab = Color(0xFF1B2C33),
)

val LocalPalette = staticCompositionLocalOf { Light }

@Composable
fun OndasTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(primary = p.cool, onPrimary = p.onCool, background = p.ground, surface = p.ground, onSurface = p.ink, onBackground = p.ink, error = p.danger)
    } else {
        lightColorScheme(primary = p.cool, onPrimary = p.onCool, background = p.ground, surface = p.ground, onSurface = p.ink, onBackground = p.ink, error = p.danger)
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
