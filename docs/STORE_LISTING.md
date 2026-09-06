# Especificações de Listagem de Loja — Raix (Google Play Store & Desktop)

**Versão:** 1.6  
**Data:** 06 de setembro de 2026  
**Entidade Publicadora:** Cat Tech (CNPJ: 67.497.085/0001-36)  
**Registro de Marca INPI:** Processo nº 945109300 (Classes 09 e 42)  
**Canal Oficial:** `https://raixtech.com`  

---

## 📱 1. Metadados para Google Play Store

### 1.1 Título e Textos Promocionais
- **Nome do Aplicativo (App Title - máx. 30 caracteres):**  
  `Raix — Mensagens Efêmeras` (26 caracteres)

- **Descrição Curta (Short Description - máx. 80 caracteres):**  
  `Mensagens efêmeras ultrasseguras com privacidade forte por design e E2EE.` (73 caracteres)

- **Descrição Completa (Full Description - máx. 4000 caracteres):**
```text
O Raix é um mensageiro efêmero multiplataforma desenvolvido sob o princípio da privacidade forte por design, com retenção estritamente limitada de metadados e destruição tempestiva de dados.

🛡️ PILARES DE SEGURANÇA E CRIPTOGRAFIA

• CRIPTOGRAFIA PÓS-QUÂNTICA HÍBRIDA (NIST FIPS 203 & 204):
Proteção contra ataques de gravação em massa para decifração futura (Harvest-Now-Decrypt-Later). Combina encapsulamento pós-quântico ML-KEM-768 com curvas elípticas clássicas X25519 e assinaturas digitais ML-DSA-65 + Ed25519.

• ISOLAMENTO DE HARDWARE (TEE / STRONGBOX):
Chaves de identidade e envelopes criptográficos protegidos pelo Android KeyStore com isolamento físico de hardware. Sem chaves em texto plano gravadas no disco.

• EFEMERIDADE RADICAL E SHREDDING ATIVO (TTL ≤ 24h):
Todas as mensagens transitam com tempo de vida máximo de 24 horas (ou temporizadores menores: 30s, 1m, 5m, 1h, 6h, 12h, 24h). Destruição com sobrescrita criptográfica (multi-pass shredding) e descarte imediato após a leitura (Vanish-After-Read).

• ZERO-KNOWLEDGE E SERVIDOR CEGO:
O servidor de trânsito em nuvem enxerga unicamente bytes opacos cifrados (SealedBox). O operador não possui meios matemáticos de decifrar o conteúdo, ler conversas ou recuperar mensagens incineradas.

• NOTIFICAÇÕES PUSH EM SEGUNDO PLANO (v1.6):
Notificações imediatas de mensagens em espera via Firebase Cloud Messaging (FCM) respeitando estritamente o princípio zero-knowledge: nenhum dado, remetente ou texto trafega na notificação.

• NÚMERO DE SEGURANÇA COMBINADO (60 DÍGITOS):
Validação visual ou presencial de autenticidade (estilo Signal) cobrindo simultaneamente as chaves clássicas e pós-quânticas dos interlocutores.

• LIMPEZA DE PÂNICO (SHAKE-TO-CLEAR):
Chacoalhe o aparelho para acionar a destruição instantânea da conversa aberta, limpando o banco local sem deixar vestígios.

• INTEGRIDADE EM TEMPO DE EXECUÇÃO:
Auditoria ativa de runtime contra ferramentas de hooking dinâmico (Frida, Xposed), binários de root e depuradores.

---
INFORMAÇÕES LEGAIS E CONFORMIDADE:
• Classificação Etária: Estritamente para maiores de 18 anos (18+) com verificação formal no primeiro acesso.
• Sem Coleta de Dados Cadastrais (Zero PII): Não solicitamos nome civil, e-mail, telefone ou documentos.
• Em conformidade com a LGPD (Lei nº 13.709/2018) e Marco Civil da Internet (Lei nº 12.965/2014).
• Política de Privacidade: https://raixtech.com/privacidade.html
• Termos de Uso: https://raixtech.com/termos.html
```

---

## 🎨 2. Diretrizes de Assets Visuais e Screenshots

### 2.1 Ícones Oficiais
- **Ícone de Alta Resolução:** 512x512 px, 32-bit PNG, canal alfa transparente (`composeApp/src/desktopMain/resources/icon.png`).
- **Gráfico de Recursos (Feature Graphic):** 1024x500 px, JPG ou PNG de 24 bits (sem canal alfa), tema escuro imersivo com logo Raix e destaque *"Privacidade Forte por Design"*.

### 2.2 Especificações de Screenshots (Telas NÃO-Sensíveis)
Para cumprir as diretrizes do Google Play e preservar a privacidade:
1. **Screenshot 1 — Boas-Vindas & Age-Gate (18+):** Tela de consentimento formal e verificação de maioridade legal v3.0.
2. **Screenshot 2 — Contatos & Segurança:** Lista de contatos verificados com selo de integridade criptográfica.
3. **Screenshot 3 — Troca Presencial via QR Code:** Apresentação do QR Code de Modelo A com fingerprint criptográfico.
4. **Screenshot 4 — Configurações de Efemeridade:** Seleção do temporizador de destruição (30s a 24h) e toggle Shake-to-Clear.
5. **Screenshot 5 — Número de Segurança Dual (60 Dígitos):** Tela de conferência de Safety Number Signal-compatible pós-quântico.
