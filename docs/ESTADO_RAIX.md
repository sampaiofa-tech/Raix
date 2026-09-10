# ESTADO_RAIX.md — Documento-Mestre do Projeto
Versão: 2.0 | Atualizado: 2024-05-22  
Propósito: Preservar o estado do projeto fora da memória de qualquer agente. Fonte única de verdade. Atualizado pelo Analista a cada ciclo.

> **DOCUMENTO-MESTRE:** Fonte única de verdade do projeto RAIX adotada por todos os agentes.  
> **DOCUMENTOS RELACIONADOS:**  
> - [`MASTER_PLAN.md`](MASTER_PLAN.md) — Master Plan do Ecossistema Fortaleza RAIX (2024–2027)  
> - [`ARQUITETURA_ECOSSISTEMA.md`](ARQUITETURA_ECOSSISTEMA.md) — Arquitetura do Ecossistema e Núcleo Criptográfico Compartilhado  

---

## 1. IDENTIDADE
- **Produto:** RAIX (plataforma de privacidade digital)
- **Marca:** RAIX depositada no INPI (classes 9 e 42) + RPC
- **Domínios:** raixtech.com + raixtech.com.br
- **E-mail oficial:** contato@raixtech.com
- **Repositório:** sampaiofa-tech/Raix (AGPL-3.0, dual licensing)
- **Empresa:** Cat Tech (CNPJ 67.497.085/0001-36)
- **Posicionamento:** "privacidade forte por design, com retenção limitada de metadados" (honesto, sem overclaim)

---

## 2. PRODUTOS (status)
- **RAIX Messaging:** EM OPERAÇÃO (Android, Windows, Web; iOS em homologação) — v1.6, zero-knowledge de conteúdo, pós-quântica híbrida (ML-KEM-768 + ML-DSA-65)
- **Cartilhas:** publicadas (Android + iOS + Brasil/LGPD)
- **Landing page:** publicada (redesign com 8 correções)
- **Consultoria de privacidade:** registrada (pós-rodada)
- **PQ-Vault:** PRÓXIMA RECEITA (evolução imediata pós-estabilidade)
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
4. Track 2 (anonimato)
5. Feature premium (análise on-device)
6. Consultoria
7. Sentinela Digital (R&D)

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

## 10. PENDÊNCIAS ATUAIS
- [ ] Desfecho da reunião com o investidor (3 cenários)
- [ ] Métricas reais dos pilotos F1 (para dimensionar Track 2)
- [ ] Recibo do INPI (número de processo real)
- [ ] D-U-N-S → contas das lojas (Play Store / Apple Developer)
- [ ] Decisão do cenário (A/B/C) com o investidor
- [ ] Contra-Inteligência → aguardando validação jurídica
- [ ] Ciclo pós-rodada (auditoria → PQ → PQ-Vault)

---

## 11. REGRAS DE GOVERNANÇA
- **Baseline congelado.** Nada de escopo novo sem aprovação.
- **Analista é o ÚNICO canal para o Executor.**
- **Assessor → Guru:** só com "Guru vamo em frente".
- **Assessor → Analista:** sempre.
- **Futuro fala só com Guru.**
- **Linguagem oficial:** "privacidade forte por design".
- **Pós-quântico:** só como "roteiro aprovado, auditoria agendada" até atestado externo.
- **Nenhum overclaim.** Nenhum número inventado (`[VALIDAR]` se faltar).
- **Documento-mestre ESTADO_RAIX.md:** fonte única de verdade, mantido pelo Analista, adotado por todos os agentes.
