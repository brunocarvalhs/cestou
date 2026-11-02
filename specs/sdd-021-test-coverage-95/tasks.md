# Task: Aumentar Cobertura de Testes para 95% (Kover)

- **Slug:** sdd-021-test-coverage-95
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-31
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** none

## 1. Contexto e Motivação
- Aumentar o gate de cobertura agregada de linhas do Kover de 80% para 95% em todo o projeto Cestou (`buildSrc/src/main/kotlin/howmuch.kover.gradle.kts`).
- Rastreabilidade: `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:125`, `AGENTS.md:14`.

## 2. Escopo
- **Inclui:**
  - Ajuste do `minBound(95)` no Kover (`howmuch.kover.gradle.kts`).
  - Criação de testes unitários adicionais em módulos/classes com cobertura pendente (repositórios, use cases e viewmodels sem testes completos).
  - Atualização da documentação (`AGENTS.md`, `.github/pipeline-config.yaml`, `.github/copilot-instructions.md`).
- **Exclui:**
  - Testes instrumentados UI (que rodam em emulador via `androidTest`).

## 3. Impacto Arquitetural
- Módulos afetados: Todos os módulos (`:core:*`, `:feature:*`, `:app`).
- Padrão aplicado: JUnit 5 / JUnit 4 + Coroutines Test + MockK.
- Novos arquivos: Testes unitários adicionais nos pacotes de teste dos módulos.

## 4. Contratos e Interfaces
- Nenhuma alteração em contratos públicos de produção. Exclusivamente adição de testes e ajuste do portão de Kover.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Cobertura mínima de 80% no Kover | Cobertura mínima de 95% no Kover | `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:125` |

## 6. Critérios de Aceitação
- Execução de `./gradlew koverVerify koverVerifyLayout` passa com sucesso atingindo ou superando 95% de cobertura de linhas.
- Testes unitários rodam limpos e rápidos.

## 7. Plano de Testes
- **Unitários:** `./gradlew testDebug`
- **Cobertura:** `./gradlew koverVerify koverVerifyLayout`
- **Qualidade:** `./gradlew lintDebug detekt`

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Falha no Kover Verify se linhas adicionais não forem testadas | Alta | Alto | Escrever testes unitários direcionados para classes com menor cobertura |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção alterado indevidamente.
- [ ] Seção 6 preenchida de forma substantiva.
- [ ] Documentação atualizada.
