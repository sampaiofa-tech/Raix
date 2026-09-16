# RAIX

**DATA:** 16 de setembro de 2026
**VERSÃƒO:** 1.0
**TÃTULO:** NOTA TÃ‰CNICA: CONFORMIDADE INTERNACIONAL DE PROTEÃ‡ÃƒO DE DADOS

---

## Resumo Executivo

A presente nota tÃ©cnica tem como objetivo esclarecer o status atual e o *roadmap* de conformidade do RAIX frente Ã s legislaÃ§Ãµes globais de proteÃ§Ã£o de dados.

**Posicionamento de Conformidade:**
Atualmente, o RAIX encontra-se **plenamente conforme no Brasil** (LGPD e Marco Civil da Internet). Para as demais jurisdiÃ§Ãµes internacionais, nossa arquitetura jÃ¡ se encontra alinhada por princÃ­pio â€” utilizando *zero-knowledge*, minimizaÃ§Ã£o de dados e *privacy by design* â€”, contudo, **nÃ£o possuÃ­mos conformidade formal ainda**. A expansÃ£o internacional seguirÃ¡ um *roadmap* de adequaÃ§Ã£o legal e regulatÃ³ria especÃ­fico por jurisdiÃ§Ã£o.

A essÃªncia do RAIX nÃ£o se negocia: **o conteÃºdo pertence ao usuÃ¡rio, nunca ao Estado nem a terceiros**. Este princÃ­pio define de forma inegociÃ¡vel como atuamos em cada paÃ­s.

---

## A. BRASIL â€” LGPD + Marco Civil (JÃ CONFORME)

O RAIX possui conformidade completa e verificÃ¡vel no Brasil.

* **Passo 0 (ConcluÃ­do):** PublicaÃ§Ã£o de PolÃ­ticas de Privacidade (PP), Termos de Uso (ToU), Data Processing Agreement (DPA) e Registro de OperaÃ§Ãµes de Tratamento (ROPA).
* **Passo 1 (ConcluÃ­do):** Resposta a requisiÃ§Ãµes de titulares de dados (DSR) em atÃ© 15 dias, com *runbook* ativo.
* **Passo 2 (ConcluÃ­do):** Logs de conexÃ£o isolados pelo perÃ­odo legal de 180 dias (Marco Civil da Internet, Art. 15).

---

## B. EUROPA â€” GDPR (RGPD) â€” Roadmap

A adequaÃ§Ã£o ao mercado europeu exigirÃ¡ um esforÃ§o burocrÃ¡tico, com custo estimado de baixo a mÃ©dio, sem perda da essÃªncia arquitetural do RAIX. A adequaÃ§Ã£o ocorrerÃ¡ em 6 passos:

* **Passo 1:** Nomear representante na UE (obrigatÃ³rio para fornecimento relevante na regiÃ£o).
* **Passo 2:** Estabelecer contrato de transferÃªncia internacional (*Standard Contractual Clauses* - SCCs da ComissÃ£o Europeia) com o suboperador (Google/Firebase).
* **Passo 3:** Realizar AvaliaÃ§Ã£o de Impacto sobre a ProteÃ§Ã£o de Dados (DPIA), que serÃ¡ amplamente facilitada por nossa arquitetura *zero-knowledge*.
* **Passo 4:** Implementar processo de atendimento a titulares compatÃ­vel com a GDPR. O "direito ao esquecimento" jÃ¡ Ã© atendido por construÃ§Ã£o na plataforma via *crypto-shredding*.
* **Passo 5:** Alinhar polÃ­tica de retenÃ§Ã£o (metadados mÃ­nimos e logs isolados).
* **Passo 6:** Produzir documentaÃ§Ã£o formal de conformidade e definir ponto de contato oficial.

---

## C. ESTADOS UNIDOS â€” Mosaico Estadual â€” Roadmap

Devido Ã  ausÃªncia de uma lei federal unificada nos EUA, a adequaÃ§Ã£o serÃ¡ um esforÃ§o administrativo e fragmentado, executado em 4 passos:

* **Passo 1:** Mapear os estados com leis de privacidade relevantes ao pÃºblico-alvo (CalifÃ³rnia - CCPA/CPRA, Colorado, VirgÃ­nia, Utah, Connecticut, entre outros).
* **Passo 2:** Ajustar avisos de privacidade e termos para atender aos requisitos especÃ­ficos de cada estado aplicÃ¡vel.
* **Passo 3:** Estabelecer mecanismo de atendimento a titulares (ex: *opt-out* de venda/compartilhamento de dados). Este passo Ã© facilitado pelo fato de o RAIX nÃ£o realizar qualquer tipo de coleta para venda de dados.
* **Passo 4:** Realizar avaliaÃ§Ã£o jurÃ­dica estado a estado antes de iniciar as operaÃ§Ãµes em cada um.

---

## D. AMÃ‰RICA LATINA â€” Roadmap

Para os mercados latino-americanos com legislaÃ§Ãµes de privacidade alinhadas (na maioria dos casos, derivadas da matriz da GDPR europeia), o esforÃ§o de adequaÃ§Ã£o Ã© considerado baixo e envolverÃ¡ 3 passos:

* **Passo 1:** Mapear os requisitos de cada paÃ­s alvo (ex.: MÃ©xico, ColÃ´mbia, Argentina, Uruguai).
* **Passo 2:** Adequar a documentaÃ§Ã£o local conforme as obrigaÃ§Ãµes especÃ­ficas de cada jurisdiÃ§Ã£o.
* **Passo 3:** Estabelecer processos de atendimento a titulares com validade local.

---

## E. RÃšSSIA E CHINA â€” ExclusÃ£o e AtuaÃ§Ã£o Soberana

**ExclusÃ£o da Conformidade OrdinÃ¡ria:**
As legislaÃ§Ãµes de jurisdiÃ§Ãµes como RÃºssia e China exigem localizaÃ§Ã£o forÃ§ada de dados em seus territÃ³rios e, em alguns casos (como na RÃºssia), mecanismos de acesso estatal garantido. Tais exigÃªncias **conflitam frontalmente com a essÃªncia do RAIX** (onde o conteÃºdo pertence exclusivamente ao usuÃ¡rio, e nunca ao Estado). Adequar o sistema a esses termos significaria abandonar a promessa central do nosso produto.

**Como Atuamos (Modelo Soberano):**
O RAIX **nÃ£o** operarÃ¡ nestes mercados atravÃ©s da nossa conformidade interna ordinÃ¡ria (plataforma SaaS padrÃ£o). O atendimento para essas regiÃµes se darÃ¡ **apenas via modelo PRIVATE / SELF-HOSTED soberano**.

Neste modelo, a prÃ³pria organizaÃ§Ã£o ou cliente licencia o sistema e mantÃ©m os dados e servidores em seu territÃ³rio, sob as leis que o cliente escolher. A plataforma RAIX permanecerÃ¡ tecnicamente cega e nÃ£o deterÃ¡ nenhuma chave de criptografia, por construÃ§Ã£o. NÃ£o ofereceremos, em nenhuma hipÃ³tese, qualquer mecanismo de acesso estatal ou *backdoor* no nosso cÃ³digo, tampouco prometeremos "conformidade local" que contradiga nossos princÃ­pios.

---

## Posicionamento Final

> *"Queremos ser o padrÃ£o mundial de privacidade â€” nÃ£o o padrÃ£o de um mundo que pede para abrir a porta. Nos mercados que valorizam privacidade, adequaÃ§Ã£o Ã© burocracia avenida. Nos regimes de controle, atuamos apenas por modelo soberano â€” nunca cedendo a promessa de que o conteÃºdo pertence ao usuÃ¡rio."*

---
**Nota de Honestidade:** Reitera-se que, na data desta publicaÃ§Ã£o, a **conformidade completa e formalizada do RAIX existe apenas no BRASIL**. As demais jurisdiÃ§Ãµes aqui citadas representam exclusivamente nosso *roadmap* de adequaÃ§Ã£o. NÃ£o afirmamos que o sistema Ã© "GDPR-ready" ou legalmente adequado nos EUA/Europa/AmÃ©rica Latina atÃ© que as avaliaÃ§Ãµes jurÃ­dicas e passos burocrÃ¡ticos formais tenham sido integralmente concluÃ­dos.
