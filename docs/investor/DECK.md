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

# RAIX
### Privacidade forte por design.
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
**Privacidade real, auditável.**
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

## 9. Cenários de Investimento
- **Cenário A:** R$ 1,8–2,1M (Time máximo)
- **Cenário B:** R$ 1,0–1,2M (Lean total)
- **Cenário C (Recomendado):** R$ 1,4–1,6M (Abordagem Híbrida)
  - Equilibra runway com time focado em segurança e B2B.
  - Pró-labore Fundador progride com receita (R$ 10k → R$ 25k → R$ 40k → R$ 50k).

---

## 10. Roadmap de Crescimento (F0-F5)
- **F0 Fundação:** Custos INPI/domínio pagos. *(Crítico: INPI s/ recibo definitivo).*
- **F1 Pilotos:** Validação sem receita. *(Crítico: Backlog de QR v1.7).*
- **F2 Lojas:** *(Crítico: D-U-N-S pendente para Play/Apple e Data Safety).*
- **F3 Auditoria Externa (R$ 50k):** *(Crítico: Credencial para B2B).*
- **F4 Migração PQ:** *(Crítico: Requer atestado externo, roteiro apenas agendado).*
- **F5 PQ-Vault:** *(Crítico: Depende do núcleo estabilizado e auditoria).*

---

## 10. Roadmap de Crescimento (F6-F11)
- **F6 Satélites B2B (Sign, Drop, Lázaro):** *(Crítico: Lázaro é premium).*
- **F7 Self-hosted (Drive + Box):** *(Crítico: Depende de parcerias NAS).*
- **F8 Track 2 (Anonimato):** *(Crítico: Precisa métricas reais F1).*
- **F9 Premium/Consultoria:** *(Crítico: Não distrair do core).*
- **F10 Sentinela (R&D):** *(Crítico: Alto esforço).*
- **F11 Fortaleza Completa:** *(Crítico: CI Travada; Governor Não-IA).*

---

## 11. Riscos e Mitigações
- **Desenvolvimento Solo com IA:** Política de agentes rigorosamente versionada, baseline congelado.
- **Dependência Infra (Firebase/Lojas):** Risco de D-U-N-S e política "Data Safety" já mapeados no `DATA_INVENTORY.md`.
- **Propriedade e Marca:** Depósito INPI realizado (classes 9 e 42).
- **Código Aberto:** Dual licensing protege o modelo B2B e garante escrutínio público transparente.

---

## 12. O Ask
**Captação Alvo: R$ 1,4M – R$ 1,6M (Cenário Híbrido C)**
*Alocação inicial rigorosa: R$ 50k Auditoria Externa + Estrutura B2B.*

**A Visão:**
Evoluir do status de mensageiro E2E zero-knowledge, já validado por pilotos, para o **ecossistema definitivo de soberania corporativa (Fortaleza RAIX)**, sem perder a disciplina financeira.
