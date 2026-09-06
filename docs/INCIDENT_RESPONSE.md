# Runbook de Resposta a Incidentes de Exposição de Segredos — Raix

> **DOCUMENTAÇÃO DE SEGURANÇA E OPERAÇÕES**  
> **Responsável / Operador:** Filippe Andrade Sampaio (`contato@raixtech.com`)  
> **Repositório:** `sampaiofa-tech/Raix` (Público — AGPL-3.0)  
> **Canais de Alerta:** GitGuardian Dashboard + E-mail (`contato@raixtech.com`)  
> **Diretrizes Base:** [`AGENTS.md`](../AGENTS.md) e [`scripts/verify_secrets.cjs`](../scripts/verify_secrets.cjs)

---

## 1. Visão Geral e Arquitetura de Defesa em Profundidade

O repositório **Raix** é público por definição arquitetural (AGPL-3.0). Em virtude de sua visibilidade global, a segurança de credenciais é baseada no princípio de **tolerância zero para vazamentos** e sustentada por uma estratégia de **Defesa em Profundidade** dividida em 3 barreiras automatizadas:

```
[Desenvolvedor / Agente]
           │
           ▼ (git commit)
┌────────────────────────────────────────┐
│ BARREIRA 1: Pré-Commit Local           │
│ - scripts/verify_secrets.cjs           │
│ - ggshield secret scan pre-commit      │
└──────────────────┬─────────────────────┘
                   │ Passa
                   ▼ (git push / PR)
┌────────────────────────────────────────┐
│ BARREIRA 2: CI/CD GitHub Actions       │
│ - .github/workflows/gitguardian.yml    │
│ - Quebra imediata do build (Fail-Fast) │
└──────────────────┬─────────────────────┘
                   │ Passa
                   ▼ (Main Branch / Histórico)
┌────────────────────────────────────────┐
│ BARREIRA 3: Monitoramento Contínuo     │
│ - GitGuardian Public Repo Monitoring   │
│ - Alerta imediato a contato@raixtech.com│
└────────────────────────────────────────┘
```

---

## 2. Prazos de Resposta e Níveis de Severidade

| Severidade | Descrição / Exemplos de Credenciais | SLA de Contenção | Ação Mandatória |
|:---:|---|:---:|---|
| **P0 (Crítico)** | Chave privada (`BEGIN PRIVATE KEY`), `GEMINI_API_KEY`, Token GitHub (`ghp_`, `github_pat_`, `gho_`), Chave de Serviço Firebase/GCP (`client_email`). | **Imediato (< 1 hora)** | Revogação instantânea no provedor de origem, rotação completa, auditoria de logs de acesso e purga de histórico. |
| **P1 (Alto)** | Tokens de serviços auxiliares com escopo limitado de leitura, webhooks ou credenciais de ambientes de staging/teste. | **< 4 horas** | Revogação, rotação e investigação da causa-raiz. |
| **P2 (Médio)** | Falsos positivos gerados por hashes aleatórios, IDs de teste ou dados sintéticos sem privilégios reais. | **< 24 horas** | Análise e registro de exceção / ignore via `.gitguardian.yaml` ou dashboard com justificativa técnica formal. |

---

## 3. Protocolo de Ação Imediata (Passo a Passo)

Ao receber notificação de exposição (via e-mail em `contato@raixtech.com`, alerta no GitGuardian Dashboard ou falha no CI):

```
                     [Alerta de Incidente Recebido]
                                    │
                                    ▼
                         Passo 1: Triagem Imediata
                       (Identificar segredo e escopo)
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
          [Falso Positivo]                     [Segredo Real]
                  │                                   │
       Marcar no Dashboard com                        ▼
         justificativa técnica              Passo 2: Revogação Imediata
                                             (Invalidação no Provedor)
                                                      │
                                                      ▼
                                            Passo 3: Rotação & Re-injeção
                                             (GCP Secret Manager / GitHub)
                                                      │
                                                      ▼
                                            Passo 4: Purga de Histórico Git
                                             (git-filter-repo / rebase)
                                                      │
                                                      ▼
                                            Passo 5: Registro no Livro
                                             (docs/INCIDENT_RESPONSE.md)
                                                      │
                                                      ▼
                                            Passo 6: Post-Mortem & Fix
```

### Passo 1: Triagem & Identificação
1. Acesse o **GitGuardian Dashboard** ou o link direto enviado para `contato@raixtech.com`.
2. Identifique:
   - Qual credencial foi detectada (tipo, provedor).
   - Onde ocorreu (commit hash, branch, arquivo, número da linha).
   - Quem realizou o commit (autor).

### Passo 2: Revogação Imediata no Provedor (Contenção P0)
> [!CAUTION]
> **REGRA DE OURO DA SEGURANÇA:**
> Qualquer segredo exposto em repositório público deve ser considerado **comprometido instantaneamente**. A purga do commit NÃO reverte o vazamento, pois bots maliciosos indexam pushes públicos em frações de segundo. **A revogação no provedor é mandatória e inegociável.**

- **Token GitHub (`gho_`, `ghp_`, `github_pat_`)**:
  - Acesse: `GitHub > Settings > Developer Settings > Personal Access Tokens`.
  - Localize o token exposto e selecione **Revoke / Delete** imediatamente.
- **Chave de API do Google / Firebase (`AIzaSy...`) / Gemini (`GEMINI_API_KEY`)**:
  - Acesse: `Google Cloud Console > APIs & Services > Credentials` ou `Secret Manager`.
  - Destrua a versão da chave (`Destroy Secret Version`) ou regenere a API Key com novas restrições.
- **Conta de Serviço GCP / Firebase (`client_email`, JSON de credenciais)**:
  - Acesse: `IAM & Admin > Service Accounts > [Conta] > Keys`.
  - Exclua a chave vazada.

### Passo 3: Rotação e Re-injeção Segura
1. Emita uma nova credencial com privilégios mínimos necessários.
2. Atualize o cofre correspondente:
   - Para infraestrutura de nuvem: `firebase-tools functions:secrets:set GEMINI_API_KEY` (sem eco de tela).
   - Para CI/CD GitHub Actions: `GitHub > Settings > Secrets and variables > Actions`.
3. Valide o restabelecimento do serviço em produção (uptime checks e logs).

### Passo 4: Purga e Higienização do Histórico Git
Se o segredo foi commitado e enviado ao repositório remoto:
1. Substitua o valor no arquivo por uma variável de ambiente ou placeholder seguro.
2. Utilize `git-filter-repo` ou BFG para remover a string de todo o histórico git, caso necessário:
   ```bash
   git filter-repo --replace-text expressions.txt
   ```
3. Execute o force push coordenado com os mantenedores:
   ```bash
   git push origin --force --all
   ```
4. Verifique que o histórico está limpo através de `ggshield secret scan repo --all`.

---

## 4. Estudo de Caso Referencial: Incidente do Token `gho_`

Como registro histórico e jurisprudência técnica interna do repositório Raix:

- **Contexto do Incidente**: Durante automações em tempo de execução, um token temporário do GitHub (`gho_`) foi referenciado em fluxo de trabalho.
- **Ações Corretivas Executadas**:
  1. O token foi imediatamente invalidado e revogado junto ao GitHub.
  2. Implementou-se no [`AGENTS.md`](../AGENTS.md) a regra permanente de **captura direta em memória volátil** via `git credential fill` (com descarte sumário e zero eco no console).
  3. Adicionou-se o padrão regex `gho_[A-Za-z0-9_]+` e assinaturas correlatas ao script [`scripts/verify_secrets.cjs`](../scripts/verify_secrets.cjs).
  4. Estabeleceu-se a integração com o **GitGuardian** para assegurar que qualquer evento similar seja barrado em múltiplas camadas.

---

## 5. Livro de Registro de Incidentes de Segurança

| ID | Data/Hora (UTC) | Tipo de Credencial | Origem da Detecção | Severidade | Ação Executada | Status |
|---|---|---|---|:---:|---|:---:|
| `INC-2026-01` | 2026-08 | GitHub OAuth Token (`gho_`) | Auditoria Interna / Script | P0 | Revogado no GitHub, adicionada regra permanente no AGENTS.md e regex no verify_secrets.cjs | **RESOLVIDO** |
| *(Próximo)* | -- | -- | -- | -- | -- | -- |

---

## 6. Procedimento Pós-Incidente (Post-Mortem)

Após a contenção do incidente:
1. **Auditoria de Acessos**: Inspecionar os logs do GCP Cloud Audit ou GitHub Security Log no intervalo entre o vazamento e a revogação para assegurar que nenhum recurso foi violado.
2. **Atualização de Padrões Locais**: Se a credencial não for coberta pelo [`scripts/verify_secrets.cjs`](../scripts/verify_secrets.cjs), adicionar a nova assinatura regex ao array de padrões proibidos.
3. **Fechamento no GitGuardian**: Marcar o incidente no painel do GitGuardian como "Resolved (Revoked)".
