# Task: Criar a estrutura de SDD e governança de agentes

- **Slug:** setup-sdd
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.1 (sem mudança de versão; apenas governança e documentação)
- **Tipo de mudança:** chore
- **Impacto público (módulo):** none

## 1. Contexto e Motivação
- O branch `chore/sdd` remove o SDD anterior (`.specs/`, `.agents/skills/spec-driven/`, `.claude/agents/`), que deixa de ser usado.
- O template `template-sdd-prompt.md` define a estrutura nova: `AGENTS.md`, skills de automação, `specs/` versionado e seção de SDD no README.
- Pedido original: `specs/tasks.txt`.
- Rastreabilidade: `AGENTS.md` §8 (fluxo SDD), `AGENTS.md` §10 (proibições), `README.md` (seção a adicionar).

## 2. Escopo
- **Inclui:**
  - `AGENTS.md` na raiz, adaptado ao Android Kotlin multi-módulo.
  - `.github/copilot-instructions.md`.
  - Skills em `.agents/skills/`: `code-planner`, `code-implementer`, `code-reviewer`, `android-architecture`, `open-pull-request`.
  - `specs/tasks.txt` e `specs/sdd-000-setup-sdd/tasks.md`.
  - Seção "Documentação e Spec-Driven Development (SDD)" no `README.md`.
- **Exclui:**
  - Qualquer código de produção (`app/`, `core/`, `feature/`, `wear/`).
  - Criação de `architecture/01-04` (ver item de follow-up na seção 9).
  - Commit, push e abertura de PR.
  - Alteração de `versionName` ou `CHANGELOG.md`.

## 3. Impacto Arquitetural
- **Camada afetada:** nenhuma (apenas arquivos de governança e documentação).
- **Padrão aplicado:** SDD com skills de automação (`code-planner → code-implementer → code-reviewer → open-pull-request`).
- **Estrutura de arquivos novos:**
  ```
  AGENTS.md
  .github/copilot-instructions.md
  .agents/skills/code-planner/SKILL.md
  .agents/skills/code-implementer/SKILL.md
  .agents/skills/code-reviewer/SKILL.md
  .agents/skills/android-architecture/SKILL.md
  .agents/skills/open-pull-request/SKILL.md
  specs/tasks.txt
  specs/sdd-000-setup-sdd/tasks.md
  ```
- **Fluxo:**
  ```
  specs/tasks.txt ──► code-planner ──► specs/<id>/tasks.md
                                          │ status: ready
                                          ▼
                     code-implementer ──► código + testes + lint/detekt
                                          ▼
                     code-reviewer ──► APROVADO | AJUSTES NECESSARIOS
                                          ▼ (só se pedido)
                     open-pull-request ──► gh pr create --base develop
  ```
- **Injeção de dependência:** não se aplica.

## 4. Contratos e Interfaces
- Nenhum contrato de código alterado.
- Contratos de processo definidos em `AGENTS.md` §8 e nas skills (formato do `tasks.md`, valores de `Status`, conclusões do reviewer).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | `specs/` não deve estar no `.gitignore` | Mantida versionada; verificado que o `.gitignore` não a cita | `.gitignore` |
| 2 | Não commitar sem testes | Sem mudança de código; checagem repetida no `code-implementer` | `AGENTS.md` §10 |
| 3 | Não usar `@Suppress` para lint | Regra registrada em `AGENTS.md` §5 e na skill `code-implementer` | `AGENTS.md` §5 |

## 6. Critérios de Aceitação
- Performance: não se aplica (sem código).
- Feature toggles: não se aplica.
- Observabilidade: não se aplica.
- Compatibilidade: nenhum arquivo de build alterado; build e testes não são afetados.
- Verificação:
  - `specs/` não aparece em `.gitignore`.
  - Os cinco `SKILL.md` têm front matter `name` e `description`.
  - `AGENTS.md` cita apenas comandos Gradle que existem (`lintDebug`, `detekt`, `testDebug`, `koverVerify`, `koverVerifyLayout`).

## 7. Plano de Testes
- **Unitários:** não se aplicam (sem código).
- **Manual:** revisar que os links relativos do `README.md` e do `AGENTS.md` apontam para arquivos existentes.
- **Lint/Detekt:** não necessários, pois nenhum arquivo Kotlin é alterado. Rodar `./gradlew detekt` apenas se o escopo mudar.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `architecture/01-04` referenciados pelo template não existem; planos ficam sem citação de docs | alta | média | Planner cita código (`arquivo:linha`) e registra a lacuna; follow-up na seção 9 |
| Caminhos de `.specs/` ainda citados em comentários (ex.: `.gitignore` linha 49) | média | baixa | Limpeza em follow-up; não bloqueia |
| Skills existentes (`feature-architecture`, etc.) conflitarem com `android-architecture` | baixa | baixa | `AGENTS.md` §9 define prevalência |

- Dependências externas: `gh` CLI autenticado (apenas para `open-pull-request`).

## 9. Perguntas em Aberto (bloqueantes)
- Nenhuma pergunta bloqueante.

**Não bloqueantes, para follow-up:**
- [ ] Criar `architecture/01-04` (visão geral de arquitetura, regras de negócio, modelos de domínio, integrações externas). Local decidido: `architecture/` na raiz, pois `docs/` é o site do GitHub Pages.
- [ ] Remover a referência a `.specs/MVP-ROADMAP.md` no comentário do `.gitignore` (linha 49), pois o diretório foi removido.
- [ ] Confirmar se `CHANGELOG.md` é gerado pelo pipeline de release ou escrito à mão, para atualizar `AGENTS.md` §3 e §6 com precisão.

## 10. Checklist de Conformidade
- [x] Decisões citam arquivos do repositório (`.gitignore`, `AGENTS.md`, `template-sdd-prompt.md`).
- [x] Nenhum código de produção escrito.
- [x] Seção 6 preenchida de forma substantiva.
- [x] Nenhum símbolo público alterado; impacto `none`.
- [x] Não se aplica endpoint nem cache novo.
- [x] Perguntas em aberto exauridas (apenas não bloqueantes registradas).
