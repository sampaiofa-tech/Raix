package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Raix v1.9.0 Design System — Paleta Oficial (WhatsApp layout / Raix cores)
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
val RaixPremiumGold = RaixAccentPremium          // Alias retro-compativel
val RaixError = Color(0xFFE5484D)               // Erro (nunca laranja/amarelo)
val RaixErrorContainer = Color(0xFF2C1316)      // Container de erro

// Borders
val RaixBorder = Color(0xFF2A2F3E)              // Borda sutil
val RaixBorderAccent = Color(0x33D4AF37)        // Borda com acento dourado (15% opacity)

// Chat Bubbles (v1.9.0 WhatsApp-style)
val RaixBubbleSent = Color(0xFF0B5B41)            // Bolha enviada (verde escuro)
val RaixBubbleReceived = Color(0xFF1E2432)         // Bolha recebida (superficie)
val RaixBubbleUser = RaixBubbleSent                // Compat alias
val RaixBubbleUserBorder = Color(0xFF0D6B4D)       // Borda bolha enviada
val RaixBubbleContact = RaixBubbleReceived          // Compat alias
val RaixBubbleContactBorder = Color(0xFF2A2F3E)    // Borda bolha recebida

// Receipts (v1.9.0)
val RaixReceiptSent = Color(0xFF8A93A6)            // Check simples (enviado)
val RaixReceiptRead = Color(0xFF00E676)            // Check duplo (lido)

// Badge / FAB
val RaixBadgeBg = Color(0xFF00E676)                // Fundo badge
val RaixBadgeText = Color(0xFF0B1325)              // Texto badge
val RaixDivider = Color(0x1FFFFFFF)                // Divisor 1dp cinza 12%

// Avatar
val RaixAvatarBg = Color(0xFF1A1F2E)               // Fundo do avatar

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
