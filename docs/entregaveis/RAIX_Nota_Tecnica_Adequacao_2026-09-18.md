# RAIX — Adequação à Legislação Brasileira (LGPD + Marco Civil), com Evidências e Preparo Global

**Data:** 18 de setembro de 2026
**Documento Técnico Formal:** Adequação regulatória e posicionamento legal do ecossistema RAIX.

## PARTE I — ADEQUAÇÃO BRASILEIRA (LGPD e Marco Civil da Internet)

O projeto RAIX opera sob a diretriz de "privacidade forte por design, com retenção limitada de metadados". O estágio atual é de conformidade formal com itens em formalização (LIA e ato de DPO). Os dados foram classificados de forma rigorosa, e implementou-se o isolamento completo da infraestrutura de faturamento em relação aos dados de uso (zero-knowledge).

### Adequação à Lei Geral de Proteção de Dados (LGPD - Lei nº 13.709/2018)

*   **Art. 7º e Art. 10 (Bases Legais e Legítimo Interesse):**
    *   *Como o RAIX cumpre:* O tratamento de metadados mínimos apoia-se estritamente na execução de contrato (prestação do serviço de mensageria) e no legítimo interesse (segurança do sistema e prevenção a fraudes).
    *   *Evidência:* LIA (Relatório de Legítimo Interesse) em fase de formalização documental; matriz de correlação de dados implementada.
*   **Art. 18 e Art. 19 (Direitos dos Titulares e Acesso):**
    *   *Como o RAIX cumpre:* Os usuários possuem autonomia total para destruir suas próprias chaves e contas, resultando na perda irreversível de acesso e anonimização efetiva da cifra em repouso.
    *   *Evidência:* Funcionalidades nativas de crypto-shredding (Account Deletion e Vanish) implementadas e ativas no cliente.
*   **Art. 20 (Revisão de Decisões Automatizadas):**
    *   *Como o RAIX cumpre:* O sistema de segurança e contra-inteligência não toma decisões jurídicas ou restritivas baseadas em perfis comportamentais automatizados sem possibilidade de recurso.
    *   *Evidência:* Arquitetura do RAIX Governor (classificada como NÃO-IA, totalmente determinística).
*   **Art. 37 e Art. 39 (Registro de Operações e Relatório de Impacto):**
    *   *Como o RAIX cumpre:* Os fluxos de dados foram mapeados de ponta a ponta; os riscos de privacidade foram avaliados em design (DPIA genérico da arquitetura base).
    *   *Evidência:* DFD (Data Flow Diagram) e Relatório de Avaliação de Impacto (em formalização complementar).
*   **Art. 41 (Encarregado pelo Tratamento - DPO):**
    *   *Como o RAIX cumpre:* A governança de dados e os papéis foram definidos.
    *   *Evidência:* Ato de nomeação do DPO em fase de formalização interna.
*   **Art. 46 e Art. 48 (Segurança, Sigilo e Comunicação de Incidentes):**
    *   *Como o RAIX cumpre:* Uso de criptografia ponta a ponta pós-quântica (híbrida ML-KEM-768/ML-DSA-65) e chaves efêmeras. Planos de resposta a incidentes atrelados ao roadmap.
    *   *Evidência:* Auditoria de dependências/SDKs de terceiros; rotina de Backup e Disaster Recovery (PITR janela de 7 dias com crypto-shredding automático) validada.

### Adequação ao Marco Civil da Internet (Lei nº 12.965/2014)

*   **Art. 7º (Direitos do Usuário):**
    *   *Como o RAIX cumpre:* O aplicativo não expõe dados pessoais a terceiros nem utiliza dados para publicidade (isolamento de faturamento comercial vs. uso privado).
    *   *Evidência:* Regras restritivas do Firestore e isolamento de bases; Termos de Uso (ToU) e Política de Privacidade (PP).
*   **Art. 10 (Proteção a Registros):**
    *   *Como o RAIX cumpre:* A arquitetura retém o estritamente necessário para prover a comunicação e mantém logs restritos.
    *   *Evidência:* Ausência de coleta passiva não essencial (DPIA).
*   **Art. 15 (Guarda de Registros de Acesso):**
    *   *Como o RAIX cumpre:* Os registros de acesso a aplicações são mantidos por 180 dias em ambiente isolado e de acesso restrito.
    *   *Evidência:* Configuração técnica de retenção de logs isolados (acesso interno estritamente auditado).
*   **Art. 22 (Fornecimento de Informações - Ordem Judicial):**
    *   *Como o RAIX cumpre:* A arquitetura técnica foi pensada para atender determinações judiciais sem violar a criptografia das mensagens (cujo conteúdo matemático é inacessível ao provedor). O provedor entrega exclusivamente o metadado que possui retido.
    *   *Evidência:* Desenho de banco de dados NoSQL compartimentado; logs de acesso separados do faturamento.

## PARTE II — PREPARO GLOBAL POR JURISDIÇÃO

*   **Europa (GDPR):** O projeto não é "GDPR-ready" de forma integral. A adequação europeia está posicionada no roadmap estratégico, demandando, no futuro, a nomeação formal de um representante (Art. 27), execução de SCCs (Standard Contractual Clauses) caso o trânsito transfronteiriço envolva suboperadores europeus, e adaptações específicas no DPIA.
*   **Estados Unidos (Mosaico Estadual):** Regulamentações fragmentadas (CCPA/CPRA, VCDPA, etc.) serão analisadas sob demanda mediante crescimento da base regional. A arquitetura de minimização atende ao núcleo de consentimento genérico.
*   **América Latina (Matriz Base):** A base regulatória se espelha predominantemente no modelo GDPR/LGPD. A matriz atual de conformidade reduz os atritos de adequação na maioria destes países (roadmap regional facilitado).
*   **Rússia e China:** Estas jurisdições encontram-se excluídas da conformidade ordinária, devido a requisitos de quebra de criptografia e leis estritas de localização de dados do estado. A atuação ocorrerá apenas via modelo Sovereign Box isolado (Self-Hosted corporativo).

## PARTE III — POSICIONAMENTO FINAL

O ecossistema RAIX encontra-se em conformidade formal completa para o território brasileiro, com itens menores em processo final de formalização (LIA e ato de DPO). Os controles técnicos garantem uma posição forte contra acessos indevidos e provam que o modelo de "privacidade forte por design" não é apenas marketing, mas sim arquitetura executada em código. Jurisdições externas constam em roadmap de maturidade progressiva.

### Evidências Anexadas (Referências Técnicas)
1. DFD + Matriz de Correlação + Teste Adversarial (Validation Set).
2. Arquitetura de Isolamento de Faturamento vs. Dados de Uso.
3. Política e Execução de Crypto-Shredding e PITR (janela de 7 dias restrita).
4. Relatório de Auditoria de SDKs (Supply Chain).
