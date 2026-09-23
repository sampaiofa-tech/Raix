# Manual do Usuario — RAIX (Desktop / Windows)

Guia completo para uso do aplicativo RAIX no Windows.

---

## 1. O que e o RAIX?

O RAIX e um mensageiro de privacidade forte por design. Suas mensagens sao criptografadas de ponta a ponta (E2E) antes de sair do seu dispositivo. O servidor funciona como um deposito cego de bits: armazena apenas dados cifrados que nao consegue ler, com retencao limitada de metadados.

O RAIX foi criado para quem precisa de comunicacao confidencial real: advogados, profissionais de saude, executivos, jornalistas e qualquer pessoa que valorize a privacidade digital.

---

## 2. Instalacao

1. Baixe o instalador MSI diretamente do site oficial (raixtech.com) ou receba o link.
2. Execute o instalador e siga as instrucoes padrao do Windows.
3. Abra o RAIX pelo atalho criado na area de trabalho ou no menu Iniciar.

> **Nota:** o Windows pode exibir um aviso de "editor desconhecido". Clique em "Mais informacoes" e depois "Executar assim mesmo". O RAIX e software livre e o codigo-fonte e auditavel.

---

## 3. Primeiro acesso (Onboarding)

O fluxo de seguranca no Desktop e identico ao Android:

1. **Confirmacao de idade:** confirme que tem 18 anos ou mais.
2. **Frase de recuperacao (12 palavras):** gerada automaticamente. Anote em papel e guarde em local seguro.
3. **PIN de 4 digitos:** crie um PIN para proteger o acesso.
4. **Biometria:** nao disponivel no Desktop. O acesso e protegido pelo PIN.

Apos essas etapas, o app esta pronto.

> **Restauracao:** se voce ja usa o RAIX no Android, pode restaurar sua identidade no Desktop usando a mesma frase de 12 palavras.

---

## 4. Adicionando contatos

1. Clique no menu (3 pontos) e selecione "Adicionar Contato".
2. Cole o URI de convite recebido do contato.
3. O contato aparecera na lista do painel lateral.

> **Nota Desktop:** no Windows, a leitura de QR Code por camera nao esta disponivel. Use o URI de convite (texto) para adicionar contatos. Voce pode gerar seu URI em Identidade > "Copiar URI de contato".

---

## 5. Enviando mensagens

1. Selecione o contato no painel lateral esquerdo.
2. A conversa abre no painel central.
3. Digite sua mensagem e pressione Enter ou clique em Enviar.

O layout do Desktop usa 3 colunas:
- **Rail lateral:** navegacao principal
- **Lista de contatos:** painel esquerdo
- **Conversa:** painel central

Todas as mensagens tem tempo de vida limitado (ate 24 horas) e sao destruidas automaticamente apos expiracao.

---

## 6. Notificacoes

No Desktop, o RAIX exibe uma mini-janela de notificacao quando uma nova mensagem chega, mesmo com o app minimizado. O conteudo da notificacao mostra apenas "Nova mensagem criptografada".

O Desktop usa polling (verificacao periodica) para buscar novas mensagens. Nao depende de push (FCM).

---

## 7. Seguranca e criptografia

| Recurso | Descricao |
|---|---|
| Criptografia E2E | AES-256-GCM + X25519 (sealed-box). O servidor nunca ve o conteudo. |
| Pos-quantica hibrida | Pipeline ML-KEM-768 + X25519 integrado e pronto; ativacao no proximo ciclo apos auditoria externa. |
| Crypto-shredding | Chaves de mensagem destruidas apos expiracao (<=24h). |
| Vanish-after-read | Mensagens autodestrutivas apos leitura (quando habilitado). |
| Servidor | Deposito cego de bits. Armazena apenas ciphertext + chave envelopada. Metadados limitados. |
| Protecao local | Chaves protegidas por DPAPI (Windows). PIN de acesso ao app. |

> **Nota sobre pos-quantica:** o RAIX ja possui o pipeline hibrido (classico + pos-quantico) integrado ao codigo. A ativacao em producao aguarda auditoria externa e publicacao de chaves pos-quanticas pelos contatos. Ate la, a comunicacao usa criptografia classica X25519, que e segura contra ameacas atuais.

---

## 8. Frase de recuperacao (12 palavras)

Sua frase de 12 palavras e a chave-mestra da sua identidade no RAIX. Com ela, voce pode restaurar sua conta em qualquer dispositivo (Android ou Windows).

**Regras criticas:**
- Anote em papel. Nunca salve em arquivo digital.
- Guarde em local seguro e privado.
- O RAIX nunca pede sua frase. Se alguem pedir, e golpe.
- Se perder a frase, perdera o acesso a sua identidade permanentemente.

Para visualizar sua frase: abra Identidade > "Exibir frase de recuperacao" > autentique com PIN.

---

## 9. Privacidade e dados

- O RAIX nao coleta seu numero de telefone, e-mail ou agenda.
- O servidor nao consegue ler suas mensagens (zero-knowledge de conteudo).
- Metadados sao limitados ao minimo necessario para entrega (remetente, destinatario, timestamp de expiracao).
- Nao ha rastreamento, analytics ou publicidade.
- Politica de Privacidade e Termos de Uso disponiveis em raixtech.com.

---

## 10. Perguntas frequentes

**P: Posso usar no Android e no Windows ao mesmo tempo?**
R: Sim. Use a mesma frase de 12 palavras em ambos os dispositivos para manter a mesma identidade.

**P: As mensagens sao sincronizadas entre dispositivos?**
R: As mensagens sao efemeras e nao ficam armazenadas permanentemente. Cada dispositivo busca as mensagens pendentes no servidor de forma independente.

**P: O que acontece quando fecho o app no Desktop?**
R: O app e encerrado. Mensagens recebidas enquanto o app estiver fechado serao entregues na proxima vez que voce abrir.

**P: As mensagens ficam salvas no servidor?**
R: Apenas temporariamente (ate 24h, cifradas). Apos a expiracao, sao destruidas junto com suas chaves de criptografia.

**P: O que e crypto-shredding?**
R: E a destruicao irreversivel das chaves de criptografia. Mesmo que alguem obtenha o ciphertext, sem a chave ele e ilegivel.

---

## 11. Suporte e contato

- Site: raixtech.com
- E-mail: contato@raixtech.com
- O RAIX e software livre (AGPL-3.0). Codigo-fonte disponivel em github.com/sampaiofa-tech/Raix.
