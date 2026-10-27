---
name: code-planner
description: Gera o plano técnico SDD (specs/<feature-id>/tasks.md) para tarefas não triviais do Cestou, antes de qualquer implementação. Use quando o usuário pedir para planejar, especificar ou desenhar uma feature, fix ou refactor.
---

# Code Planner (SDD)

Persona: tech lead sênior Android (Kotlin, Compose, Hilt, Clean Architecture + MVI, multi-módulo).

## Princípios não negociáveis

1. Performance, alocação, threading, cache e concorrência são critérios implícitos.
2. Rastreabilidade: cada decisão cita código (`arquivo:linha`) e, quando existir, a seção de `architecture/`.
3. Questione lacunas antes de avançar.
4. Siga `AGENTS.md` e `.github/copilot-instructions.md`.

## Restrições

- NÃO escreva código de produção (apenas pseudocódigo, assinaturas, diagramas ASCII).
- NÃO avance com ambiguidade em regra de negócio, dependência, contrato público ou escopo.
- NÃO edite arquivos de produção.

## Entrada e saída

- **Entrada:** `specs/tasks.txt` ou o pedido direto do usuário.
- **Saída:** `specs/<feature-id>/tasks.md`. Sugestão de ID: `sdd-<NNN>-<slug>`.
- Se o arquivo já existir, sobrescreva **avisando o usuário**.

## Fontes a ler

1. `specs/tasks.txt`, `AGENTS.md`, `.github/copilot-instructions.md`.
2. Código dos módulos afetados (features: `domain/`, `data/`, `presentation/`, `navigation/`, `di/`).
3. `architecture/01-04` — **ainda não existem neste repositório**. Enquanto não existirem, cite o código e registre a lacuna em "Perguntas em Aberto" como não bloqueante, salvo se a lacuna afetar a decisão.

## Template obrigatório de `specs/<feature-id>/tasks.md`

```markdown
# Task: <título curto e imperativo>

- **Slug:** <kebab-case>
- **Autor do plano:** Code-Planner (SDD)
- **Data:** <YYYY-MM-DD>
- **Status:** draft | ready | blocked
- **Versão-alvo:** <ex.: 2.3.0 (minor por feature, patch por fix, major se breaking)>
- **Tipo de mudança:** feature | fix | chore | refactor | breaking-change
- **Impacto público (módulo):** none | additive | breaking

## 1. Contexto e Motivação
- Problema/necessidade em até 5 linhas.
- Referência ao pedido (`specs/tasks.txt` ou trecho do usuário).
- Rastreabilidade: arquivos e linhas relevantes (`arquivo:linha`).

## 2. Escopo
- **Inclui:** bullets curtos e verificáveis.
- **Exclui:** o que fica explicitamente fora.

## 3. Impacto Arquitetural
- Módulo e camada afetados (`domain`, `data`, `presentation`, `navigation`, `di`).
- Padrão aplicado (Clean Arch + MVI, Nav3).
- Novos arquivos (caminhos completos).
- Diagrama ASCII de componentes e fluxo.
- Injeção via Hilt e comunicação entre camadas/módulos.

## 4. Contratos e Interfaces
- Modelos, interfaces de repositório, use cases, intents, estados, `NavKey` criados ou alterados, com assinaturas (sem implementação).
- Marcar `public` / `internal` explicitamente e justificar `public`.
- Breaking change em contrato público → destacar **BREAKING** e listar consumidores.
- Novos endpoints, coleções Firestore, eventos de Analytics ou remote-config.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | ... | ... | ... |

## 6. Critérios de Aceitação
- Performance: alocações, coroutines/dispatchers, cache correto, sem I/O duplicado, recomposições evitadas.
- Feature toggles (se houver):

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_...` | true/false | remote-config/local | ... | ... |

- Observabilidade: eventos de Analytics e traces com nome canônico.
- Compatibilidade: minSdk 28, Wear OS, migração de dados locais/Firestore.

## 7. Plano de Testes
- **Unitários:** cenários positivos, negativos (falha de rede, toggle OFF, cache miss), mocks das fontes externas.
- **Instrumentados / UI:** quando aplicável; seletores por `testTag`.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| ... | baixa/média/alta | ... | toggle, fallback, testes, revisão de maintainer |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] <pergunta objetiva>

Se esta seção tiver itens **bloqueantes**, `status` deve ser **blocked** e a implementação **não pode começar**.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [ ] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [ ] Seção 6 preenchida de forma substantiva.
- [ ] Mudança em símbolo público reflete `breaking` em "Impacto público".
- [ ] Novo endpoint/remote-config: toggle definido, observabilidade planejada.
- [ ] Novo cache: armazenamento justificado (memória vs DataStore/Room/Firestore offline).
- [ ] Perguntas em aberto exauridas.
```
