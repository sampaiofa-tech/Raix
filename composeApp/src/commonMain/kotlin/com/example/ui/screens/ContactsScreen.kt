package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.launch

/**
 * Raix v1.8.0 Contacts Screen
 *
 * Visual: minimalist, 1 primary action (FAB add contact), max 2 info levels per row.
 * Functional: rename, delete, favorite, category (local groups) via long-press context menu.
 * All metadata is local-only, encrypted at rest, never touches the server graph.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    contactRepository: ContactRepository,
    onContactSelected: (ContactItem) -> Unit,
    onOpenIdentity: () -> Unit,
    onOpenDataPrivacy: () -> Unit = {},
    onOpenBlockedContacts: () -> Unit = {},
    onOpenAgenda: () -> Unit,
    onAddContactModelA: () -> Unit,
    onCompareSafetyNumber: (ContactItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val contacts by contactRepository.getContacts().collectAsState(initial = emptyList())
    var showPanicDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Derive categories from contacts
    val categories = remember(contacts) {
        contacts.mapNotNull { it.category }.distinct().sorted()
    }

    // Filter, search and sort: favorites first, then alphabetical
    val filteredContacts = remember(contacts, activeFilter, searchQuery) {
        val filtered = when (activeFilter) {
            "Todos" -> contacts
            "Favoritos" -> contacts.filter { it.isFavorite }
            else -> contacts.filter { it.category == activeFilter }
        }
        val searched = if (searchQuery.isBlank()) filtered else {
            filtered.filter {
                (it.nickname ?: it.displayName).contains(searchQuery, ignoreCase = true) ||
                it.fingerprint.contains(searchQuery, ignoreCase = true)
            }
        }
        searched.sortedWith(
            compareByDescending<ContactItem> { it.isFavorite }
                .thenBy { (it.nickname ?: it.displayName).lowercase() }
        )
    }

    // Separate favorites from others for section headers
    val favoriteContacts = remember(filteredContacts) { filteredContacts.filter { it.isFavorite } }
    val otherContacts = remember(filteredContacts) { filteredContacts.filter { !it.isFavorite } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Contatos",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (contacts.isNotEmpty()) {
                            Text(
                                text = "${contacts.size} contato${if (contacts.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Minha Identidade") },
                            onClick = {
                                expanded = false
                                onOpenIdentity()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Agenda de Contatos") },
                            onClick = {
                                expanded = false
                                onOpenAgenda()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Contatos Bloqueados") },
                            onClick = {
                                expanded = false
                                onOpenBlockedContacts()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Privacidade (LGPD)") },
                            onClick = {
                                expanded = false
                                onOpenDataPrivacy()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Apagar Todos os Contatos", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expanded = false
                                showPanicDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactModelA,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Contato"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar (animated visibility)
            if (isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar contatos...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (contacts.isEmpty()) {
                EmptyContactsView(
                    onAddContactModelA = onAddContactModelA,
                    onOpenIdentity = onOpenIdentity
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Filter chips row
                    item {
                        ContactFilterChips(
                            activeFilter = activeFilter,
                            categories = categories,
                            contactCount = contacts.size,
                            onFilterChanged = { activeFilter = it }
                        )
                    }

                    // Favorites section header
                    if (favoriteContacts.isNotEmpty() && activeFilter != "Favoritos") {
                        item {
                            SectionHeader(title = "Favoritos")
                        }
                        items(favoriteContacts, key = { "fav_${it.fingerprint}" }) { contact ->
                            ContactRowWhatsApp(
                                contact = contact,
                                onClick = { onContactSelected(contact) },
                                onVerifyClick = { onCompareSafetyNumber(contact) },
                                onRename = { newName ->
                                    coroutineScope.launch {
                                        contactRepository.renameContact(contact.fingerprint, newName)
                                    }
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        contactRepository.deleteContact(contact.fingerprint)
                                    }
                                },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        contactRepository.setFavorite(contact.fingerprint, !contact.isFavorite)
                                    }
                                },
                                onSetCategory = { category ->
                                    coroutineScope.launch {
                                        contactRepository.setCategory(contact.fingerprint, category)
                                    }
                                },
                                onBlock = {
                                    coroutineScope.launch {
                                        contactRepository.blockContact(contact.fingerprint)
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // All/Other contacts section header
                    if (otherContacts.isNotEmpty() && favoriteContacts.isNotEmpty() && activeFilter != "Favoritos") {
                        item {
                            SectionHeader(title = "Todos os contatos")
                        }
                    }

                    val displayList = if (activeFilter == "Favoritos") filteredContacts else otherContacts
                    items(displayList, key = { it.fingerprint }) { contact ->
                        ContactRowWhatsApp(
                            contact = contact,
                            onClick = { onContactSelected(contact) },
                            onVerifyClick = { onCompareSafetyNumber(contact) },
                            onRename = { newName ->
                                coroutineScope.launch {
                                    contactRepository.renameContact(contact.fingerprint, newName)
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    contactRepository.deleteContact(contact.fingerprint)
                                }
                            },
                            onToggleFavorite = {
                                coroutineScope.launch {
                                    contactRepository.setFavorite(contact.fingerprint, !contact.isFavorite)
                                }
                            },
                            onSetCategory = { category ->
                                coroutineScope.launch {
                                    contactRepository.setCategory(contact.fingerprint, category)
                                }
                            },
                            onBlock = {
                                coroutineScope.launch {
                                    contactRepository.blockContact(contact.fingerprint)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Panic Wipe Dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            title = { Text("Apagar Todos os Contatos?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Esta acao sobrescreve e destroi imediatamente todos os contatos salvos sem chance de recuperacao.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            contactRepository.panicWipe()
                            showPanicDialog = false
                        }
                    }
                ) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactFilterChips(
    activeFilter: String,
    categories: List<String>,
    contactCount: Int,
    onFilterChanged: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            FilterChip(
                selected = activeFilter == "Todos",
                onClick = { onFilterChanged("Todos") },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        item {
            FilterChip(
                selected = activeFilter == "Favoritos",
                onClick = { onFilterChanged("Favoritos") },
                label = { Text("Favoritos") },
                leadingIcon = if (activeFilter == "Favoritos") {
                    { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = activeFilter == category,
                onClick = { onFilterChanged(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * WhatsApp-style flat contact row: 48dp avatar, name + fingerprint, divider at 76dp start.
 * Long-press opens context menu with rename/delete/favorite/category.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRowWhatsApp(
    contact: ContactItem,
    onClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetCategory: (String?) -> Unit,
    onBlock: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar 48dp (WhatsApp standard)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (contact.verified)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (contact.nickname ?: contact.displayName).take(1).uppercase(),
                color = if (contact.verified)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info: name (line 1) + fingerprint preview (line 2)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.nickname ?: contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Favorite star inline
                if (contact.isFavorite) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorito",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Verification badge inline
                if (contact.verified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fingerprint preview (line 2 — secondary)
                Text(
                    text = contact.fingerprint.take(16) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Category tag
                if (contact.category != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = contact.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Action: verify button for unverified contacts
        if (!contact.verified) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                modifier = Modifier.clickable { onVerifyClick() }
            ) {
                Text(
                    text = "Verificar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Icone de 3 pontos para abrir menu
        Spacer(modifier = Modifier.width(4.dp))
        Box {
            IconButton(
                onClick = { showContextMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opcoes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Context Menu (long-press)
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text(if (contact.isFavorite) "Desfavoritar" else "Favoritar") },
            onClick = {
                showContextMenu = false
                onToggleFavorite()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Renomear") },
            onClick = {
                showContextMenu = false
                showRenameDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Categoria") },
            onClick = {
                showContextMenu = false
                showCategoryDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
            onClick = {
                showContextMenu = false
                showDeleteDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Bloquear", color = MaterialTheme.colorScheme.error) },
            onClick = {
                showContextMenu = false
                onBlock()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(contact.nickname ?: contact.displayName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renomear Contato", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName.trim())
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Salvar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Contato?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "O contato \"${contact.nickname ?: contact.displayName}\" sera removido permanentemente.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Category Dialog
    if (showCategoryDialog) {
        var categoryInput by remember { mutableStateOf(contact.category ?: "") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Definir Categoria", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = "Digite uma categoria (ex: Familia, Trabalho) ou deixe em branco para remover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Categoria") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cat = categoryInput.trim().ifBlank { null }
                        onSetCategory(cat)
                        showCategoryDialog = false
                    }
                ) {
                    Text("Salvar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun VerificationBadge(verified: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Verificado",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EmptyContactsView(
    onAddContactModelA: () -> Unit,
    onOpenIdentity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Nenhum Contato",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Para conversar, faca a troca de chaves presencial (Modelo A).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(
            onClick = onAddContactModelA,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Adicionar Contato",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onOpenIdentity) {
            Text("Ver Minha Identidade", color = MaterialTheme.colorScheme.primary)
        }
    }
}
