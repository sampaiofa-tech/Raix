# Raix v1.6.0 — Release Notes

**Data:** 06 de setembro de 2026  
**Tag:** `v1.6`  
**Commit:** `e544264`  
**URL da Tag no GitHub:** [https://github.com/sampaiofa-tech/Raix/releases/tag/v1.6](https://github.com/sampaiofa-tech/Raix/releases/tag/v1.6)  

---

## 🌟 Destaques da Versão v1.6.0

A versão **v1.6.0** consolida a transição do Raix para o ecossistema de produção com entrega de notificações push seguras, re-aceite legal v3.0 compulsório, integridade de cadeia de suprimentos e o fechamento integral das fases de endurecimento criptográfico pós-quântico (**P0**) e operacional (**P1**).

---

### 1. 🔔 Notificações Push Zero-Knowledge (Multiplataforma)
- **Android (FCM)**:
  - Integração nativa com Firebase Cloud Messaging (`firebase-messaging`).
  - Serviço em background (`RaixFirebaseMessagingService`) para recepção em repouso e exibição em canal prioritário (`new_conversations_channel`).
  - **Zero-Knowledge**: O payload remoto não transporta chaves, conteúdo ou nomes de interlocutores. Exibe estritamente aviso genérico *"Você tem uma nova mensagem efêmera no Raix"*.
- **Windows Desktop (WinRT Native Toast)**:
  - Notificações emitidas nativamente através do runtime do Windows (`[Windows.UI.Notifications.ToastNotificationManager]`).
  - **Zero Dependência Externa**: 100% autônomo, sem requerer servidor WNS ou credenciais da Microsoft.
- **iOS & Web (Stubs Declarativos)**:
  - Camada de abstração KMP (`PushNotificationManager.ios.kt`) pronta para ativação imediata com APNs assim que a conta Apple Developer for homologada.
  - Web Push (VAPID) mapeado para a versão v1.7.
- **Cloud Functions (`registerPushToken` & `storeMessageKey`)**:
  - Nova Callable Function `registerPushToken` associando o token do dispositivo ao hash da identidade autenticada.
  - Disparo assíncrono em `storeMessageKey` com TTL estritamente limitado ($\le 24$h), compatível com a expiração do envelope.

---

### 2. ⚖️ Rebuild Legal v3.0 Compulsório
- Re-aceite obrigatório dos Termos de Uso e Política de Privacidade v3.0 (`LegalConsentManager.CURRENT_LEGAL_VERSION = "3.0"`).
- Limpeza forçada de consentimentos legados no primeiro boot, exigindo verificação formal de maioridade legal (18+) e consentimento voluntário.

---

### 3. 🛡️ Fechamento do Roteiro de Segurança (P0 & P1)
- **P0.1 (Criptografia Pós-Quântica Híbrida)**: Assinaturas ML-DSA-65 (FIPS 204) + Ed25519; Acordo de chaves KEM híbrido ML-KEM-768 (FIPS 203) + X25519 com combiner NIST SP 800-227 / RFC 9180 HPKE; Safety Number dual de 60 dígitos.
- **P0.2 (Isolamento Adversarial do Firestore)**: Arquitetura *deny-by-default* na raiz (`match /{document=**} { allow read, write: if false; }`), anti-correlação de identidades, e 89 asserções adversariais automatizadas.
- **P0.3 (Destruição Ativa & Shredder 15min)**: Shredder tempestivo a cada 15 minutos (`scheduledMessageShredder`, 4x por hora), exclusão idempotente em lote, métricas de latência e PITR desabilitado no projeto Firebase (`POINT_IN_TIME_RECOVERY_DISABLED`).
- **P0.4 (Supply-Chain & Integridade)**: Validação SHA-256 no Gradle (`verification-metadata.xml`), SBOM CycloneDX v1.5 JSON e detecção de tampering em tempo de execução (`RuntimeIntegrityVerifier`).
- **P1.1 (Minimização de Metadados & Pseudonimização)**: Pseudonimização de IPs por HMAC-SHA256 rotativo mensal (Art. 15 MCI) sem gravação de IPs brutos em repouso. Modelo de Ameaça v1.3 com matriz exaustiva de adversários.
- **P1.2 (Custódia em Hardware & RAM Anti-DSE)**: Zeroização de memória com barreira volátil contra Dead-Store Elimination; armazenamento de sementes exclusivamente em KeyStore de hardware (TEE / StrongBox / DPAPI / Keychain).
- **P1.3 (Detecção Proativa de Anomalias)**: Alertas automáticos para picos anômalos de leitura de inbox (>20 req/min) e ataques de força bruta (>5 falhas de permissão/min).

---

### 4. 📦 Artefatos de Distribuição
- **Android APK v1.6.0**: `composeApp-release.apk` (`versionCode=6`, `versionName="1.6.0"`).
- **Android AAB v1.6.0**: `composeApp-release.aab` (Assinado com release keystore para Google Play Store).
- **Windows MSI v1.6.0**: `Raix-1.6.0.msi` (Instalador nativo Windows desktop x64).

---

## 🔒 Garantias Criptográficas e Governança
- Repositório auditado via GitGuardian CLI e CI/CD.
- Zero chaves de API, certificados privados, frases mnemônicas ou segredos comitados.
