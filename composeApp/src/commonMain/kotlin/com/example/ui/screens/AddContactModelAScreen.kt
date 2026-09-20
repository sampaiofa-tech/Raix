package com.example.ui.screens

import com.example.data.network.PlatformEnvironment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.data.network.IdentityNetworkClient
import com.example.data.repository.ContactRepository
import com.example.security.ClipboardSensivel
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityManager
import com.example.ui.components.QrCodeView
import com.example.ui.components.QrScannerView
import com.example.ui.components.isQrScannerSupported
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.example.data.network.FirestoreRestClient
import com.example.data.network.KeyStoreClient
import com.example.security.identity.AesGcm
import com.example.security.identity.SealedBox
import kotlin.random.Random
import com.example.data.model.FirestoreMessage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun AddContactModelAScreen(
    contactRepository: ContactRepository,
    onBack: () -> Unit,
    onContactCreated: (ContactItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // My URI for sharing
    var myUri by remember { mutableStateOf("") }
    var myFingerprint by remember { mutableStateOf("") }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    var showQrCodeMyTab by remember { mutableStateOf(true) }

    // Input fields for pasting code
    var inputName by remember { mutableStateOf("") }
    var inputUri by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isScanningCameraTab1 by remember { mutableStateOf(isQrScannerSupported) }

    // State for Modelo C Remote Invites
    val clipboardSensivel = remember { ClipboardSensivel() }
    var inviteLink by remember { mutableStateOf<String?>(null) }
    var isCreatingInvite by remember { mutableStateOf(false) }
    var isAcceptingInvite by remember { mutableStateOf(false) }
    var inputInviteLink by remember { mutableStateOf("") }
    var inputRemoteName by remember { mutableStateOf("") }
    var remoteInviteError by remember { mutableStateOf<String?>(null) }
    var remoteInviteSuccess by remember { mutableStateOf<String?>(null) }
    var showQrInviteTab2 by remember { mutableStateOf(true) }
    var isScanningCameraTab2 by remember { mutableStateOf(isQrScannerSupported) }

    LaunchedEffect(Unit) {
        val identity = IdentityManager.getOrGenerateIdentity()
        myFingerprint = identity.fingerprintHex
        val uid = com.example.security.DeviceAuthManager.getUserId()
        myUri = IdentityManager.createContactUri(uid) ?: ""
        try {
            com.example.security.DeviceAuthManager.getIdToken()
            val authedUid = com.example.security.DeviceAuthManager.getUserId()
            myUri = IdentityManager.createContactUri(authedUid) ?: ""
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Adicionar Contato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: [Meu Código (Presencial), Colar Código (Presencial), Convite Remoto (Modelo C)]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0D1B2A),
                contentColor = Color(0xFF00FFC2),
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00FFC2)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Meu Código", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Colar Presencial", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Convite Remoto", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTab == 0) {
                    // TAB 0: Share My Code
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Compartilhe seu Código com o Contato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No celular escaneie o QR Code; no Desktop copie o código URI abaixo e envie ao contato presencialmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle: QR Code vs String URI
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = showQrCodeMyTab,
                            onClick = { showQrCodeMyTab = true },
                            label = { Text("QR Code Presencial", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FFC2),
                                selectedLabelColor = Color(0xFF0A1128),
                                containerColor = Color(0xFF131B2A),
                                labelColor = Color(0xFFB0BEC5)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = !showQrCodeMyTab,
                            onClick = { showQrCodeMyTab = false },
                            label = { Text("String URI", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FFC2),
                                selectedLabelColor = Color(0xFF0A1128),
                                containerColor = Color(0xFF131B2A),
                                labelColor = Color(0xFFB0BEC5)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showQrCodeMyTab && myUri.isNotBlank()) {
                        QrCodeView(
                            data = myUri,
                            size = 230.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Aponte a câmera do seu celular para este QR Code para adicionar o contato presencialmente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0BEC5),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(myUri))
                                copyFeedback = "Código copiado para a área de transferência!"
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3A))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFC2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copiar Código URI",
                                    color = Color(0xFF00FFC2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // URI Code Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "STRING-CÓDIGO (MODELO A):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00FFC2),
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = myUri,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E0E0),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(myUri))
                                        copyFeedback = "Código copiado para a área de transferência!"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = Color(0xFF0A1128),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Copiar Código",
                                            color = Color(0xFF0A1128),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    copyFeedback?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = Color(0xFF00FFC2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                } else if (selectedTab == 1) {
                    // TAB 1: Paste Other's Code
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Adicionar Novo Contato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cole o código `pmsg://contact` fornecido pelo seu contato. A chave pública será validada criptograficamente contra o fingerprint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isScanningCameraTab1) {
                        Button(
                            onClick = { isScanningCameraTab1 = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3A))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFC2),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Escanear QR Code com a Câmera",
                                    color = Color(0xFF00FFC2),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        QrScannerView(
                            onQrCodeScanned = { scanned ->
                                isScanningCameraTab1 = false
                                inputUri = scanned.trim()
                                errorMessage = null
                                
                                if (scanned.trim().startsWith("pmsg://invite?")) {
                                    handleDeviceLinkScan(
                                        scanned = scanned,
                                        coroutineScope = coroutineScope,
                                        onSuccess = {
                                            coroutineScope.launch {
                                                errorMessage = "Desktop vinculado com sucesso! 🔗"
                                                kotlinx.coroutines.delay(1000)
                                                onBack()
                                            }
                                        },
                                        onError = { err ->
                                            errorMessage = err
                                        }
                                    )
                                    return@QrScannerView
                                }
                                
                                val parseResult = IdentityManager.parseContactUri(scanned.trim())
                                if (parseResult.isSuccess) {
                                    // Apenas preenche o URI para permitir que o usuário nomeie o contato
                                    // e clique no botão "Validar" manualmente.
                                } else {
                                    errorMessage = parseResult.exceptionOrNull()?.message
                                        ?: "QR code escaneado não contém um link pmsg://contact válido."
                                }
                            },
                            onDismiss = { isScanningCameraTab1 = false },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                    }

                    // Name Input
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nome do Contato (ex: Alice)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color(0xFF2E3D52),
                            focusedContainerColor = Color(0xFF131B2A),
                            unfocusedContainerColor = Color(0xFF131B2A)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // URI Code Input
                    OutlinedTextField(
                        value = inputUri,
                        onValueChange = {
                            inputUri = it
                            errorMessage = null
                        },
                        label = { Text("Cole o Código (pmsg://contact?...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color(0xFF2E3D52),
                            focusedContainerColor = Color(0xFF131B2A),
                            unfocusedContainerColor = Color(0xFF131B2A)
                        ),
                        maxLines = 4
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (inputName.isBlank()) {
                                errorMessage = "Por favor, digite um nome para o contato."
                                return@Button
                            }
                            if (inputUri.isBlank()) {
                                errorMessage = "Por favor, cole a string-código do contato."
                                return@Button
                            }

                            val parseResult = IdentityManager.parseContactUri(inputUri.trim())
                            if (parseResult.isFailure) {
                                errorMessage = parseResult.exceptionOrNull()?.message
                                    ?: "Código inválido ou corrompido."
                                return@Button
                            }

                            val contactData = parseResult.getOrThrow()
                            val myIdentity = IdentityManager.getOrGenerateIdentity()

                            // Compute commutative Pair Safety Number
                            val pairSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
                                myPubKey = myIdentity.publicKey,
                                peerPubKey = contactData.publicKeyBytes
                            )

                            val newContact = ContactItem(
                                fingerprint = contactData.fingerprintHex,
                                pubKey = contactData.publicKeyBase64,
                                currentAuthUid = contactData.authUid,
                                displayName = inputName.trim(),
                                securityNumber = pairSafetyNumber,
                                verified = false,
                                addedAt = PlatformEnvironment.currentTimeMillis()
                            )

                            coroutineScope.launch {
                                contactRepository.saveContact(newContact)
                                sendAutoHandshake(newContact, myUri)
                                onContactCreated(newContact)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF0A1128)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Validar e Comparar Código (60 Dígitos)",
                                color = Color(0xFF0A1128),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (selectedTab == 2) {
                    // TAB 2: Modelo C Remote Invites
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Convite Efêmero Remoto (Modelo C)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Gera um link criptográfico único com validade de 24 horas. O convite é permanentemente incinerado no primeiro aceite (vanish-after-accept).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 1: CREATE INVITE
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF131B2A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. GERAR MEU CONVITE DE 24 HORAS",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00FFC2),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Envie o link seguro para um contato remoto por qualquer canal seguro (Signal, e-mail cifrado, etc).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0BEC5)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isCreatingInvite = true
                                    remoteInviteError = null
                                    remoteInviteSuccess = null
                                    coroutineScope.launch {
                                        val identity = IdentityManager.getOrGenerateIdentity()
                                        val pubKeyB64 = Base64.encode(identity.publicKey)
                                        val signingPubKeyB64 = Base64.encode(identity.signingPublicKey)
                                        val myToken = com.example.security.DeviceAuthManager.getIdToken() ?: "anonymous_token"
                                        val result = IdentityNetworkClient.createInvite(
                                            creatorFingerprint = identity.fingerprintHex,
                                            creatorPubKey = pubKeyB64,
                                            idToken = myToken,
                                            creatorSigningPubKey = signingPubKeyB64
                                        )
                                        isCreatingInvite = false
                                        if (result.isSuccess) {
                                            val data = result.getOrThrow()
                                            inviteLink = data.inviteLink
                                            remoteInviteSuccess = "Convite gerado com sucesso! Válido por 24 horas."
                                        } else {
                                            remoteInviteError = result.exceptionOrNull()?.message ?: "Falha ao gerar convite."
                                        }
                                    }
                                },
                                enabled = !isCreatingInvite,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                            ) {
                                Text(
                                    text = if (isCreatingInvite) "Gerando Convite..." else "Gerar Convite de 24 Horas",
                                    color = Color(0xFF0A1128),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (inviteLink != null) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = showQrInviteTab2,
                                        onClick = { showQrInviteTab2 = true },
                                        label = { Text("QR Code Presencial", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00FFC2),
                                            selectedLabelColor = Color(0xFF0A1128),
                                            containerColor = Color(0xFF131B2A),
                                            labelColor = Color(0xFFB0BEC5)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = !showQrInviteTab2,
                                        onClick = { showQrInviteTab2 = false },
                                        label = { Text("Link Texto", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00FFC2),
                                            selectedLabelColor = Color(0xFF0A1128),
                                            containerColor = Color(0xFF131B2A),
                                            labelColor = Color(0xFFB0BEC5)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (showQrInviteTab2) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        QrCodeView(
                                            data = inviteLink ?: "",
                                            size = 180.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E17))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "LINK SEGURO (USO ÚNICO):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF00FFC2),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = inviteLink ?: "",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFFE0E0E0),
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                inviteLink?.let { link ->
                                                    clipboardSensivel.copySensitive(link, "Raix Convite")
                                                    copyFeedback = "Link copiado com auto-limpeza em 30 segundos! â±ï¸"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3A))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00FFC2),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Copiar Link Seguro (30s wipe)",
                                                    color = Color(0xFF00FFC2),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: ACCEPT INVITE
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF131B2A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "2. ACEITAR CONVITE RECEBIDO",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00FFC2),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Cole o link 'pmsg://invite?token=...' que você recebeu ou escaneie o QR code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0BEC5)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isScanningCameraTab2) {
                                Button(
                                    onClick = { isScanningCameraTab2 = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3A))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF00FFC2),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Escanear QR de Convite com a Câmera",
                                            color = Color(0xFF00FFC2),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            } else {
                                QrScannerView(
                                    onQrCodeScanned = { scanned ->
                                        isScanningCameraTab2 = false
                                        inputInviteLink = scanned.trim()
                                        
                                        if (scanned.trim().startsWith("pmsg://invite?")) {
                                            handleDeviceLinkScan(
                                                scanned = scanned,
                                                coroutineScope = coroutineScope,
                                                onSuccess = {
                                                    coroutineScope.launch {
                                                        remoteInviteSuccess = "Desktop vinculado com sucesso! 🔗"
                                                        kotlinx.coroutines.delay(1000)
                                                        onBack()
                                                    }
                                                },
                                                onError = { err ->
                                                    remoteInviteError = err
                                                }
                                            )
                                            return@QrScannerView
                                        }
                                    },
                                    onDismiss = { isScanningCameraTab2 = false },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = inputRemoteName,
                                onValueChange = { inputRemoteName = it },
                                label = { Text("Nome do Contato (ex: Bob)") },
                                placeholder = { Text("Identificação local (100% privada)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FFC2),
                                    unfocusedBorderColor = Color(0xFF2A3B4D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputInviteLink,
                                onValueChange = { inputInviteLink = it },
                                label = { Text("Link ou Token do Convite") },
                                placeholder = { Text("pmsg://invite?token=...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FFC2),
                                    unfocusedBorderColor = Color(0xFF2A3B4D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (inputRemoteName.isBlank()) {
                                        remoteInviteError = "Por favor, digite um nome para o contato."
                                        return@Button
                                    }
                                    val token = IdentityNetworkClient.parseInviteToken(inputInviteLink)
                                    if (token == null) {
                                        remoteInviteError = "Link ou token de convite inválido (formato esperado: pmsg://invite?token=...)."
                                        return@Button
                                    }

                                    isAcceptingInvite = true
                                    remoteInviteError = null
                                    coroutineScope.launch {
                                        val myToken = com.example.security.DeviceAuthManager.getIdToken() ?: "anonymous_token"
                                        val acceptResult = IdentityNetworkClient.acceptInvite(token, myToken)
                                        if (acceptResult.isFailure) {
                                            isAcceptingInvite = false
                                            remoteInviteError = acceptResult.exceptionOrNull()?.message ?: "Falha ao aceitar convite."
                                            return@launch
                                        }

                                        val creatorData = acceptResult.getOrThrow()
                                        val myIdentity = IdentityManager.getOrGenerateIdentity()
                                        val creatorPubKeyBytes = try {
                                            Base64.decode(creatorData.creatorPubKey)
                                        } catch (e: Exception) {
                                            isAcceptingInvite = false
                                            remoteInviteError = "Chave pública do criador corrompida."
                                            return@launch
                                        }

                                        // Resolve routing UID
                                        val resolveResult = IdentityNetworkClient.resolveFingerprint(creatorData.creatorFingerprint, myToken)
                                        val targetUid = if (resolveResult.isSuccess) {
                                            resolveResult.getOrThrow().currentAuthUid
                                        } else {
                                            "user_${creatorData.creatorFingerprint.take(8)}"
                                        }

                                        // Compute Pair Safety Number
                                        val pairSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
                                            myPubKey = myIdentity.publicKey,
                                            peerPubKey = creatorPubKeyBytes
                                        )

                                        val newContact = ContactItem(
                                            fingerprint = creatorData.creatorFingerprint,
                                            pubKey = creatorData.creatorPubKey,
                                            currentAuthUid = targetUid,
                                            displayName = inputRemoteName.trim(),
                                            securityNumber = pairSafetyNumber,
                                            verified = false,
                                            addedAt = PlatformEnvironment.currentTimeMillis()
                                        )

                                        contactRepository.saveContact(newContact)
                                        isAcceptingInvite = false
                                        onContactCreated(newContact)
                                    }
                                },
                                enabled = !isAcceptingInvite,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF0A1128),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isAcceptingInvite) "Validando no Servidor..." else "Aceitar Convite e Validar (60 Dígitos)",
                                        color = Color(0xFF0A1128),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (remoteInviteError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = remoteInviteError ?: "",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun sendAutoHandshake(contact: ContactItem, myUri: String) {
    if (myUri.isBlank()) return
    try {
        val (myUid, myIdToken) = com.example.security.DeviceAuthManager.ensureAuthenticated()
        val recipientPubKey = Base64.decode(contact.pubKey)
        val dek = ByteArray(32).also { Random.nextBytes(it) }
        val iv = ByteArray(12).also { Random.nextBytes(it) }

        val handshakePayload = "[AUTO-HANDSHAKE] $myUri"
        val cipherBytes = AesGcm.encrypt(
            plaintext = handshakePayload.encodeToByteArray(),
            key = dek,
            iv = iv
        )
        val ciphertextB64 = Base64.encode(cipherBytes)
        val ivB64 = Base64.encode(iv)

        val envelope = SealedBox.seal(dek = dek, recipientPubKey = recipientPubKey)
        val now = PlatformEnvironment.currentTimeMillis()
        val expiresAt = now + 60_000L // 1 minuto de TTL para o handshake

        val localMsgId = "msg_handshake_${now}_${Random.nextInt(1000, 9999)}"

        val storeKeyResult = KeyStoreClient.storeMessageKey(
            messageId = localMsgId,
            senderId = myUid,
            recipientId = contact.currentAuthUid,
            ephemeralPubKey = envelope.ephemeralPubKeyHex,
            wrappedDek = envelope.wrappedDekBase64,
            expiresAtMillis = expiresAt,
            idToken = myIdToken
        )

        if (storeKeyResult.success) {
            val firestoreMsg = FirestoreMessage(
                id = localMsgId,
                ciphertext = ciphertextB64,
                iv = ivB64,
                senderId = myUid,
                recipientId = contact.currentAuthUid,
                expiresAt = expiresAt
            )
            FirestoreRestClient.createMessage(firestoreMsg, myIdToken)
        }
    } catch (e: Exception) {
        // Falhas no auto-handshake são silenciosas para não interromper a UI
        println("Auto-handshake falhou: ${e.message}")
    }
}
private fun handleDeviceLinkScan(
    scanned: String,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val uriStr = scanned.trim()
        val query = uriStr.substringAfter("pmsg://invite?")
        val params = query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }
        
        val token = params["i"] ?: params["token"]
        val peerPubKeyBase64 = params["fp"]
        
        if (token == null || peerPubKeyBase64 == null) {
            onError("QR Code de handshake inválido.")
            return
        }

        val peerPubKey = kotlin.io.encoding.Base64.decode(peerPubKeyBase64)
        val myPriv = ByteArray(32).apply { kotlin.random.Random.nextBytes(this) }
        val myPub = com.example.security.identity.IdentityCurve25519.generatePublicKey(myPriv)
        val myPubBase64 = kotlin.io.encoding.Base64.encode(myPub)
        
        val sharedSecret = com.example.security.identity.IdentityCurve25519.computeSharedSecret(myPriv, peerPubKey)
        val iv = ByteArray(12).apply { kotlin.random.Random.nextBytes(this) }
        
        val words = com.example.security.identity.IdentityManager.getMnemonicWords()
        if (words == null) {
            onError("Este dispositivo não possui uma identidade (seed) para compartilhar.")
            return
        }
        
        val wordsStr = words.joinToString(" ")
        val ciphertext = com.example.security.identity.AesGcm.encrypt(wordsStr.encodeToByteArray(), sharedSecret, iv)
        val encryptedPayload = iv + ciphertext
        val encryptedPayloadBase64 = kotlin.io.encoding.Base64.encode(encryptedPayload)
        
        coroutineScope.launch {
            val result = com.example.data.network.IdentityNetworkClient.submitHandshake(
                token = token,
                peerPubKey = myPubBase64,
                encryptedPayload = encryptedPayloadBase64,
                idToken = "anonymous_token"
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError("Erro ao vincular dispositivo: " + (result.exceptionOrNull()?.message ?: "Erro desconhecido") + "")
            }
        }
    } catch (e: Exception) {
        onError("Erro fatal: " + e.message + "")
    }
}
