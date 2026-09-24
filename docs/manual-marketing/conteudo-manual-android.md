# Manual do Usuario — RAIX (Android)

Guia completo para uso do aplicativo RAIX no Android.

---

## 1. O que e o RAIX?

O RAIX e um mensageiro de privacidade forte por design. Suas mensagens sao criptografadas de ponta a ponta (E2E) antes de sair do seu dispositivo. O servidor funciona como um deposito cego de bits: armazena apenas dados cifrados que nao consegue ler, com retencao limitada de metadados.

O RAIX foi criado para quem precisa de comunicacao confidencial real: advogados, profissionais de saude, executivos, jornalistas e qualquer pessoa que valorize a privacidade digital.

---

## 2. Instalacao

1. Baixe o APK diretamente do site oficial (raixtech.com) ou receba o link de instalacao.
2. Toque no arquivo baixado e autorize a instalacao de fontes externas (se solicitado).
3. Abra o app e siga o onboarding (Secao 3).

> **Nota:** o RAIX ainda nao esta na Play Store. A distribuicao e feita por convite ou download direto.

---

## 3. Primeiro acesso (Onboarding)

O RAIX exige um fluxo de seguranca antes do primeiro uso:

1. **Confirmacao de idade:** voce deve confirmar que tem 18 anos ou mais.
2. **Frase de recuperacao (12 palavras):** o app gera 12 palavras aleatorias que sao a UNICA forma de recuperar sua identidade. Anote em papel e guarde em local seguro. O RAIX nunca armazena essas palavras.
3. **PIN de 4 digitos:** crie um PIN para proteger o acesso ao app no dia a dia.
4. **Biometria (opcional):** voce pode habilitar desbloqueio por impressao digital ou reconhecimento facial.
5. **Permissao de notificacoes:** autorize para receber alertas de novas mensagens.

Apos essas etapas, o app esta pronto.

---

## 4. Adicionando contatos

Para conversar com alguem no RAIX, ambos precisam trocar seus codigos de identidade:

1. Toque no menu (3 pontos) e selecione "Adicionar Contato".
2. Escaneie o QR Code do contato OU cole o URI de convite recebido.
3. O contato aparecera na lista principal.

> **Importante:** a troca de QR Code ou URI e a unica forma de adicionar contatos. O RAIX nao acessa sua agenda telefonica.

---

## 5. Enviando mensagens

1. Toque no contato na lista principal.
2. Digite sua mensagem no campo inferior e toque em Enviar.
3. A mensagem e criptografada no seu dispositivo antes de ser transmitida.

Todas as mensagens tem tempo de vida limitado (ate 24 horas). Apos o prazo, sao destruidas automaticamente em ambos os dispositivos e no servidor (crypto-shredding).

---

## 6. Notificacoes

O RAIX notifica quando uma nova mensagem criptografada chega, mesmo com o app em segundo plano. A notificacao mostra apenas "Nova mensagem criptografada" — o conteudo so aparece ao abrir o app.

Para que as notificacoes funcionem:
- Autorize a permissao de notificacoes no onboarding.
- Mantenha o app com permissao de execucao em segundo plano (configuracoes do Android > Apps > RAIX > Bateria > Sem restricoes).

---

## 7. Seguranca e criptografia

| Recurso | Descricao |
|---|---|
| Criptografia E2E | AES-256-GCM + X25519 (sealed-box). O servidor nunca ve o conteudo. |
| Pos-quantica hibrida | Pipeline ML-KEM-768 + X25519 integrado e pronto; ativacao no proximo ciclo apos auditoria externa. |
| Crypto-shredding | Chaves de mensagem destruidas apos expiracao (<=24h). |
| Vanish-after-read | Mensagens autodestrutivas apos leitura (quando habilitado). |
| Servidor | Deposito cego de bits. Armazena apenas ciphertext + chave envelopada. Metadados limitados. |

> **Nota sobre pos-quantica:** o RAIX ja possui o pipeline hibrido (classico + pos-quantico) integrado ao codigo. A ativacao em producao aguarda auditoria externa e publicacao de chaves pos-quanticas pelos contatos. Ate la, a comunicacao usa criptografia classica X25519, que e segura contra ameacas atuais.

---

## 8. Frase de recuperacao (12 palavras)

Sua frase de 12 palavras e a chave-mestra da sua identidade no RAIX. Com ela, voce pode restaurar sua conta em qualquer dispositivo.

**Regras criticas:**
- Anote em papel. Nunca salve em arquivo digital.
- Guarde em local seguro e privado.
- O RAIX nunca pede sua frase. Se alguem pedir, e golpe.
- Se perder a frase, perdera o acesso a sua identidade permanentemente.

Para visualizar sua frase: abra Identidade > toque em "Exibir frase de recuperacao" > autentique com biometria ou PIN.

---

## 9. Privacidade e dados

- O RAIX nao coleta seu numero de telefone, e-mail ou agenda.
- O servidor nao consegue ler suas mensagens (zero-knowledge de conteudo).
- Metadados sao limitados ao minimo necessario para entrega (remetente, destinatario, timestamp de expiracao).
- Nao ha rastreamento, analytics ou publicidade.
- Politica de Privacidade e Termos de Uso disponiveis em raixtech.com.

---

## 10. Perguntas frequentes

**P: Posso usar em mais de um dispositivo?**
R: Sim. Use sua frase de 12 palavras para restaurar a identidade em outro dispositivo (Android ou Windows).

**P: O que acontece se eu perder o celular?**
R: Suas mensagens expiram automaticamente. Com a frase de recuperacao, voce restaura sua identidade em um novo dispositivo.

**P: O RAIX funciona sem internet?**
R: Nao. E necessaria conexao com a internet para enviar e receber mensagens.

**P: As mensagens ficam salvas no servidor?**
R: Apenas temporariamente (ate 24h, cifradas). Apos a expiracao, sao destruidas junto com suas chaves de criptografia.

**P: O que e crypto-shredding?**
R: E a destruicao irreversivel das chaves de criptografia. Mesmo que alguem obtenha o ciphertext, sem a chave ele e ilegivel.

---

## 11. Suporte e contato

- Site: raixtech.com
- E-mail: contato@raixtech.com
- O RAIX opera sob licenciamento AGPL-3.0 + licenca comercial (open-core); o nucleo sera publicado. Codigo disponivel sob solicitacao para auditoria e due diligence.
