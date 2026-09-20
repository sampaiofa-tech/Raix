package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    contactRepository: ContactRepository,
    onBack: () -> Unit,
    onCompareSafetyNumber: (ContactItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val contacts by contactRepository.getContacts().collectAsState(initial = emptyList())
    var contactToDelete by remember { mutableStateOf<ContactItem?>(null) }
    var contactToBlock by remember { mutableStateOf<ContactItem?>(null) }
    var contactToRename by remember { mutableStateOf<ContactItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showPanicDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda de Contatos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum contato na agenda.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${contacts.size} Contato(s)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFB0BEC5)
                            )
                            SuggestionChip(
                                onClick = { showPanicDialog = true },
                                label = { Text("💥 Pânico", fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF331111),
                                    labelColor = Color(0xFFFF8080)
                                )
                            )
                        }
                    }

                    items(contacts, key = { it.fingerprint }) { contact ->
                        AgendaRowItem(
                            contact = contact,
                            onVerifyClick = { onCompareSafetyNumber(contact) },
                            onRenameClick = { 
                                contactToRename = contact
                                renameText = contact.displayName 
                            },
                            onBlockClick = { contactToBlock = contact },
                            onDeleteClick = { contactToDelete = contact }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    contactToRename?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToRename = null },
            title = { Text("Renomear Contato") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome do Contato") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val newName = renameText.trim()
                            if (newName.isNotBlank()) {
                                contactRepository.renameContact(contact.fingerprint, newName)
                            }
                            contactToRename = null
                        }
                    }
                ) {
                    Text("Salvar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Block Dialog
    contactToBlock?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToBlock = null },
            title = { Text("Bloquear Contato?") },
            text = { Text("O contato será bloqueado e mensagens descartadas.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        contactRepository.blockContact(contact.fingerprint)
                        contactToBlock = null
                    }
                }) { Text("Bloquear", color = Color(0xFFFF5252)) }
            },
            dismissButton = { TextButton(onClick = { contactToBlock = null }) { Text("Cancelar") } }
        )
    }

    // Delete Dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Excluir Contato?") },
            text = { Text("O contato e mensagens associadas serão destruídas.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        contactRepository.deleteContact(contact.fingerprint)
                        contactToDelete = null
                    }
                }) { Text("Excluir", color = Color(0xFFFF5252)) }
            },
            dismissButton = { TextButton(onClick = { contactToDelete = null }) { Text("Cancelar") } }
        )
    }

    // Panic Dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            title = { Text("💥 PÂNICO: Incinerar Todos os Contatos?") },
            text = { Text("Esta ação destrói todos os contatos.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        contactRepository.panicWipe()
                        showPanicDialog = false
                    }
                }) { Text("INCINERAR TUDO", color = Color(0xFFFF5252)) }
            },
            dismissButton = { TextButton(onClick = { showPanicDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun AgendaRowItem(
    contact: ContactItem,
    onVerifyClick: () -> Unit,
    onRenameClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (contact.verified) Color(0xFF004D40) else Color(0xFF263238))
                    .clickable { onVerifyClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.displayName.take(1).uppercase(),
                    color = if (contact.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${contact.fingerprint.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF78909C),
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = onRenameClick) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Renomear", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onBlockClick) {
                Icon(imageVector = Icons.Default.Block, contentDescription = "Bloquear", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFF546E7A), modifier = Modifier.size(20.dp))
            }
        }
    }
}
