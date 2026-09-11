# Master Plan do Ecossistema Fortaleza RAIX (2024–2027)
## Rumo ao Estado Digital Soberano: Arquitetura Estratégica em 6 Camadas

> **STATUS:** Referência Estratégica Oficial / Visão Sistêmica de 3 Anos  
> **BASELINE:** Congelado — Nenhuma alteração ou escopo de código novo neste ciclo  
> **DATA DO CONSENSO:** 22 de Maio de 2024 (Registro Formal e Definitivo)  
> **CHANCELA:** Consenso Técnico Conjunto (Guru Criptográfico ↔ Futuro/Arquitetura) + Validação Executiva (Assessor de Investimentos)  
> **CLASSIFICAÇÃO:** Documento Estratégico Diretor (Sem mais discussões pendentes)  
> **DOCUMENTO-MESTRE:** [ESTADO_RAIX.md](ESTADO_RAIX.md) (Fonte Única de Verdade do Projeto v2.0)  

---

## 1. Visão Executiva: RAIX como Estado Digital Soberano

A evolução do ecossistema RAIX no horizonte de três anos projeta a transição de um aplicativo de mensageria segura para a construção de um **Estado Digital Soberano** centrado no indivíduo e na organização autônoma.

Em um cenário geopolítico e tecnológico marcado pela vigilância estatal onipresente, quebra algorítmica iminente de cifras clássicas pela computação quântica (*Harvest Now, Decrypt Later*) e monetização predatória de metadados por corporações centralizadas, a **Fortaleza RAIX** ergue-se sob a premissa de que a soberania digital não é concedida por terceiros, mas matematicamente garantida por infraestrutura própria e verificável.

### 1.1 O Epicentro Gravitacional: `raix-crypto-core`
Todo o ecossistema orbita um núcleo criptográfico unificado, modular e hermético: o **`raix-crypto-core`**.
- **Raiz de Confiança Determinística:** O mnemônico BIP-39 (12 palavras, 128 bits de entropia) funciona como a "Chave-Mestra da Soberania".
- **Separação Rígida de Domínios:** Derivações hierárquicas endurecidas (BIP-32/BIP-44) garantem que nenhum segredo ou chave seja reutilizado entre diferentes produtos ou camadas.
- **Criptografia Pós-Quântica (PQC) Híbrida:** Adoção mandatória das normas do NIST (FIPS 203 ML-KEM-768 pareado com X25519; FIPS 204 ML-DSA-65 pareado com Ed25519), combinadas a cifras simétricas autenticadas (ChaCha20-Poly1305 / AES-256-GCM) e algoritmos KDF resistentes a hardware dedicado (Argon2id).
- **Especificação Técnica de Derivação:** O mapeamento matemático de derivação HD (BIP-44) e detalhamento de isolamento de chaves encontra-se formalizado em [`ARQUITETURA_ECOSSISTEMA.md`](ARQUITETURA_ECOSSISTEMA.md).

---

## 2. As 6 Camadas da Fortaleza RAIX

A arquitetura da Fortaleza RAIX é estruturada em **6 camadas concêntricas e interdependentes**, organizadas hierarquicamente da base criptográfica fundamental até a governança autônoma:

```
                      ┌────────────────────────────────────────┐
                      │        CAMADA 6: GOVERNANÇA            │
                      │    (Consenso, Atestação, Federação)    │
                      └───────────────────┬────────────────────┘
                                          │
                      ┌───────────────────▼────────────────────┐
                      │    CAMADA 5: CONTRA-INTELIGÊNCIA       │
                      │  (RFI Forense, Custódia, Travamento)   │
                      └───────────────────┬────────────────────┘
                                          │
                      ┌───────────────────▼────────────────────┐
                      │          CAMADA 4: DEFESA              │
                      │   (Sentinela Digital, Higiene Local)   │
                      └───────────────────┬────────────────────┘
                                          │
                      ┌───────────────────▼────────────────────┐
                      │        CAMADA 3: COMUNICAÇÃO           │
                      │   (RAIX Messaging E2EE PQC Híbrido)    │
                      └───────────────────┬────────────────────┘
                                          │
                      ┌───────────────────▼────────────────────┐
                      │          CAMADA 2: ATIVOS              │
                      │   (PQ-Vault - Cofre Zero-Knowledge)    │
                      └───────────────────┬────────────────────┘
                                          │
                      ┌───────────────────▼────────────────────┐
                      │         CAMADA 1: FUNDAÇÃO             │
                      │  (raix-crypto-core, BIP-39, Hardware)  │
                      └────────────────────────────────────────┘
```

### 2.1 Camada 1: Fundação (A Base Criptográfica)
- **Status:** Base consolidada / Núcleo em fase de amadurecimento e abstração modular.
- **Papel:** Provedor absoluto de entropia, derivação determinística e encapsulamento pós-quântico para todas as camadas superiores.
- **Atributos Principais:**
  - Gerador seguro de entropia local (CSPRNG nativo da plataforma).
  - Gestão do mnemônico BIP-39 (12 palavras) exclusivamente na interface do usuário (UI) mediante biometria ou PIN local, em conformidade com as diretrizes do `AGENTS.md`.
  - Isolamento de chaves via Hardware Keystore / Secure Enclave / TPM.
  - Sanitização obrigatória de memória RAM (`zeroize` / destruição imediata de material sensível pós-uso).

### 2.2 Camada 2: Ativos (PQ-Vault — A Próxima Alavanca de Receita)
- **Status:** Próxima frente de desenvolvimento e monetização pós-estabilização do mensageiro.
- **Papel:** Cofre pós-quântico de credenciais, identidades, chaves privadas, certificados e dados confidenciais com arquitetura Zero-Knowledge estrita.
- **Modelo de Negócio e Sustentabilidade:**
  - Produto com repositório, empacotamento e ciclo de vida separados da mensageria, mantendo ambos os produtos leves e com superfície mínima de auditoria.
  - Ataca a obsolescência crítica dos gerenciadores legados frente à descriptografia pós-quântica retrospectiva (*Harvest Now, Decrypt Later*).
  - Canal direto de monetização de curto prazo (planos individuais e corporativos B2B), gerando o fluxo de caixa que financiará o desenvolvimento das camadas de P&D (R&D) de longo prazo.

### 2.3 Camada 3: Comunicação (RAIX Messaging — O Core em Operação)
- **Status:** Base do sistema em consolidação operacional (v1.x).
- **Papel:** Mensageria segura ponta a ponta com privacidade matemática absoluta.
- **Atributos Principais:**
  - Protocolo Double Ratchet híbrido pós-quântico (ML-KEM-768 + X25519 / ML-DSA-65 + Ed25519).
  - Envelopes opacos sem vazamento de metadados relacionais para os relays.
  - Chamadas de voz/vídeo P2P cifradas via WebRTC com canais de sinalização autenticados.
  - Destruição determinística de mensagens e expiração criptográfica no cliente e relays.

### 2.4 Camada 4: Defesa (Sentinela Digital — R&D)
- **Status:** Linha de Pesquisa e Desenvolvimento (R&D) de longo prazo (sem alocação de código no ciclo atual).
- **Papel:** Agente de defesa ativa e higiene cibernética executado estritamente *on-device*.
- **Atributos Principais:**
  - Análise heurística local para detecção de engenharia social, spear-phishing e fraudes de identidade sem envio de telemetria à nuvem.
  - Sandboxing e inspeção binária local de anexos recebidos antes da renderização.
  - Atestação criptográfica de modelos locais de linguagem (LLM Attestation) para resguardar a integridade das respostas contra adulterações em trânsito.

### 2.5 Camada 5: Contra-Inteligência (R&D Travado Legalmente — Cadeia de Custódia RFI Pronta)
- **Status:** R&D com Módulo de Cadeia de Custódia concluído; travamento legal permanente.
- **Papel:** Prova pericial forense imutável e proteção contra interceptações hostis e ataques avançados.
- **Atributos Principais:**
  - O módulo **Raix Forensic Intelligence (RFI)** já possui a arquitetura de trilha de auditoria local e assinatura em hardware (`CustodyHardwareSigner`, `LocalAuditTrail`, `CustodyStorage`) plenamente implementada.
  - **Travamento Legal Estrito:** As capacidades de contra-inteligência são formal e juridicamente restritas à defesa cibernética local e preservação probatória em conformidade com o Marco Civil da Internet, LGPD e normas internacionais de perícia digital (ISO/IEC 27037). Proibição expressa de qualquer mecanismo de vigilância ofensiva, espionagem ou monitoramento invasivo de terceiros.

### 2.6 Camada 6: Governança (R&D Institucional)
- **Status:** Linha de Pesquisa e Desenvolvimento (R&D) de longo prazo.
- **Papel:** Coordenação descentralizada, consenso de integridade do ecossistema e federação soberana.
- **Atributos Principais:**
  - Gestão de federações e nós de retransmissão comunitários ou institucionais sem centralização de controle.
  - Mecanismos de auditoria pública e verificabilidade independente de builds (reproducible builds e SBOMs contínuos).

---

## 3. Blindagens Obrigatórias do Ecossistema

Para garantir a invulnerabilidade da Fortaleza mesmo sob condições extremas de coerção física, intrusão em endpoints ou falha de infraestrutura, foram homologadas cinco blindagens mandatórias:

| Blindagem | Mecanismo e Implementação Técnica | Objetivo de Sobrevivência |
| :--- | :--- | :--- |
| **Panic PIN (Volume Isca)** | Mecanismo de coação (*Plausible Deniability*). O usuário possui um PIN/senha alternativo que, quando inserido na tela de autenticação, desbloqueia um ambiente operacional simulado ("volume isca") contendo conversas e arquivos inócuos, enquanto destrói silenciosamente em memória as chaves de alto valor, sem alertar o coator. | Proteção contra coação física, sequestro relâmpago e mandados abusivos sem possibilidade de quebra da soberania real. |
| **Tokens Hardware (Recovery Seed Offline)** | Integração nativa com dispositivos FIDO2/U2F, YubiKeys e cartões inteligentes para isolamento da chave mestra. Suporte a backup e guarda estritamente a frio (*cold storage*) da semente de 12 palavras, dissociada permanentemente de máquinas conectadas à rede. | Blindagem contra malwares de extração de memória e trojans residentes em endpoints corporativos ou pessoais. |
| **Secure Guest Bridge (Sessão Volátil)** | Módulo de interconexão com clientes de menor garantia técnica (*Low-Assurance Clients*, e.g., Web/Wasm em navegadores). Toda sessão de convidado opera exclusivamente em memória RAM volátil, sem geração de chaves permanentes de identidade no navegador e sem persistência local no DOM. | Evita contaminação do grafo de confiança do ecossistema por ambientes degradados sujeitos a extensões maliciosas ou XSS. |
| **RAIX Governor (Determinístico NÃO-IA)** | O motor diretor e de controle de políticas de segurança do ecossistema opera sob **lógica formal determinística pura**. Rejeição categórica de algoritmos probabilísticos, heurísticas de "caixa-preta" ou modelos de inteligência artificial generativa em funções críticas de segurança. Dotado de **kill-switch humano e físico** irrevogável. | Elimina riscos de alucinação, jailbreak por prompt injection e desvios comportamentais imprevisíveis em decisões de segurança crítica. |
| **Contra-Inteligência Legalmente Travada** | Confinamento operacional do módulo RFI à esfera defensiva, probatória e de atestação local. Travas contratuais, técnicas e regulatórias garantem que nenhuma telemetria seja exfiltrada e nenhuma funcionalidade ofensiva possa ser ativada. | Resguardo irrestrito de conformidade legal e responsabilidade civil, penal e regulatória para os desenvolvedores e usuários. |

---

## 4. Princípios Inegociáveis de Soberania

1. **Processamento On-Device Inviolável:**
   - O dispositivo do usuário é a única autoridade computacional confiável (*Zero-Trust* em relação à infraestrutura externa).
   - Cifragem, decifragem, derivação de chaves, verificação de integridade e checagens heurísticas ocorrem exclusivamente no processador e na memória volátil do endpoint.
   - Nenhuma chave privada ou texto claro jamais trafega pela rede ou toca servidores de nuvem.

2. **Armazenamento Híbrido como "Depósito Cego de Bits":**
   - Qualquer infraestrutura em nuvem, provedor de hospedagem ou rede de relays (Firebase, Cloudflare, servidores dedicados) é classificada como **adversária potencial** ou canal não confiável.
   - Os servidores atuam estritamente como depósitos cegos de blobs cifrados e autenticados (*blind bit-store*).
   - O backend não possui capacidade matemática de decifrar mensagens, deduzir gráficos sociais ou correlacionar remetentes e destinatários além do mínimo estritamente efêmero necessário para a entrega pontual.

---

## 5. Cascata Estratégica de 3 Anos (Roadmap & Modelo Econômico)

A execução do Master Plan segue uma **cascata sequencial estrita**, onde a maturidade de cada etapa e a geração de receita de curto prazo financiam a pesquisa e expansão de longo prazo:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CASCATA ESTRATÉGICA DE 3 ANOS                         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ ANO 1: CORE (Fundações & Consolidação)                                      │
│ • Estabilização final do RAIX Messaging (Android, iOS, Desktop).           │
│ • Auditoria Criptográfica Externa Independente (Prioridade P0).             │
│ • Consolidação das primitivas FIPS 203 (ML-KEM) e FIPS 204 (ML-DSA).        │
│ • Pilotos institucionais fechados e entrada nas Lojas Oficiais.             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ ANO 1.5 - 2: ATIVOS & IDENTIDADE (PQ-Vault & Monetização)                  │
│ • Extração formal do raix-crypto-core como módulo compartilhado.            │
│ • Desenvolvimento e lançamento comercial do PQ-Vault (Cofre PQC Zero-K).   │
│ • Início da monetização recorrente B2B/B2C (Gestão de Credenciais).         │
│ • Modelo de Identidade Descentralizada ancorada em BIP-39/BIP-44.           │
│ • "Receita de curto prazo financia a expansão de longo prazo".              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ ANO 2.5 - 3: HIGIENE, CONTRA-INTELIGÊNCIA & GOVERNANÇA                     │
│ • P&D e implementação do Sentinela Digital (Higiene e Defesa On-Device).   │
│ • Maturação da Trilha Pericial RFI (Contra-Inteligência travada).           │
│ • Ativação do RAIX Governor determinístico com kill-switch de hardware.     │
│ • Estrutura de Governança e Federação Soberana.                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Dinâmica Econômica de Financiamento
- **Pilotos Institucionais → Lojas Oficiais:** Validação empírica e tração inicial do mensageiro, consolidando reputação de confiabilidade e primeiros fluxos comerciais.
- **Lançamento do PQ-Vault:** Entrada no mercado global de gerenciadores de senhas com o diferencial competitivo de imunidade pós-quântica zero-knowledge. O fluxo de caixa gerado por assinaturas corporativas financia a expansão da infraestrutura e o laboratório de R&D.
- **Autonomia Estratégica e Financeira:** A capacidade de autofinanciamento via produtos de alto valor agregado garante que o ecossistema preserve integralmente sua integridade ética e arquitetural, dispensando concessões a interesses de terceiros.

---

## 6. Governança Técnica e Congelamento de Baseline

1. **Baseline Estritamente Congelado:**
   - Este Master Plan é um documento de referência estratégica e arquitetural de 3 anos.
   - Fica expressamente vedada a abertura de novas branches, stubs de código, interfaces preliminares ou dependências para as camadas de R&D (Camadas 4, 5 e 6) no repositório do aplicativo principal antes do cumprimento integral dos marcos de estabilização e auditoria externa do Ano 1.
2. **Canal Único de Comunicação e Aprovação:**
   - Qualquer evolução nas diretrizes deste documento exige concordância unânime do Consenso Técnico e aprovação do Assessor Executivo, veiculada exclusivamente através do canal formal de Análise.
3. **Segurança Permanente do Agente:**
   - Aplicação irrestrita das regras do [`AGENTS.md`](../AGENTS.md) em qualquer iteração presente ou futura: sigilo total de chaves, anonimato por padrão e zero tolerância ao vazamento de credenciais ou segredos.

---

## 7. Adendo de Evolução: Endurecimento e Satélites B2B (Registro Referencial)

Conforme consenso estabelecido em 22 de Maio de 2024, fica registrado neste Master Plan o escopo complementar de endurecimento (Bloco A) e os satélites de monetização B2B (Bloco B), que comporão o roadmap pós-rodada. Este registro atua estritamente como referência documental, respeitando a regra de **Baseline Congelado** (nenhuma inclusão de código ou escopo novo no ciclo atual).

### 7.1 Bloco A: Endurecimento da Fortaleza (Hardening)
- **Padding de Tráfego:** Ofuscação e calibração de padrões de rede com injeção de pacotes (calibrado com piloto, Track 2).
- **Isolamento Wasm/Rust em Clientes Web:** Garantia de execução protegida da lógica de chaves no navegador com limpezas rigorosas via `zeroize` na memória volátil.
- **Contra-Inteligência:** O escopo do módulo RFI (Raix Forensic Intelligence) fica estritamente documentado sob o status mandatório `LOCKED_PENDING_LEGAL_REVIEW`.

### 7.2 Bloco B: Satélites B2B e Continuidade
Os satélites complementares ingressam na camada de Ativos (Camada 2) e Comunicação (Camada 3), estruturados como propulsores de monetização sucessores do PQ-Vault:
1. **RAIX Sign (P1 - Prioridade Máxima):** Plataforma de assinatura digital B2B. Reutiliza as primitivas do `raix-crypto-core` (ML-DSA-65). Estratégia de precificação transacional por documento/assinatura corporativa.
2. **RAIX Drop (P2):** Sistema efêmero para transferência de arquivos (*Low-Assurance*), focado em compartilhamento ágil e pontual. Modelo de aquisição Freemium/Pro.
3. **Protocolo Lázaro (P3):** Solução premium de continuidade da semente (Mnemônico) baseada no esquema SLIP-0039 (*Shamir's Secret Sharing*). Endereçado aos segmentos Private/Enterprise.

### 7.3 Bloco C: Infraestrutura Privada e RAIX Drive (Self-Hosted)
Extensão da camada de Ativos (Camada 2), a ser priorizada após o PQ-Vault e os satélites B2B. O RAIX Drive adapta a arquitetura para modelos de infraestrutura sob controle físico do cliente corporativo ou premium.

- **Conceito e Soberania:** O **RAIX Drive** permanece um cliente zero-knowledge estrito. A única mudança arquitetural é o roteamento do backend: em vez da nuvem RAIX, o sistema aponta para um servidor/NAS local do cliente. Processamento e cifragem ocorrem 100% *on-device*. O NAS atua meramente como "depósito cego de bits" (armazenando apenas fragmentos criptografados, sem chaves).
- **Modelos de Negócio e Precificação:**
  - **SaaS (Padrão):** Hospedagem na infraestrutura RAIX. Precificação via **assinatura mensal**.
  - **Self-Hosted:** O cliente hospeda no próprio NAS. Precificação via **licença única** + **consultoria de setup** + **suporte opcional**.
  - **Sovereign Box:** Kit premium *plug-and-play* (requer parceria de hardware). Precificação premium em pacote único: **hardware + licença + setup + assinatura de suporte**.
- **Sinergia:** Este bloco impulsiona a vertical de Consultoria ("engenharia de infraestrutura privada"), elevando significativamente o ticket médio B2B.

### 7.4 Priorização Integrada do Pipeline
A alocação de recursos e introdução das novas features respeitará inegociavelmente a seguinte ordem sequencial de desenvolvimento e deploy:
1. Auditoria Externa (P0)
2. Integração Pós-Quântica (PQ)
3. Cofre Pós-Quântico (PQ-Vault)
4. **Satélites B2B (RAIX Sign > RAIX Drop > Protocolo Lázaro)**
5. **Infraestrutura Privada (RAIX Drive / Self-Hosted / Sovereign Box)**
6. Track 2 (Modelos de tração alternativos)
7. Camadas Premium e Consultoria B2B
8. Sentinela Digital (R&D)
