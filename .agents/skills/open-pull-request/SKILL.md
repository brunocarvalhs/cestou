---
name: open-pull-request
description: Abre um pull request do Cestou via gh CLI, com título em Conventional Commits e corpo padronizado. Carregue somente quando o usuário pedir explicitamente para abrir o PR.
---

# Open Pull Request

Persona: assistente de automação de Git/PR.

**Atenção:** só execute quando o usuário pedir explicitamente. Nunca dispare automaticamente após implementar ou revisar.

## Pré-condições

1. Repositório Git com remote `origin` configurado.
2. `gh` instalado e autenticado (`gh auth status`).
3. Branch atual **não** é `develop` (base do repositório).
4. Branch atual publicada no remote.
5. Existem mudanças a submeter (commits à frente da base).

## Fluxo

1. Base: `develop` (`.github/PULL_REQUEST_TEMPLATE/pull_request_template.md` define o corpo).
2. Valide o estado: `git status`, `git log develop..HEAD --oneline`, `git diff develop...HEAD --stat`.
3. Confirme que `./gradlew lintDebug`, `./gradlew detekt` e `./gradlew testDebug` passaram nesta branch (ou informe que não foram rodados).
4. Verifique se já existe PR aberto: `gh pr list --head <branch> --state open`. Se existir, informe e pare.
5. Título em Conventional Commits, ex.: `feat(shopping): ...`, `fix(data): ...`, `chore(sdd): ...`.
6. Corpo com as seções do template do repositório, incluindo: What, How, Testing, Impact, Checklist. Termine o corpo com:
   `🤖 Generated with [Claude Code](https://claude.com/claude-code)`
7. Crie o PR:
   ```bash
   gh pr create --base develop --title "<título>" --body-file <arquivo-temporário>
   ```
8. Informe o link retornado.

## Limites

- Não force push, não reescreva histórico e não faça merge.
- Não inclua segredos, `google-services.json` ou `.env` no diff.
