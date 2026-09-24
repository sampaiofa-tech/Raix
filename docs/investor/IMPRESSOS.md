# RAIX: Materiais Executivos para Investimento

> **Aviso Oficial:** Projeto pre-receita, AINDA SEM RECEITA. Baseline de produto congelado.
> **Posicionamento:** Privacidade forte por design, com retencao limitada de metadados.

---

## 1. One-Pager Executivo

### O Problema

O uso de mensageiros comuns em contextos profissionais (advocacia, saude, executivos) gera um **passivo LGPD oculto**. O armazenamento continuo de metadados, contatos e historico nao-criptografado em servidores nao auditaveis ou atrelados a redes sociais compromete irreparavelmente o sigilo profissional, criando alto risco de vazamentos e multas.

### A Solucao: RAIX

O RAIX e uma plataforma B2B de **privacidade forte por design**. Diferente de solucoes massificadas, opera com **servidor cego (zero-knowledge)** e **retencao limitada de metadados** (logs isolados por 180 dias estritamente para Marco Civil, sem mineracao). Nosso compliance ja nasce pronto (PP, ToU, DPA, ROPA publicados).

### Diferencial Tecnologico

- **Arquitetura Zero-Knowledge & Crypto-Shredding:** Destruicao ativa de chaves (TTL <=15min) e recurso *vanish-after-read*.
- **Pos-Quantico (PQ):** Pos-quantica hibrida (roteiro aprovado, auditoria agendada).
- **Licenciamento AGPL-3.0 + Licenca Comercial (open-core):** O nucleo sera publicado -- hoje, codigo disponivel sob solicitacao para auditoria e due diligence. Garante transparencia sem abrir mao do controle corporativo.
- **Infraestrutura Otimizada:** Mensageiro rodando sobre GCP/Firebase com custo operacional atual de **R$ 0,00/mes**.

### Tracao Atual & Metricas

- **Pilotos em uso ativo:** 5 advogados operando o sistema de ponta a ponta (~2h/dia).
- **Testes rigorosos:** >200 testes automatizados (backend, Android, desktop) e auditoria de sanitizacao concluida (7 fases).
- **Demanda validada:** Pilotos solicitaram o "Trilho de Auditoria Local" (ja inserido no roadmap).
- **Custo Fixo Anual:** Apenas US$ 170-240 em licencas de dominio/ferramentas.

---

## 2. Os 3 Cenarios de Investimento (Comparativo)

| Cenario | Captacao (R$) | Estrategia / Perfil | Pros | Contras |
| :--- | :--- | :--- | :--- | :--- |
| **A** | **1,8M - 2,1M** | **Agressiva** (Expansao rapida de time) | Maior velocidade no roadmap e multiplos produtos. | Alto burn-rate; risco a cultura de seguranca pela entrada simultanea de devs. |
| **B** | **1,0M - 1,2M** | **Lean Total** (Foco exclusivo no core) | Extrema disciplina financeira; pista de voo estendida. | Limita a expansao B2B e retarda muito as ferramentas Satelite (Sign, Drop). |
| **C (Hibrido)** | **1,4M - 1,6M** | **Equilibrada (Foco no B2B)** | **Pista segura para o B2B; absorve R$ 50k da auditoria; forma time de base.** | **Requer forte gestao financeira para nao assumir os custos de A.** |

### Alocacao Detalhada por Cenario

**CENARIO A - RODADA MAIOR (R$ 1,8-2,1M; equipe Ano 1: 5 pessoas; custo ~R$ 102k/mes):**

- Salarios ~80% (~R$ 1,52M sobre medio R$ 1,9M)
- Infraestrutura/nuvem ~5% (~R$ 95k)
- Licencas/ferramentas ~3% (~R$ 57k)
- Equipamentos (uma vez) ~2% (~R$ 38k)
- Auditoria externa (R$ 50k) ~3% (~R$ 57k)
- Contador/juridico ~3% (~R$ 57k)
- Reserva operacional ~4% (~R$ 76k)

**CENARIO B - ESCALONADO (R$ 1,0-1,2M; equipe Ano 1: 3 pessoas; custo ~R$ 56k/mes):**

- Salarios ~80% (~R$ 880k sobre medio R$ 1,1M)
- Infra/nuvem ~4% (~R$ 44k)
- Licencas ~3% (~R$ 33k)
- Equipamentos ~2% (~R$ 22k)
- Auditoria (R$ 50k) ~5% (~R$ 55k)
- Contador/juridico ~3% (~R$ 33k)
- Reserva ~3% (~R$ 33k)

**CENARIO C - HIBRIDO, RECOMENDADO (R$ 1,4-1,6M; equipe Ano 1: 4 pessoas; custo ~R$ 81k/mes):**

- Salarios ~82% (~R$ 1,23M sobre medio R$ 1,5M)
- Infra/nuvem ~4% (~R$ 60k)
- Licencas ~3% (~R$ 45k)
- Equipamentos ~2% (~R$ 30k)
- Auditoria (R$ 50k) ~3% (~R$ 45k)
- Contador/juridico ~3% (~R$ 45k)
- Reserva ~3% (~R$ 45k)

> **PONTO-CHAVE:** Nos 3 cenarios, a maior parte do capital vai para salarios (o ativo mais valioso). Infra + licencas + equipamentos < 10%. A unica despesa de capital relevante e a auditoria (R$ 50k), que destrava o B2B. O que diferencia os cenarios e a velocidade de montagem do time: A = contrata rapido (mais queima/diluicao), B = cresce devagar (disciplina, menos velocidade), C = nucleo critico + expansao conforme receita.

### Recomendacao Estrategica: Cenario C (Hibrido)

O Cenario C garante capital suficiente para atravessar a primeira grande despesa de capital (**Auditoria Externa - R$ 50k**) e construir a base de clientes B2B (Escritorios) antes de expandir P&D de forma irresponsavel.
**Evolucao do Pro-labore do Fundador (atrelada a receita/estabilidade):**
R$ 10k (A1) -> R$ 25k (A2) -> R$ 40k (A3) -> R$ 50k (A4).

---

## 3. Ponto de Equilibrio e Lucro (Ano 1)

O RAIX ataca o B2B utilizando uma precificacao elastica, permitindo que pequenos escritorios e grandes contas sustentem a operacao GCP.

**Estrutura de Planos (B2B):**

- **Profissional:** R$ 29-39 / mes
- **Escritorio:** R$ 29-49 / seat / mes
- **Private:** Ticket premium, 100% customizavel (previsto 2026)

| Estagio de Operacao | Composicao 1 (Contas Premium) | Composicao 2 (Volume B2B) |
| :--- | :--- | :--- |
| **Breakeven (Empate)** | ~8 clientes **Private** | 5 **Privates** + 100 seats **Escritorio** |
| **Escala de Lucro** | (Foco em expandir tickets B2B) | ~6 **Privates** + 120 seats **Escritorio** |

*Nota: A operacao atual tem custo R$ 0/mes no free tier GCP.*

---

## 4. Roadmap Completo: Fortaleza RAIX & Pontos Criticos

Este roadmap de 3 anos nao permite atalhos arquitetonicos. O sistema esta baseado na evolucao progressiva de confianca (onde a fase seguinte **herda** a blindagem da fase anterior).

| Fase | Descricao da Meta | Ponto Critico / Gargalo (Risco Mapeado) |
| :--- | :--- | :--- |
| **F0 (Fundacao)** | Custos pagos, compliance (PP, ToU) no ar. | INPI ainda sem recibo definitivo (processo em andamento). |
| **F1 (Pilotos)** | Validacao qualitativa (5 advogados ativos). | **Nao gera receita.** Atrito de UX (QR Code) empurrado p/ v1.7. |
| **F2 (Lojas)** | Entrada nas App Stores. | Depende da emissao do D-U-N-S; `DATA_INVENTORY.md` critico para aprovacao de "Data Safety". |
| **F3 (Auditoria Externa)** | Atestado de seguranca. Primeira despesa de caixa (**R$ 50k**). | **Credencial indispensavel** para vender aos planos B2B/Escritorio. Sem auditoria, nao ha B2B. |
| **F4 (Migracao PQ)** | Upgrade TLS-PQ e Double Ratchet. | Sem atestado externo, mantemos: *"roteiro aprovado, auditoria agendada"*. Nao afirmar *"pronto"*. |
| **F5 (PQ-Vault)** | Geracao de nova receita baseada em arquivos. | Depende da estabilidade absoluta do nucleo E2E (F4). |
| **F6 (Satelites B2B)** | RAIX Sign (P1), Drop (P2), Lazaro (P3). | Sign depende de mercado; Lazaro exige precificacao premium altissima. |
| **F7 (Self-Hosted)** | RAIX Drive + Sovereign Box. | Depende de acordos B2B com fabricantes NAS (Nao comprometer prazos). |
| **F8 (Track 2)** | Ferramenta de Anonimato nativo. | Requer levantamento profundo de metricas operacionais da F1. |
| **F9 (Premium/Consult.)** | Analise on-device e Consultoria de Privacidade. | Receita complementar: **nao deve distrair o time** da plataforma core. |
| **F10 (Sentinela - R&D)** | Monitoramento de ameacas. | Alto esforco tecnico; posicionamento apenas como visao de longo prazo. |
| **F11 (Fortaleza RAIX)** | Contra-Inteligencia e RAIX Governor. | **Contra-Inteligencia TRAVADA** ate validacao legal. Governor precisa ser 100% deterministico (NAO usar IA). |

### Prazos Estimados por Fase x Cenario (em meses, a partir de T+0)

*Premissas:*

- *T+0 = inicio da execucao apos o aporte (entrada do capital).*
- *As duracoes sao estimativas sequenciais de cronograma; em equipes paralelas (Cenario A/C) varias fases podem sobrepor-se.*
- *Base de equipe: Cenario A (equipe maior/paralela), Cenario C (hibrido - recomendado), Cenario B (enxuta/sequencial).*
- *Projeto pre-receita, AINDA SEM RECEITA: os prazos nao implicam receita garantida.*

| Fase | Entrega-chave | A | C (rec.) | B |
| :--- | :--- | :--- | :--- | :--- |
| **F2 - Lojas** | Play/Apple no ar | T+1-2 | T+2-3 | T+3-5 |
| **F3 - Auditoria (R$ 50k)** | Laudo/atestado | T+2-4 | T+3-5 | T+5-7 |
| **F4 - Migracao PQ** | Nucleo PQ homologado | T+4-7 | T+5-8 | T+8-12 |
| **F5 - PQ-Vault** | Lancamento comercial | T+7-10 | T+9-13 | T+13-18 |
| **F6 - Satelites B2B** | Sign no mercado (Sign/Drop/Lazaro) | T+10-14 | T+13-17 | T+18-24 |
| **F7 - Self-hosted** | Drive/NAS + Box (apos parceria) | T+14-18 | T+17-22 | T+24-30 |
| **F8 - Track 2 (anonimato)** | Roteamento/Tor-like | T+18-22 | T+22-26 | T+30-36 |
| **F9 - Feature premium + Consultoria** | Analise on-device + servico | T+16-20 (paralela) | T+20-24 | T+26-32 |
| **F10 - Sentinela (R&D)** | 1o prototipo on-device | T+24-30 | T+28-34 | T+36-42 |
| **F11 - Fortaleza completa** | Ecossistema maduro | T+30-36 | T+34-40 | T+42-48 |

> **Nota de Honestidade:** Prazos sao estimativas de roadmap pos-rodada, sujeitas a contratacao da equipe, auditoria externa e metricas dos pilotos. Nao constituem promessa de receita. *(F0 e F1 sao pre-rodada - F0 concluida; F1 pilotos em andamento).*
