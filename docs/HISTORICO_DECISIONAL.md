# HISTÓRICO DECISIONAL — RAIX

**Propósito:** Registro cronológico dos marcos-chave e decisões do projeto, para reconstrução de contexto e continuidade plena.

---

## 1. FUNDAÇÃO & ESTRATÉGIA

### Migração do app AI Studio → RAIX (multiplataforma)

- **Decisão:** converter o app Android gerado no AI Studio (mensageria efêmera) para Kotlin Multiplatform + Compose Multiplatform (Android, Windows, Web/Wasm, iOS).
- **Marco:** v1.0–v1.5 homologadas; repositório `sampaiofa-tech/Raix` (AGPL-3.0).

### Renome e identidade

- **Decisão:** nome definitivo **RAIX** (substituiu "Pmsg" provisório).
- **Marcos:** domínio `raixtech.com` + `raixtech.com.br`; marca depositada no INPI (classes 9 e 42) + RPC; landing page e cartilhas publicadas.

### Master Plan "Fortaleza RAIX" (3 anos, 6 camadas)

- **Decisão (22/05/2024):** ecossistema de "Estado Digital Soberano" orbitando o núcleo `raix-crypto-core`.
- **Camadas:** Fundação → Ativos → Comunicação → Defesa → Contra-Inteligência → Governança.
- **Blindagens obrigatórias:** Panic PIN (volume isca), Tokens Hardware (recovery offline), Secure Guest Bridge (Low-Assurance), RAIX Governor (determinístico NÃO-IA, kill-switch), Contra-Inteligência travada.
- **Soberania:** processamento on-device inviolável + armazenamento híbrido como "depósito cego de bits".

---

## 2. SEGURANÇA & CRIPTOGRAFIA

### Núcleo criptográfico compartilhado (`raix-crypto-core`)

- **Decisão:** BIP-39 (12 palavras) como chave-mestra; derivação HD (BIP-32/44) com isolamento de domínios; PQC híbrida NIST (ML-KEM-768 + X25519 / ML-DSA-65 + Ed25519); ChaCha20-Poly1305 / AES-256-GCM; Argon2id; sanitização RAM (zeroize).

### Migração pós-quântica híbrida (P0.1)

- **Decisão:** assinatura ML-DSA-65 + Ed25519 e KEM ML-KEM-768 + X25519, com anti-downgrade, forward secrecy e derivação dual BIP-39.
- **Status:** implementado e testado (65+ testes).

### E2E da DEK v1.2 (servidor zero-knowledge)

- **Decisão:** o servidor armazena apenas ciphertext + DEK envelopada (sealed-box X25519 + ML-KEM). Servidor nunca vê a DEK em claro.
- **Prova:** 3 provas — servidor cego, round-trip, isolamento adversarial.

### Crypto-shredding + expiração

- **Decisão:** shredder a cada 15 min (idempotente) + TTL nativo (≤24h) + vanish-after-read. PITR desabilitado.
- **Marcos:** incidente P0 resolvido (índice collectionGroup + isolamento transacional); health-check estendido ativo.

### Incidente de Vazamento e Consolidação de Chaves

- **Decisão:** Encerrar o incidente de segurança executando a consolidação das chaves: chave Android restrita a app + 5 APIs; Browser key a 3 APIs; segredo v1 no Secret Manager destruído; chave "Default" excluída; "Gemini API Key 2" definida como canônica; senha do keystore local trocada.
- **Incidente de Indisponibilidade (Outage):** Durante a consolidação, um deploy referenciou o segredo `ACCESS_LOG_ENC_KEY` antes de ser provisionado, causando falha de cold start nas funções principais e indisponibilidade temporária. Recuperado com a criação do segredo e redeploy.
- **Marco:** Incidente encerrado em setembro de 2026.

---

## 3. COMPLIANCE & NEGÓCIO

### LGPD / Marco Civil / Licenciamento

- **Decisão:** PP v3, ToU v3, DPA, ROPA publicados em `raixtech.com`; logs MCI 180 dias isolados; AGPL-3.0 + licença comercial (dual licensing) + CLA.

### Captação & modelo de receita

- **Decisão:** 3 cenários A/B/C — **recomendação: Cenário C (Híbrido, R$ 1,4–1,6M)**.
- **Planos:** Free / Profissional R$ 29–39 / Escritório R$ 29–49/seat / Private (2026).
- **Pró-labore:** R$ 10k (A1) → 25k (A2) → 40k (A3) → 50k (A4).
- **Filosofia:** "receita de curto prazo financia o longo prazo".

### Pilotos F1

- **Decisão/Marco:** 5 advogados em uso ativo (~2h/dia); pediram "rastreabilidade" → Trilho de Auditoria Local; veem o app como monetizável.

---

## 4. PRODUTOS & ROADMAP

### Priorização pós-rodada (ordem estrita)

**Decisão:** Auditoria externa (R$ 50k, P0) → Migração PQ → PQ-Vault → Satélites B2B → Self-hosted → Track 2 → Feature premium → Consultoria → Sentinela (R&D).

### Satélites B2B (adendo)

- **Decisão:** RAIX Sign (P1, ML-DSA-65, assinatura B2B), RAIX Drop (P2, transferência efêmera, freemium/Pro), Protocolo Lázaro (P3, SLIP-0039, continuidade da semente).

### Self-hosted (Bloco C)

- **Decisão:** RAIX Drive zero-knowledge client-side; NAS do cliente como "depósito cego de bits"; modelos SaaS / Self-Hosted / Sovereign Box.

---

## 5. MÓDULOS ESPECIAIS

### Cadeia de Custódia (RFI)

- **Decisão/Status:** implementada (commit `420a814`) — assinatura hardware-backed + hash chaining + laudo pericial. Testes 8/8 PASS.

### Contra-Inteligência

- **Decisão:** TRAVADA (`LOCKED_PENDING_LEGAL_REVIEW`). Mirror Sandbox, Honey-Vaults e Coleta de Metadados vetados até validação jurídica profunda.

### Auditoria de sanitização do repo (7 fases)

- **Decisão:** repo público mantido (AGPL-3.0) com risco residual gerenciável; disciplina contínua + re-auditoria periódica (release significativo ou 6 meses).

---

## 6. OPERACIONAL & RESILIÊNCIA

### Health-check estendido

- **Decisão/Status:** regressão + integridade (hashes) + fragilidades (CVEs/segredos/rules/cripto) + monitoramento de hospedagem + relatório diário e-mail + escalonamento (2x falha → prioridade alta) + watchdog de missed execution.

### Backup/DR automático

- **Decisão/Status:** rotina `RaixBackupAutomatico` (00h/08h/16h → `G:\Meu Drive\Raix`); **keystore, secrets e mnemônico fora da nuvem** (backup físico manual mantido).

---

## 7. GOVERNANÇA DOS AGENTES

### Organograma

- **Decisão:** Assessor (estratégia), Guru (segurança/arquitetura), Analista (operação — único canal ao Executor), Futuro (inovação — fala só com Guru), Executor (implementação — recebe só do Analista).

### Protocolo de continuidade

- **Decisão:** comando "Salvem quem vocês são" → textos de 5 blocos (Identidade, Histórico, Estado, Regras, Bloco p/ Executor) persistidos em `docs/CONTINUIDADE.md`.
- **Adicional:** preferência de comunicação do dono — respostas **sem emojis**.

### Nota Técnica de Adequação (MCI / LGPD)

- **Decisão (18/09/2026):** Parecer do Guru aprovado, com decisão do Assessor (Opção 1, redação corrigida, consenso fechado). Gerada versão v3.2 contemplando pseudonimização provisória no estágio atual e mapeamento formal para retenção de IP cifrado (Art. 15 MCI) no próximo ciclo.

---

## 8. SAGA DE CORRECOES v1.7--v1.9.3 (13 correcoes, Set/2026)

### Contexto

Apos a homologacao da v1.6, o app foi submetido a testes fisicos intensivos pelo Principal (dispositivo real). Foram identificadas 13 correcoes em cascata, executadas entre v1.7 e v1.9.3.

### Correcoes aplicadas (ordem cronologica)

1. **Corr 1-3 (v1.7):** Visual (tokens RAIX: paleta #0B1325/#1E2432/#F5F7FA/#8A93A6/#00E676/#D4AF37/#E5484D; tipografia Inter+JetBrains Mono), layout WhatsApp, QR do menu.
2. **Corr 4-5 (v1.8):** Notificacoes (canais, deep-link direto na conversa via fingerprint), audio, componentes visuais.
3. **Corr 6-8 (v1.8.x):** Layout desktop 3 colunas (rail + lista + detalhe), listener E2E unificado (commonMain), mini-janela de notificacao desktop.
4. **Corr 9 (v1.9.0):** Onboarding com gates (AgeGate, RecoverySeed, MasterPassword, BiometricOffer), wipe total por build.
5. **Corr 10 (v1.9.0):** Home vazia (causa raiz: TTL de 48h do AddressBook expirando contatos), abas (Todas/Nao lidas/Favoritos), QR do menu.
6. **Corr 11 (v1.9.1):** Insets do input (adjustResize + imePadding), busca (altura/padding), home em branco (causa raiz confirmada: TTL).
7. **Corr 12 (v1.9.2):** Menu de item com Bloquear (ChannelListScreen).
8. **Corr 13 (v1.9.3):** Permissao POST_NOTIFICATIONS no gate de onboarding (causa raiz: LaunchedEffect prematuro durante AgeGate), menu 3 pontos visivel + Bloquear na tela Contatos, paridade desktop.

### Decisao do wipe total por build

- **Decisao do Principal:** o wipe permanece TOTAL a cada atualizacao de versao (mantido por escolha deliberada do dono do produto).
- **Justificativa:** fresh-start garante que nenhum dado residual sobreviva entre versoes; alinhado com a filosofia de efemeridade maxima.
- **Impacto:** apos cada atualizacao, o usuario passa pelo onboarding completo (AgeGate -> RecoverySeed -> MasterPassword -> BiometricOffer -> NotificationPermission -> UNLOCKED).

---

## 9. SAGA DA NOTIFICACAO EM SEGUNDO PLANO (v1.9.4--v1.9.10, Set/2026)

### Contexto

Apos a homologacao da v1.9.4, testes fisicos no Android revelaram que mensagens em segundo plano nao geravam notificacao. O PC (Windows) funcionava via polling. A investigacao durou 6 builds (v1.9.5--v1.9.10) com multiplas hipoteses.

### Hipoteses investigadas e descartadas

1. **Hipotese 1 (v1.9.5): SyncMessageWorker vazio.** O `doWork()` era um TODO. Implementado o fetch real em background (buscar, descriptografar, cachear). **Parcialmente correta** -- era necessaria, mas nao suficiente.
2. **Hipotese 2 (v1.9.6): Polling como fonte unica.** Removeu-se a notificacao do polling (E2EMessageListener) supondo que o FCM bastaria. **Incorreta** -- o PC nao tem FCM e dependia do polling. Revertido na v1.9.7.
3. **Hipotese 3 (v1.9.7): Deduplicacao faltante.** Restaurou polling + FCM e adicionou dedup por ID. **Correta mas insuficiente** -- PC voltou a funcionar, Android continuou sem notificar em background.
4. **Hipotese 4 (v1.9.8): Backend nao disparava push.** O campo `recipientUid` em `onMessageCreated.ts` nao existia no Firestore (o app grava `recipientId`). **Correta** -- causa raiz do push nunca ser enviado. Corrigido e deploy feito.
5. **Hipotese 5 (v1.9.9): Token registrado sob chave errada.** Validou-se que `currentAuthUid` = Firebase Auth UID = chave em `devicePushTokens`. **Cadeia consistente**, nao era problema.
6. **Hipotese 6 (v1.9.10): NotificationManagerCompat e hasNotificationPermission falhavam em background.** Investigada mas **descartada como causa isolada** -- o guard de permissao e o Compat podiam contribuir, mas nao eram a raiz.
7. **Causa raiz real (v1.9.10): Canal de notificacao nao criado em processo recriado a frio pelo FCM.** Quando o Android mata o processo e o FCM o recria para entregar um push, o `Application.onCreate` nao e chamado da mesma forma, e `createNotificationChannels()` nao havia sido executado. Sem canal registrado no sistema, `NotificationManager.notify()` descartava silenciosamente a notificacao.

### Causa raiz real (resolvida na v1.9.10)

**Canal de notificacao inexistente em processo cold-start (recriado pelo FCM).**

Quando o Android recria o processo para entregar um data message FCM, o canal de notificacao (`CHANNEL_NEW_CONVERSATIONS_ID`) podia nao existir ainda. O `NotificationManager.notify()` descarta silenciosamente notificacoes em canais inexistentes (Android 8.0+). O `catch (_: Throwable)` engolia qualquer erro sem log.

### Correcao definitiva

- Garantir `createNotificationChannels()` **antes de cada chamada** a `notify()` em `showPushNotification` (idempotente, custo zero se o canal ja existe).
- Usar `NotificationManager` direto do sistema (robusto em qualquer contexto: Service, Worker, BroadcastReceiver).
- Remover guard `hasNotificationPermission` de `showPushNotification` (desnecessario; o canal IMPORTANCE_HIGH ja garante entrega).
- Adicionar log de diagnostico para rastrear sucesso/falha.

### Resultado

v1.9.10 homologada 100% em Android (primeiro plano e segundo plano) e PC (Windows). Notificacao unica, sem duplicacao, em todos os cenarios.
