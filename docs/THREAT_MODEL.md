# Modelo de Ameaça e Postura de Segurança Pós-Quântica — Raix

**Versão:** 1.5 (P1+ — Governança Pós-Quântica, Defesa contra Análise de Tráfego por IA, Auditoria de Sanitização do Repositório e Disciplina Contínua de Governança)  
**Data:** 07 de setembro de 2026  
**Status:** Implementado, Verificado em CI/CD, Auditoria de Sanitização (7 Fases) Concluída e Governança Atualizada  

---

## 1. Visão Geral e Vetores de Ameaça

O **Raix** é projetado sob a premissa de **privacidade forte por design com retenção estritamente limitada de metadados**. O modelo de ameaça assume um adversário com capacidades estendidas em ambiente de rede, infraestrutura compartilhada e ferramentas avançadas de aprendizado de máquina / inteligência artificial:

### Atores e Vetores de Ameaça

| Ator / Ameaça | Capacidades e Motivação | Mitigação Implementada |
| :--- | :--- | :--- |
| **Atacante HNDL (*Harvest-Now-Decrypt-Later*)** | Gravação passiva em massa do tráfego de rede e dados de envelopes para decifragem futura quando um computador quântico criptograficamente relevante (CRQC) estiver disponível. | **KEM Híbrido NIST FIPS 203 (ML-KEM-768) + X25519**, assegurado pelo combiner NIST SP 800-227 / RFC 9180 HPKE. Conteúdo permanece indecifrável mesmo contra adversários quânticos. **Prioridade Crítica Permanente (P-C1 e P-C2)**: TLS-PQ na camada de transporte e evolução para Double Ratchet PQ pós-quântico. |
| **Atacante Ativo MitM (*Man-in-the-Middle*)** | Interceptação de tráfego, injeção de pacotes e tentativa de coerção de downgrade para protocolos clássicos (forçando apenas curvas elípticas Ed25519/X25519). | **Proteção Anti-Downgrade no Handshake (Emenda A.2)**: `minSecurityLevel` e suítes suportadas embutidas criptograficamente no payload autenticado (`pmsg-routing-v2`). Qualquer sessão abaixo do nível acordado é terminada com `DowngradeAttackException`. |
| **Adversário com Análise de Tráfego por IA (Track 2 — Ameaça Ativa)** | Correlação estatística avançada em tempo real de fluxos de rede via modelos de IA/ML, inferindo interlocutores por padrões temporais, volume, rajadas (*bursts*) de pacotes e grafos sociais, mesmo sob túneis cifrados ou onion routing simples. | **Defesa Ativa contra Análise de Tráfego por IA (Track 2)**: Mitigação em 3 fases progressivas calibradas no piloto: **Fase 1** (obrigatória/barata: padding a buckets 512B/1KB/2KB sem impactar latência + delivery delay estocástico ajustável); **Fase 2** (custo médio: cover traffic de mensagens-dummy com custo de banda + padding adaptativo); **Fase 3** (mixnet com batching). |
| **Adversário Quântico via Algoritmo de Grover (Track 1 — Hashing Quântico-Seguro)** | Redução quadrática da segurança de funções hash ($2^{n/2}$) por computador quântico (CRQC), rebaixando hashes de 256 bits para ~128 bits de entropia efetiva contra busca exaustiva. | **Hashing Quântico-Seguro (Track 1)**: Regra de ouro: nenhum hash de 256 bits para compromissos de longa duração. Derivação mnemônico$\to$seed mantida em PBKDF2-HMAC-SHA512 (preservando recovery BIP-39). Adoção de SHA-384 para `identityHash`, IDs de documentos e compromissos duradouros (192 bits de segurança quântica, anti length-extension, saída de 48 bytes). ML-DSA mantido com SHAKE256 nativamente PQ-safe. |
| **Engenharia Social Automatizada por IA (P-H1 — Hardening do Mnemônico)** | Campanhas adaptativas de phishing hiperpersonalizado, vishing, clones de voz e agentes de IA simulando suporte técnico para induzir o usuário a entregar a frase mnemônica BIP-39 (12 palavras). | **Hardware-Backed Keys (P-H1)**: Isolamento das chaves em silício seguro de hardware (StrongBox/TEE no Android, Secure Enclave no Apple, DPAPI/TPM no Windows) e **UX de Segurança Anti-Phishing**: alerta ostensivo de que o mnemônico NUNCA é solicitado por humanos ou por IA sob hipótese alguma. |
| **Comprometimento de Supply Chain por Código Gerado por IA (P0.4 Reforço)** | Introdução sutil de dependências alucinadas (*package hallucination*), backdoors lógicos ou enfraquecimento de invariantes criptográficas por assistentes de IA (Executores). | **Governança Estrita de Código por IA (P0.4)**: Revisão humana cética e minuciosa obrigatória de 100% do código gerado por IA, testes adversariais automatizados em CI e geração determinística de SBOM CycloneDX v1.5 com atestado SLSA Nível 2+ a cada release. |
| **Vazamento de Metadados e Rastreabilidade em Mídias (Track 5 — Hierarquia de 4 Níveis)** | Rastreamento e desanonimização de usuários através de metadados ocultos em anexos/mídias (EXIF, GPS, autor, timestamps) ou correlação de procedência por IA. | **Análise Forense e Sanitização em 4 Níveis (Track 5)**: **Nível 1** (local determinística, grátis, zero-rastro); **Nível 2** (IA on-device, padrão premium, zero-rastro real via TFLite/ONNX/Core ML; Web/Wasm limitado ou não oferecido; esteganografia básica); **Nível 3** (self-hosted com agente próprio pós-rodada, 100% sem rastro externo); **Nível 4** (IA externa terceirizada como último recurso, rastro reduzido — nunca zero-trace — sob consentimento explícito, anonimização prévia, minimização e provedor zero-retention). |
| **Operador de Nuvem / Servidor Comprometido** | Acesso ao banco de dados Firestore, snapshots ou memória do backend Cloud Functions. | Criptografia ponta-a-ponta (E2E) em nível de aplicação com envelopes selados (*SealedBox*). O servidor armazena apenas ciphertexts opacos da DEK e do conteúdo. Zero posse de chaves privadas. |
| **Adversário de Trânsito de Metadados** | Interceptação de cabeçalhos de transporte HTTP/2 e conexões QUIC para correlação de tráfego. | **TLS Pós-Quântico Híbrido (X25519MLKEM768 - Emenda A.4 / P-C1)**: Planejado para clientes nativos (Android/Desktop), com ativação quando suportado pelos runtimes (BoringSSL/Conscrypt/JVM) e infraestrutura GCP. **Limitação da Plataforma Web (Wasm)**: A versão Web não pode entregar ou garantir TLS pós-quântico, visto que navegadores não expõem a seleção de grupos TLS-PQ às páginas web (Web = cliente de menor garantia técnica). O conteúdo e as chaves contam com proteção pós-quântica E2E na camada de aplicação nos clientes suportados. |
| **SSL Stripping, MitM de Borda e Spoofing de E-mail (P2 & P3)** | Ataques de downgrade para HTTP não cifrado e envio de mensagens fraudulentas em nome do canal institucional `contato@raixtech.com`. | **HSTS Mandatório na Borda (`max-age=31536000; includeSubDomains; preload`)** em Cloudflare e GitHub Pages, aliado à proteção anti-phishing tríplice: **SPF (`include:_spf.google.com ~all`)**, **DKIM (RSA 2048-bit `google._domainkey`)**, **DMARC (`_dmarc.raixtech.com`)** e **DNSSEC ativo**. Regras de WAF e Super Bot Fight Mode configuradas no Cloudflare (ativação de proxy como hardening pós-reunião, preservando DNS-only pré-reunião para estabilidade do GitHub Pages). Detalhes em `docs/DNS_SECURITY.md`. |
| **Comprometimento de Chave de Longa Duração** | Exfiltração de chaves de identidade de longo prazo em momento futuro. | **Forward Secrecy Preservada (Emenda A.3)**: A DEK de cada mensagem é encapsulada via par de chaves X25519 efêmero e seed efêmera ML-KEM-768. O comprometimento das chaves de identidade não compromete mensagens passadas. Evolução contínua planejada para **P-C2 (Double Ratchet PQ)**. |

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

### 2.5 Hashing Quântico-Seguro, Nuances da Regra de Ouro e Primitivas (Track 1)
- **Impacto do Algoritmo de Grover sobre Funções Hash**:
  O algoritmo quântico de Grover acelera buscas não estruturadas com ganho quadrático, reduzindo a complexidade de busca de pré-imagem de $2^n$ para $2^{n/2}$. Como consequência, funções hash de 256 bits (ex.: SHA-256) oferecem ~128 bits de entropia efetiva contra um computador quântico criptograficamente relevante (CRQC).
- **Regra de Ouro Arquitetural e suas Nuances (Evitar Aplicação Excessiva)**:
  1. **$\ge 384$ bits é OBRIGATÓRIO para compromissos de longa duração**:
     - Qualquer identificador canônico de identidade (`identityHash`), chaves de documentos no Firestore, registros com retenção estendida de 180 dias do MCI (`HMAC-SHA-384`) e compromissos persistentes devem empregar hashes com $\ge 384$ bits de comprimento de saída.
  2. **256 bits é perfeitamente ACEITÁVEL para usos estritamente efêmeros**:
     - Derivação de chaves de envelope pontual (`SealedBox` KEK efêmera), chaves transitórias de transporte ($\le 24$h) e o checksum do BIP-39 (que atua estritamente como código detector de erro de digitação de 4 bits na interface, e não como compromisso criptográfico de longo prazo).
  3. **Proibição de Alteração Desnecessária de Hashing Efêmero**:
     - É proibido alterar primitivas efêmeras desnecessariamente. Em particular, **NÃO alterar o checksum do BIP-39**: qualquer alteração quebraria a interoperabilidade internacional e o algoritmo de recovery das 12 palavras, sem gerar nenhum ganho real de segurança pós-quântica.
- **Decisões Estruturais por Componente**:
  1. **Derivação Mnemônico $\to$ Seed**: **MANTER PBKDF2-HMAC-SHA512**.
     - *Justificativa*: A derivação clássica BIP-39 já emprega SHA-512 (512 bits), fornecendo 256 bits de segurança quântica efetiva sob Grover ($2^{256}$). **Não alterar essa derivação**, pois qualquer modificação quebraria a interoperabilidade e a capacidade do usuário de restaurar sua identidade a partir das 12 palavras originais (*wallet recovery*).
  2. **`identityHash`, IDs de Documentos e Compromissos Criptográficos**: **ADOTAR SHA-384**.
     - Fornece **192 bits de segurança quântica de colisão** sob Grover ($2^{384/2} = 2^{192}$), excedendo com folga a margem de segurança do NIST.
     - Naturalmente **resistente a ataques de extensão de comprimento** (*length-extension attacks*), visto que o SHA-384 utiliza o bloco de 64 bits do SHA-512 com truncamento e valores iniciais próprios (IVs distintos).
     - Gera saída compacta e balanceada de **48 bytes** (vs. 64 bytes do SHA-512), otimizando o overhead em identificadores de roteamento e chaves de documentos Firestore.
  3. **Pseudonimização de IP nos Logs de Conexão (P1.1 / MCI Art. 15)**: **ADOTAR HMAC-SHA-384**.
     - Adoção de **HMAC-SHA-384** (com salt rotativo mensal) para os registros de auditoria legal mantidos por 180 dias em `accessLogs`, garantindo conformidade rigorosa com a Regra de Ouro para dados persistidos.
  4. **Assinaturas Pós-Quânticas ML-DSA (FIPS 204)**: **MANTER SHAKE256 Interno**.
     - O esquema de assinatura ML-DSA utiliza internamente a função esponja extensível SHAKE256 (Keccak), que é nativamente resistente a ataques quânticos e plenamente alinhada ao FIPS 204.

### 2.6 Duas Frentes Pós-Quânticas Separadas: TLS-PQ (P-C1) e Double Ratchet PQ (P-C2)
A consolidação estratégica pós-rodada divide a frente pós-quântica em duas entregas sequenciadas, ambas **reusando diretamente as primitivas implementadas no P0.1 (ML-KEM-768 + X25519)**, sem necessidade de re-aprendizado criptográfico:

1. **Frente 1 — TLS Pós-Quântico (P-C1) [PRIMEIRO — ~1 a 2 semanas]**:
   - **Escopo**: Integração e configuração na camada de transporte HTTP/2 e QUIC dos clientes nativos (Android/Desktop), ativando o grupo híbrido `X25519MLKEM768` via BoringSSL / Conscrypt / JVM JSSE.
   - **Execução**: Alocada imediatamente como primeira entrega de transporte pós-rodada, blindando o canal de trânsito contra coleta passiva em massa (*Harvest-Now-Decrypt-Later*).
   - **Teto Técnico na Web (Wasm)**: Formalmente documentado como limitação da plataforma web — os navegadores comerciais não expõem a seleção de grupos TLS-PQ às páginas web (WebCrypto / `fetch` / `WebSocket`), mantendo a versão Web como cliente de menor garantia técnica de transporte.
2. **Frente 2 — Double Ratchet Pós-Quântico (P-C2) [DEPOIS — ~4 a 8 semanas]**:
   - **Escopo**: Continuação e evolução da camada criptográfica do P0.1 (`SealedBox` e envelopes efêmeros) para um protocolo *stateful* de Double Ratchet com KEM híbrido (PQXDH / Signal-style ratchet).
   - **KDF do Ratchet PQ**: Adoção mandatória de **HKDF-SHA512** para a evolução da chave-raiz (*root key*) e chaves de cadeia (*chain keys*). Enquanto o SHA-256 permanece aceitável para KEKs de mensagens efêmeras isoladas, o **HKDF-SHA512** é a escolha segura e consistente para a preservação contínua de segurança do estado pós-quântico.
   - **Execução**: Alocada na sequência do TLS-PQ, demandando baterias rigorosas de testes de máquina de estados, sessões concorrentes, perda/reordenação de mensagens e validação de *Break-in Recovery* e *Forward Secrecy* contínua pós-quântica por mensagem.
3. **Criptoagilidade Contínua**:
   - Princípio arquitetural permanente através das interfaces `SignatureScheme`, `KeyExchangeScheme` e versionamento `pmsg-routing-v2`, garantindo substituição rápida de algoritmos caso surjam novas recomendações do NIST.

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

- **Plataforma Web (Wasm) — Cliente de Menor Garantia Técnica:**
  - **Inviabilidade de TLS-PQ no Browser:** A plataforma Web (Wasm) **não pode entregar ou garantir TLS pós-quântico**, uma vez que os navegadores comerciais não expõem grupos TLS nem oferecem APIs JavaScript/Wasm para que a aplicação web influencie ou audite o handshake TLS da conexão de transporte (`fetch`/`WebSocket`).
  - **Ausência de PQC Nativo na WebCrypto:** As APIs nativas de WebCrypto dos navegadores não implementam as primitivas pós-quânticas FIPS 203/204. A versão Web opera com suporte clássico Ed25519/X25519 em modo degradado controlado e com aviso explícito na UI, até eventual viabilização de binários Wasm compilados de referência (C pq-crystals).
  - **Ausência de Keystore e Isolamento de Memória:** Sem suporte a hardware seguro (TEE/StrongBox/DPAPI) e sujeita à inspeção de memória por extensões ou Developer Tools do navegador.
- **Metadados Transitórios:** Metadados de roteamento existem no Firestore estritamente pelo tempo de vida da mensagem transitória ($\le 24$h), sendo incinerados no momento da leitura (*Vanish*) ou pelo Shredder automatizado a cada 15 minutos.
- **Réplicas de Infraestrutura e Snapshots Físicos:** Deleção de software no Firestore não purga instantaneamente mídias de armazenamento físicas de baixo nível do Google Cloud Platform (Colossus/Spanner). A segurança depende da destruição irreversível da DEK e do segredo compartilhado KEM (*Crypto-Shredding*).

### 4.1 Arquitetura de Anonimato e Defesa contra Análise de Tráfego por IA (Track 2 — Fases e Métricas de Piloto)
O modelo de ameaça trata a análise de tráfego via inteligência artificial / machine learning como uma **Ameaça Ativa**. Adversários com visibilidade de rede (ISPs, operadores de trânsito ou nós intermediários) utilizam modelos neurais treinados em correlação temporal, tamanho de rajadas (*bursts* de pacotes) e grafos sociais para correlacionar remetente e destinatário, superando defesas tradicionais de túneis cifrados e onion routing estático.

A mitigação é estruturada em **três fases progressivas**, condicionadas e calibradas com base em métricas coletadas no piloto de produção:

1. **Fase 1 (Obrigatória, Baixo Custo Operacional)**:
   - **Packet Padding a Buckets Discretos**: Normalização de todos os payloads cifrados para tamanhos discretos padronizados (**512 B, 1 KB e 2 KB**). Envelopes menores são preenchidos determinísticamente até o próximo bucket, impedindo que o tamanho exato da mensagem revele metadados de conteúdo.
   - **Atraso Aleatório de Entrega (*Delivery Delay*)**: Injeção de atrasos estocásticos calculados para quebrar a correlação direta de causa-efeito temporal entre envio e recepção.
   - **Trade-offs Documentados**:
     - O *padding* **NÃO compromete latência**, impactando unicamente o consumo de banda (overhead residual de bytes por envelope).
     - O *delay* de entrega é **ajustável** e será estritamente calibrado com base nas métricas de latência aceitável do piloto de produção, evitando degradação na fluidez da mensageria instantânea.
2. **Fase 2 (Custo Médio)**:
   - **Cover Traffic (Tráfego de Cobertura)**: Injeção periódica de mensagens-dummy indistinguíveis de payloads reais entre o cliente e o backend, mascarando períodos de atividade, inatividade e rajadas de digitação.
   - **Padding Adaptativo entre Relays**: Preenchimento dinâmico de tráfego entre nós de retransmissão intermediários.
   - **Trade-offs Documentados**:
     - O *cover traffic* **custa banda adicional** e consumo computacional em dispositivos móveis (impacto de bateria mitigado por agendamentos oportunistas de rádio).
3. **Fase 3 (Opcional, Custo Elevado)**:
   - **Mixnet com Batching Estocástico**: Camada de mixagem com reordenação de envelopes em lotes temporais aleatórios para casos de uso corporativos ou ambientes de vigilância estatal total, implementada sob demanda volumétrica comprovada.

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

### 6.3 Código Gerado por IA como Risco Contínuo de Supply Chain (P0.4 Reforço)
O emprego de assistentes e executores autônomos de Inteligência Artificial na engenharia de software introduz uma nova categoria permanente de risco na cadeia de suprimentos de software (*AI-Generated Code Supply Chain Threat*):
- **Vetores de Risco Mapeados**:
  - *Package Hallucination / Dependency Confusion*: IA sugerindo bibliotecas inexistentes que podem ser registradas por atacantes em registros públicos (npm, Maven Central, PyPI).
  - *Enfraquecimento Sutil de Invariantes*: Introdução de regressões imperceptíveis em verificações constant-time, regras de isolamento de memória, permissões do Firestore ou tratamento de exceções de downgrade.
  - *Injeção Inadvertida de Backdoors ou Falhas Lógicas*: Código gerado sem contexto adversarial completo que neutralize ataques de canal lateral ou vazamento de metadados.
- **Controles Institucionais Mandatórios de Governança**:
  1. **Revisão Humana de Segurança Rigorosa e Cética**: Todo commit contendo código sugerido ou gerado por IA deve passar por revisão humana formal com escrutínio focado em primitivas criptográficas, gerenciamento de memória e regras de acesso.
  2. **Baterias de Testes Adversariais Mandatórios**: O pipeline de CI/CD deve executar asserções adversariais automatizadas (ex.: `functions/test/rules.test.ts` e suites de integridade do cliente) antes de qualquer autorização de merge.
  3. **SBOM Determinístico e Atestação a Cada Release**: Atualização e validação contínua do inventário CycloneDX 1.5 (`docs/sbom/`) e atestado de proveniência criptográfica SLSA 2+ em todo build de produção, garantindo rastreabilidade integral.

---

## 7. Matriz Exaustiva de Metadados: "Quem Vê o Quê, por Quanto Tempo e Sob Quais Condições" (P1.1)

Em conformidade com a substituição de alegações de "zero-trace absoluto" por **"privacidade forte por design, com retenção estritamente limitada de metadados"**, a matriz a seguir especifica os limites matemáticos e operacionais de exposição de dados e metadados no sistema:

| Ator | O Que Vê (Metadados / Dados Visíveis) | O Que NÃO Vê (Garantia Matemática / Criptográfica) | Tempo de Retenção | Condições de Acesso / Hipótese |
| :--- | :--- | :--- | :--- | :--- |
| **Google Cloud Platform** (Provedor de Infraestrutura) | • Endereço IP e porta de transporte nos Load Balancers / Cloud NAT.<br/>• Timestamps de requisição HTTP/2 / QUIC.<br/>• Volume de tráfego e tamanho dos pacotes TLS.<br/>• Certificado TLS do servidor.<br/>• Envelopes cifrados transitórios em `identities/*/inbox/*`.<br/>• Ciphertexts de DEK em `messageKeys/*`. | • Conteúdo em claro de mensagens.<br/>• Chaves privadas de identidade (X25519, Ed25519, ML-KEM-768, ML-DSA-65).<br/>• Sementes e mnemônicos BIP-39.<br/>• DEK em claro (protegida por Sealed-Box híbrido).<br/>• Correlação estática de pares (emenda B.3). | • Logs de borda GCP: até 30 dias (padrão Cloud Logging).<br/>• Firestore: $\le 24$h (ou incinerado imediatamente no *Vanish-After-Read*). | Comprometimento administrativo de infraestrutura do GCP ou ordem judicial expedida sob jurisdição norte-americana / FISA / CLOUD Act. |
| **Operador do Raix** (Cat Tech / Administrador) | • Registro de conexão sob o MCI Art. 15: timestamp UTC e endpoint acessado (`/storeMessageKey`, `/getMessageKey`).<br/>• Hash HMAC-SHA-384 pseudonimizado do IP (com salt rotativo mensal, em estrita conformidade com a Regra de Ouro).<br/>• Fingerprint de roteamento da caixa postal.<br/>• Métricas agregadas de anomalias (`securityAnomalies`). | • Endereço IP bruto em repouso (descartado no ato da requisição).<br/>• Porta de conexão efêmera do usuário (não armazenada).<br/>• Identificador de hardware ou fingerprinting de dispositivo.<br/>• Conteúdo legível de conversas (*Zero-Knowledge*).<br/>• Chaves privadas ou sementes mnemônicas.<br/>• Grafo de interlocução remetente↔destinatário. | • `accessLogs` (MCI Art. 15): estritamente 180 dias com expurgo automático.<br/>• Envelopes efêmeros: $\le 24$h (ou destruição imediata no *Vanish*).<br/>• Anomalias: 30 dias. | Ordem judicial brasileira individualizada e fundamentada expedida por autoridade judiciária competente (MCI Art. 15, § 1º). |
| **Atacante de Rede Passivo / MitM / Análise de Tráfego por IA** (Operador de ISP, Roteador Wi-Fi, Grampo de Tráfego, Modelos Neurais de Fluxo) | • IPs de origem e destino da sessão TLS.<br/>• Porta de conexão e volume de bytes transmitidos.<br/>• SNI e domínio de conexão (`firebaseio.com`, `raixtech.com`).<br/>• Padrões temporais de atividade online e rajadas (*bursts*).<br/>• Correlação heurística via ML de timing e fluxo de envelopes. | • URLs exatas dos endpoints REST / Cloud Functions.<br/>• Conteúdo dos envelopes HTTP/2.<br/>• Chaves efêmeras e ciphertexts de mensagens.<br/>• Fingerprints de roteamento e identidades.<br/>• Conteúdo decifrado de qualquer natureza.<br/>• Correlação determinística quando ativos os controles da Track 2 (*packet padding*, *cover traffic* e *timing jitter* estocástico). | Ilimitado (caso o atacante grave tráfego passivo para cenário HNDL). | Interceptação física de cabos submarinos, roteadores ou redes locais. O KEM Híbrido PQC (ML-KEM-768 + X25519) e o TLS-PQ (P-C1) garantem que o conteúdo permanece indecifrável mesmo para atacantes quânticos com gravação perene. A Trilha 2 (Onion Routing, Mixnet, padding uniforme e tráfego de cobertura) quebra correlações estatísticas avançadas de IA. |
| **Atacante de Servidor Comprometido** (Invasor com Acesso ao Firestore / Admin SDK) | • Envelopes transitórios ativos não lidos (`payloadEncrypted` em AES-256-GCM).<br/>• Metadados de envelope: `createdAt`, `expiresAt`, `ephemeralPublicKey`.<br/>• Ciphertexts de `wrappedDek` cifrados com KEK híbrida.<br/>• Logs de conexão pseudonimizados com salt mensal (`accessLogs`). | • Texto plano de qualquer mensagem.<br/>• Chaves de identidade e decapsulação KEM (isoladas nos endpoints).<br/>• Chaves DEK em claro.<br/>• Histórico de mensagens antigas já incineradas pelo Shredder.<br/>• Associação de pares de conversa (inexistência de `senderHash` + `recipientHash` associados no mesmo doc). | Enquanto o documento não for purgado pelo Shredder ($\le 24$h). | Exfiltração de credenciais de serviço ou bypass de autenticação do backend. |
| **Destinatário Autorizado** (Interlocutor na Conversa) | • Conteúdo legível da mensagem decifrada.<br/>• Timestamp de emissão informado pelo remetente.<br/>• Mídias e anexos efêmeros decifrados.<br/>• Fingerprint e Safety Number dual de 60 dígitos do remetente. | • Frase mnemônica BIP-39 do remetente.<br/>• Chaves privadas do remetente.<br/>• IP ou porta do remetente.<br/>• Outras conversas ou caixas postais do remetente. | Controlado pela política de efemeridade local: até a leitura (*Vanish-After-Read*), expiração do temporizador local ($\le 24$h) ou acionamento voluntário do *Shake-to-Clear* / *Panic Wipe*. | Posse legítima do par de chaves privadas no dispositivo e autorização biométrica / PIN no client. |

---

## 8. Custódia em Keystore Nativa e Zeroização de Memória RAM Anti-DSE (P1.2)

### 8.1 Zeroização de Memória com Barreira Volátil Anti-Dead-Store Elimination
Compiladores JIT modernos (HotSpot JVM, Android ART, GraalVM) realizam análises de código morto agressivas (*Dead-Store Elimination - DSE*): rotinas como `Arrays.fill(buffer, 0)` são rotineiramente descartadas se a variável não for lida novamente antes de sua coleta pelo Garbage Collector.

Para mitigar dumps de memória em Windows Desktop e Android:
1. **Módulo `MemorySanitizer`:** Implementa uma barreira de memória via `@Volatile private var volatileSink: Int`.
2. **Ciclo de Zeroização:**
   - Preenche o array com zeros determinísticos (`0.toByte()` ou `' '`).
   - Executa uma leitura cumulativa XOR através do buffer e força a atribuição ao campo volátil:
     $$\text{volatileSink} = \sum_{i=0}^{N-1} (\text{buffer}[i] \oplus i)$$
   - A dependência de leitura do campo volátil quebra o grafo de análise de código morto do compilador, garantindo que a sobrescrita física na memória RAM seja executada.
3. **Buffers Intermediários Sanitizados:** Sementes intermediárias de derivação (`entropy`, `seed`, `rawPriv`, `rawMlDsaSeed`, `rawMlKemSeed`) são sanitizadas imediatamente após o uso.

### 8.2 Custódia em Keystore Nativa de Hardware
- **Android:** Utiliza `AndroidKeyStore` com suporte StrongBox / TEE. Chaves de criptografia e envelopes de identidade não residem desprotegidos no sistema de arquivos.
- **Desktop (Windows):** Cifragem via Windows Data Protection API (DPAPI / `Crypt32Util`), vinculando os dados cifrados à credencial física da conta de usuário do sistema operacional.
- **Apple (macOS / iOS):** Proteção delegada à Apple Keychain integrada com Secure Enclave.
- **Web (Wasm):** Alerta claro ao usuário sobre a ausência de keystore nativa de hardware, mantendo dados estritamente em memória volátil de sessão.

### 8.3 UX de Backup com Alerta de Alto Risco
A restauração da identidade criptográfica é de **responsabilidade exclusiva do usuário**. A interface do usuário (`IdentityScreen`):
- Exibe o mnemônico de 12 palavras exclusivamente sob demanda consciente.
- Apresenta aviso de alto risco enfatizando que a perda do mnemônico acarreta na perda permanente e irreversível da identidade e dos contatos.
- Recomenda expressamente o armazenamento em **gerenciador de senhas confiável offline, cofre físico ou hardware wallet**, desaconselhando screenshots e anotações digitais desprotegidas.

### 8.4 Proteção do Mnemônico contra Engenharia Social por IA e Hardening de Hardware (P-H1)
A ascensão de ferramentas avançadas de Inteligência Artificial generativa transforma o usuário no principal vetor de ataque através de engenharia social de alta fidelidade:
- **Ameaça Ativa de Engenharia Social por IA**:
  Agentes adversariais utilizam IA para clonagem de voz (vishing), geração de mensagens de phishing hiperpersonalizadas em canais de mensageria e automação de chatbots persuasivos fingindo suporte técnico, atualização de segurança urgente ou validação de conta para induzir a vítima a fornecer suas 12 palavras BIP-39.
- **Elevação de Prioridade de Hardware-Backed Keys (P-H1)**:
  - Elevação no roadmap para prioridade **P-H1**: isolamento físico das chaves mestras e derivadas diretamente em hardware criptográfico dedicado (**StrongBox / TEE** no Android, **Secure Enclave** no iOS/macOS e **TPM / DPAPI** no Windows).
  - Justificativa técnica: Sob custódia de hardware, mesmo que o sistema operacional host ou a camada de aplicação sofram comprometimento ou o usuário seja alvo de persuasão, as chaves privadas nunca deixam o enclave seguro em texto claro.
- **UX de Segurança Anti-Phishing Explícita**:
  A interface do Raix incorpora salvaguardas explícitas e indelúveis na tela de gerenciamento de chaves e visualização de backup:
  > **Aviso Permanente Anti-Engenharia Social**:  
  > *"O mnemônico de 12 palavras NUNCA é solicitado por nenhum desenvolvedor, funcionário, atendente, canal de suporte, sistema automatizado ou Inteligência Artificial sob hipótese alguma. Se qualquer pessoa ou IA solicitar essas palavras, trata-se inequivocamente de um golpe."*

### 8.5 Criptografia Local de Backup e Proteção do Mnemônico por Passphrase (Argon2id)
Nos cenários em que o aplicativo exporta backups cifrados de chaves e histórico local (backup E2E protegido por senha) ou opera em plataformas sem suporte a hardware seguro (ex.: Web/Wasm ou ambientes desktop em que DPAPI/TPM não estejam disponíveis):
- **Argon2id como KDF Mandatória de Backup (RFC 9106)**:
  - A derivação de chave simétrica a partir da passphrase do usuário emprega estritamente o **Argon2id** (3 iterações, 32 MB de RAM, paralelismo 1 via `Argon2Kmp`).
  - *Justificativa Adversarial*: O algoritmo PBKDF2 é suscetível a ataques de quebra de senha acelerados por hardware massivo (clusters de GPUs e ASICs dedicados) devido ao seu ínfimo consumo de memória RAM. O Argon2id é uma função comprovadamente *memory-hard*, exigindo alocação física de memória por thread e inviabilizando ataques de dicionário e força bruta em larga escala contra backups locais.
  - *Esforço de Engenharia*: Baixo esforço de integração, reaproveitando diretamente a implementação multiplataforma `Argon2Kmp` já validada no projeto.

---

## 9. Detecção Proativa de Anomalias de Acesso e Força Bruta (P1.3)

O módulo `functions/src/anomalyDetector.ts` introduz telemetria de contenção proativa no backend Cloud Functions, correlacionando eventos anômalos sem expor PII:

1. **Alerta de Leitura Anômala de Inbox por Identidade:**
   - Limiar configurável: **> 20 leituras por minuto** associadas ao mesmo `identityHash`.
   - Sinaliza potencial script malicioso ou enumeração automatizada de caixas postais.
   - Registrado como evento estruturado `ANOMALOUS_INBOX_READ_FREQUENCY` na coleção restrita `securityAnomalies`.
2. **Alerta de Tentativas Repetidas de Permission-Denied / Força Bruta:**
   - Limiar configurável: **> 5 rejeições por minuto** associadas à mesma origem (IP pseudonimizado).
   - Sinaliza sondagem não autorizada, tentativa de colheita de envelopes ou força bruta em rotas protegidas.
   - Registrado como evento estruturado `REPEATED_PERMISSION_DENIED` com correlação temporal.
3. **Isolamento de Segurança:**
   - A coleção `securityAnomalies` é protegida com `allow read, write: if false;` em `firestore.rules`, sendo manipulável exclusivamente pelo Firebase Admin SDK.

---

## 10. Governança de Criptoagilidade e Monitoramento Quântico Contínuo (Track 6)

A preparação pós-quântica do Raix não é uma fotografia pontual, mas um processo vivo de governança de segurança contínua para assegurar resiliência à medida que o estado da arte de computadores quânticos e criptoanálise avança:

### 10.1 Monitoramento Sistemático de Ameaças e Quebras de Primitivas
A equipe de engenharia e governança institucionaliza o monitoramento contínuo das seguintes fontes de inteligência técnica:
1. **Rastreamento no `ecdsa.fail`**:
   - Acompanhamento do observatório público `ecdsa.fail` e bases de vulnerabilidade de implementações criptográficas para identificação tempestiva de falhas de nonce, vulnerabilidades de canal lateral, ataques de curva elíptica e eventuais fraquezas descobertas em esquemas clássicos e híbridos.
2. **Normas e Publicações do NIST**:
   - Acompanhamento formal das emendas e padrões definitivos do NIST pós-quântico: **FIPS 203** (ML-KEM), **FIPS 204** (ML-DSA), **FIPS 205** (SLH-DSA), além das diretrizes para combiners híbridos em **NIST SP 800-227**.
   - Avaliação de algoritmos adicionais em padronização (ex.: candidatos stateless baseados em hash ou esquemas de assinaturas baseados em reticulados alternativos como FALCON).
3. **Roadmaps Globais de Hardware Quântico (CRQC Watch)**:
   - Acompanhamento trimestral dos roadmaps públicos de fornecedores de computação quântica de grande porte:
     - **IBM Quantum**: Metas de contagem de qubits físicos/lógicos e métricas de fidelidade (Heron, Flamingo, Kookaburra).
     - **Google Quantum AI**: Avanços em correção quântica de erros (QEC) e processadores Willow/Sycamore.
     - **QuEra Computing**: Avanços em processadores de átomos neutros e arquiteturas de computação quântica tolerante a falhas (FTQC).

### 10.2 Cadência Periódica de Revisão de Primitivas Criptográficas
- **Revisão Semestral Mandatória**: A cada 6 meses, o comitê de governança e arquitetura executa uma revisão formal de todas as suítes criptográficas ativas no código (`ML-KEM-768`, `ML-DSA-65`, `X25519`, `Ed25519`, `AES-256-GCM`, `Argon2id`, `HKDF-SHA256`).
- **Gatilhos de Rotação Criptoágil**: Caso uma primitiva atinja qualquer limiar de degradação teórica ou prática (ex.: novo ataque em reticulados, enfraquecimento de parâmetros ou aproximação de marco de CRQC), o mecanismo de criptoagilidade (`SignatureScheme`, `KeyExchangeScheme` e payload versionado `pmsg-routing-v2`) é acionado para rotacionar ou introduzir novas primitivas híbridas sem necessidade de refatoração do protocolo de transporte e persistência.

---

## 11. Análise Forense de Arquivos, Detecção de Rastros e Governança de IA (Track 5)

A introdução planejada do módulo de análise forense de arquivos e detecção de pegadas digitais (*footprint detection*) atende à demanda de proteção preventiva de usuários corporativos e de alta exposição, respeitando estritamente a **hierarquia final de 4 níveis** e os princípios de soberania e não-rastreabilidade:

### 11.1 Hierarquia de 4 Níveis e Modelo de Confiança
1. **Nível 1 (Local Determinística — Grátis / Core — Zero-Rastro)**:
   - Extração e auditoria forense de metadados puramente no dispositivo do usuário (*on-device*): dados EXIF, autor/proprietário, modelo de hardware, coordenadas GPS de localização, timestamps e softwares de edição.
   - **Garantia de Isolamento**: Processamento 100% local, custo operacional ~0 (base do modelo freemium), sem qualquer tráfego de rede, sem requisições remotas e com **ZERO-RASTRO**.
2. **Nível 2 (IA On-Device — Padrão Premium — Zero-Rastro Real)**:
   - **Detecção de Rastros On-Device**: Inspeção inteligente via modelos locais dedicados: detecção de capturas de tela (*screenshots*), correlação de metadados/EXIF e esteganografia básica.
   - **Zero-Rastro Real**: Processamento estritamente local no silício do cliente, com esforço estimado em ~8–16 semanas pós-rodada (**PADRÃO PREMIUM**). Nenhum dado deixa o dispositivo.
   - **Motores por Plataforma**: Integração nativa via TensorFlow Lite (TFLite) no Android, ONNX Runtime no Windows e Core ML no iOS futuro.
   - **Limitação da Plataforma Web (Wasm)**: A versão Web possui IA on-device limitada ou não oferecida devido a restrições de sandbox de navegador, tamanho de modelo e limitações de aceleração por GPU/NPU.
   - **Escopo Técnico (Esteganografia)**: Esteganografia avançada classificada como parcial (⚠️) — apenas técnicas básicas de inspeção de ruído e canais LSB são executadas on-device no Nível 2.
3. **Nível 3 (Self-Hosted / Servidor Próprio — Pós-Rodada)**:
   - **Infraestrutura Própria Dedicada**: Implantação de servidor próprio executando agente autônomo local dedicado (*self-hosted*), viabilizando análise aprofundada, inferência pesada, análise de origem e busca reversa via base própria com **100% sem rastro externo** e sem intermediação de terceiros comerciais.
4. **Nível 4 (IA Externa / Terceirizada — Rastro Reduzido / Último Recurso)**:
   - **Análise Opt-In com Rastro Reduzido**: IA externa acionada exclusivamente como **último recurso** para análises que demandam bases globais externas (ex.: busca reversa de imagem na web, inferência de origem ampla). **NUNCA é classificada como zero-trace / zero-rastro**.

### 11.2 Correção de Escopo: Vetores Não Zero-Rastro
- **Origem e Busca Reversa**: Não constituem operações zero-rastro por demandarem indexação e consulta a bancos/serviços externos. Foram explicitamente movidas para os **Níveis 3 e 4**, sendo vedada sua classificação como zero-rastro no Nível 2.
- **Esteganografia Avançada**: Requer análise profunda de anomalias estatísticas de alta dimensionalidade (classificada como parcial ⚠️); somente esteganografia básica é suportada localmente no Nível 2.

### 11.3 Mitigação de Rastro em Processamento Terceirizado (Nível 4)
Para qualquer processamento terceirizado em nuvem acionado como último recurso no Nível 4:
- **Anonimização Prévia**: Higienização e expurgo irrestrito de qualquer metadado pessoal identificável antes do despacho dos dados.
- **Minimização de Dados**: Envio exclusivo da versão mínima necessária para o processamento do modelo (ex.: imagem reduzida/downsampled ou recorte específico).
- **Provedores com Zero-Retention**: Contratação estrita de APIs corporativas com política formal de retenção zero (*zero-retention*), com garantia contratual de não-armazenamento e vedação de uso dos dados para retreino de IA.
- **Consentimento Explícito com Alerta de Trade-Off**: Interface com alerta ostensivo ao usuário detalhando os limites de privacidade e o trade-off do processamento antes de qualquer envio.

### 11.4 Linguagem de Marketing e Transparência na UX
- **Terminologia Rigorosa**: O Nível 2 é comercializado como *"detecção de rastros on-device"*; a designação *"100% sem rastro"* é reservada exclusivamente para processamento on-device (Níveis 1 e 2) e self-hosted (Nível 3). O Nível 4 é expressamente rotulado como *"análise opt-in com rastro reduzido"* (proibido o uso de termos como zero-trace).
- **UX de Transparência**: Inclusão de aviso transparente na UI do aplicativo: *"Em breve teremos uma forma 100% sem rastro de análise"*, enquadrado estritamente como o roadmap do servidor próprio (Nível 3), e NÃO como garantia antecipada incondicional.

### 11.5 Governança de Privacidade & Enquadramento DPA / ROPA (LGPD)
- **Nível 2 (On-Device)**: O processamento ocorre exclusivamente no hardware do usuário. A Cat Tech não coleta, transmite ou armazena os arquivos em sua infraestrutura, inexistindo operação de tratamento de dados pessoais no servidor (dispensado consentimento para envio à nuvem).
- **Nível 4 (Terceirizado)**: Constitui formalmente operação de tratamento de dados pessoais sob o **Art. 7º, I da LGPD (Consentimento Explícito)**, sujeita a anonimização prévia, minimização de dados, acordo de processamento com provedor parceiro com política de zero-retention e registro formal específico no ROPA e DPA.
 
---
 
## 12. Auditoria de Sanitização do Repositório e Disciplina Contínua de Governança
 
### 12.1 Conclusão da Auditoria de Sanitização (Commit `b5b57a1`)
 O repositório do **Raix** é mantido publicamente sob a licença **GNU AGPL-3.0**, assegurando auditabilidade aberta enquanto mantém o risco residual de segurança estritamente em **nível gerenciável e mínimo**.
 
 Em 07 de setembro de 2026, foi concluída com sucesso a **Auditoria Integral de Sanitização do Repositório (7 Fases)**, formalmente documentada em [`docs/AUDITORIA_SANITIZACAO_REPOSITORIO.md`](AUDITORIA_SANITIZACAO_REPOSITORIO.md):
 - **Zero Segredos e Chaves Privadas (Fase 1)**: Varredura de 100% do histórico e estado atual (`ggshield v1.54.0` e regex `git log -S`). Chaves públicas de cliente Firebase confirmadas como seguras por design (protegidas por App Check, regras Firestore *deny-by-default* e IAM). Falso-positivo de token sintético de teste unitário isolado em [`.gitguardian.yaml`](../.gitguardian.yaml). Zero credenciais de infraestrutura ou service accounts no repositório.
 - **Zero PII Real (Fase 2)**: Zero CPFs, telefones, endereços ou e-mails de usuários finais no código ou fixtures. Sementes BIP-39 (12 palavras) em testes usam apenas vetores fixos ou entropia aleatória transitória; zero seed phrases reais expostas.
 - **Zero Material Interno Confidencial (Fase 3)**: Documentação estritamente de código aberto. Zero IPs corporativos ou arquivos `.env` commitados.
 - **Histórico Íntegro e Purga Avaliada (Fase 4)**: Inexistência de segredos administrativos comprometidos; purga destrutiva desnecessária, preservando a imutabilidade e a cadeia de confiança do Git.
 - **Cadeia de Suprimentos & Dependências (Fase 5)**: Zero CVEs críticas ou altas (`npm audit`). Pinning de hashes SHA-256 no Gradle para Bouncy Castle 1.79. SBOM CycloneDX v1.5 determinístico versionado em `docs/sbom/`.
 - **Configuração de CI/CD (Fase 6)**: Workflows auditados sem segredos expostos, operando com permissões mínimas OIDC (`id-token: write`) e isolamento hermético.
 - **Verificação Final (Fase 7)**: 100% das suítes de teste (backend, emulador, índices, KMP Desktop e Android) verdes e pre-commit hook ativo.
 
### 12.2 Disciplina Contínua de Governança e Re-Auditoria Periódica
 A manutenção do repositório público e sanitizado exige governança ativa e ininterrupta:
 1. **GitGuardian Ativo e Mandatório**:
    - Bloqueio preventivo na estação de trabalho via hook pre-commit (`ggshield`).
    - Barreira de CI/CD em todas as Pull Requests e pushes para `main` (`.github/workflows/gitguardian.yml`).
    - Monitoramento contínuo em tempo real via dashboard institucional e alertas automáticos para `contato@raixtech.com`.
 2. **Ciclo de Dependências e SBOM por Release**:
    - Atualização periódica de dependências com verificação de integridade criptográfica.
    - Geração determinística e versionamento obrigatório de SBOMs CycloneDX v1.5 (`scripts/generate_sbom.cjs`) a cada nova versão ou release tag.
 3. **Revisão Rigorosa de Código (Especialmente Código Gerado por IA)**:
    - Todo código assistido ou gerado por agentes de IA é submetido a escrutínio humano rigoroso e testes adversariais antes de ser integrado, prevenindo *package hallucination*, degradação de invariantes criptográficas e vazamento acidental de chaves.
 4. **Re-Auditoria Periódica como Item Permanente de Governança**:
    - A auditoria completa de sanitização de 7 fases é institucionalizada como processo de governança periódica, detalhado na subseção 12.3 a seguir.

### 12.3 Protocolo Operacional de Re-Auditoria Periódica

A re-auditoria periódica é institucionalizada como processo permanente de governança contínua com cadência, gatilhos, escopo diferenciado e matriz de responsabilidade bem definidos:

#### 1. Cadência e Gatilhos
- **Gatilho por Evento**: A cada **release significativo** (mudança de stack técnica, adição/atualização de dependências críticas ou alterações na infraestrutura de nuvem).
- **Gatilho Temporal**: **A cada 6 meses** caso nenhum release com mudanças estruturais tenha ocorrido no período.
- **Regra Geral**: O que ocorrer primeiro (critério temporal de 6 meses ou evento de release significativo).

#### 2. Matriz de Escopo da Re-Auditoria
Nem todas as fases demandam a mesma profundidade quando não há alterações de infraestrutura, otimizando o esforço operacional sem degradar a segurança do repositório:

| Fase da Auditoria | Escopo na Re-Auditoria | Obrigatoriedade | Justificativa Técnica |
| :--- | :--- | :---: | :--- |
| **Fase 1 (Segredos)** | Varredura de segredos no histórico recente e árvore atual (`ggshield` + regex). | **SEMPRE** | Prevenir commit acidental de tokens, chaves ou credenciais. |
| **Fase 2 (PII)** | Busca por e-mails, CPFs, telefones, mnemônicos BIP-39 e dados reais em testes. | **SEMPRE** | Garantir que fixtures e novos testes continuem usando exclusivamente dados sintéticos. |
| **Fase 3 (Material Interno)** | Busca por planos, docs internos, credenciais privadas, IPs e arquivos `.env`. | **SEMPRE** | Evitar vazamento acidental de propriedade intelectual ou arquivos de ambiente. |
| **Fase 4 (Purga de Histórico)** | Avaliação da necessidade de reescrita via `git filter-repo` / BFG. | **Condicional** | Re-rodar apenas se uma violação real de segredo for detectada nas Fases 1–3. |
| **Fase 5 (Dependências/CVEs)** | Varredura de vulnerabilidades (`npm audit`), pinning de hashes SHA-256 e geração de SBOM. | **SEMPRE** | Novas CVEs públicas surgem continuamente mesmo sem mudanças de código. |
| **Fase 6 (Config / Infra)** | Auditoria de workflows de CI/CD, permissões OIDC, logs e chaves de nuvem. | **Condicional** | Re-rodar apenas se houver alteração em `.github/workflows/` ou na infraestrutura GCP/Firebase. |
| **Fase 7 (Verificação Final)** | Clone limpo, conferência de pre-commit e homologação formal do relatório. | **Condicional** | Execução plena quando houver mudanças relevantes em código ou dependências. |

#### 3. Matriz de Responsabilidade (Papéis de Governança)
- **Executor**: Responsável técnico pela execução operacional das fases de auditoria (executar scanners, testes adversariais, gerar SBOMs e redigir o relatório de evidências).
- **Analista**: Validador independente responsável pela conferência rigorosa dos critérios de aceite (verificar conformidade estrita de zero segredos/PII/CVEs e regras do `AGENTS.md`).
- **Principal (Liderança Técnica / DPO)**: Revisor final responsável pela aprovação formal do resumo executivo, aceite do risco residual e publicação institucional do relatório atualizado.




