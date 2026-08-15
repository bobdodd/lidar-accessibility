package com.bobdodd.lidaraccessibility.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale intentionally capped at a 3:1 ratio between the largest
 * and smallest text in a single view, to protect users of screen
 * magnifiers (Bob's CNIB constraint — see docs/architecture.md).
 *
 * Font families use platform defaults for v1. Atkinson Hyperlegible
 * (body) and Source Serif 4 (headings) can be added as bundled fonts
 * later — the theme reserves the FontFamily slots so nothing else
 * needs to change.
 */
private val BodyFamily = FontFamily.Default      // TODO: Atkinson Hyperlegible
private val HeadingFamily = FontFamily.Serif     // TODO: Source Serif 4

// Ratio anchors: base = 16 sp, top = 48 sp -> exactly 3:1.
private val Body = TextStyle(fontFamily = BodyFamily, fontSize = 16.sp)
private val BodyLarge = TextStyle(fontFamily = BodyFamily, fontSize = 20.sp)
private val Title = TextStyle(fontFamily = HeadingFamily, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
private val Display = TextStyle(fontFamily = HeadingFamily, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)

val LidarAccessibilityTypography = Typography(
    bodyMedium = Body,
    bodyLarge = BodyLarge,
    titleLarge = Title,
    headlineLarge = Display,
    displayLarge = Display,
)
