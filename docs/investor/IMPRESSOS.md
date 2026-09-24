# RAIX: Materiais Executivos para Investimento

> **Aviso Oficial:** Projeto prÃ©-receita, AINDA SEM RECEITA. Baseline de produto congelado.
> **Posicionamento:** Privacidade forte por design, com retenÃ§Ã£o limitada de metadados.

---

## 1. One-Pager Executivo

### O Problema

O uso de mensageiros comuns em contextos profissionais (advocacia, saÃºde, executivos) gera um **passivo LGPD oculto**. O armazenamento contÃ­nuo de metadados, contatos e histÃ³rico nÃ£o-criptografado em servidores nÃ£o auditÃ¡veis ou atrelados a redes sociais compromete irreparavelmente o sigilo profissional, criando alto risco de vazamentos e multas.

### A SoluÃ§Ã£o: RAIX

O RAIX Ã© uma plataforma B2B de **privacidade forte por design**. Diferente de soluÃ§Ãµes massificadas, opera com **servidor cego (zero-knowledge)** e **retenÃ§Ã£o limitada de metadados** (logs isolados por 180 dias estritamente para Marco Civil, sem mineraÃ§Ã£o). Nosso compliance jÃ¡ nasce pronto (PP, ToU, DPA, ROPA publicados).

### Diferencial TecnolÃ³gico

- **Arquitetura Zero-Knowledge & Crypto-Shredding:** DestruiÃ§Ã£o ativa de chaves (TTL â‰¤15min) e recurso *vanish-after-read*.
- **PÃ³s-QuÃ¢ntico (PQ):** PÃ³s-quÃ¢ntica hÃ­brida (roteiro aprovado, auditoria agendada).
- **Licenciamento AGPL-3.0 + Licença Comercial (open-core):** O núcleo será publicado — hoje, código disponível sob solicitação para auditoria e due diligence. Garante transparência sem abrir mão do controle corporativo.
- **Infraestrutura Otimizada:** Mensageiro rodando sobre GCP/Firebase com custo operacional atual de **R$ 0,00/mÃªs**.

### TraÃ§Ã£o Atual & MÃ©tricas

- **Pilotos em uso ativo:** 5 advogados operando o sistema de ponta a ponta (~2h/dia).
- **Testes rigorosos:** >200 testes automatizados (backend, Android, desktop) e auditoria de sanitizaÃ§Ã£o concluÃ­da (7 fases).
- **Demanda validada:** Pilotos solicitaram o "Trilho de Auditoria Local" (jÃ¡ inserido no roadmap).
- **Custo Fixo Anual:** Apenas US$ 170â€“240 em licenÃ§as de domÃ­nio/ferramentas.

---

## 2. Os 3 CenÃ¡rios de Investimento (Comparativo)

| CenÃ¡rio | CaptaÃ§Ã£o (R$) | EstratÃ©gia / Perfil | PrÃ³s | Contras |
| :--- | :--- | :--- | :--- | :--- |
| **A** | **1,8M â€“ 2,1M** | **Agressiva** (ExpansÃ£o rÃ¡pida de time) | Maior velocidade no roadmap e mÃºltiplos produtos. | Alto burn-rate; risco Ã  cultura de seguranÃ§a pela entrada simultÃ¢nea de devs. |
| **B** | **1,0M â€“ 1,2M** | **Lean Total** (Foco exclusivo no core) | Extrema disciplina financeira; pista de voo estendida. | Limita a expansÃ£o B2B e retarda muito as ferramentas SatÃ©lite (Sign, Drop). |
| **C (HÃ­brido)** | **1,4M â€“ 1,6M** | **Equilibrada (Foco no B2B)** | **Pista segura para o B2B; absorve R$ 50k da auditoria; forma time de base.** | **Requer forte gestÃ£o financeira para nÃ£o assumir os custos de A.** |

### AlocaÃ§Ã£o Detalhada por CenÃ¡rio

**CENÃRIO A â€” RODADA MAIOR (R$ 1,8â€“2,1M; equipe Ano 1: 5 pessoas; custo ~R$ 102k/mÃªs):**

- SalÃ¡rios ~80% (~R$ 1,52M sobre mÃ©dio R$ 1,9M)
- Infraestrutura/nuvem ~5% (~R$ 95k)
- LicenÃ§as/ferramentas ~3% (~R$ 57k)
- Equipamentos (uma vez) ~2% (~R$ 38k)
- Auditoria externa (R$ 50k) ~3% (~R$ 57k)
- Contador/jurÃ­dico ~3% (~R$ 57k)
- Reserva operacional ~4% (~R$ 76k)

**CENÃRIO B â€” ESCALONADO (R$ 1,0â€“1,2M; equipe Ano 1: 3 pessoas; custo ~R$ 56k/mÃªs):**

- SalÃ¡rios ~80% (~R$ 880k sobre mÃ©dio R$ 1,1M)
- Infra/nuvem ~4% (~R$ 44k)
- LicenÃ§as ~3% (~R$ 33k)
- Equipamentos ~2% (~R$ 22k)
- Auditoria (R$ 50k) ~5% (~R$ 55k)
- Contador/jurÃ­dico ~3% (~R$ 33k)
- Reserva ~3% (~R$ 33k)

**CENÃRIO C â€” HÃBRIDO, RECOMENDADO (R$ 1,4â€“1,6M; equipe Ano 1: 4 pessoas; custo ~R$ 81k/mÃªs):**

- SalÃ¡rios ~82% (~R$ 1,23M sobre mÃ©dio R$ 1,5M)
- Infra/nuvem ~4% (~R$ 60k)
- LicenÃ§as ~3% (~R$ 45k)
- Equipamentos ~2% (~R$ 30k)
- Auditoria (R$ 50k) ~3% (~R$ 45k)
- Contador/jurÃ­dico ~3% (~R$ 45k)
- Reserva ~3% (~R$ 45k)

> **PONTO-CHAVE:** Nos 3 cenÃ¡rios, a maior parte do capital vai para salÃ¡rios (o ativo mais valioso). Infra + licenÃ§as + equipamentos < 10%. A Ãºnica despesa de capital relevante Ã© a auditoria (R$ 50k), que destrava o B2B. O que diferencia os cenÃ¡rios Ã© a velocidade de montagem do time: A = contrata rÃ¡pido (mais queima/diluiÃ§Ã£o), B = cresce devagar (disciplina, menos velocidade), C = nÃºcleo crÃ­tico + expansÃ£o conforme receita.

### RecomendaÃ§Ã£o EstratÃ©gica: CenÃ¡rio C (HÃ­brido)

O CenÃ¡rio C garante capital suficiente para atravessar a primeira grande despesa de capital (**Auditoria Externa - R$ 50k**) e construir a base de clientes B2B (EscritÃ³rios) antes de expandir P&D de forma irresponsÃ¡vel.
**EvoluÃ§Ã£o do PrÃ³-labore do Fundador (atrelada Ã  receita/estabilidade):**
R$ 10k (A1) â†’ R$ 25k (A2) â†’ R$ 40k (A3) â†’ R$ 50k (A4).

---

## 3. Ponto de EquilÃ­brio e Lucro (Ano 1)

O RAIX ataca o B2B utilizando uma precificaÃ§Ã£o elÃ¡stica, permitindo que pequenos escritÃ³rios e grandes contas sustentem a operaÃ§Ã£o GCP.

**Estrutura de Planos (B2B):**

- **Profissional:** R$ 29â€“39 / mÃªs
- **EscritÃ³rio:** R$ 29â€“49 / seat / mÃªs
- **Private:** Ticket premium, 100% customizÃ¡vel (previsto 2026)

| EstÃ¡gio de OperaÃ§Ã£o | ComposiÃ§Ã£o 1 (Contas Premium) | ComposiÃ§Ã£o 2 (Volume B2B) |
| :--- | :--- | :--- |
| **Breakeven (Empate)** | ~8 clientes **Private** | 5 **Privates** + 100 seats **EscritÃ³rio** |
| **Escala de Lucro** | (Foco em expandir tickets B2B) | ~6 **Privates** + 120 seats **EscritÃ³rio** |

*Nota: A operaÃ§Ã£o atual tem custo R$ 0/mÃªs no free tier GCP.*

---

## 4. Roadmap Completo: Fortaleza RAIX & Pontos CrÃ­ticos

Este roadmap de 3 anos nÃ£o permite atalhos arquitetÃ´nicos. O sistema estÃ¡ baseado na evoluÃ§Ã£o progressiva de confianÃ§a (onde a fase seguinte **herda** a blindagem da fase anterior).

| Fase | DescriÃ§Ã£o da Meta | Ponto CrÃ­tico / Gargalo (Risco Mapeado) |
| :--- | :--- | :--- |
| **F0 (FundaÃ§Ã£o)** | Custos pagos, compliance (PP, ToU) no ar. | INPI ainda sem recibo definitivo (processo em andamento). |
| **F1 (Pilotos)** | ValidaÃ§Ã£o qualitativa (5 advogados ativos). | **NÃ£o gera receita.** Atrito de UX (QR Code) empurrado p/ v1.7. |
| **F2 (Lojas)** | Entrada nas App Stores. | Depende da emissÃ£o do D-U-N-S; `DATA_INVENTORY.md` crÃ­tico para aprovaÃ§Ã£o de "Data Safety". |
| **F3 (Auditoria Externa)** | Atestado de seguranÃ§a. Primeira despesa de caixa (**R$ 50k**). | **Credencial indispensÃ¡vel** para vender aos planos B2B/EscritÃ³rio. Sem auditoria, nÃ£o hÃ¡ B2B. |
| **F4 (MigraÃ§Ã£o PQ)** | Upgrade TLS-PQ e Double Ratchet. | Sem atestado externo, mantemos: *"roteiro aprovado, auditoria agendada"*. NÃ£o afirmar *"pronto"*. |
| **F5 (PQ-Vault)** | GeraÃ§Ã£o de nova receita baseada em arquivos. | Depende da estabilidade absoluta do nÃºcleo E2E (F4). |
| **F6 (SatÃ©lites B2B)** | RAIX Sign (P1), Drop (P2), LÃ¡zaro (P3). | Sign depende de mercado; LÃ¡zaro exige precificaÃ§Ã£o premium altÃ­ssima. |
| **F7 (Self-Hosted)** | RAIX Drive + Sovereign Box. | Depende de acordos B2B com fabricantes NAS (NÃ£o comprometer prazos). |
| **F8 (Track 2)** | Ferramenta de Anonimato nativo. | Requer levantamento profundo de mÃ©tricas operacionais da F1. |
| **F9 (Premium/Consult.)** | AnÃ¡lise on-device e Consultoria de Privacidade. | Receita complementar: **nÃ£o deve distrair o time** da plataforma core. |
| **F10 (Sentinela - R&D)** | Monitoramento de ameaÃ§as. | Alto esforÃ§o tÃ©cnico; posicionamento apenas como visÃ£o de longo prazo. |
| **F11 (Fortaleza RAIX)** | Contra-InteligÃªncia e RAIX Governor. | **Contra-InteligÃªncia TRAVADA** atÃ© validaÃ§Ã£o legal. Governor precisa ser 100% determinÃ­stico (NÃƒO usar IA). |

### Prazos Estimados por Fase Ã— CenÃ¡rio (em meses, a partir de T+0)

*Premissas:*

- *T+0 = inÃ­cio da execuÃ§Ã£o apÃ³s o aporte (entrada do capital).*
- *As duraÃ§Ãµes sÃ£o estimativas sequenciais de cronograma; em equipes paralelas (CenÃ¡rio A/C) vÃ¡rias fases podem sobrepor-se.*
- *Base de equipe: CenÃ¡rio A (equipe maior/paralela), CenÃ¡rio C (hÃ­brido â€” recomendado), CenÃ¡rio B (enxuta/sequencial).*
- *Projeto prÃ©-receita, AINDA SEM RECEITA: os prazos nÃ£o implicam receita garantida.*

| Fase | Entrega-chave | A | C (rec.) | B |
| :--- | :--- | :--- | :--- | :--- |
| **F2 â€” Lojas** | Play/Apple no ar | T+1â€“2 | T+2â€“3 | T+3â€“5 |
| **F3 â€” Auditoria (R$ 50k)** | Laudo/atestado | T+2â€“4 | T+3â€“5 | T+5â€“7 |
| **F4 â€” MigraÃ§Ã£o PQ** | NÃºcleo PQ homologado | T+4â€“7 | T+5â€“8 | T+8â€“12 |
| **F5 â€” PQ-Vault** | LanÃ§amento comercial | T+7â€“10 | T+9â€“13 | T+13â€“18 |
| **F6 â€” SatÃ©lites B2B** | Sign no mercado (Sign/Drop/LÃ¡zaro) | T+10â€“14 | T+13â€“17 | T+18â€“24 |
| **F7 â€” Self-hosted** | Drive/NAS + Box (apÃ³s parceria) | T+14â€“18 | T+17â€“22 | T+24â€“30 |
| **F8 â€” Track 2 (anonimato)** | Roteamento/Tor-like | T+18â€“22 | T+22â€“26 | T+30â€“36 |
| **F9 â€” Feature premium + Consultoria** | AnÃ¡lise on-device + serviÃ§o | T+16â€“20 (paralela) | T+20â€“24 | T+26â€“32 |
| **F10 â€” Sentinela (R&D)** | 1Âº protÃ³tipo on-device | T+24â€“30 | T+28â€“34 | T+36â€“42 |
| **F11 â€” Fortaleza completa** | Ecossistema maduro | T+30â€“36 | T+34â€“40 | T+42â€“48 |

> **Nota de Honestidade:** Prazos sÃ£o estimativas de roadmap pÃ³s-rodada, sujeitas Ã  contrataÃ§Ã£o da equipe, auditoria externa e mÃ©tricas dos pilotos. NÃ£o constituem promessa de receita. *(F0 e F1 sÃ£o prÃ©-rodada â€” F0 concluÃ­da; F1 pilotos em andamento).*
