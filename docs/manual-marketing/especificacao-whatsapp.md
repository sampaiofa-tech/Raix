# Especificacao de Distribuicao via WhatsApp — RAIX

## Objetivo

Definir o formato, fluxo e conteudo da distribuicao do manual do usuario e material de marketing do RAIX via WhatsApp (envio direto, listas de transmissao e status).

---

## Formato de distribuicao

### Opcao 1: Carrossel de imagens (recomendado)

- **5-8 cards** por plataforma (Android / Desktop)
- **Formato:** 1080x1080px (quadrado) para envio direto; 1080x1920px (stories) para status
- **Tipo:** PNG com compressao alta (WhatsApp comprime; otimizar antes do envio)
- **Conteudo por card:**

| Card | Conteudo |
|---|---|
| 1 | Capa: logo RAIX + "Privacidade forte por design" + CTA |
| 2 | O que e o RAIX (resumo visual) |
| 3 | Como instalar (passo a passo visual) |
| 4 | Frase de recuperacao (importancia, regras) |
| 5 | Seguranca e criptografia (tabela visual) |
| 6 | Como adicionar contatos (QR Code / URI) |
| 7 | Mensagens efemeras (crypto-shredding visual) |
| 8 | CTA final: "Baixe em raixtech.com" + QR do site |

### Opcao 2: PDF compacto

- **1 arquivo PDF** (manual completo, 12-16 paginas)
- **Limite WhatsApp:** 100 MB (PDF ficara bem abaixo)
- **Vantagem:** conteudo completo em um unico envio
- **Desvantagem:** menos engajamento visual que o carrossel

### Opcao 3: Link + preview

- **Mensagem de texto** com link para raixtech.com/manual
- **Vantagem:** sempre atualizado, sem limite de formato
- **Desvantagem:** depende de acesso a internet; preview depende de meta tags OG

---

## Fluxo de distribuicao

### Envio direto (1:1)

```
[Mensagem de texto]
RAIX — Mensageiro de Privacidade Forte

Manual do usuario: veja como proteger suas conversas
com criptografia de ponta a ponta.

Baixe: raixtech.com

[Anexo: carrossel de cards OU PDF]
```

### Lista de transmissao

- **Maximo:** 256 contatos por lista (limite WhatsApp)
- **Frequencia:** 1 envio por lancamento de versao ou material novo
- **Conteudo:** mensagem de texto + carrossel OU PDF
- **Regra:** so enviar para contatos que autorizaram (LGPD)

### Status do WhatsApp

- **Formato:** 1080x1920px (stories verticais)
- **Duracao:** 24 horas (efemero, alinhado com a filosofia RAIX)
- **Conteudo:** 3-5 cards resumidos (capa, seguranca, CTA)

---

## Texto padrao (copiar e colar)

### Versao curta (WhatsApp)

```
RAIX — Privacidade forte por design

Suas mensagens protegidas com criptografia de ponta a ponta.
Mensagens efemeras. Servidor deposito cego. Sem rastreamento.

Disponivel para Android e Windows.
Baixe: raixtech.com
```

### Versao longa (e-mail ou site)

```
RAIX e um mensageiro de privacidade forte por design.

Criptografia E2E (AES-256-GCM + X25519), mensagens efemeras
com crypto-shredding, servidor deposito cego de bits,
sem coleta de telefone ou agenda, sem analytics.

Pipeline pos-quantico hibrido (ML-KEM-768) integrado e pronto
para ativacao no proximo ciclo apos auditoria externa.

Disponivel para Android e Windows.
Codigo-fonte aberto (AGPL-3.0): github.com/sampaiofa-tech/Raix
Baixe: raixtech.com
Contato: contato@raixtech.com
```

---

## Restricoes de conteudo

- **Nenhum overclaim:** nao usar "100% seguro", "inquebravel", "a prova de hackers".
- **Linguagem aprovada:** "privacidade forte por design", "criptografia de ponta a ponta", "servidor deposito cego", "metadados limitados".
- **Pos-quantica:** "pipeline integrado e pronto; ativacao no proximo ciclo apos auditoria externa".
- **Sem emojis** em nenhum material.
- **LGPD:** enviar apenas para contatos que autorizaram; incluir opt-out.

---

## Metricas de acompanhamento

| Metrica | Como medir |
|---|---|
| Envios | Contagem manual (lista de transmissao) |
| Respostas / interesse | Mensagens recebidas apos envio |
| Downloads | Analytics do site (raixtech.com) |
| Instalacoes | Firebase Analytics (anonimizado) |
