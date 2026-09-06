# Modelo de Ameaça e Postura de Segurança Pós-Quântica — Raix

**Versão:** 1.0 (P0.1 — Pós-Quântica Híbrida)  
**Data:** 06 de setembro de 2026  
**Status:** Implementado e Coberto por Testes Automatizados  

---

## 1. Visão Geral e Vetores de Ameaça

O **Raix** é projetado sob a premissa de **privacidade forte por design com retenção estritamente limitada de metadados**. O modelo de ameaça assume um adversário com capacidades estendidas em ambiente de rede e infraestrutura compartilhada:

### Atores e Vetores de Ameaça

| Ator / Ameaça | Capacidades e Motivação | Mitigação Implementada |
| :--- | :--- | :--- |
| **Atacante HNDL (*Harvest-Now-Decrypt-Later*)** | Gravação passiva em massa do tráfego de rede e dados de envelopes para decifragem futura quando um computador quântico criptograficamente relevante (CRQC) estiver disponível. | **KEM Híbrido NIST FIPS 203 (ML-KEM-768) + X25519**, assegurado pelo combiner NIST SP 800-227 / RFC 9180 HPKE. Conteúdo permanece indecifrável mesmo contra adversários quânticos. |
| **Atacante Ativo MitM (*Man-in-the-Middle*)** | Interceptação de tráfego, injeção de pacotes e tentativa de coerção de downgrade para protocolos clássicos (forçando apenas curvas elípticas Ed25519/X25519). | **Proteção Anti-Downgrade no Handshake (Emenda A.2)**: `minSecurityLevel` e suítes suportadas embutidas criptograficamente no payload autenticado (`pmsg-routing-v2`). Qualquer sessão abaixo do nível acordado é terminada com `DowngradeAttackException`. |
| **Operador de Nuvem / Servidor Comprometido** | Acesso ao banco de dados Firestore, snapshots ou memória do backend Cloud Functions. | Criptografia ponta-a-ponta (E2E) em nível de aplicação com envelopes selados (*SealedBox*). O servidor armazena apenas ciphertexts opacos da DEK e do conteúdo. Zero posse de chaves privadas. |
| **Adversário de Trânsito de Metadados** | Interceptação de cabeçalhos de transporte HTTP/2 e conexões QUIC para correlação de tráfego. | **TLS Pós-Quântico Híbrido (X25519MLKEM768 - Emenda A.4)** entre clientes e Google Frontends/Cloud Functions, eliminando HNDL sobre os metadados de transporte. |
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

## 3. Matriz de Concessões e Limitações Conhecidas

- **Plataformas com Modo Híbrido Total:** Desktop (JVM/Windows/Linux/macOS) e Android (via Bouncy Castle 1.79 nativo FIPS 203/204).
- **Plataforma Web (Wasm):** A WebCrypto nativa dos navegadores atuais não suporta primitivas pós-quânticas. O cliente Web opera com suporte clássico Ed25519/X25519 e modo degradado controlado, explicitamente indicado na interface do usuário, até a incorporação dos binários Wasm compilados de referência (C pq-crystals).
- **Metadados Transitórios:** Metadados de roteamento (`senderId`, `recipientId`) existem no Firestore estritamente pelo tempo de vida da mensagem transitória ($\le 24$h), sendo incinerados no momento da leitura (*Vanish*) ou pelo Shredder automatizado.
