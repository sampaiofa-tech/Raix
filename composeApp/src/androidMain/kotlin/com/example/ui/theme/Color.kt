package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Pmsg Official Brand Specifications: Stealth Obsidian & Brushed Titanium
// =========================================================================
val PmsgMatteDark = Color(0xFF111215)       // Fundo: Grafite Obsidiana Profundo (#111215)
val PmsgPlatinum = Color(0xFFE4E4E7)        // Elemento Principal: Platina Titânio (#E4E4E7)
val PmsgEvasionGray = Color(0xFF52525B)     // Detalhe Neutro (#52525B)
val PmsgBorder = Color(0xFF26282E)          // Borda Refinada (#26282E)
val PmsgCardBg = Color(0xFF181A1F)          // Superfície Elevada (#181A1F)

// =========================================================================
// Harmonious Executive Semantic Tokens (Fase 5)
// =========================================================================
val ObsidianBlack = Color(0xFF0A1128) // Raix Navy Blue Background
val ObsidianSurface = Color(0xFF0B1430)
val ObsidianCard = Color(0xFF043927) // Raix Emerald Green
val ObsidianCardElevated = Color(0xFF064D35)
val ObsidianBorder = Color(0x33D4AF37) // Subtle Gold Border
val ObsidianBorderSubtle = Color(0x18D4AF37)

val TitaniumPrimary = Color(0xFFD4AF37) // Raix Gold Accent
val TitaniumSecondary = Color(0xFFE2C873)
val TitaniumMuted = Color(0xFF8A92A6)

val SecurityEmerald = Color(0xFF10B981)
val SecurityEmeraldContainer = Color(0xFF064E3B)
val EmberFlame = Color(0xFFF59E0B)
val EmberFlameContainer = Color(0xFF451A03)
val IncinerateCrimson = Color(0xFFEF4444)
val IncinerateCrimsonBg = Color(0xFF2C1316)

// Chat Bubbles
val BubbleUser = Color(0xFF043927) // Emerald Green for user
val BubbleUserBorder = Color(0x33D4AF37) // Subtle Gold
val BubbleContact = Color(0xFF0D183B) // Slightly lighter Navy Blue
val BubbleContactBorder = Color(0xFF14224D)

// =========================================================================
// Immersive UI Palette (Maintained for total backward compatibility)
// =========================================================================
val ImmersiveSurface = ObsidianBlack
val ImmersiveHeader = ObsidianSurface
val ImmersiveCard = ObsidianCard
val ImmersiveCardVariant = ObsidianCardElevated
val ImmersivePrimary = TitaniumPrimary
val ImmersivePrimaryContainer = Color(0xFF2D3039)
val ImmersiveOnPrimary = ObsidianBlack
val ImmersiveSecondary = TitaniumSecondary
val ImmersiveOutline = ObsidianBorder
val ImmersiveOnSurface = Color(0xFFF4F4F5)
val ImmersiveMuted = TitaniumMuted
val ImmersiveMutedLight = TitaniumSecondary
val ImmersiveExpiring = IncinerateCrimson
val ImmersiveOnlineGreen = SecurityEmerald
val ImmersiveAvatarDeep = Color(0xFF20232A)

// Mappings for theme interoperability
val StealthBlack = ImmersiveSurface
val StealthDarkSurface = ImmersiveHeader
val StealthCardSurface = ImmersiveCard
val StealthCardSurfaceLight = ImmersiveCardVariant

// Accents
val ElectricCyan = ImmersivePrimary
val ElectricCyanDim = ImmersiveSecondary
val NeonEmerald = ImmersiveOnlineGreen
val EmberOrange = EmberFlame
val IncinerateRed = ImmersiveExpiring
val GhostPurple = ImmersivePrimary

// Text & Neutral Colors
val TextPrimaryDark = ImmersiveOnSurface
val TextSecondaryDark = ImmersiveMutedLight
val TextMutedDark = ImmersiveMuted
val BorderSubtleDark = ImmersiveOutline
val GlowOverlay = Color(0x1AE4E4E7)
