# Governança de DNS, Autenticação de E-mail (SPF/DKIM/DMARC), DNSSEC e HSTS — Raix

Este documento estabelece o estado autoritativo, a auditoria perimetral e as instruções operacionais de segurança para o domínio institucional **`raixtech.com`**.

---

## 1. Autenticação de E-mail contra Phishing e Spoofing (P2)

O canal institucional de governança e segurança do Raix opera sob o endereço **`contato@raixtech.com`** hospedado via Google Workspace com DNS gerenciado na Cloudflare.

### 1.1 SPF (Sender Policy Framework)
- **Status:** ✅ **ATIVO e VERIFICADO**
- **Registro DNS:**
  - **Tipo:** `TXT`
  - **Nome:** `@` (`raixtech.com`)
  - **Valor:** `v=spf1 include:_spf.google.com ~all`
  - **TTL:** 300 (ou Automático)
- **Função:** Autoriza exclusivamente os servidores MX do Google a disparar e-mails autenticados em nome de `@raixtech.com`.

### 1.2 DKIM (DomainKeys Identified Mail)
- **Status:** ✅ **ATIVO e VERIFICADO**
- **Registro DNS:**
  - **Tipo:** `TXT`
  - **Nome:** `google._domainkey` (`google._domainkey.raixtech.com`)
  - **Valor:** `v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw01N5LFv2/PtbCEBQiNcGjUfM+x0XD2k09yVaEf+glqJ37xbPYftioxRASJqvl9L0OylM6zGAmk+mslMS30hQ/U25yGhJd92aNmCQIxByIMxo3k7zbr9B8tcusMM+E42+lQGnScZT8waPDb+Qq9Ym1E+jup2mQFd54W3nKY8QaC74RQAAdSPvDoKuOx/gd/Bp, RlNskd83Fi73Rg49zDRjPaEY47XhWQ97zfSaA++1BHfqY7EpIv5JY/4xRzRSuQg4iea+e2/fh+nBhlHFhWovA3s567KyX/q/6xUOJjpKxzz4uH4eC2qeDW2Tb/5c6OWr49rgeG7UlHwAoq/q+1m4QIDAQAB`
- **Função:** Assinatura criptográfica RSA de 2048 bits que garante a integridade de corpo e cabeçalhos contra adulteração em trânsito.

### 1.3 DMARC (Domain-based Message Authentication)
- **Status:** ✅ **ATIVO e VERIFICADO**
- **Registro DNS:**
  - **Tipo:** `TXT`
  - **Nome:** `_dmarc` (`_dmarc.raixtech.com`)
  - **Valor:** `v=DMARC1; p=none; rua=mailto:contato@raixtech.com; pct=100`
- **Evolução de Governança:**
  - Fase atual: `p=none` (monitoramento e telemetria de entregabilidade sem descarte).
  - Fase recomendada pós-estabilização: alterar `p=none` para `p=quarantine` ou `p=reject` para bloqueio automatizado de mensagens forjadas.

---

## 2. Auditoria de CNAME e Prevenção de Subdomain Takeover

- **Status:** ✅ **CORRIGIDO E VERIFICADO**
- O apontamento CNAME de `www.raixtech.com` foi validado como `sampaiofa-tech.github.io.`, eliminando o risco anterior de erro tipográfico e prevenindo qualquer tentativa de subdomain takeover.

### Matriz de Apontamentos DNS Atual vs. Hardening Pós-Reunião:
| Tipo | Nome | Conteúdo | Status Atual (Pré-Reunião) | Pós-Reunião (Hardening) | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CNAME** | `www` | `sampaiofa-tech.github.io` | **DNS Only (Cinza)** | **Proxied (Laranja)** | Redirecionamento canônico |
| **A** | `@` | `185.199.108.153` | **DNS Only (Cinza)** | **Proxied (Laranja)** | Apex para GitHub Pages |
| **A** | `@` | `185.199.109.153` | **DNS Only (Cinza)** | **Proxied (Laranja)** | Apex para GitHub Pages |
| **A** | `@` | `185.199.110.153` | **DNS Only (Cinza)** | **Proxied (Laranja)** | Apex para GitHub Pages |
| **A** | `@` | `185.199.111.153` | **DNS Only (Cinza)** | **Proxied (Laranja)** | Apex para GitHub Pages |

---

## 3. DNSSEC (Domain Name System Security Extensions)

- **Status:** ✅ **ATIVO e PROPAGADO**
- **Verificação:** Registro `DS` (Delegation Signer) confirmado na zona raiz `.com`:
  - **Key Tag:** `2371`
  - **Algorithm:** `13` (ECDSA Curve P-256 with SHA-256)
  - **Digest Type:** `2` (SHA-256)
  - **Digest:** `134DCCC4A64F6B154468238852A620B154351813E95324EFC0FD9C2ACFA2E089`

---

## 4. HSTS (HTTP Strict Transport Security) — Política de Transporte (P3)

Para blindar o tráfego HTTP contra ataques de *SSL Stripping*, *downgrade* de cifras e interceptação man-in-the-middle:

### 4.1 Configuração Mandatória na Borda Cloudflare:
No painel Cloudflare em **SSL/TLS ➔ Edge Certificates ➔ HTTP Strict Transport Security (HSTS)**:
- **Enable HSTS:** `On`
- **Max-Age Header:** `12 months (31536000 seconds)`
- **Apply HSTS to subdomains (includeSubDomains):** `On`
- **Preload:** `On`
- **No-Sniff Header (`X-Content-Type-Options: nosniff`):** `On`

### 4.2 Cabeçalho Transmitido:
```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
```

---

## 5. Decisão de Borda: Proxy Cloudflare, WAF e Super Bot Fight Mode (Hardening Pós-Reunião)

### 5.1 Decisão do Assessor Técnico
- **Postura Pré-Reunião (Atual)**: O proxy da Cloudflare (nuvem laranja) **permanece desativado** (modo DNS-Only / Cinza).
  - *Fundamentação Técnica*: O GitHub Pages requer resolução DNS direta para emissão e validação inicial ininterrupta de certificados TLS Let's Encrypt. A ativação precipitada do proxy antes da homologação de certificados de origem poderia gerar erro `520/525 (SSL Handshake Failed)` na véspera da reunião. Ademais, a superfície de ataque em páginas puramente estáticas é mínima.
- **Linguagem Honesta no Data Room e README**:
  > *"Domínio com HTTPS + security.txt + DMARC + DNSSEC ativos; WAF e Super Bot Fight Mode CONFIGURADOS, ativos quando o proxy for habilitado (hardening pós-reunião)."*
  > É vedado afirmar que o WAF está ativo em tempo real enquanto o domínio operar em modo DNS-only.

### 5.2 Roteiro de Ativação Pós-Reunião
1. No painel Cloudflare em **SSL/TLS ➔ Origin Server**, gerar o certificado de origem Cloudflare.
2. Configurar modo SSL como **Full (Strict)**.
3. Alternar os registros `A` e `CNAME` de DNS-Only para **Proxied (Nuvem Laranja)**.
4. Ativar as regras do **WAF Managed Rules** e o **Super Bot Fight Mode** para inspeção e bloqueio perimetral de tráfego HTTP.
