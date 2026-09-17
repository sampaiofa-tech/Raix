# Auditoria de SDKs (SBOM) e Permissões do Aplicativo Raix

Este relatório provê uma análise estática (SBOM - Software Bill of Materials) das dependências incorporadas aos binários de produção do Raix (Android e iOS), atestando a conformidade com as diretrizes de privacidade por design.

## 1. Ausência de SDKs Intrusivos e Publicidade
Uma auditoria direta nos manifestos de dependências da aplicação (`composeApp/build.gradle.kts` e equivalentes) atesta a **remoção deliberada e a ausência estrutural** de rastreadores comportamentais de terceiros.

* **NÃO POSSUI** SDKs de telemetria de anúncios (Google Analytics for Firebase, AppsFlyer, Facebook/Meta Pixel, Adjust).
* **NÃO POSSUI** Crashlytics agressivos vinculados a IP (a coleta é estritamente de crashes vitais, sem cruzamento de device fingerprint).
* **NÃO POSSUI** módulos de monetização baseados em dados (AdMob, Unity Ads).

As dependências incluídas se limitam a:
- UI e Layout (Jetpack Compose / Kotlin Multiplatform).
- Infraestrutura E2EE (Bibliotecas de Criptografia).
- Backend essencial (Firebase AppCheck e Firebase Cloud Messaging).

## 2. Minimização de Permissões no Device
Os manifestos do sistema operacional (`AndroidManifest.xml` e `Info.plist`) foram blindados para declarar apenas a superfície de acesso essencial, configurada como não obrigatória onde possível.

| Recurso | Permissão Solicitada | Natureza | Justificativa |
|---------|----------------------|----------|---------------|
| **Câmera** | `android.hardware.camera` (required=false) | Opcional | Utilizada apenas in-memory para captura de QR Code (troca de chaves públicas). A imagem **nunca** é salva em disco ou enviada à rede. |
| **Biometria** | `USE_BIOMETRIC` | Opcional | Utilizada apenas para proteger a abertura local do app (App Lock). A validação biométrica ocorre no Secure Enclave, o app recebe apenas o callback de sucesso. |
| **Internet** | `INTERNET` | Essencial | Comunicação com o Firebase (envio e recebimento de envelopes). |
| **Push** | `POST_NOTIFICATIONS` | Opcional | Recepção cega de alertas de novas mensagens (payloads não contêm preview da mensagem). |

## 3. Permissões Rejeitadas por Design
O Raix proíbe expressamente e **não requisita** as seguintes permissões em nenhuma de suas builds:
- Acesso à Agenda de Contatos do Celular (`READ_CONTACTS`).
- Acesso à Localização (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`).
- Leitura do IMEI, MAC ou Advertising ID (`READ_PHONE_STATE`).
- Acesso irrestrito a fotos e mídia (`READ_EXTERNAL_STORAGE`).

## 4. Veredito Técnico
A análise estática do SBOM conclui que o aplicativo Raix atua sob um sandbox rigoroso. Ele não possui os motores necessários para realizar "fingerprinting" ou criar dossiês comportamentais silenciosos. Toda a operação está confinada aos canais estritamente necessários para viabilizar a mensageria P2P efêmera.
