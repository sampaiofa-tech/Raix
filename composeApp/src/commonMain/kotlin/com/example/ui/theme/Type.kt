package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import raix.composeapp.generated.resources.Res
import raix.composeapp.generated.resources.inter_regular
import raix.composeapp.generated.resources.inter_medium
import androidx.compose.runtime.Composable

/**
 * Raix v1.8.0 Typography Scale
 *
 * Body/UI: Inter (400 Normal, 500 Medium) -- nunca >600 no corpo
 * Technical (safety number, codes, mnemonic): JetBrains Mono (via FontFamily.Monospace)
 *
 * Scale:
 *   Titulo:    20-22sp / 500
 *   Subtitulo: 16-17sp / 500
 *   Base:      14-15sp / 400
 *   Metadados: 12sp    / 400
 *   Codigo:    14sp    / 400 Monospace
 *
 * Hierarchy by size and weight, never by color.
 */
@Composable
fun getRaixTypography(): Typography {
    val InterFontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium)
    )

    return Typography(
        // Titulo grande (22sp / Medium)
        headlineLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        ),
        // Titulo (22sp / Medium)
        headlineMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.25).sp
        ),
        // Titulo (20sp / Medium)
        titleLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        // Subtitulo (16sp / Medium)
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        // Subtitulo pequeno (14sp / Medium)
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        // Texto base (15sp / Normal)
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        // Texto base (14sp / Normal)
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        // Metadados (12sp / Normal)
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        // Label UI (14sp / Medium)
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        // Codigo / numero de seguranca (14sp / Normal / Monospace)
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp
        ),
        // Metadados tech (12sp / Normal / Monospace)
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
