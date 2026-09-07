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

> [!CAUTION]
> **ALERTA CRÍTICO DE SUBDOMAIN TAKEOVER DETECTADO NA AUDITORIA**:
> O registro CNAME de `www.raixtech.com` foi configurado na Cloudflare com erro de digitação apontando para:
> `ampaiofa-tech.github.io` (faltou a letra **`s`** inicial!).
> **Ação Imediata Necessária no Painel Cloudflare DNS**:
> Corrigir o CNAME do subdomínio `www` para:
> `sampaiofa-tech.github.io`

### Matriz de Apontamentos DNS Recomendada para GitHub Pages na Cloudflare:
| Tipo | Nome | Conteúdo | Proxy Status | Finalidade |
| :--- | :--- | :--- | :--- | :--- |
| **CNAME** | `www` | `sampaiofa-tech.github.io` | Proxied (Laranja) | Redirecionamento canônico |
| **A** | `@` | `185.199.108.153` | Proxied (Laranja) | Apex para GitHub Pages |
| **A** | `@` | `185.199.109.153` | Proxied (Laranja) | Apex para GitHub Pages |
| **A** | `@` | `185.199.110.153` | Proxied (Laranja) | Apex para GitHub Pages |
| **A** | `@` | `185.199.111.153` | Proxied (Laranja) | Apex para GitHub Pages |

---

## 3. DNSSEC (Domain Name System Security Extensions)

- **Diagnóstico:** A Cloudflare oferece suporte nativo a DNSSEC, porém o registro **DS (Delegation Signer)** ainda não foi publicado na zona pai `.com`.
- **Procedimento para Ativação:**
  1. No painel da Cloudflare em `DNS ➔ Settings ➔ DNSSEC`, clicar em **Enable DNSSEC**.
  2. A Cloudflare fornecerá os parâmetros:
     - **Key Tag:** (ex: 2371)
     - **Algorithm:** 13 (ECDSA Curve P-256 with SHA-256)
     - **Digest Type:** 2 (SHA-256)
     - **Digest:** (hash hexadecimal fornecido)
  3. No registrador do domínio (Squarespace Domains / Google Domains), acessar as configurações avançadas de DNS e adicionar o registro DS com esses parâmetros.

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
