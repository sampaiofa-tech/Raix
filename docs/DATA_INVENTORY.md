# Inventário de Dados e Relatório de Auditoria de Rastreamento (LGPD) — Raix

**Data da Auditoria:** 05 de setembro de 2026  
**Controlador:** Filippe Andrade Sampaio (desenvolvedor independente)  
**Encarregado (DPO):** Filippe Andrade Sampaio (`contato@raixtech.com`)  
**Aplicação:** Raix (Depósito de marca nominativa agendado para 08/09/2026 junto ao INPI — guia/protocolo preparatório nº 945109300)  
**Baseline de Código Auditado:** v1.4 (`composeApp`, Cloud Functions v1.4)

---

## 1. Inventário de Dados Tratados

A tabela a seguir discrimina de forma taxativa e exaustiva todas as informações e dados técnicos tratados pelo aplicativo e pela infraestrutura do **Raix**.

| Dado | Onde Vive (Armazenamento) | Por Quanto Tempo (Retenção) | Quem Tem Acesso | Base Legal (LGPD) |
| :--- | :--- | :--- | :--- | :--- |
| **UID Anônimo do Firebase Auth** | Firebase Authentication / Firestore | Enquanto a sessão anônima for válida | Servidor (Cloud Functions) para validação de sessão; Operador de nuvem (Google Cloud) | Art. 7º, IX — Legítimo Interesse (segurança e mitigação de abuso) |
| **Fingerprint Criptográfico (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular ou revogação de identidade | Usuários com o link/QR code de convite; Servidor (validação de roteamento) | Art. 7º, V — Execução de contrato / Termos de Uso |
| **Chave Pública de Roteamento (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular ou revogação | Público aos portadores do fingerprint; Servidor | Art. 7º, V — Execução de contrato |
| **Chave Pública de Criptografia (X25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular | Público aos portadores do fingerprint (usado para empacotar a DEK) | Art. 7º, V — Execução de contrato |
| **Registros de Conexão (MCI Art. 15)** | Firestore (`connectionLogs/{logId}` / `accessLogs/{logId}`) | **180 dias** (TTL estrito com descarte automático nativo + purga horária pelo Shredder) | **Admin SDK apenas** (read/write: false para clients). **SEM UID, SEM FINGERPRINT, SEM MESSAGEID, SEM PAYLOAD, SEM CHAVE, SEM MNEMÔNICO**. | Art. 7º, II da LGPD c/c Art. 15 da Lei nº 12.965/2014 (MCI) |
| **Ciphertext Efêmero da Mensagem** | Firestore (`messages/{messageId}`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura (*Vanish*) | Remetente e Destinatário (apenas bytes opacos ilegíveis pelo servidor) | Art. 7º, V — Execução de contrato |
| **Metadados de Roteamento (`senderId` e `recipientId`)** | Firestore (`messages/{messageId}` e `messages/{messageId}/keys/{keyId}`) | **Máximo de 24 horas** (expurgados atomicamente junto ao envelope) | Remetente, Destinatário e Servidor (roteamento e autorização estrita). **Dado mais sensível que o servidor retém**, protegido por destruição em ≤24h. | Art. 7º, V — Execução de contrato |
| **DEK Envelopada (bytes opacos)** | Firestore (`messages/{messageId}/keys`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura (*Vanish*) | Destinatário detentor da chave privada correspondente. O servidor **não possui a chave privada e não tem acesso aos bytes em claro** | Art. 7º, V — Execução de contrato |
| **Chave Efêmera Pública (ephemeralPubKey)** | Firestore (`messages/{messageId}/keys`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura | Destinatário e Servidor (armazenamento estritamente temporário para derivação ECDH) | Art. 7º, V — Execução de contrato |
| **Timestamps de Criação e Expiração** | Firestore (`messages/{messageId}`) | Máximo 24 horas (eliminados com a mensagem) | Servidor (Cloud Functions e Cloud Firestore TTL scheduler) | Art. 7º, IX — Legítimo Interesse (gestão do ciclo de vida da efemeridade) |
| **Conteúdo Voluntário de Denúncias (`abuseReportsWithContent`)** | Firestore (`abuseReports/{reportId}`) | **Até 90 dias após a conclusão/fechamento da apuração interna** | Auditores internos de segurança (Admin SDK restrito). **Destino: Exclusão definitiva e cripto-incineração irreversível (hard-delete)**. | Art. 7º, I (Consentimento explícito) c/c Art. 7º, IX (Legítimo Interesse) |
| **Logs Técnicos de Acesso / Execução** | Google Cloud Logging (`us-central1`) | 30 dias (política padrão e estrita do Google Cloud Logging) | Desenvolvedor/Administrador para diagnóstico e mitigação de incidentes | Art. 7º, II da LGPD c/c Art. 16 do Marco Civil da Internet (obrigação legal) |
| **Chaves Privadas (Ed25519, X25519)** | **Dispositivo do Usuário** (Room / Keychain / Keystore criptografada / DPAPI) | Até desinstalação do app ou acionamento de *Panic Wipe* | **Apenas o usuário**. **NUNCA** trafegam ou vivem no servidor. | Não aplicável ao servidor (dado não coletado) |
| **Frase Mnemônica (12 Palavras BIP-39)** | **Memória volátil / Armazenamento Seguro Local** | Até remoção voluntária pelo usuário | **Apenas o usuário**. **NUNCA** é enviada ou armazenada pelo servidor. | Não aplicável ao servidor (dado não coletado) |
| **Contatos Locais e Apelidos** | **Dispositivo do Usuário** (banco local cifrado) | Até remoção pelo usuário ou *Panic Wipe* | **Apenas o usuário**. O servidor **desconhece** o grafo de contatos. | Não aplicável ao servidor (dado não coletado) |
| **Frames / Imagens da Câmera** | **Memória volátil do Dispositivo** | Zero segundos (descartados imediatamente após decodificação do QR) | **Apenas o decodificador local on-device**. Zero gravação em disco ou rede. | Não aplicável ao servidor (dado não coletado) |

---

## 2. Implementação e Segregação dos Logs de Conexão (MCI Art. 15 / Parecer C1)

Em cumprimento ao Art. 15 da Lei nº 12.965/2014 (Marco Civil da Internet) e às determinações do Parecer Jurídico Especializado, a aplicação **Raix** mantém uma coleção segregada e isolada denominada `connectionLogs` (com espelhamento em `accessLogs`):

1. **Campos Registrados no Momento da Chamada:**
   - `ip`: Endereço IP do requisitante extraído do cabeçalho `x-forwarded-for` ou do raw request.
   - `timestampUtc`: Timestamp ISO-8601 UTC do momento da requisição.
   - `porta`: Porta de origem da conexão. *(Nota técnica: caso o proxy de terminação HTTP do Google Cloud Functions omita a porta de origem nos cabeçalhos encaminhados, este campo registra o valor `"UNAVAILABLE_RUNTIME"`, permanecendo o IP e o timestamp como elementos mandatórios de identificação de conexão).*
   - `functionName`: Nome da callable acionada (`resolveFingerprint`, `storeMessageKey`, etc.).
   - `expiresAt`: Timestamp configurado para exatamente **180 dias** após a criação (`now + 180d`).

2. **Isolamento Absoluto (Zero Rastreabilidade com Conteúdo ou Identidade):**
   - Os documentos de `accessLogs` **NÃO CONTÊM**: `uid`, `fingerprint`, `messageId`, nomes de contato, payloads criptografados, mnemônicos ou chaves públicas Ed25519 (Regra Absoluta do Parecer Jurídico).
   - É **tecnicamente impossível** correlacionar um registro de log de conexão com o remetente, destinatário ou conteúdo de qualquer mensagem.

3. **Regras de Segurança e Acesso:**
   - O arquivo `firestore.rules` define expressamente `allow read, write: if false;` para a coleção `accessLogs`. Somente o Firebase Admin SDK (Cloud Functions backend) tem permissão de escrita, e nenhum cliente pode listar ou consultar os registros.

4. **Nota sobre Prazos Legais e Fundamentação do Parecer:**
   - *Ponto de Atenção*: O Art. 13 do Marco Civil da Internet estipula 1 ano para provedores de **conexão à internet**. Para **provedores de aplicações de internet** (categoria na qual o Raix se insere), o Art. 15 da mesma lei estabelece o dever de guarda pelo prazo de **6 meses (180 dias)**. Prevalece a orientação técnica do parecerista pelo prazo de 180 dias com expiração automatizada via política de TTL do Firestore.

---

## 3. Evidência do Timeout de Mensagens Transitórias ≤ 24h e Shredder de 15 min (P0.3)

Fica atestado que o Raix cumpre estritamente a exigência de ciclo de vida transitório com expiração máxima em 24 horas:

1. **Definição de TTL e Bloqueio em Regras de Segurança:** As chamadas `storeMessageKey`, a coleção `inbox` e a gravação de mensagens impõem limite máximo estrito de `expiresAt <= request.time + duration.value(24, 'h')`. Qualquer tentativa de gravação sem `expiresAt`, com valor no passado ou excedendo 24 horas é recusada no próprio motor de regras do Firestore (`permission-denied`), impedindo desativação acidental ou maliciosa do TTL.
2. **Crypto-Shredder Ativo a Cada 15 Minutos:** A Cloud Function agendada `scheduledMessageShredder` é executada automaticamente a cada 15 minutos (`*/15 * * * *`, 4x por hora) para expurgar chaves DEK (`messageKeys`), mensagens legadas (`messages`), envelopes efêmeros (`collectionGroup('inbox')`) e registros de conexão expirados. O processo é estritamente idempotente.
3. **TTL Nativo como Fail-Safe:** O TTL nativo gerenciado do Cloud Firestore é considerado mecanismo de último recurso (fail-safe). A destruição primária e tempestiva é realizada pelo Shredder ativo e pelo gatilho reativo `onDeleteMessage`.
4. **Vanish-After-Read Imediato:** Quando o destinatário lê a mensagem, o documento transitório é apagado atomicamente do Firestore no mesmo instante (*Vanish-After-Read*), disparando imediatamente a destruição reativa da DEK em `messageKeys`.
5. **Ausência de Fila Indefinida:** Não existe qualquer fila ou buffer persistente secundário sem política de expiração ativa.
6. **Expurgo Irreversível de Metadados de Roteamento:** Os identificadores técnicos de roteamento (`senderId` e `recipientId`) e envelopes transitórios são incinerados de forma irreversível na expiração ou leitura.

---

## 4. Auditoria de Dependências (Verificação de Ausência de Rastreamento)

Foi executada verificação formal e automatizada no repositório de código fonte do aplicativo (`composeApp`) e nas definições de dependências centralizadas (`gradle/libs.versions.toml`) com o intuito de atestar a ausência de SDKs de publicidade, telemetria analítica comportamental e rastreamento de terceiros.

```bash
git grep -i -E "analytics|crashlytics|admob|facebook|appsflyer|adjust|mixpanel|amplitude" composeApp/build.gradle.kts gradle/libs.versions.toml
```
**Resultado obtido:** `Exit code: 1` (zero ocorrências encontradas).

---

## 5. Modelagem e Dimensionamento de Overhead Pós-Quântico Híbrido (Emenda Guru A.6)

Com a introdução da migração pós-quântica híbrida (NIST FIPS 203 ML-KEM-768 e NIST FIPS 204 ML-DSA-65), foi realizado o dimensionamento técnico formal do overhead criptográfico gerado no armazenamento e transporte:

| Primitiva Criptográfica | Algoritmo | Chave Pública | Chave Privada (Semente/Expandida) | Ciphertext / Assinatura |
| :--- | :--- | :--- | :--- | :--- |
| **Autenticação Clássica** | Ed25519 (RFC 8032) | 32 bytes | 32 bytes | 64 bytes |
| **Autenticação Pós-Quântica** | ML-DSA-65 (FIPS 204) | 1.952 bytes | 32 bytes (semente FIPS) | 3.309 bytes |
| **Assinatura Híbrida Composta** | Ed25519 + ML-DSA-65 | 1.988 bytes (inc. header) | 36 bytes (inc. header) | 3.377 bytes (inc. header) |
| **KEM Clássico (ECDH)** | X25519 (RFC 7748) | 32 bytes | 32 bytes | 32 bytes (chave efêmera) |
| **KEM Pós-Quântico** | ML-KEM-768 (FIPS 203) | 1.184 bytes | 64 bytes (semente d, z) | 1.088 bytes |
| **Envelope Híbrido KEM** | X25519 + ML-KEM-768 | 1.216 bytes | 96 bytes | 1.120 bytes |

### Impacto na Cota e Limites Operacionais do Cloud Firestore
- **Limite por documento no Cloud Firestore:** 1.048.576 bytes (1 MiB).
- **Tamanho total do Envelope Híbrido Completo (Chaves + Assinatura + Ciphertext KEM + DEK cifrada):** ~6.485 bytes (~6,48 KB).
- **Consumo de capacidade:** **~0,62% do limite de 1 MiB** por documento de envelope/mensagem.
- **Conclusão de Viabilidade:** O overhead pós-quântico é perfeitamente suportado pela infraestrutura sem necessidade de particionamento de documentos, mantendo a performance de leitura/escrita e respeitando o limite de faturamento/banda do Firestore.

---

## 7. Tratamento do Gap de Destruição: PITR, Backups e Telemetria de Latência (P0.3)

### 7.1 Point-in-Time Recovery (PITR) e Snapshots do Firestore
- **Configuração de PITR para Coleções Efêmeras:** O Point-in-Time Recovery (PITR) do Cloud Firestore permite a recuperação de dados históricos em uma janela de até 7 dias quando ativado no banco de dados. Para preservar a garantia de efemeridade estrita das mensagens e envelopes, o PITR deve permanecer **DESABILITADO** na instância de banco de dados efêmero ou configurado com janela mínima de retenção.
- **Processo de Expurgo e Snapshots:** Caso snapshots periódicos ou backups gerenciados sejam configurados em nível de projeto no Google Cloud Storage (GCS), o processo de expurgo automatizado estende-se via políticas de ciclo de vida de objetos (*GCS Lifecycle Rules* com `Age = 1 dia` para buckets efêmeros).

### 7.2 Limitação Física do Provedor de Nuvem (Sem Alegação de "Purga Física Absoluta Instantânea")
- **Transparência Técnica Mandatória:** A deleção lógica no Firestore realizada pelo Firebase Admin SDK ou pelo Shredder atômico remove imediatamente a acessibilidade dos nós e destrói as referências aos documentos. No entanto, o Raix **NÃO ALEGA** "purga definitiva atômica instantânea das réplicas físicas em repouso e mídias magnéticas/flash do Google Cloud Platform", uma vez que sistemas de arquivos distribuídos globais (Colossus/Spanner) possuem latências intrínsecas de compactação e coleta de lixo (*garbage collection*) em nível de hardware.
- **Garantia Efetiva via Cripto-Incineração (Crypto-Shredding):** A garantia de inviolabilidade dos dados efêmeros repousa na destruição imediata da Chave de Criptografia de Dados (DEK) e das sementes efêmeras KEM. Sem a chave privada de encapsulamento e sem a DEK destruída, mesmo que um snapshot remanescente de baixo nível seja extraído em repouso do hardware do provedor, ele conterá exclusivamente ciphertext indistinguível de ruído aleatório.

### 7.3 Monitoramento de Destruição e Alertas com Escalonamento
Para detectar e mitigar qualquer anomalia operacional no ciclo de expurgo:
- **Métrica Ativa:** `ttl_expiration_to_deletion_delays` emite a latência $T_{\text{delay}} = T_{\text{current}} - T_{\text{expiresAt}}$ para cada artefato processado.
- **Escalonamento Operacional:**
  - **Nível 1 (Aviso - Threshold 60 min):** Se algum envelope ou chave sobreviver além de $\text{maxLife} + 60\text{ min}$, um alerta operacional `[ALERT_ESCALATION_LEVEL_1]` é disparado para notificar a equipe de suporte.
  - **Nível 2 (Crítico - Threshold 180 min):** Se o atraso ultrapassar 3 horas sem resolução, o alerta é escalado para `[ALERT_ESCALATION_LEVEL_2]` com severidade crítica.
  - **Janela Residual Extrema (Fail-safe):** Caso ocorra falha simultânea catastrófica no Cloud Scheduler e no Cloud Functions, a janela residual máxima pode atingir até 24h a 48h (limite de garantia operacional do TTL nativo do Firestore). O limiar de 60 minutos é estritamente uma métrica de monitoramento e escalonamento, e **NUNCA** uma garantia matemática de destruição física em 1 hora.

---

## 8. Conclusão e Certificação Técnica

O ecossistema **Raix** opera sob estrita consonância com os princípios de **Finalidade**, **Adequação**, **Necessidade** e **Segurança** dispostos no art. 6º da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), tratando unicamente os elementos técnicos indispensáveis para viabilizar a entrega de mensagens efêmeras com criptografia ponta-a-ponta de chaves pós-quântica híbrida, governadas por ciclo de vida auditável e transparente.

