# Instruções para Copilot e agentes — Cestou

A fonte de verdade é o `AGENTS.md` na raiz do repositório. Leia-o antes de qualquer alteração.

## Resumo rápido

- App Android 100% Kotlin, Jetpack Compose (Material 3), Navigation 3 tipada, Hilt, Clean Architecture + MVI.
- Módulos: `:app`, `:core:*`, `:feature:*`, `:wear`. Features têm `domain/`, `data/`, `presentation/`, `navigation/`, `di/`.
- Antes de codar tarefa não trivial: escreva em `.specs/tasks.txt` e gere o plano com a skill `code-planner`. Implemente só com `status: ready` (skill `code-implementer`).
- Validação antes de commit: `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`.
- Cobertura mínima de 85% (`./gradlew koverVerify koverVerifyLayout`).

## Regras que o agente deve seguir

- Não silencie findings de lint/detekt com `@Suppress`; corrija a causa.
- Strings visíveis ao usuário vão para `strings.xml` (pt-BR e en-US).
- Em testes de UI, use `testTag`, não texto literal (o app roda em PT e EN).
- Visibilidade `internal` por padrão; `public` só para contratos entre módulos, com justificativa.
- Não versione segredos (`.env`, `google-services.json`, keystores, `key.properties`).
- Commits em Conventional Commits. Não faça `git push -f` ou reescrita de histórico sem confirmação.
- Não abra PR sem pedido explícito.
