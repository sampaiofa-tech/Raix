# Governança de Cadeia de Suprimentos e Integridade de Software (SLSA 2+) — Raix

**Versão:** 1.0 (P0.4 — Supply-Chain & Integridade de Dependências)  
**Data:** 06 de setembro de 2026  
**Status:** Implementado, Coberto por Testes Automatizados e Atestado em CI  

---

## 1. Princípio Fundamental de Integridade (Emenda Guru B.1)

> [!CRITICAL]
> **Axioma de Confiança do Cliente e do Servidor:**  
> *Sem integridade de cliente rigorosamente verificável, a alegação técnica "comprometimento total do servidor entrega exclusivamente ciphertext" não se sustenta no mundo real.*

Se a cadeia de suprimentos for envenenada (*dependency confusion*, *typosquatting*, injeção de dependência maliciosa) ou o binário do cliente for adulterado durante a compilação ou em runtime (via *hooking*, injeção de memória Frida/Xposed ou agentes Java), o atacante obtém as chaves privadas de longo prazo e as Chaves de Encriptação de Dados (DEKs) decifradas diretamente no cliente, tornando a proteção do servidor e o KEM híbrido pós-quântico inócuos.

Para neutralizar esse vetor, o **Raix** implementa controles em quatro camadas:
1. **Fixação estrita de versões e verificação de hashes criptográficos dos artefatos**;
2. **Software Bill of Materials (SBOM) padronizado CycloneDX v1.5**;
3. **Builds reproduzíveis com atestados criptográficos de proveniência (SLSA Nível 2+)**;
4. **Detecção ativa de tampering e hooks no runtime do cliente**.

---

## 2. Pin de Versões e Verificação de Hashes Criptográficos

### 2.1 Ecossistema Kotlin Multiplatform / Gradle
- **Arquivo de Configuração:** [`gradle/verification-metadata.xml`](file:///c:/Dev/Pmsg/gradle/verification-metadata.xml)
- **Mecanismo:** *Gradle Dependency Verification*.
- **Padrão de Hashes:** SHA-256 obrigatório para dependências críticas de segurança (ex.: Bouncy Castle `bcprov-jdk18on` e `bcpkix-jdk18on` versão 1.79).
- **Comportamento em Ataques:** Se um atacante alterar um único byte de qualquer dependência ou se um repositório remoto for comprometido, o build é abortado imediatamente com código de erro não-zero (`Dependency verification failed`).

### 2.2 Ecossistema Node.js / Cloud Functions
- **Arquivo de Configuração:** [`functions/package-lock.json`](file:///c:/Dev/Pmsg/functions/package-lock.json)
- **Mecanismo:** `npm ci --ignore-scripts`.
- **Integridade Criptográfica:** Cada pacote tem seu hash `sha512` verificado contra o registro oficial no momento da instalação. Tentativas de injeção de scripts no pós-instalação são mitigadas com a flag `--ignore-scripts`.

---

## 3. Software Bill of Materials (SBOM CycloneDX v1.5)

O inventário de componentes de software é gerado de forma determinística e versionado diretamente no repositório:

- **Gerador Automatizado:** [`scripts/generate_sbom.cjs`](file:///c:/Dev/Pmsg/scripts/generate_sbom.cjs)
- **SBOM do Cliente Multiplatform:** [`docs/sbom/sbom-composeApp.json`](file:///c:/Dev/Pmsg/docs/sbom/sbom-composeApp.json)
  - Mapeia 72 componentes e bibliotecas oficiais (Kotlin, Compose, Bouncy Castle FIPS 203/204, Ktor, SQLite, etc.) com seus respectivos `PURL` (*Package Uniform Resource Locator*) e hashes.
- **SBOM do Backend Cloud Functions:** [`docs/sbom/sbom-functions.json`](file:///c:/Dev/Pmsg/docs/sbom/sbom-functions.json)
  - Mapeia o Firebase Admin SDK, Firebase Functions e dependências de runtime com integridade SHA-512.
- **Conformidade de Auditoria:** Arquivos JSON no padrão CycloneDX 1.5, prontos para ingestão em plataformas de gestão de vulnerabilidades (OWASP Dependency-Track, Trivy, Grype, Snyk).

---

## 4. Builds Reproduzíveis e Atestado SLSA Nível 2+

### 4.1 Esteira de CI/CD de Alta Confiança
- **Workflow:** [`.github/workflows/supply-chain-attestation.yml`](file:///c:/Dev/Pmsg/.github/workflows/supply-chain-attestation.yml)
- **Execução:** GitHub Actions em runners isolados oficiais (*Hosted Runners*).
- **Atestação Criptográfica:** Uso da action oficial `actions/attest-build-provenance@v2`, integrando com a autoridade de certificação Sigstore (Fulcio + Rekor transparência pública).
- **Garantias SLSA Level 2+:**
  1. **Build Platform As A Service:** O build é gerado em ambiente efêmero controlado sem acesso direto de desenvolvedores.
  2. **Identidade Imutável:** O atestado é assinado via OIDC (`id-token: write`) vinculado especificamente ao repositório `sampaiofa-tech/Raix` e commit SHA.
  3. **Não-Repúdio:** Qualquer usuário ou auditor pode verificar matematicamente que o binário entregue corresponde exatamente ao código-fonte inspecionado.

---

## 5. Detecção de Tampering no Runtime (`RuntimeIntegrityVerifier`)

O cliente Raix implementa uma camada multiplataforma em tempo de execução para inspecionar seu próprio ambiente e alertar o usuário ou abortar operações sensíveis caso detecte adulteração:

- **Contrato Multiplataforma:** [`RuntimeIntegrityVerifier.kt`](file:///c:/Dev/Pmsg/composeApp/src/commonMain/kotlin/com/example/security/integrity/RuntimeIntegrityVerifier.kt)
- **Implementação Desktop JVM:** [`RuntimeIntegrityVerifier.desktop.kt`](file:///c:/Dev/Pmsg/composeApp/src/desktopMain/kotlin/com/example/security/integrity/RuntimeIntegrityVerifier.desktop.kt)
  - Inspeção de argumentos de inicialização da JVM (`ManagementFactory.getRuntimeMXBean().inputArguments`);
  - Detecção de injeção de agentes Java (`-javaagent`);
  - Detecção de depuradores JDWP ativos (`-agentlib:jdwp`, `-Xrunjdwp`, `-Xdebug`);
  - Detecção de instrumentação de ClassLoader.
- **Implementação Android:** [`RuntimeIntegrityVerifier.android.kt`](file:///c:/Dev/Pmsg/composeApp/src/androidMain/kotlin/com/example/security/integrity/RuntimeIntegrityVerifier.android.kt)
  - Detecção de binários de Root (`su`, `Superuser.apk`);
  - Varredura de memória de processo em `/proc/self/maps` procurando assinaturas de frameworks de hooking dinâmico (`frida-agent`, `frida-gadget`, `xposed`);
  - Detecção de depurador ativo (`Debug.isDebuggerConnected()`) e flags `FLAG_DEBUGGABLE`.
- **Implementação Web (WasmJs) e iOS:** [`RuntimeIntegrityVerifier.wasmJs.kt`](file:///c:/Dev/Pmsg/composeApp/src/wasmJsMain/kotlin/com/example/security/integrity/RuntimeIntegrityVerifier.wasmJs.kt) e [`RuntimeIntegrityVerifier.ios.kt`](file:///c:/Dev/Pmsg/composeApp/src/iosMain/kotlin/com/example/security/integrity/RuntimeIntegrityVerifier.ios.kt) com avisos e auditorias específicas do ambiente.
