---
marp: true
theme: default
class: lead
backgroundColor: #ffffff
color: #000000
style: |
  section {
    background-color: #001f3f; /* Azul Marinho */
    color: #ffffff;
  }
  h1, h2, h3, h4, h5, h6 {
    color: #D4AF37; /* Dourado */
  }
  strong {
    color: #D4AF37;
  }
  a {
    color: #2ECC40; /* Verde Esmeralda */
  }
  ul > li::marker {
    color: #2ECC40;
  }
---
<!-- markdownlint-disable MD033 -->

# RAIX

## Privacidade forte por design

*Projeto pré-receita, AINDA SEM RECEITA.*
*Solução de sigilo profissional com retenção limitada de metadados.*

---

## 1. O Problema: Passivo Oculto

- **Mensageiros comuns** geram passivo LGPD e expõem metadados sensíveis.
- **Risco legal:** Comunicação profissional (ex: advogados, médicos, executivos) traficada em servidores não auditáveis.
- **Vazamentos de ponta a ponta:** "Criptografia E2E" em redes sociais não protege o histórico local ou metadados de rede.

---

## 2. A Solução RAIX

**Sigilo profissional + Compliance por design.**

- **Zero-knowledge de conteúdo** (servidor cego).
- **Retenção limitada de metadados** (Logs isolados de 180 dias, Marco Civil).
- **Dual Licensing:** AGPL-3.0 + Comercial (protege IP corporativo).
- **Compliance nativo:** PP, ToU, DPA, ROPA já publicados.

---

## 3. Demonstração Técnica

**Privacidade real, verificável.**

- **E2E DEK v1.2:** Homologada (isolamento adversarial, servidor cego).
- **Vanish-after-read:** Mensagens destruídas localmente e remotamente.
- **Crypto-shredding:** TTL ativo (chaves destruídas em ≤15min).
- **Pós-quântica:** Roteiro aprovado, auditoria agendada.

---

## 4. Validação de Mercado

- **Pilotos Ativos:** 5 advogados operando o RAIX Messaging (~2h/dia).
- **Feedback Chave:** Demanda por "rastreabilidade" validou a criação do **Trilho de Auditoria Local**.
- **Percepção B2B:** Pilotos enxergam a ferramenta como monetizável e essencial para mitigar risco profissional (multas/vazamentos).

---

## 5. Modelo de Receita (B2B SaaS)

- **Free:** Operação com custo zero.
- **Profissional:** R$ 29–39 / mês.
- **Escritório:** R$ 29–49 / seat / mês.
- **Private:** Ticket premium, custom self-hosted (2026).
- **Consultoria de Privacidade:** Receita auxiliar pós-estabilidade.

---

## 6. Ecossistema: Fortaleza RAIX (6 Camadas)

1. **Fundação:** Core, PQ-ID, HSM.
2. **Ativos:** PQ-Vault, Drive.
3. **Comunicação:** Messaging, Sala, Bridge.
4. **Defesa:** Eraser, Selo.
5. **Contra-Inteligência:** RFI (Travada/R&D).
6. **Governança:** Hive, Maestro.

---

## 7. Custos e Disciplina Financeira

- **Custo Operacional Mensal:** R$ 0,00 (Free Tier GCP/Firebase otimizado).
- **Custo Fixo Anual:** US$ 170–240 (domínios, licenças).
- **Pista Longa:** Produto funcionando com compliance completo sem queima de caixa.
- **Próximo Passo:** Primeira despesa de capital alocada para **Auditoria Externa (R$ 50k).**

---

## 8. Ponto de Equilíbrio e Lucro (Ano 1)

Baseado no mix B2B realista:

- **Breakeven (Empate):** ~8 instâncias Private OU (5 Privates + 100 assinantes Escritório).
- **Lucro (Escala Inicial):** ~6 instâncias Private + 120 assinantes Escritório.

---

## 9. Cenários de Investimento e Alocação

<!-- _class: lead -->
<style scoped>
p { font-size: 0.65em; margin: 0px; }
h3 { font-size: 0.8em; margin-bottom: 5px; color: #D4AF37; }
</style>

### **Cenário A: Rodada Maior (R$ 1,8–2,1M \| 5 pessoas)**

Salários: ~80% \| Infra/Lic/Eq: ~10% \| Auditoria (R$ 50k): ~3% \| Outros: ~7%

### **Cenário C: Híbrido/Recomendado (R$ 1,4–1,6M \| 4 pessoas)**

Salários: ~82% \| Infra/Lic/Eq: ~9% \| Auditoria (R$ 50k): ~3% \| Outros: ~6%

### **Cenário B: Escalonado (R$ 1,0–1,2M \| 3 pessoas)**

Salários: ~80% \| Infra/Lic/Eq: ~9% \| Auditoria (R$ 50k): ~5% \| Outros: ~6%

> **PONTO-CHAVE:** Nos 3 cenários, a maior parte do capital vai para salários (ativo mais valioso). Infra + licenças + equipamentos < 10%. A única despesa de capital relevante é a auditoria (R$ 50k), que destrava o B2B. A diferença é a velocidade de montagem do time: A (rápido, mais queima), B (devagar, disciplina), C (núcleo crítico + expansão c/ receita).

---

## 10. Roadmap de Crescimento (F0-F5)

- **F0 Fundação:** Custos INPI/domínio pagos. *(Crítico: INPI s/ recibo definitivo).*
- **F1 Pilotos:** Validação sem receita. *(Crítico: Backlog de QR v1.7).*
- **F2 Lojas:** *(Crítico: D-U-N-S pendente para Play/Apple e Data Safety).*
- **F3 Auditoria Externa (R$ 50k):** *(Crítico: Credencial para B2B).*
- **F4 Migração PQ:** *(Crítico: Requer atestado externo, roteiro apenas agendado).*
- **F5 PQ-Vault:** *(Crítico: Depende do núcleo estabilizado e auditoria).*

---

## 11. Roadmap de Crescimento (F6-F11)

- **F6 Satélites B2B (Sign, Drop, Lázaro):** *(Crítico: Lázaro é premium).*
- **F7 Self-hosted (Drive + Box):** *(Crítico: Depende de parcerias NAS).*
- **F8 Track 2 (Anonimato):** *(Crítico: Precisa métricas reais F1).*
- **F9 Premium/Consultoria:** *(Crítico: Não distrair do core).*
- **F10 Sentinela (R&D):** *(Crítico: Alto esforço).*
- **F11 Fortaleza Completa:** *(Crítico: CI Travada; Governor Não-IA).*

---

## Cronograma Estimado (T+0)

<!-- _class: lead -->
<style scoped>
table { font-size: 0.65em; margin: 0 auto; }
li { font-size: 0.7em; }
p { font-size: 0.65em; }
</style>

*Prazos são estimativas de roadmap pós-rodada, sujeitas à contratação da equipe, auditoria externa e métricas dos pilotos. Não constituem promessa de receita. F0 concluída; F1 pilotos em andamento.*

| Fase | Entrega-chave | A | C (rec.) | B |
| :--- | :--- | :--- | :--- | :--- |
| **F2 — Lojas** | Play/Apple no ar | T+1–2 | T+2–3 | T+3–5 |
| **F3 — Auditoria (R$ 50k)** | Laudo/atestado | T+2–4 | T+3–5 | T+5–7 |
| **F4 — Migração PQ** | Núcleo PQ homologado | T+4–7 | T+5–8 | T+8–12 |
| **F5 — PQ-Vault** | Lançamento comercial | T+7–10 | T+9–13 | T+13–18 |
| **F6 — Satélites B2B** | Sign no mercado (Sign/Drop/Lázaro) | T+10–14 | T+13–17 | T+18–24 |
| **F7 — Self-hosted** | Drive/NAS + Box (após parceria) | T+14–18 | T+17–22 | T+24–30 |
| **F8 — Track 2 (anonimato)** | Roteamento/Tor-like | T+18–22 | T+22–26 | T+30–36 |
| **F9 — Feature premium** | Análise on-device + serviço | T+16–20 (paralela) | T+20–24 | T+26–32 |
| **F10 — Sentinela (R&D)** | 1º protótipo on-device | T+24–30 | T+28–34 | T+36–42 |
| **F11 — Fortaleza completa** | Ecossistema maduro | T+30–36 | T+34–40 | T+42–48 |

---

## 12. Riscos e Mitigações

- **Desenvolvimento Solo com IA:** Política de agentes rigorosamente versionada, baseline congelado.
- **Dependência Infra (Firebase/Lojas):** Risco de D-U-N-S e política "Data Safety" já mapeados no `DATA_INVENTORY.md`.
- **Propriedade e Marca:** Depósito INPI realizado (classes 9 e 42).
- **Código disponível sob solicitação para auditoria e due diligence:** Dual licensing (AGPL-3.0 + Comercial) protege o modelo B2B; o núcleo será publicado.

---

## 13. O Ask

**Captação Alvo: R$ 1,4M – R$ 1,6M (Cenário Híbrido C)**
*Alocação inicial rigorosa: R$ 50k Auditoria Externa + Estrutura B2B.*

**A Visão:**
Evoluir do status de mensageiro E2E zero-knowledge, já validado por pilotos, para o **ecossistema definitivo de soberania corporativa (Fortaleza RAIX)**, sem perder a disciplina financeira.
