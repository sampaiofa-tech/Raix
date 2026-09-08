# Arquitetura do Ecossistema Raix e Núcleo Criptográfico Compartilhado

> **STATUS:** Referência de Design / RFC de Arquitetura Futura (Roadmap Pós-Rodada)  
> **BASELINE:** Congelado — Nenhuma alteração de código neste ciclo  
> **DATA:** 22 de Maio de 2024 (Registro Formal)  
> **AUTOR:** Consenso Técnico (Guru Criptográfico ↔ Futuro / Arquitetura) + Validação Estratégica (Assessor)  
> **CLASSIFICAÇÃO:** Documentação Estratégica de Plataforma e Engenharia de Segurança  

---

## 1. Visão Executiva e Estratégia de Plataforma

A visão de longo prazo da **Raix** transcende um único aplicativo de mensageria segura. O ecossistema evolui para uma **Plataforma de Identidade e Privacidade por Design Pós-Quântica (PQC)**, ancorada no princípio da soberania individual sobre chaves criptográficas, credenciais e comunicações.

### 1.1 O Mnemônico BIP-39 como "Chave-Mestra da Soberania Digital"
No modelo tradicional, usuários gerenciam múltiplas identidades digitais dispersas, dependendo de terceiros centralizados, senhas fracas ou silos proprietários. No Ecossistema Raix, a posse exclusiva de **12 palavras mnemônicas (BIP-39 em Português / multilíngue)** constitui a raiz de confiança determinística de toda a vida digital do usuário.

A partir de uma única entropia de 128 bits guardada pelo usuário:
1. Deriva-se a identidade de mensageria inviolável e pós-quântica (**Raix**).
2. Deriva-se o cofre pós-quântico de senhas e dados confidenciais (**PQ-Vault**).
3. Habilitam-se capacidades futuras de soberania de dados, autenticação e proteção proativa (**Sentinela Digital**).

### 1.2 Separação Estrita de Produtos e Repositórios
Para resguardar a superfície de ataque, viabilizar modelos de negócio independentes e manter a governança ágil:
- **Raix (Mensageria Segura E2EE PQC):** Foco em comunicação privada, envelopes criptográficos opacos, destruição de metadados e chamadas ponto a ponto. Modelo freemium com foco em adoção e privacidade civil/empresarial.
- **PQ-Vault (Gerenciador de Credenciais e Cofre PQC Zero-Knowledge):** Produto separado (repositório e empacotamento distintos), monetizável de forma independente no mercado corporativo e consumidor de gerenciadores de senhas, atacando diretamente a vulnerabilidade pós-quântica dos cofres atuais.
- **Sentinela Digital (Agente de Defesa Ativa e Higiene Local):** Iniciativa de Pesquisa e Desenvolvimento (R&D) de longo prazo, focada em análise on-device, detecção de phishing e prova de integridade de IA, sem alocação de recursos de engenharia no presente ciclo.

---

## 2. O Núcleo Criptográfico Compartilhado: `raix-crypto-core`

O coração da estratégia de plataforma é o desacoplamento das primitivas fundamentais em uma biblioteca modular: o **`raix-crypto-core`**.

```
                           ┌────────────────────────────────────────┐
                           │            BIP-39 Mnemonic             │
                           │         (128 bits de Entropia)         │
                           └───────────────────┬────────────────────┘
                                               │
                                               ▼
                           ┌────────────────────────────────────────┐
                           │          BIP-39 Master Seed            │
                           │          (HMAC-SHA512 512 bits)        │
                           └───────────────────┬────────────────────┘
                                               │
                                               ▼
                           ┌────────────────────────────────────────┐
                           │           raix-crypto-core             │
                           │    (BIP-32 / BIP-44 HD Derivation)     │
                           └───────┬────────────────────────┬───────┘
                                   │                        │
         m/44'/RAIX'/0'            │                        │  m/44'/RAIX_VAULT'/0'
    (Separação de Domínio 1)       ▼                        ▼  (Separação de Domínio 2)
┌──────────────────────────────────────┐        ┌──────────────────────────────────────┐
│             PRODUTO RAIX             │        │          PRODUTO PQ-VAULT            │
│       (Mensageria E2EE PQC)          │        │      (Cofre de Senhas Zero-K)        │
├──────────────────────────────────────┤        ├──────────────────────────────────────┤
│ - X25519 + Ed25519 (Clássico)        │        │ - ML-KEM-768 (Cifragem de Cofre)     │
│ - ML-KEM-768 (KEM Pós-Quântico)      │        │ - ML-DSA-65 (Assinatura de Registros)│
│ - ML-DSA-65 (Assinatura Pós-Quântica)│        │ - Cifragem Simétrica Autenticada     │
│ - Double Ratchet PQC Híbrido         │        │   (ChaCha20-Poly1305 / AES-256-GCM)  │
│ - Safety Numbers e Anti-Downgrade    │        │ - Zero-Knowledge Auth & Sync         │
└──────────────────────────────────────┘        └──────────────────────────────────────┘
```

### 2.1 Componentes e Responsabilidades do `raix-crypto-core`
1. **Entropia e Mnemônico (BIP-39):**
   - Gerador de entropia criptograficamente seguro (CSPRNG nativo da plataforma).
   - Conversão determinística para lista de palavras (BIP-39 PT-BR e wordlists internacionais).
   - Validação estrita de checksum e normalização Unicode NFKD.
   - Derivação da semente mestre de 512 bits via PBKDF2-HMAC-SHA512.

2. **Derivação Hierárquica Determinística (BIP-32 / BIP-44):**
   - Implementação de derivação em árvore HD com chaves endurecidas (*hardened derivation* `i' = 2^31 + i`).
   - Isolamento matemático completo entre ramos irmãos da árvore.
   - Proibição absoluta de compartilhamento de chaves privadas entre diferentes escopos de aplicação.

3. **Primitivas Pós-Quânticas e Clássicas (Camada Híbrida):**
   - **KEM (Key Encapsulation Mechanism):** ML-KEM-768 (FIPS 203) para estabelecimento de segredos e encapsulamento de chaves de envelopes/cofres, pareado com X25519 (RFC 7748).
   - **Assinatura Digital (DSA):** ML-DSA-65 (FIPS 204) para autenticação e prova de posse, pareado com Ed25519 (RFC 8032).
   - **KDF e Hashing:** HKDF-SHA256, HKDF-SHA512 e Argon2id para proteção de dados em repouso e derivação de chaves de expansão.
   - **Cifragem Simétrica:** ChaCha20-Poly1305 e AES-GCM (256/512 bits) com garantia de autenticidade (AEAD).

4. **Abstração de Hardware Keystore e Destruição de Memória:**
   - Camada de integração com módulos de segurança de hardware (Android Keystore, Apple Secure Enclave / Keychain, Windows DPAPI / TPM).
   - Práticas mandatórias de sanitização de RAM (`wipe` imediato de arrays de bytes contendo material sensível).

---

## 3. Isolamento Criptográfico via Separação de Domínio HD (BIP-44)

Um dos pilares mandatórios da arquitetura é a **separação formal de domínio**. Usuários utilizarão a mesma frase mnemônica de recuperação para restaurar tanto o mensageiro quanto o cofre de senhas, porém **nenhuma chave é reutilizada entre os produtos**.

### 3.1 Esquema de Caminhos de Derivação (Derivation Paths)

Seguindo a estrutura normalizada pelo padrão BIP-44:
`m / purpose' / coin_type' / account' / change / address_index`

Adota-se uma alocação específica de domínio por produto:

| Produto / Contexto | Caminho de Derivação Base | Descrição e Finalidade Criptográfica |
| :--- | :--- | :--- |
| **Raix Mensageria (Identidade)** | `m/44'/9999'/0'` | Raiz das chaves de identidade e comunicação da mensageria (X25519, Ed25519, ML-KEM, ML-DSA). |
| **Raix Mensageria (Sessões E2EE)**| `m/44'/9999'/0'/0'/i'` | Derivação de chaves efêmeras e pré-chaves de sessão para o protocolo Double Ratchet PQC. |
| **PQ-Vault (Cofre Master)** | `m/44'/9998'/0'` | Chave-mestra de cifragem de envelopes do cofre de credenciais e senhas. |
| **PQ-Vault (Autenticação ZK)** | `m/44'/9998'/0'/1'/0'` | Credencial para prova de conhecimento zero (Zero-Knowledge Proof) junto a servidores de sincronização opaca. |
| **PQ-Vault (Compartilhamento)** | `m/44'/9998'/0'/2'/i'` | Par de chaves específico para recebimento e concessão de acesso seguro a cofres compartilhados. |
| **Sentinela Digital (R&D)** | `m/44'/9997'/0'` | Chaves de atestação local, verificação de integridade e assinatura de relatórios de auditoria on-device. |

### 3.2 Garantia Matemática de Não-Interferência
- **Derivação Hardened (`'`):** Todos os passos da raiz aos nós de aplicação utilizam índices endurecidos ($i \ge 2^{31}$). Isso impede matematicamente que o vazamento de uma chave pública ou privada em qualquer nó filho permita a recuperação de chaves privadas irmãs ou pai.
- **Independência de Risco:** Caso o usuário venha a ter seu cofre PQ-Vault comprometido em um endpoint infectado, **as chaves de mensageria Raix permanecem matematicamente inacessíveis**, e vice-versa.
- **Zero Vazamento de Metadados:** Os identificadores de domínio não são expostos na rede; apenas os resultados públicos derivados necessários para a operação de cada produto são utilizados em seus respectivos contextos.

---

## 4. PQ-Vault: Especificação do Produto e Modelo de Mercado

O **PQ-Vault** foi concebido para atender a uma vulnerabilidade crítica e iminente no mercado de cibersegurança: **a obsolescência dos gerenciadores de senhas diante da computação quântica**.

### 4.1 Problema de Mercado
Os principais gerenciadores de senha do mercado global (1Password, Bitwarden, LastPass, KeePass) operam com cifras e trocas de chaves baseadas em RSA, ECDH clássico ou AES derivado puramente de PBKDF2/Argon2 clássicos. Sob a ótica de ataques do tipo *"Harvest Now, Decrypt Later"* (Coletar Agora, Decifrar Depois), bases de senhas corporativas interceptadas e armazenadas em trânsito serão decifráveis com o advento de computadores quânticos funcionais (Algoritmo de Shor).

### 4.2 Proposta de Valor do PQ-Vault
1. **Cifragem Híbrida Pós-Quântica de Itens do Cofre:** Cada registro (senhas, chaves de API, notas seguras, identidades e chaves privadas) é encapsulado utilizando **ML-KEM-768** e cifrado com AES-256-GCM / ChaCha20-Poly1305.
2. **Assinatura e Imutabilidade com ML-DSA-65:** Modificações em registros do cofre são assinadas digitalmente pelo autor via ML-DSA, garantindo histórico infalsificável mesmo contra atacantes dotados de computação quântica.
3. **Arquitetura Zero-Knowledge Estrita:** O backend de sincronização opaca (se habilitado) jamais recebe texto claro, chaves mestre ou senhas derivadas. O servidor atua estritamente como repositório de blobs cifrados.
4. **Sincronização P2P e Backup Físico:** Habilidade de sincronizar entre dispositivos móveis e desktop via rede local cifrada (Local P2P via TLS-PQ / mDNS) ou através de backups herméticos blindados pelo mnemônico.

### 4.3 Posicionamento de Receita e Monetização Pós-Estabilidade
- **Produto e Venda Independentes:** O PQ-Vault não será embutido no aplicativo de mensageria Raix, mantendo ambos os apps leves, focados e com superfícies de auditoria isoladas.
- **Modelo de Licenciamento:**
  - *Community / Individual (Open-Source / Freemium):* Uso individual com cofre local e backup via mnemônico.
  - *Pro / Enterprise (Assinatura Paga):* Sincronização em nuvem zero-knowledge com alta disponibilidade, compartilhamento de cofres corporativos com controle granular de acesso, relatórios de conformidade e auditoria de vazamento de credenciais.
- **Timing:** O desenvolvimento e a comercialização do PQ-Vault iniciarão estritamente **após a conclusão da auditoria externa e a estabilização completa do Raix**.

---

## 5. Sentinela Digital: R&D de Longo Prazo (Defesa Ativa On-Device)

O **Sentinela Digital** representa a linha de pesquisa e desenvolvimento (R&D) da Raix para proteção ativa contra engenharia social avançada, ciberameaças geradas por IA e ataques dirigidos.

### 5.1 Escopo Conceitual
1. **Análise Heurística e Detecção de Phishing On-Device:**
   - Processamento de mensagens e links exclusivamente em memória volátil local no dispositivo do usuário, sem envio de conteúdo para nuvens de terceiros.
   - Detecção de padrões de extorsão, impersonificação, phishing de credenciais e golpes financeiros.
2. **Higiene de Arquivos e Sandboxing:**
   - Inspeção local de metadados e estrutura binária de anexos recebidos (documentos PDF, imagens, executáveis disfarçados).
   - Verificação de esteganografia e bloqueio de payloads maliciosos antes da renderização na UI.
3. **Assinatura e Atestação Criptográfica de Modelos de Linguagem (LLM Attestation):**
   - Assinatura criptográfica (ML-DSA) sobre inferências e respostas geradas por agentes locais para garantir que a resposta não foi alterada por middlewares maliciosos.
   - Cadeia de custódia e prova de integridade de verificações de segurança executadas localmente.

### 5.2 Diretriz Operacional: Sem Alocação de Recursos no Ciclo Atual
- O Sentinela Digital permanece formalmente classificado como **R&D de Baixa Prioridade Imediata**.
- Nenhum orçamento, tempo de desenvolvimento ou esforço de engenharia será direcionado a essa frente antes da consolidação de receita do PQ-Vault e maturidade do produto principal.

---

## 6. Priorização Estratégica Pós-Rodada (Roadmap de Execução)

Em conformidade com a decisão do Consenso Técnico (Guru Criptográfico ↔ Futuro) e a validação do Assessor de Investimentos, a ordem de precedência técnica e financeira pós-captação é **estrita e imutável**:

```
┌────────────────────────────────────────────────────────────────────────┐
│               PRIORIZAÇÃO ESTRATÉGICA PÓS-RODADA                       │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 1. AUDITORIA EXTERNA INDEPENDENTE (R$ 50k) ─────────── [PRIORIDADE P0] │
│    Auditoria de código, modelo de ameaças e primitivas criptográficas   │
│    por consultoria externa especializada e renomada.                   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Condição de Sucesso
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. MIGRAÇÃO PÓS-QUÂNTICA COMPLETA                                      │
│    Refinamento e consolidação das especificações finais FIPS 203/204    │
│    (ML-KEM / ML-DSA) no protocolo de mensageria Raix.                  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. DESENVOLVIMENTO E LANÇAMENTO DO PQ-VAULT                            │
│    Extração do raix-crypto-core, desenvolvimento do cofre zero-knowledge│
│    e abertura de nova linha de receita corporativa/B2B.                 │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. TRACK 2 (EXPANSÃO DE PLATAFORMA)                                    │
│    Suporte corporativo avançado, controles de política e integrações.  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. RECURSOS PREMIUM NO RAIX                                            │
│    Monetização no aplicativo principal (armazenamento estendido, etc.)  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 6. CONSULTORIA ESPECIALIZADA EM CRIPTOGRAFIA PQC                       │
│    Oferta de advisory e serviços técnicos para transição PQC de clientes│
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 7. SENTINELA DIGITAL (R&D DE LONGO PRAZO)                              │
│    Início das pesquisas práticas em segurança ativa e IA local.         │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.1 Justificativa das Dependências
- **A Auditoria Externa (R$ 50k) precede tudo:** Nenhum produto novo ou expansão de plataforma possui credibilidade sem o atesto formal e independente de auditores de criptografia reconhecidos.
- **A Migração PQ precede novos produtos:** O núcleo criptográfico deve estar auditado, estabilizado e formalmente padronizado antes de ser compartilhado com o PQ-Vault.
- **O PQ-Vault precede features secundárias:** Representa a maior alavanca de nova receita e diferenciação mercadológica imediata para a companhia.

---

## 7. Regras de Engenharia e Governança

1. **Baseline Congelado no Ciclo Atual:**
   - É estritamente proibido criar arquivos de código, stubs ou dependências para o `raix-crypto-core`, `PQ-Vault` ou `Sentinela Digital` no repositório atual antes da autorização formal de abertura de novos épicos pós-rodada.
2. **Higiene Criptográfica e Segurança Permanente:**
   - Todas as futuras implementações do núcleo deverão seguir rigorosamente as regras estabelecidas no [`AGENTS.md`](../AGENTS.md): custódia do mnemônico na UI com biometria, zero eco no terminal, supressão de segredos e testes herméticos em memória.
3. **Padrão Aberto e Auditável:**
   - As especificações de derivação e formatos de envelope deverão permanecer abertos e documentados para permitir escrutínio público e interoperabilidade segura.
