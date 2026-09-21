package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Raix v1.8.0 Design System — Paleta Oficial
// WCAG AA: 4.5:1 texto normal, 3:1 texto grande
// =========================================================================

// Core Backgrounds
val RaixBackground = Color(0xFF0B1325)         // Fundo principal
val RaixSurface = Color(0xFF1E2432)            // Superficies / Cards / Headers
val RaixSurfaceElevated = Color(0xFF252A3A)    // Superficie elevada (dialogs, menus)

// Text
val RaixTextPrimary = Color(0xFFF5F7FA)        // Texto principal
val RaixTextSecondary = Color(0xFF8A93A6)       // Texto secundario / metadados

// Action / Status
val RaixActionPrimary = Color(0xFF00E676)       // Acao primaria / status ativo / sucesso
val RaixAccentPremium = Color(0xFFD4AF37)       // Acento premium (Escritorio/Private, selo verificacao, seguranca)
val RaixError = Color(0xFFE5484D)               // Erro (nunca laranja/amarelo)
val RaixErrorContainer = Color(0xFF2C1316)      // Container de erro

// Borders
val RaixBorder = Color(0xFF2A2F3E)              // Borda sutil
val RaixBorderAccent = Color(0x33D4AF37)        // Borda com acento dourado (15% opacity)

// Chat Bubbles
val RaixBubbleUser = Color(0xFF1A3A2A)          // Esverdeado sutil para usuario
val RaixBubbleUserBorder = Color(0xFF1E4A32)    // Borda do bubble usuario
val RaixBubbleContact = Color(0xFF1E2432)       // Superficie para contato
val RaixBubbleContactBorder = Color(0xFF2A2F3E) // Borda do bubble contato

// Avatar
val RaixAvatarBg = Color(0xFF1A1F2E)            // Fundo do avatar

// =========================================================================
// Backward-Compatibility Aliases (Immersive/Obsidian/Stealth series)
// Gradual migration: screens should prefer RaixXxx tokens.
// =========================================================================
val ObsidianBlack = RaixBackground
val ObsidianSurface = RaixSurface
val ObsidianCard = RaixSurface
val ObsidianCardElevated = RaixSurfaceElevated
val ObsidianBorder = RaixBorderAccent
val ObsidianBorderSubtle = Color(0x18D4AF37)

val TitaniumPrimary = RaixAccentPremium
val TitaniumSecondary = Color(0xFFE2C873)
val TitaniumMuted = RaixTextSecondary

val SecurityEmerald = RaixActionPrimary
val SecurityEmeraldContainer = Color(0xFF0D2818)
val EmberFlame = RaixError                      // v1.8.0: remapped from orange to error red
val EmberFlameContainer = RaixErrorContainer
val IncinerateCrimson = RaixError
val IncinerateCrimsonBg = RaixErrorContainer

// Chat Bubbles (compat)
val BubbleUser = RaixBubbleUser
val BubbleUserBorder = RaixBubbleUserBorder
val BubbleContact = RaixBubbleContact
val BubbleContactBorder = RaixBubbleContactBorder

// Immersive UI Palette (compat aliases)
val ImmersiveSurface = RaixBackground
val ImmersiveHeader = RaixSurface
val ImmersiveCard = RaixSurface
val ImmersiveCardVariant = RaixSurfaceElevated
val ImmersivePrimary = RaixActionPrimary
val ImmersivePrimaryContainer = RaixSurfaceElevated
val ImmersiveOnPrimary = RaixBackground
val ImmersiveSecondary = TitaniumSecondary
val ImmersiveOutline = RaixBorder
val ImmersiveOnSurface = RaixTextPrimary
val ImmersiveMuted = RaixTextSecondary
val ImmersiveMutedLight = TitaniumSecondary
val ImmersiveExpiring = RaixError
val ImmersiveOnlineGreen = RaixActionPrimary
val ImmersiveAvatarDeep = RaixAvatarBg

// Legacy mappings
val StealthBlack = ImmersiveSurface
val StealthDarkSurface = ImmersiveHeader
val StealthCardSurface = ImmersiveCard
val StealthCardSurfaceLight = ImmersiveCardVariant

// Accents (compat)
val ElectricCyan = RaixActionPrimary
val ElectricCyanDim = TitaniumSecondary
val NeonEmerald = RaixActionPrimary
val EmberOrange = RaixError
val IncinerateRed = RaixError
val GhostPurple = RaixActionPrimary

// Text & Neutral Colors (compat)
val TextPrimaryDark = RaixTextPrimary
val TextSecondaryDark = TitaniumSecondary
val TextMutedDark = RaixTextSecondary
val BorderSubtleDark = RaixBorder
val GlowOverlay = Color(0x1AF5F7FA)

// Pmsg Brand (compat)
val PmsgMatteDark = RaixBackground
val PmsgPlatinum = RaixTextPrimary
val PmsgEvasionGray = RaixTextSecondary
val PmsgBorder = RaixBorder
val PmsgCardBg = RaixSurface
