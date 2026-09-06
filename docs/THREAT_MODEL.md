# Modelo de Ameaça e Postura de Segurança Pós-Quântica — Raix

**Versão:** 1.2 (P0.4 — Supply-Chain & Integridade de Cliente)  
**Data:** 06 de setembro de 2026  
**Status:** Implementado, Atestado em CI (SLSA 2+) e Coberto por Testes Automatizados  

---

## 1. Visão Geral e Vetores de Ameaça

O **Raix** é projetado sob a premissa de **privacidade forte por design com retenção estritamente limitada de metadados**. O modelo de ameaça assume um adversário com capacidades estendidas em ambiente de rede e infraestrutura compartilhada:

### Atores e Vetores de Ameaça

| Ator / Ameaça | Capacidades e Motivação | Mitigação Implementada |
| :--- | :--- | :--- |
| **Atacante HNDL (*Harvest-Now-Decrypt-Later*)** | Gravação passiva em massa do tráfego de rede e dados de envelopes para decifragem futura quando um computador quântico criptograficamente relevante (CRQC) estiver disponível. | **KEM Híbrido NIST FIPS 203 (ML-KEM-768) + X25519**, assegurado pelo combiner NIST SP 800-227 / RFC 9180 HPKE. Conteúdo permanece indecifrável mesmo contra adversários quânticos. |
| **Atacante Ativo MitM (*Man-in-the-Middle*)** | Interceptação de tráfego, injeção de pacotes e tentativa de coerção de downgrade para protocolos clássicos (forçando apenas curvas elípticas Ed25519/X25519). | **Proteção Anti-Downgrade no Handshake (Emenda A.2)**: `minSecurityLevel` e suítes suportadas embutidas criptograficamente no payload autenticado (`pmsg-routing-v2`). Qualquer sessão abaixo do nível acordado é terminada com `DowngradeAttackException`. |
| **Operador de Nuvem / Servidor Comprometido** | Acesso ao banco de dados Firestore, snapshots ou memória do backend Cloud Functions. | Criptografia ponta-a-ponta (E2E) em nível de aplicação com envelopes selados (*SealedBox*). O servidor armazena apenas ciphertexts opacos da DEK e do conteúdo. Zero posse de chaves privadas. |
| **Adversário de Trânsito de Metadados** | Interceptação de cabeçalhos de transporte HTTP/2 e conexões QUIC para correlação de tráfego. | **TLS Pós-Quântico Híbrido (X25519MLKEM768 - Emenda A.4)**: Planejado, ativação plena quando suportado pelo cliente/runtime (Conscrypt/BoringSSL/JVM JSSE) e infraestrutura GCP. O conteúdo e chaves já possuem proteção pós-quântica E2E na camada de aplicação. |
| **Comprometimento de Chave de Longa Duração** | Exfiltração de chaves de identidade de longo prazo em momento futuro. | **Forward Secrecy Preservada (Emenda A.3)**: A DEK de cada mensagem é encapsulada via par de chaves X25519 efêmero e seed efêmera ML-KEM-768. O comprometimento das chaves de identidade não compromete mensagens passadas. |

---

## 2. Arquitetura Pós-Quântica Híbrida (NIST FIPS 203 & 204)

Conforme as emendas técnicas e recomendações do NIST:

### 2.1 Autenticação e Prova de Posse (ML-DSA-65 + Ed25519)
- **ML-DSA (FIPS 204 Level 3)** substitui e complementa a assinatura clássica Ed25519.
- **Crypto-Agility**: A camada de segurança opera sob a interface abstrata `SignatureScheme`, permitindo evolução contínua de suítes sem acoplamento a literais no código.
- **Assinatura Híbrida Composta**: A verificação da prova de posse de roteamento e autenticação de identidade exige semântica booleana `AND` estrita:
  $$\text{Verify}_{\text{hybrid}}(m, \sigma) = \text{Verify}_{\text{Ed25519}}(m, \sigma_{\text{Ed}}) \land \text{Verify}_{\text{ML-DSA-65}}(m, \sigma_{\text{ML-DSA}})$$
  Se qualquer um dos algoritmos falhar ou for corrompido, a assinatura inteira é rejeitada.

### 2.2 Acordo de Chaves E2E e Combiner Híbrido (ML-KEM-768 + X25519)
- **Combiner NIST SP 800-227 / RFC 9180 (HPKE)**:
  1. Gera chave efêmera clássica $pk_e^{\text{X25519}}$ e calcula $SS_{\text{classical}} = \text{X25519}(sk_e, pk_{\text{Bob}})$.
  2. Executa encapsulamento pós-quântico FIPS 203: $(c_{\text{ML-KEM}}, SS_{\text{PQC}}) = \text{Encaps}(pk_{\text{Bob}}^{\text{ML-KEM}})$.
  3. Derivação do Segredo Combinado via HKDF-SHA256:
     $$SS_{\text{combined}} = \text{HKDF-Extract}(\text{salt}=\emptyset, IKM = SS_{\text{classical}} \parallel SS_{\text{PQC}})$$
     $$\text{KEK} = \text{HKDF-Expand}(SS_{\text{combined}}, \text{info} = \text{"raix-pqc-hybrid-kem-v1"}, L = 32)$$
  4. A chave KEK derivada cifra a Chave de Encriptação de Dados (DEK) com AES-GCM-256 e autenticação adicional vinculada ao fingerprint do destinatário.

### 2.3 Proteção Anti-Downgrade Mandatória
A emenda A.2 estipula que dispositivos híbridos não podem ser rebaixados silenciosamente:
1. Ao assinar a atualização de roteamento e capacidade, a mensagem autenticada inclui:
   `pmsg-routing-v2|fingerprint|authUid|timestamp|minSecurityLevel|supportedSuites`
2. Na decifragem do envelope `SealedBox`, caso o destinatário possua capacidade híbrida mas receba um envelope degradado sem justificativa de compatibilidade de par legado autenticado, o envelope é rejeitado com `DowngradeAttackException`.

### 2.4 Derivação Dual a partir de BIP-39 e Safety Numbers
- Uma única semente BIP-39 (12 palavras) deriva deterministicamente ambos os conjuntos de chaves via Argon2id com salts isolados:
  - Salt `pmsg-identity-x25519-salt-v1`: Chaves clássicas (X25519 e Ed25519).
  - Salt `pmsg-identity-mldsa-salt-v1`: Semente FIPS 204 ML-DSA-65.
  - Salt `pmsg-identity-mlkem-salt-v1`: Semente FIPS 203 ML-KEM-768.
- **Safety Number de 60 Dígitos (Signal-Style Híbrido)**:
  O número de segurança de verificação presencial/verbal é gerado a partir do hash combinado de todas as quatro chaves públicas ($\text{Pub}_{\text{X25519}} \parallel \text{Pub}_{\text{Ed25519}} \parallel \text{Pub}_{\text{ML-KEM}} \parallel \text{Pub}_{\text{ML-DSA}}$), garantindo que a comparação manual cobre simultaneamente a segurança clássica e a pós-quântica.

---

## 3. Isolamento e Regras de Acesso do Firestore (P0.2 & Emenda B.3)

O armazenamento de metadados de roteamento e envelopes efêmeros no Cloud Firestore segue uma política estrita de defesa em profundidade:

### 3.1 Deny-by-Default na Raiz
- Regra raiz explícita: `match /{document=**} { allow read, write: if false; }`.
- Nenhum caminho ou coleção não declarada concede leitura ou gravação.

### 3.2 Estrutura Baseada na Raiz de Identidade
- Não existem coleções top-level de conteúdo acessíveis livremente: todo o fluxo efêmero é organizado sob `identities/{identityHash}/inbox/{envelopeId}`.
- O `identityHash` é a impressão digital criptográfica derivada do mnemônico e chaves de identidade (SHA-256 da chave pública), impedindo o desacoplamento de identidade e autenticação.
- O acesso à leitura e descarte de envelopes em `inbox` é restrito com exclusividade ao titular legítimo que comprovou posse de `currentAuthUid`.

### 3.3 Anti-Correlação de Identidades no Roteamento (Emenda B.3)
- O modelo de envelopes efêmeros proíbe o armazenamento simultâneo de ambos os hashes de identidade (`senderHash` e `recipientHash`) no mesmo documento de rota.
- Envelopes transitórios são endereçados diretamente à caixa de entrada do destinatário (`identities/{recipientHash}/inbox/{envelopeId}`), acompanhados unicamente de tokens efêmeros de sessão ou chaves descartáveis, eliminando o grafo de correlação estático entre remetente e destinatário em caso de comprometimento da visão de leitura.

### 3.4 Isolamento Absoluto de Chaves e Auditoria (DEK Isolation)
- Coleções `messageKeys` (DEKs cifradas), `accessLogs`, `connectionLogs`, `abuseReports`, `abuseMetrics` e `userRateLimits` possuem regra de bloqueio total para clientes SDK (`allow read, write: if false;`).
- Toda interação com chaves mestras e registros de conexão do Marco Civil da Internet é restrita ao Firebase Admin SDK executado em ambiente protegido (Cloud Functions).

### 3.5 Teste Adversarial em CI/CD
- Todas as regras são submetidas a 20+ testes adversariais automatizados no Firebase Emulator Suite (`functions/test/rules.test.ts`), executados a cada PR e push via GitHub Actions (`.github/workflows/firestore-rules.yml`). Qualquer violação de isolamento bloqueia o merge.

---

## 4. Matriz de Concessões e Limitações Conhecidas

- **Plataformas com Modo Híbrido Total:** Desktop (JVM/Windows/Linux/macOS) e Android (via Bouncy Castle 1.79 nativo FIPS 203/204).
- **Plataforma Web (Wasm):** A WebCrypto nativa dos navegadores atuais não suporta primitivas pós-quânticas. O cliente Web opera com suporte clássico Ed25519/X25519 e modo degradado controlado, explicitamente indicado na interface do usuário, até a incorporação dos binários Wasm compilados de referência (C pq-crystals).
- **Metadados Transitórios:** Metadados de roteamento existem no Firestore estritamente pelo tempo de vida da mensagem transitória ($\le 24$h), sendo incinerados no momento da leitura (*Vanish*) ou pelo Shredder automatizado a cada 15 minutos.
- **Réplicas de Infraestrutura e Snapshots Físicos:** Deleção de software no Firestore não purga instantaneamente mídias de armazenamento físicas de baixo nível do Google Cloud Platform (Colossus/Spanner). A segurança depende da destruição irreversível da DEK e do segredo compartilhado KEM (*Crypto-Shredding*).

---

## 5. Tratamento do Gap de Destruição e Ciclo de Vida do Envelope (P0.3)

O ciclo de destruição de dados efêmeros aborda as três camadas críticas de retenção residual:

### 5.1 Frequência e Idempotência do Shredder Ativo
- **Cloud Scheduler (15 minutos):** O job agendado opera em cadência de `*/15 * * * *` (4x por hora), executando batch-deletes atômicos no Firestore.
- **Idempotência Estrita:** Operações de deleção sobre documentos já removidos ou processados concorrentemente são não-bloqueantes e idempotentes, garantindo consistência sem corrupção de estado.
- **TTL Nativo como Fail-Safe:** O recurso de TTL nativo do Cloud Firestore atua exclusivamente como salvaguarda passiva de último recurso; a destruição tempestiva depende do Shredder ativo e do gatilho reativo `onDeleteMessage` acionado no momento da leitura (*Vanish-After-Read*).

### 5.2 Alinhamento Estrito do Lifecycle e Regras de Segurança
- As regras de segurança do Firestore (`firestore.rules`) rejeitam no ato da escrita qualquer envelope cuja propriedade `expiresAt` seja nula, ausente, retroativa ou superior ao tempo atual acrescido de 24 horas (`request.time + duration.value(24, 'h')`), neutralizando o risco de injeção de documentos persistentes não-expiráveis por clientes adulterados.

### 5.3 Point-in-Time Recovery (PITR) e Snapshots do Provedor
- **Política de PITR:** O Point-in-Time Recovery (PITR) mantém um log contínuo de alterações por até 7 dias no Firestore. Para dados efêmeros de mensageria (`identities/*/inbox`, `messages`, `messageKeys`), o PITR deve permanecer **DESABILITADO** no banco operacional.
- **Transparência sobre Backups de Nuvem:** O sistema documenta expressamente que chamadas do Admin SDK e o TTL nativo destroem registros no nível do banco de dados, mas **NÃO expurgam réplicas em repouso de baixo nível nem backups gerenciados do provedor** até a expiração da política de retenção global da nuvem. O compromisso de segurança baseia-se na **inviabilização criptográfica definitiva**: destruída a DEK, qualquer cópia física remanescente no provedor torna-se permanentemente indecifrável.

### 5.4 Monitoramento Contínuo e Escalonamento Operacional
- **Métrica de Latência de Destruição:** Cada execução do Shredder calcula a diferença temporal $\text{delayMs} = T_{\text{current}} - T_{\text{expiresAt}}$ e exporta a métrica `ttl_expiration_to_deletion_delays`.
- **Gatilhos de Alerta com Escalonamento:**
  - **Alerta Nível 1 (Warning):** Disparado quando qualquer documento sobrevive $\ge 60$ minutos além de sua expiração programada.
  - **Alerta Nível 2 (Crítico):** Disparado e escalado quando o atraso de sobrevivência atinge $\ge 180$ minutos (3 horas), sinalizando potencial anomalia no Cloud Scheduler ou degradação na API do Firestore.
  - **Janela Residual Máxima (Fail-Safe):** Caso tanto o Shredder quanto o gatilho reativo falhem simultaneamente, a janela residual pode atingir de 24h a 48h (limite do processador de TTL assíncrono do Firestore). O limiar de 60 minutos é um gatilho de monitoramento e alarme, e **não** uma garantia de expurgo físico total em 1 hora.

---

## 6. Integridade de Cliente e Cadeia de Suprimentos (P0.4 & Emenda Guru B.1)

### 6.1 O Axioma de Confiança e o Limite do Servidor
A alegação central de segurança do Raix afirma que:
$$\text{Comprometimento Total do Servidor} \implies \text{Adversário obtém unicamente Ciphertext}$$
No entanto, sob a análise adversarial formal da Emenda B.1:
> **Sem integridade de cliente verificável, essa garantia desmorona.**  
> Se o cliente for adulterado ou a cadeia de suprimentos for envenenada, as chaves privadas e o texto em claro podem ser exfiltrados diretamente da memória do endpoint, contornando a criptografia pós-quântica híbrida.

### 6.2 Camadas de Defesa de Supply Chain Implementadas
1. **Fixação Estrita de Versões e Hashes Criptográficos:**
   - Gradle Dependency Verification (`gradle/verification-metadata.xml`) bloqueia builds se qualquer dependência (como Bouncy Castle FIPS 203/204) sofrer alteração em seu hash SHA-256 oficial.
   - Cloud Functions opera sob `functions/package-lock.json` com validação de hashes SHA-512 e `npm ci --ignore-scripts`.
2. **Software Bill of Materials (SBOM) Versionado:**
   - Inventário transparente gerado no formato padrão CycloneDX 1.5 JSON para o cliente Multiplatform (`docs/sbom/sbom-composeApp.json`) e para o backend (`docs/sbom/sbom-functions.json`).
3. **Builds Reproduzíveis e Atestado SLSA Nível 2+:**
   - Esteira de integração contínua ([`.github/workflows/supply-chain-attestation.yml`](file:///c:/Dev/Pmsg/.github/workflows/supply-chain-attestation.yml)) gera atestados criptográficos de proveniência assinados via Sigstore/Fulcio (`actions/attest-build-provenance`).
4. **Detecção de Tampering no Runtime (`RuntimeIntegrityVerifier`):**
   - O aplicativo audita seu próprio ambiente em tempo de execução:
     - **Desktop:** Detecta agentes de instrumentação Java (`-javaagent`), protocolos de depuração JDWP (`-agentlib:jdwp`) e manipulações de ClassLoader.
     - **Android:** Detecta binários de root (`su`), injeção dinâmica de frameworks de hooking (Frida em portas 27042 e `/proc/self/maps`, Xposed) e anexação de depuradores.
     - **Web/Wasm:** Alerta sobre a execução em contexto de navegador sem garantias de isolamento de memória contra DevTools.

