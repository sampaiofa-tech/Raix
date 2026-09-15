# RAIX: Materiais Executivos para Investimento

> **Aviso Oficial:** Projeto pré-receita, AINDA SEM RECEITA. Baseline de produto congelado.
> **Posicionamento:** Privacidade forte por design, com retenção limitada de metadados.

---

## 1. One-Pager Executivo

### O Problema
O uso de mensageiros comuns em contextos profissionais (advocacia, saúde, executivos) gera um **passivo LGPD oculto**. O armazenamento contínuo de metadados, contatos e histórico não-criptografado em servidores não auditáveis ou atrelados a redes sociais compromete irreparavelmente o sigilo profissional, criando alto risco de vazamentos e multas.

### A Solução: RAIX
O RAIX é uma plataforma B2B de **privacidade forte por design**. Diferente de soluções massificadas, opera com **servidor cego (zero-knowledge)** e **retenção limitada de metadados** (logs isolados por 180 dias estritamente para Marco Civil, sem mineração). Nosso compliance já nasce pronto (PP, ToU, DPA, ROPA publicados).

### Diferencial Tecnológico
- **Arquitetura Zero-Knowledge & Crypto-Shredding:** Destruição ativa de chaves (TTL ≤15min) e recurso *vanish-after-read*.
- **Pós-Quântico (PQ):** Pós-quântica híbrida (roteiro aprovado, auditoria agendada).
- **Código Auditável & Dual Licensing:** Licença AGPL-3.0 pública, combinada com Licença Comercial, garantindo transparência sem abrir mão do controle corporativo.
- **Infraestrutura Otimizada:** Mensageiro rodando sobre GCP/Firebase com custo operacional atual de **R$ 0,00/mês**.

### Tração Atual & Métricas
- **Pilotos em uso ativo:** 5 advogados operando o sistema de ponta a ponta (~2h/dia).
- **Testes rigorosos:** >200 testes automatizados (backend, Android, desktop) e auditoria de sanitização concluída (7 fases).
- **Demanda validada:** Pilotos solicitaram o "Trilho de Auditoria Local" (já inserido no roadmap).
- **Custo Fixo Anual:** Apenas US$ 170–240 em licenças de domínio/ferramentas.

---

## 2. Os 3 Cenários de Investimento (Comparativo)

| Cenário | Captação (R$) | Estratégia / Perfil | Prós | Contras |
| :--- | :--- | :--- | :--- | :--- |
| **A** | **1,8M – 2,1M** | **Agressiva** (Expansão rápida de time) | Maior velocidade no roadmap e múltiplos produtos. | Alto burn-rate; risco à cultura de segurança pela entrada simultânea de devs. |
| **B** | **1,0M – 1,2M** | **Lean Total** (Foco exclusivo no core) | Extrema disciplina financeira; pista de voo estendida. | Limita a expansão B2B e retarda muito as ferramentas Satélite (Sign, Drop). |
| **C (Híbrido)** | **1,4M – 1,6M** | **Equilibrada (Foco no B2B)** | **Pista segura para o B2B; absorve R$ 50k da auditoria; forma time de base.** | **Requer forte gestão financeira para não assumir os custos de A.** |

### Recomendação Estratégica: Cenário C (Híbrido)
O Cenário C garante capital suficiente para atravessar a primeira grande despesa de capital (**Auditoria Externa - R$ 50k**) e construir a base de clientes B2B (Escritórios) antes de expandir P&D de forma irresponsável.
**Evolução do Pró-labore do Fundador (atrelada à receita/estabilidade):**
R$ 10k (A1) → R$ 25k (A2) → R$ 40k (A3) → R$ 50k (A4).

---

## 3. Ponto de Equilíbrio e Lucro (Ano 1)

O RAIX ataca o B2B utilizando uma precificação elástica, permitindo que pequenos escritórios e grandes contas sustentem a operação GCP.

**Estrutura de Planos (B2B):**
- **Profissional:** R$ 29–39 / mês
- **Escritório:** R$ 29–49 / seat / mês
- **Private:** Ticket premium, 100% customizável (previsto 2026)

| Estágio de Operação | Composição 1 (Contas Premium) | Composição 2 (Volume B2B) |
| :--- | :--- | :--- |
| **Breakeven (Empate)** | ~8 clientes **Private** | 5 **Privates** + 100 seats **Escritório** |
| **Escala de Lucro** | (Foco em expandir tickets B2B) | ~6 **Privates** + 120 seats **Escritório** |

*Nota: A operação atual tem custo R$ 0/mês no free tier GCP.*

---

## 4. Roadmap Completo: Fortaleza RAIX & Pontos Críticos

Este roadmap de 3 anos não permite atalhos arquitetônicos. O sistema está baseado na evolução progressiva de confiança (onde a fase seguinte **herda** a blindagem da fase anterior).

| Fase | Descrição da Meta | Ponto Crítico / Gargalo (Risco Mapeado) |
| :--- | :--- | :--- |
| **F0 (Fundação)** | Custos pagos, compliance (PP, ToU) no ar. | INPI ainda sem recibo definitivo (processo em andamento). |
| **F1 (Pilotos)** | Validação qualitativa (5 advogados ativos). | **Não gera receita.** Atrito de UX (QR Code) empurrado p/ v1.7. |
| **F2 (Lojas)** | Entrada nas App Stores. | Depende da emissão do D-U-N-S; `DATA_INVENTORY.md` crítico para aprovação de "Data Safety". |
| **F3 (Auditoria Externa)** | Atestado de segurança. Primeira despesa de caixa (**R$ 50k**). | **Credencial indispensável** para vender aos planos B2B/Escritório. Sem auditoria, não há B2B. |
| **F4 (Migração PQ)** | Upgrade TLS-PQ e Double Ratchet. | Sem atestado externo, mantemos: *"roteiro aprovado, auditoria agendada"*. Não afirmar *"pronto"*. |
| **F5 (PQ-Vault)** | Geração de nova receita baseada em arquivos. | Depende da estabilidade absoluta do núcleo E2E (F4). |
| **F6 (Satélites B2B)** | RAIX Sign (P1), Drop (P2), Lázaro (P3). | Sign depende de mercado; Lázaro exige precificação premium altíssima. |
| **F7 (Self-Hosted)** | RAIX Drive + Sovereign Box. | Depende de acordos B2B com fabricantes NAS (Não comprometer prazos). |
| **F8 (Track 2)** | Ferramenta de Anonimato nativo. | Requer levantamento profundo de métricas operacionais da F1. |
| **F9 (Premium/Consult.)** | Análise on-device e Consultoria de Privacidade. | Receita complementar: **não deve distrair o time** da plataforma core. |
| **F10 (Sentinela - R&D)** | Monitoramento de ameaças. | Alto esforço técnico; posicionamento apenas como visão de longo prazo. |
| **F11 (Fortaleza RAIX)** | Contra-Inteligência e RAIX Governor. | **Contra-Inteligência TRAVADA** até validação legal. Governor precisa ser 100% determinístico (NÃO usar IA). |
