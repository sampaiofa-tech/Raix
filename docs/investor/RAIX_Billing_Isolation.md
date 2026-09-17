# Isolamento Arquitetural: Faturamento vs. Mensageria Efêmera Raix

Este documento certifica a arquitetura de segregação de dados adotada pela Raix, atestando o isolamento formal entre o gateway de faturamento (dados cadastrais de clientes pagantes) e a infraestrutura de mensageria (identidades criptográficas anônimas).

## 1. Segregação de Bases e Regras de Acesso

O projeto Raix utiliza o Firebase Cloud Firestore para o tráfego da mensageria (IdentityHash, Envelopes, AccessLogs). A documentação oficial do ambiente de regras de segurança (`firestore.rules`) comprova que **não existem coleções ou estruturas de dados** dedicadas a pagamentos, assinaturas ou faturamento dentro da mesma base.

* **Firestore de Mensageria:** Armazena identidades geradas no dispositivo (`/identities/{identityHash}`) via chaves X25519 e AES-GCM. 
* **Gateway de Faturamento:** Sistemas de assinatura (ex: Stripe, Google Play Billing) operam em bancos de dados relacionais e gateways de terceiros **completamente separados** do Firebase Firestore do Raix. 

As regras do Firestore (v2) aplicam uma abordagem restrita (`allow read, write: if false;` na raiz) e **bloqueiam qualquer cruzamento de dados de faturamento** na infraestrutura de mensagens.

## 2. Ausência de Vínculo entre Cliente e Identidade

Para clientes de planos pagos ou institucionais:
1. O faturamento gera um identificador de cliente (Customer ID) vinculado aos dados de pagamento (Nome, Cartão, Endereço).
2. O aplicativo Raix gera um token de acesso para a licença, sem atrelar a identidade criptográfica (IdentityHash).
3. A identidade de mensageria (`IdentityHash` / Mnemônico) continua sendo um UUID local anônimo.
4. **Conclusão Técnica:** Se a infraestrutura de faturamento sofrer um vazamento ou for auditada judicialmente, ela revelará que "João da Silva pagou por uma licença", mas **não há qualquer ponte matemática ou arquitetural** que vincule a assinatura de João ao `IdentityHash XYZ` no Firestore.

## 3. Veredito de Arquitetura (Aprovado por Due Diligence)

O design atual garante que:
- O Gateway de Faturamento é Cego para o tráfego de mensagens e chaves criptográficas.
- O Firestore (Mensageria) é Cego para os dados do cartão de crédito, nome real e Customer ID.
- As identidades efêmeras do Raix preservam seu anonimato integral independentemente do status da assinatura.
