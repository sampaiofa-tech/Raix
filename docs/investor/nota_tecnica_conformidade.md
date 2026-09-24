# RAIX

**DATA:** 16 de setembro de 2026
**VERSAO:** 1.0
**TITULO:** NOTA TECNICA: CONFORMIDADE INTERNACIONAL DE PROTECAO DE DADOS

---

## Resumo Executivo

A presente nota tecnica tem como objetivo esclarecer o status atual e o *roadmap* de conformidade do RAIX frente as legislacoes globais de protecao de dados.

**Posicionamento de Conformidade:**
Atualmente, o RAIX encontra-se **plenamente conforme no Brasil** (LGPD e Marco Civil da Internet). Para as demais jurisdicoes internacionais, nossa arquitetura ja se encontra alinhada por principio - utilizando *zero-knowledge*, minimizacao de dados e *privacy by design* -, contudo, **nao possuimos conformidade formal ainda**. A expansao internacional seguira um *roadmap* de adequacao legal e regulatoria especifico por jurisdicao.

A essencia do RAIX nao se negocia: **o conteudo pertence ao usuario, nunca ao Estado nem a terceiros**. Este principio define de forma inegociavel como atuamos em cada pais.

---

## A. BRASIL - LGPD + Marco Civil (JA CONFORME)

O RAIX possui conformidade completa e verificavel no Brasil.

* **Passo 0 (Concluido):** Publicacao de Politicas de Privacidade (PP), Termos de Uso (ToU), Data Processing Agreement (DPA) e Registro de Operacoes de Tratamento (ROPA).
* **Passo 1 (Concluido):** Resposta a requisicoes de titulares de dados (DSR) em ate 15 dias, com *runbook* ativo.
* **Passo 2 (Concluido):** Logs de conexao isolados pelo periodo legal de 180 dias (Marco Civil da Internet, Art. 15).

---

## B. EUROPA - GDPR (RGPD) - Roadmap

A adequacao ao mercado europeu exigira um esforco burocratico, com custo estimado de baixo a medio, sem perda da essencia arquitetural do RAIX. A adequacao ocorrera em 6 passos:

* **Passo 1:** Nomear representante na UE (obrigatorio para fornecimento relevante na regiao).
* **Passo 2:** Estabelecer contrato de transferencia internacional (*Standard Contractual Clauses* - SCCs da Comissao Europeia) com o suboperador (Google/Firebase).
* **Passo 3:** Realizar Avaliacao de Impacto sobre a Protecao de Dados (DPIA), que sera amplamente facilitada por nossa arquitetura *zero-knowledge*.
* **Passo 4:** Implementar processo de atendimento a titulares compativel com a GDPR. O "direito ao esquecimento" ja e atendido por construcao na plataforma via *crypto-shredding*.
* **Passo 5:** Alinhar politica de retencao (metadados minimos e logs isolados).
* **Passo 6:** Produzir documentacao formal de conformidade e definir ponto de contato oficial.

---

## C. ESTADOS UNIDOS - Mosaico Estadual - Roadmap

Devido a ausencia de uma lei federal unificada nos EUA, a adequacao sera um esforco administrativo e fragmentado, executado em 4 passos:

* **Passo 1:** Mapear os estados com leis de privacidade relevantes ao publico-alvo (California - CCPA/CPRA, Colorado, Virginia, Utah, Connecticut, entre outros).
* **Passo 2:** Ajustar avisos de privacidade e termos para atender aos requisitos especificos de cada estado aplicavel.
* **Passo 3:** Estabelecer mecanismo de atendimento a titulares (ex: *opt-out* de venda/compartilhamento de dados). Este passo e facilitado pelo fato de o RAIX nao realizar qualquer tipo de coleta para venda de dados.
* **Passo 4:** Realizar avaliacao juridica estado a estado antes de iniciar as operacoes em cada um.

---

## D. AMERICA LATINA - Roadmap

Para os mercados latino-americanos com legislacoes de privacidade alinhadas (na maioria dos casos, derivadas da matriz da GDPR europeia), o esforco de adequacao e considerado baixo e envolvera 3 passos:

* **Passo 1:** Mapear os requisitos de cada pais alvo (ex.: Mexico, Colombia, Argentina, Uruguai).
* **Passo 2:** Adequar a documentacao local conforme as obrigacoes especificas de cada jurisdicao.
* **Passo 3:** Estabelecer processos de atendimento a titulares com validade local.

---

## E. RUSSIA E CHINA - Exclusao e Atuacao Soberana

**Exclusao da Conformidade Ordinaria:**
As legislacoes de jurisdicoes como Russia e China exigem localizacao forcada de dados em seus territorios e, em alguns casos (como na Russia), mecanismos de acesso estatal garantido. Tais exigencias **conflitam frontalmente com a essencia do RAIX** (onde o conteudo pertence exclusivamente ao usuario, e nunca ao Estado). Adequar o sistema a esses termos significaria abandonar a promessa central do nosso produto.

**Como Atuamos (Modelo Soberano):**
O RAIX **nao** operara nestes mercados atraves da nossa conformidade interna ordinaria (plataforma SaaS padrao). O atendimento para essas regioes se dara **apenas via modelo PRIVATE / SELF-HOSTED soberano**.

Neste modelo, a propria organizacao ou cliente licencia o sistema e mantem os dados e servidores em seu territorio, sob as leis que o cliente escolher. A plataforma RAIX permanecera tecnicamente cega e nao detera nenhuma chave de criptografia, por construcao. Nao ofereceremos, em nenhuma hipotese, qualquer mecanismo de acesso estatal ou *backdoor* no nosso codigo, tampouco prometeremos "conformidade local" que contradiga nossos principios.

---

## Posicionamento Final

> *"Queremos ser o padrao mundial de privacidade - nao o padrao de um mundo que pede para abrir a porta. Nos mercados que valorizam privacidade, adequacao e burocracia avenida. Nos regimes de controle, atuamos apenas por modelo soberano - nunca cedendo a promessa de que o conteudo pertence ao usuario."*

---
**Nota de Honestidade:** Reitera-se que, na data desta publicacao, a **conformidade completa e formalizada do RAIX existe apenas no BRASIL**. As demais jurisdicoes aqui citadas representam exclusivamente nosso *roadmap* de adequacao. Nao afirmamos que o sistema e "GDPR-ready" ou legalmente adequado nos EUA/Europa/America Latina ate que as avaliacoes juridicas e passos burocraticos formais tenham sido integralmente concluidos.
