# ESTADO_RAIX.md — Documento-Mestre do Projeto

Versão: 2.0 | Atualizado: 2026-09-18 (fuso UTC-03:00, Brasília)
Propósito: Preservar o estado do projeto fora da memória de qualquer agente. Fonte única de verdade. Atualizado pelo Analista a cada ciclo.

> **DOCUMENTO-MESTRE:** Fonte única de verdade do projeto RAIX adotada por todos os agentes.
> **DOCUMENTOS RELACIONADOS:**
>
> - [`MASTER_PLAN.md`](MASTER_PLAN.md) — Master Plan do Ecossistema Fortaleza RAIX (2024–2027)
> - [`ARQUITETURA_ECOSSISTEMA.md`](ARQUITETURA_ECOSSISTEMA.md) — Arquitetura do Ecossistema e Núcleo Criptográfico Compartilhado

---

## 1. IDENTIDADE

- **Produto:** RAIX (plataforma de privacidade digital)
- **Marca:** RAIX depositada no INPI (classes 9 e 42) + RPC
- **Domínios:** raixtech.com + raixtech.com.br
- **E-mail oficial:** <contato@raixtech.com>
- **Repositório:** sampaiofa-tech/Raix (AGPL-3.0, dual licensing)
- **Empresa:** Cat Tech (CNPJ 67.497.085/0001-36)
- **Posicionamento:** "privacidade forte por design, com retenção limitada de metadados" (honesto, sem overclaim)

---

## 2. PRODUTOS (status)

- **RAIX Messaging:** EM OPERACAO (Android, Windows, Web; iOS em homologacao) -- v1.9.3, zero-knowledge de conteudo, pos-quantica hibrida (ML-KEM-768 + ML-DSA-65)
  - **Versao vigente:** v1.9.3 (versionCode 39), confirmada em `composeApp/build.gradle.kts`.
  - **Data de referencia do ambiente:** 2026-09-21 (fuso UTC-03:00, Brasilia).
  - **Binarios autoritativos (build de 2026-09-21):**
    - APK: `release-artifacts/Raix-1.9.3.apk` -- `DB9ACE89C62B7870112FF97194DC012723EC79B520370AE5AF1EAB9D7B0ECA09`
    - AAB: `release-artifacts/Raix-1.9.3.aab` -- `E3B64BD51906DF4353831AD3150E2387C0FF22311B8CFB1F0A193D8E62967AB0`
    - MSI: `release-artifacts/Raix-1.9.3.msi` -- `9D996316278ACFBEB87E526BC280B7D0934753668C71C8DDE160172F79D2F812`
  - **Checksums canonicos:** `docs/release-checksums.sha256`
  - **Changelog v1.7--v1.9.3 (13 correcoes):** home vazia (TTL), abas, QR do menu, insets, busca, deep-link de notificacao, menu Bloquear, icone 3 pontos, permissao POST_NOTIFICATIONS no onboarding, paridade desktop, versionamento unificado.
- **Cartilhas:** publicadas (Android + iOS + Brasil/LGPD)
- **Landing page:** publicada (redesign com 8 correções)
- **Consultoria de privacidade:** registrada (pós-rodada)
- **PQ-Vault:** PRÓXIMA RECEITA (evolução imediata pós-estabilidade)
- **Satélites B2B:** ROADMAP (Sign, Drop, Lázaro)
- **RAIX Drive / Sovereign Overlay / Bridge:** ROADMAP (pós satélites B2B)
- **Sentinela Digital:** R&D (longo prazo)
- **Cadeia de Custódia (RFI):** IMPLEMENTADA (commit `420a814`)
- **Contra-Inteligência (RFI):** TRAVADA (`LOCKED_PENDING_LEGAL_REVIEW`)
- **Trilho de Auditoria Local:** ROADMAP (Escritório/Private)
- **Análise de arquivos:** PREMIUM (on-device)

---

## 3. SEGURANÇA (núcleo)

- **E2E DEK v1.2** homologada com 3 provas (servidor cego, round-trip, isolamento adversarial)
- **Pós-quântica híbrida:** ML-KEM-768 + X25519 (KEM), ML-DSA-65 + Ed25519 (assinatura)
- **Crypto-shredding:** destruição de DEKs ≤15min + TTL + vanish-after-read
- **Regras Firestore adversariais** (deny-by-default, 20 asserções)
- **Supply chain:** SBOM, SLSA L2+, GitGuardian, re-auditoria periódica
- **Health-check estendido:** integridade + fragilidades + hospedagem + relatório diário
- **Logs MCI** isolados 180 dias (`accessLogs`)
- **Zero-knowledge de conteúdo;** metadados mínimos
- **Cadeia de Custódia:** assinatura hardware-backed + hash chaining
- **Contra-Inteligência:** TRAVADA (R&D sob revisão legal)
- **Repo público:** mantido (auditoria de sanitização concluída, risco gerenciável)
- **Incidente de Chaves (Set/2026):** RESOLVIDO (chave Android restrita a app + 5 APIs; Browser key a 3 APIs; segredo v1 destruído; "Default" excluída; canônica = Gemini API Key 2; senha do keystore trocada).

---

## 4. COMPLIANCE

- **LGPD:** PP v3, ToU v3, DPA, ROPA publicados (raixtech.com)
- **Marco Civil:** logs 180 dias isolados
- **Licenciamento:** AGPL-3.0 + licença comercial (dual licensing) + CLA
- **Auditoria externa:** R$ 50k (pós-rodada, primeira despesa de capital)
- **INPI:** marca RAIX depositada

---

## 5. VALIDAÇÃO DE MERCADO

- **5 advogados em uso ativo** (~2h/dia cada)
- Pediram "rastreabilidade" → Trilho de Auditoria Local
- Veem o app como monetizável
- **Testes automatizados:** 70+ backend, 90 Android, 66 desktop, 20 asserções adversariais, 8/8 Cadeia de Custódia
- **Auditoria de sanitização:** 7 fases aprovadas

---

## 6. NÚMEROS-CHAVE

- **Custo operacional:** R$ 0,00/mês (free tier)
- **Custos fixos anuais:** US$ 170–240
- **Captação:** 3 cenários (A: R$ 1,8–2,1M / B: R$ 1,0–1,2M / C: R$ 1,4–1,6M) — recomendação: **C (Híbrido)**
- **Pró-labore:** R$ 10k (A1) → R$ 25k (A2) → R$ 40k (A3) → R$ 50k (A4)
- **Planos:** Free / Profissional R$ 29–39 / Escritório R$ 29–49/seat / Private (2026)
- **Auditoria externa:** R$ 50k (linha única)
- **Roadmap:** 3 anos, 6 camadas, 10+ produtos

---

## 7. ROADMAP PÓS-RODADA (priorização)

1. Auditoria externa (R$ 50k)
2. Migração PQ (TLS-PQ + Double Ratchet)
3. PQ-Vault (nova receita)
4. Satélites B2B (RAIX Sign, Drop, Lázaro)
5. Infraestrutura Privada (RAIX Drive / Sovereign Overlay / Bridge / Sovereign Box)
6. Track 2 (anonimato)
7. Feature premium (análise on-device)
8. Consultoria (potencializada por setup Self-Hosted)
9. Sentinela Digital (R&D)

---

## 8. MASTER PLAN (Fortaleza RAIX — 3 anos, 6 camadas)

- **6 camadas:** Fundação (core, HSM, PQ-ID), Ativos (Vault, Drive, Testamento), Comunicação (Messaging, Sala, Bridge), Defesa (Eraser, Selo, Sentinela), Contra-Inteligência (RFI, Custódia, Coerção), Governança (Hive, Governor, Maestro). Detalhamento integral em [`MASTER_PLAN.md`](MASTER_PLAN.md).
- **Blindagens obrigatórias:** Panic PIN (isca), Tokens Hardware (recovery offline), Secure Guest Bridge (Low-Assurance), RAIX Governor (NÃO-IA, kill-switch), Contra-Inteligência (travada).
- **Soberania:** processamento on-device + armazenamento híbrido.
- **Cascata 3 anos:** Core → Ativos → Identidade → Higiene → Contra-Inteligência (R&D) → Governança (R&D).
- **Núcleo Compartilhado & Isolamento de Domínio:** Especificado em [`ARQUITETURA_ECOSSISTEMA.md`](ARQUITETURA_ECOSSISTEMA.md).

---

## 9. EQUIPE (Cenário C — Híbrido)

- **Ano 1:** Fundador + Eng. Segurança + Backend + UX
- **Ano 2:** + Mobile + Eng. IA/ML + QA
- **Ano 3:** + DevOps + Business Dev + PM
- **Ano 4:** + Legal + Suporte
- **Salários de referência (mercado Brasil):** Eng. Segurança R$ 25–35k; Backend R$ 20–28k; Mobile R$ 18–25k; IA/ML R$ 25–35k; UX R$ 10–18k; etc.
- **Agentes:**
  - **Assessor:** estratégia e negócio
  - **Guru:** segurança e arquitetura
  - **Analista:** operação, governança e único canal ao Executor
  - **Futuro:** inovação (comunicação exclusiva com o Guru)
  - **Executor:** implementação (recebe instruções exclusivamente do Analista)

---

## 10. OPERAÇÃO E RECUPERAÇÃO DE DESASTRES

- **Rotina de Backup:** Automática a cada 8 horas (via Agendador de Tarefas do Windows: `RaixBackupAutomatico`).
- **Destino do Backup:** Google Drive (`G:\Meu Drive\Raix`). Sincronizado para a nuvem automaticamente.
- **Escopo do Backup:** Código fonte (`C:\Dev\Pmsg`) e backups locais (`C:\Pmsg-Backups`).
- **Exclusões de Segurança:** Segredos, chaves (`.keystore`, `google-services.json`, `.env`) e diretórios de build não são copiados para a nuvem. O backup de keystores requer decisão/aprovação e gestão manual.
- **Fluxo de Recuperação (Disaster Recovery):**
  1. Instalar o Google Drive for Desktop no novo PC e logar na conta correta.
  2. Restaurar o conteúdo de `G:\Meu Drive\Raix\Pmsg` para `C:\Dev\Pmsg`.
  3. Restaurar o conteúdo de `G:\Meu Drive\Raix\Pmsg-Backups` para `C:\Pmsg-Backups`.
  4. Recuperar chaves/segredos sensíveis (não enviados à nuvem) de backup frio/físico do administrador.

---

## 11. PENDÊNCIAS ATUAIS

- [x] Ciclo de Feedback (9 Itens): IMPLEMENTADO
- [x] Due diligence LGPD/Marco Civil CONCLUÍDA (nota técnica PDF + 4 evidências aprovadas — DFD/matriz de correlação, isolamento faturamento, crypto-shredding/PITR com janela de 7 dias, auditoria de SDKs). Liberação externa autorizada. AdversarialCorrelationTest pendente de inclusão na suíte de CI (item técnico, não bloqueante).
- [ ] Desfecho da reunião com o investidor (3 cenários)
- [ ] Métricas reais dos pilotos F1 (para dimensionar Track 2)
- [ ] Recibo do INPI (número de processo real)
- [ ] D-U-N-S → contas das lojas (Play Store / Apple Developer)
- [ ] Decisão do cenário (A/B/C) com o investidor
- [ ] Contra-Inteligência → aguardando validação jurídica
- [ ] Ciclo pós-rodada (auditoria → PQ → PQ-Vault)

---

## 12. REGRAS DE GOVERNANÇA

- **Baseline congelado.** Nada de escopo novo sem aprovação.
- **Analista é o ÚNICO canal para o Executor.**
- **Assessor → Guru:** só com "Guru vamo em frente".
- **Assessor → Analista:** sempre.
- **Futuro fala só com Guru.**
- **Linguagem oficial:** "privacidade forte por design".
- **Pós-quântico:** só como "roteiro aprovado, auditoria agendada" até atestado externo.
- **Nenhum overclaim.** Nenhum número inventado (`[VALIDAR]` se faltar).
- **Documento-mestre ESTADO_RAIX.md:** fonte única de verdade, mantido pelo Analista, adotado por todos os agentes.
