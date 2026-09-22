package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BurnerChannel
import com.example.data.model.ContactItem
import com.example.data.model.PmsgContact
import com.example.ui.theme.RaixAvatarBg
import com.example.ui.theme.RaixSurface
import com.example.ui.theme.RaixSurfaceElevated
import com.example.ui.theme.RaixError
import com.example.ui.theme.RaixErrorContainer
import com.example.ui.theme.RaixTextSecondary
import com.example.ui.theme.RaixTextPrimary
import com.example.ui.theme.RaixBackground
import com.example.ui.theme.RaixBorder
import com.example.ui.theme.RaixActionPrimary
import com.example.ui.theme.RaixBadgeBg
import com.example.ui.theme.RaixBadgeText
import com.example.ui.theme.RaixDivider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChannelListScreen(
    channels: List<BurnerChannel>,
    contacts: List<PmsgContact> = emptyList(),
    e2eContacts: List<ContactItem> = emptyList(),
    currentTime: Long,
    screenProtectionEnabled: Boolean,
    biometricLockEnabled: Boolean = false,
    autoLockEnabled: Boolean = true,
    autoLockTimeoutMinutes: Int = 5,
    securityPin: String = "1234",
    notificationsEnabled: Boolean = true,
    hasContactsPermission: Boolean = true,
    userFeedback: String?,
    onSelectChannel: (BurnerChannel) -> Unit,
    onCreateChannel: (String, String, Float) -> Unit,
    onStartChatWithContact: (PmsgContact) -> Unit = {},
    onSelectE2eContact: (ContactItem) -> Unit = {},
    onDeleteChannel: (String) -> Unit,
    onPanicWipe: () -> Unit,
    onToggleScreenProtection: () -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLock: (Boolean) -> Unit = {},
    onSetAutoLockTimeout: (Int) -> Unit = {},
    onSetSecurityPin: (String) -> Unit = {},
    onLockNow: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    onRefreshContacts: () -> Unit = {},
    onSimulateIncomingNewConversation: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenIdentity: () -> Unit = {},
    onOpenQrHandshake: () -> Unit = {},
    onOpenAddContact: () -> Unit = {},
    onToggleFavorite: (ContactItem) -> Unit = {},
    onRenameContact: (ContactItem, String) -> Unit = { _, _ -> },
    onDeleteContact: (ContactItem) -> Unit = {},
    onBlockContact: (ContactItem) -> Unit = {},
    onClearFeedback: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(0) }
    var showDiagnostics by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userFeedback) {
        userFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            onClearFeedback()
        }
    }

    val filteredE2eContacts = remember(e2eContacts, searchQuery, activeFilter) {
        val searchFiltered = if (searchQuery.isBlank()) e2eContacts
        else e2eContacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            (it.nickname ?: "").contains(searchQuery, ignoreCase = true)
        }
        when (activeFilter) {
            1 -> searchFiltered.filter { !it.verified } // Pendentes
            2 -> searchFiltered.filter { it.isFavorite } // Favoritos
            else -> searchFiltered // Todas
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        },
        containerColor = RaixBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaixSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RAIX",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = RaixTextPrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { showDiagnostics = !showDiagnostics }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { searchQuery = if (searchQuery.isEmpty()) " " else "" },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = RaixTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(40.dp).testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = RaixTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = RaixSurfaceElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Contatos", color = RaixTextPrimary) },
                                    onClick = { showMenu = false; onOpenContacts() },
                                    leadingIcon = {
                                        Icon(Icons.Default.Contacts, contentDescription = null, tint = RaixTextSecondary)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Identidade", color = RaixTextPrimary) },
                                    onClick = { showMenu = false; onOpenIdentity() },
                                    leadingIcon = {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = RaixTextSecondary)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Adicionar Contato", color = RaixTextPrimary) },
                                    onClick = { showMenu = false; onOpenAddContact() },
                                    leadingIcon = {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RaixTextSecondary)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Configuracoes", color = RaixTextPrimary) },
                                    onClick = { showMenu = false; onOpenSettings() },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = RaixTextSecondary)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = RaixActionPrimary,
                contentColor = RaixBackground,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("create_channel_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nova conversa",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(RaixBackground)
        ) {

            // Painel de diagnostico push (temporario, para depuracao sem adb)
            if (showDiagnostics) {
                com.example.ui.components.PushDiagnosticsPanel(
                    onDismiss = { showDiagnostics = false }
                )
            }

            // Search bar (expansivel)
            AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery.trim(),
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "Buscar conversas..." else "Buscar contatos...",
                            fontSize = 13.sp,
                            color = RaixTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = RaixTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.DeleteOutline, "Limpar", tint = RaixTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = RaixTextPrimary,
                        unfocusedTextColor = RaixTextPrimary,
                        focusedBorderColor = RaixTextSecondary,
                        unfocusedBorderColor = RaixBorder,
                        focusedContainerColor = RaixSurface,
                        unfocusedContainerColor = RaixSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).heightIn(min = 48.dp)
                )
            }

            // Filtros — 48dp: "Todas" (ativo), "Nao lidas", "Favoritos"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas", "Pendentes", "Favoritos").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (activeFilter == index) RaixActionPrimary
                                else Color.Transparent
                            )
                            .clickable { activeFilter = index }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (activeFilter == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (activeFilter == index) RaixBackground else RaixTextSecondary
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: CONVERSAS E2E
                if (filteredE2eContacts.isEmpty()) {
                    // Estado vazio centralizado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = RaixTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma conversa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adicione um contato para iniciar uma conversa criptografada.",
                                fontSize = 13.sp,
                                color = RaixTextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onOpenAddContact,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RaixActionPrimary)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RaixActionPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adicionar Contato", color = RaixActionPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredE2eContacts, key = { it.fingerprint }) { contact ->
                            E2eContactRow(
                                contact = contact,
                                onClick = { onSelectE2eContact(contact) },
                                onToggleFavorite = { onToggleFavorite(contact) },
                                onRename = { newName -> onRenameContact(contact, newName) },
                                onDelete = { onDeleteContact(contact) },
                                onBlock = { onBlockContact(contact) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            } else {
                // TAB 1: CONTATOS COM PMSG INSTALADO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Contact header banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RaixSurfaceElevated)
                            .border(1.dp, RaixBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identificação de contatos com Pmsg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RaixTextPrimary
                            )
                        }

                        // Test incoming new conversation simulation (silent by default)
                        OutlinedButton(
                            onClick = onSimulateIncomingNewConversation,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, RaixActionPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp).testTag("simulate_new_conv_button")
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = RaixActionPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simular Chegada", color = RaixActionPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (!hasContactsPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(RaixSurface)
                                .border(1.dp, RaixActionPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Permissão de Contatos Necessária",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = RaixTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Permita o acesso aos contatos para verificar quais amigos já utilizam o Pmsg e iniciar conversas criptografadas.",
                                    fontSize = 12.sp,
                                    color = RaixTextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestContactsPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = RaixActionPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Autorizar Contatos", color = RaixBackground, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactListItem(
                                contact = contact,
                                onStartChat = { onStartChatWithContact(contact) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Create New Channel Dialog
    if (showCreateDialog) {
        var contactName by remember { mutableStateOf("") }
        // Default to 24 hours as required ("caso não escolha o tempo, sera usado 24 horas como padrão")
        var selectedTtlHours by remember { mutableStateOf(24f) }

        val ttlOptions = listOf(
            24f to "24 HRS",
            12f to "12 HRS",
            6f to "6 HRS",
            1f to "1 HR",
            0.08333f to "5 MN",
            0.00833f to "30 SEG"
        )

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = RaixSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = RaixActionPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Novo Chat",
                        fontWeight = FontWeight.Medium,
                        color = RaixTextPrimary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Informe o nome da pessoa e selecione o tempo até a exclusão automática das mensagens.",
                        fontSize = 12.sp,
                        color = RaixTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Nome do Contato") },
                        placeholder = { Text("Ex: Mariana, Carlos, Beatriz...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaixTextPrimary,
                            unfocusedTextColor = RaixTextPrimary,
                            focusedBorderColor = RaixActionPrimary,
                            unfocusedBorderColor = RaixBorder,
                            focusedLabelColor = RaixActionPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TEMPO ATÉ A EXCLUSÃO:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = RaixActionPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // TTL Chips selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ttlOptions.take(3).forEach { (hours, label) ->
                                val isSelected = selectedTtlHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RaixActionPrimary else RaixSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) RaixActionPrimary else RaixBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTtlHours = hours }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        color = if (isSelected) RaixBackground else RaixTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ttlOptions.drop(3).forEach { (hours, label) ->
                                val isSelected = selectedTtlHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RaixActionPrimary else RaixSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) RaixActionPrimary else RaixBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTtlHours = hours }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        color = if (isSelected) RaixBackground else RaixTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank()) {
                            onCreateChannel(contactName.trim(), "", selectedTtlHours)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaixActionPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_create_channel_button")
                ) {
                    Text("Iniciar Chat", color = RaixBackground, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = RaixTextSecondary)
                }
            }
        )
    }

    // Panic Wipe Confirmation Dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            containerColor = RaixSurface,
            icon = {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = RaixError, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = "🚨 INCINERAÇÃO TOTAL EM PÂNICO",
                    fontWeight = FontWeight.Medium,
                    color = RaixError,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ATENÇÃO: Todas as conversas ativas, contatos vinculados e mensagens armazenadas no Room serão DESTRUÍDAS e sobrescritas de forma permanente. Nenhum dado poderá ser recuperado.",
                    color = RaixTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicDialog = false
                        onPanicWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaixError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_panic_wipe_button")
                ) {
                    Text("VAPORIZAR TUDO AGORA", fontWeight = FontWeight.Medium, color = Color(0xFF601410))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Cancelar", color = RaixTextSecondary)
                }
            }
        )
    }
}

@Composable
fun ContactListItem(
    contact: PmsgContact,
    onStartChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = contact.hasPmsgInstalled) { onStartChat() }
            .testTag("contact_item_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.hasPmsgInstalled) RaixSurface else RaixSurfaceElevated.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (contact.hasPmsgInstalled) RaixActionPrimary.copy(alpha = 0.25f) else RaixBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with contact initial
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(contact.avatarColorHex).copy(alpha = 0.2f))
                        .border(1.5.dp, Color(contact.avatarColorHex).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color(contact.avatarColorHex),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = RaixTextPrimary
                        )
                        if (contact.hasPmsgInstalled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RaixActionPrimary.copy(alpha = 0.15f))
                                    .border(0.5.dp, RaixActionPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Pmsg Ativo",
                                    color = RaixActionPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }


                    Text(
                        text = contact.phoneNumber,
                        fontSize = 11.5.sp,
                        color = RaixTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = contact.statusDescription,
                        fontSize = 10.5.sp,
                        color = if (contact.hasPmsgInstalled) RaixActionPrimary else RaixTextSecondary
                    )
                }
            }

            if (contact.hasPmsgInstalled) {
                Button(
                    onClick = onStartChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RaixActionPrimary,
                        contentColor = RaixBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("start_chat_with_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RaixBackground,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Conversar",
                        color = RaixBackground,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RaixSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Não possui",
                        color = RaixTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Linha de conversa — spec de pixel v1.10:
 * 72dp height, avatar 48dp, nome 16sp/500, preview 14sp, timestamp 12sp,
 * badge 20dp, divisor 1dp indentado 80dp.
 */
@Composable
private fun ConversationRow(
    channel: BurnerChannel,
    currentTime: Long,
    onClick: () -> Unit
) {
    val timestamp = remember(channel) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(channel.lastMessageTimestamp))
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable { onClick() }
                .padding(horizontal = 16.dp)
                .testTag("channel_item_${channel.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar — circulo 48dp, fundo RaixAvatarBg, inicial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RaixAvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(1).uppercase(),
                    color = RaixTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Centro: nome + preview
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = channel.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = RaixTextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.lastMessagePreview,
                    fontSize = 14.sp,
                    color = RaixTextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Direita: timestamp + badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timestamp,
                    fontSize = 12.sp,
                    color = RaixTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Badge de nao-lidas (placeholder: mostra se customCode nao vazio)
                if (channel.customCode.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(RaixBadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaixBadgeText
                        )
                    }
                }
            }
        }

        // Divisor — 1dp, #8A93A6 a 12%, indentado 80dp (16 padding + 48 avatar + 16 gap)
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 1.dp,
            color = RaixTextSecondary.copy(alpha = 0.12f)
        )
    }
}

@Composable
fun ChannelListItem(
    channel: BurnerChannel,
    currentTime: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Retrocompatibilidade: delega para ConversationRow
    ConversationRow(channel = channel, currentTime = currentTime, onClick = onClick)
}

@Composable
private fun E2eContactRow(
    contact: ContactItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onBlock: () -> Unit = {}
) {
    val displayLabel = contact.nickname ?: contact.displayName
    var showItemMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable { onClick() }
                .padding(horizontal = 16.dp)
                .testTag("e2e_contact_${contact.fingerprint}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar -- circulo 48dp, fundo RaixAvatarBg, inicial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RaixAvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLabel.take(1).uppercase(),
                    color = RaixTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Centro: nome + status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = RaixTextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Favorito",
                            tint = RaixActionPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (contact.verified) "Verificado - E2E" else "Criptografia de ponta a ponta",
                    fontSize = 14.sp,
                    color = if (contact.verified) RaixActionPrimary else RaixTextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Menu de 3 pontos por item
            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier.size(36.dp).testTag("item_menu_${contact.fingerprint}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opcoes",
                        tint = RaixTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false },
                    containerColor = RaixSurfaceElevated
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (contact.isFavorite) "Remover favorito" else "Favoritar",
                                color = RaixTextPrimary
                            )
                        },
                        onClick = { showItemMenu = false; onToggleFavorite() },
                        leadingIcon = {
                            Icon(
                                imageVector = if (contact.isFavorite) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (contact.isFavorite) RaixActionPrimary else RaixTextSecondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Renomear", color = RaixTextPrimary) },
                        onClick = { showItemMenu = false; showRenameDialog = true },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = RaixTextSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = RaixError) },
                        onClick = { showItemMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RaixError)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bloquear", color = RaixError) },
                        onClick = { showItemMenu = false; onBlock() },
                        leadingIcon = {
                            Icon(Icons.Default.Block, contentDescription = null, tint = RaixError)
                        }
                    )
                }
            }
        }

        // Divisor
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 1.dp,
            color = RaixTextSecondary.copy(alpha = 0.12f)
        )
    }

    // Dialog de renomear
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(contact.nickname ?: contact.displayName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    if (newName.isNotBlank()) onRename(newName.trim())
                }) {
                    Text("Salvar", color = RaixActionPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar", color = RaixTextSecondary)
                }
            },
            title = { Text("Renomear contato", color = RaixTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = RaixTextPrimary,
                        unfocusedTextColor = RaixTextPrimary,
                        focusedBorderColor = RaixActionPrimary,
                        unfocusedBorderColor = RaixBorder,
                        focusedContainerColor = RaixSurface,
                        unfocusedContainerColor = RaixSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = RaixSurfaceElevated
        )
    }
}
