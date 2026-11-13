# AGENTS.md — Cestou (How-Much)

Fonte de verdade para agentes de IA neste repositório. Em caso de divergência com o código, **o código prevalece** — valide no código e proponha a correção aqui.

## 1. Comandos do Projeto

Execute a partir da raiz do repositório.

| Comando | Descrição | Quando usar |
|---|---|---|
| `./gradlew lintDebug` | Lint Android da variante debug. | Antes de commit/PR. |
| `./gradlew detekt` | Análise estática Kotlin (config em `buildSrc/config/detekt/detekt.yml`). | Antes de commit/PR. |
| `./gradlew testDebug` | Testes unitários de todos os módulos. | Antes de commit/PR. |
| `./gradlew koverVerify koverVerifyLayout` | Verifica cobertura mínima (85%). | Antes de PR que altera lógica. |
| `./gradlew :app:connectedDebugAndroidTest :core:ui:connectedDebugAndroidTest :feature:settings:connectedDebugAndroidTest` | Testes instrumentados (exigem emulador/dispositivo). | Mudanças em UI/fluxos cobertos. |
| Abrir o projeto no Android Studio + *Sync Project* | Setup inicial e após mudanças em Gradle/versões. | Primeira vez ou mudança em `libs.versions.toml`. |

Chave de configuração do pipeline: `.github/pipeline-config.yaml` (ordem: lint → detekt → testes unitários → cobertura).

## 2. Arquitetura e Padrões

- **Propósito:** app Android de lista de compras (carrinhos, produtos, orçamento, histórico, compartilhamento, assistente de IA e módulo Wear OS). Código interno mantém `br.com.brunocarvalhs.howmuch` por decisão (ver `README.md`).
- **Padrões adotados:**

| Padrão | Onde | Observação |
|---|---|---|
| Clean Architecture | Cada `feature:*` | `domain/` puro, `data/` como infraestrutura |
| MVI (Model-View-Intent) | `presentation/` das features | Intents como data class; estado imutável |
| Navegação tipada (Nav3) | `navigation/` + `core:navigation` | Rotas como `NavKey` |
| Injeção de dependência (Hilt) | `di/` em cada módulo | Sem service locator |
| Domain-centric modular | `core:*` e `feature:*` | Features não dependem umas das outras diretamente |

- **Camadas (por feature):**
  - `domain/` — modelos, repositórios (interfaces), use cases.
  - `data/` — implementações de repositório, DTOs, mappers, fontes remotas/locais.
  - `presentation/` — Composables, ViewModels, estado e intents.
  - `navigation/` — `NavKey` e grafo da feature.
  - `di/` — módulos Hilt.
- **Pacotes permitidos em `:core:*`:** definidos em `CORE_LAYERS` (`app/src/test/.../architecture/ArchitectureTest.kt`); inclui `prompt` (prompts de IA compartilhados em `core:ai`) e `modifiers` (`core:ui`).
- **Papéis dos módulos:**
  - `:core:*` — transversais (`common`, `domain`, `theme`, `ui`, `navigation`, `auth`, `data`, `ai`, `remote-config`, `analytics`, `billing`, `ads`). `:core:ads` (sdd-059) é o SDK de anúncios do plano FREE; só o `:app` depende dele, e as features usam as interfaces e `CompositionLocal`s de `:core:ui` (`contract/`).
  - `:feature:*` — `shopping`, `products`, `settings`, `auth`, `profile`, `chat`, `cart`, `ai-agent`, `subscription`, `feedback`.
  - `:app` — entrypoint; orquestra features via `FeatureInitializer`.
  - `:wear` — módulo Wear OS standalone, com `applicationId` e pareamento próprios.

## 3. Princípios de Decisão

- **Código prevalece sobre documentação.** Antes de afirmar comportamento, leia o código (arquivo:linha).
- **Atualização de documentação** — tabela de correlação:

| Tipo de alteração | Atualizar |
|---|---|
| Mudança de comportamento visível | `CHANGELOG.md` (via processo de release) e `fastlane/metadata` se afetar loja |
| Novo módulo ou mudança de camada | `AGENTS.md` §2 e `.agents/skills/android-architecture/SKILL.md` |
| Novo fluxo de pipeline/CI | `.github/PIPELINE.md` e `.github/pipeline-config.yaml` |
| Novo fluxo de trabalho SDD | `README.md` (seção SDD) e este arquivo §8 |

## 4. Estrutura de Pastas

```
app/                      entrypoint Android (Application, FeatureInitializer)
core/
├── common/ domain/ data/ theme/ ui/ navigation/
├── auth/ ai/ remote-config/ analytics/ billing/ ads/
feature/
├── shopping/ products/ settings/ auth/ profile/
├── chat/ cart/ ai-agent/ subscription/ feedback/
    └── src/main/java/br/com/brunocarvalhs/howmuch/feature/<nome>/
wear/                     Wear OS standalone
buildSrc/config/detekt/detekt.yml  regras Detekt
gradle/libs.versions.toml versões centrais
.github/                  pipelines, PR template, danger
.agents/skills/           skills de automação (SDD + Android)
.specs/                   artefatos SDD versionados
architecture/             documentação de arquitetura (01-05) — fonte para o SDD
docs/                     site estático (GitHub Pages) e textos legais — NÃO é documentação de arquitetura
```

## 5. Convenções de Código

- **Linguagem:** 100% Kotlin. Compose para UI. `compileSdk` e `targetSdk` = 37, `minSdk` = 28 (`gradle/libs.versions.toml`).
- **Nomenclatura:** classes/Composables em `PascalCase`; funções, propriedades e variáveis em `camelCase`; pacotes em minúsculas; nomes de módulo Gradle em `kebab-case`.
- **Visibilidade:** `internal` por padrão dentro do módulo; `public` apenas para contratos entre módulos (ver §6).
- **DI:** Hilt. Não instanciar repositórios ou use cases manualmente em Composables/ViewModels.
- **Estado de UI:** imutável; coleções imutáveis em estados expostos.
- **Strings:** toda string visível ao usuário em `strings.xml` com traduções pt-BR e en-US (app é usado em PT e EN). Não use texto literal em Composables.
- **Testes:** JUnit + coroutines test para unidades; testes de UI com `testTag` (não asserts de texto, pois o app roda em pt-BR ou en).
- **Lint/Detekt:** não silenciar findings com `@Suppress` — corrija a causa. Exceções exigem justificativa explícita no PR.
- **Logs/observabilidade:** Firebase Crashlytics e Analytics via módulo `core:analytics`. Não logar dados pessoais ou tokens.

## 6. Fluxo Obrigatório para Novos Parâmetros Públicos

Aplica-se a mudanças em modelos, interfaces de repositório, enums, contratos de IA/remote-config e rotas (`NavKey`) expostos entre módulos:

1. Validar a necessidade no código (usos atuais via busca) e na especificação em `.specs/`.
2. Marcar explicitamente como `public` e justificar por que não pode ser `internal`.
3. Adicionar/atualizar testes unitários.
4. Atualizar a documentação de integrações afetadas (ex.: `core:ai`, `core:remote-config`).
5. Registrar a mudança no processo de release (`CHANGELOG.md`).
6. Fazer bump de versão (`versionName` em `app/build.gradle.kts`, via `.github/scripts/version.sh`) se houver breaking change ou release.
7. Solicitar revisão de, no mínimo, um maintainer.

## 7. Instruções para Agentes de IA

- **Co-autoria em commits:**

| Agente | Trailer |
|---|---|
| Claude | `Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>` (ajuste o modelo conforme usado) |

- **Formato de commit:** Conventional Commits (`feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`, `build:`, `ci:`). O pipeline valida via commitlint.
- **Comportamento geral:** nunca expor segredos; confirme regras no código; cite arquivos e linhas nas justificativas; não invente comportamento de produto.

## 8. Fluxo de Trabalho com SDD

```
1. Escreva a tarefa em `.specs/tasks.txt`.
2. Gere o plano técnico com a skill `code-planner`.
   → Produz `.specs/<feature-id>/tasks.md` (10 seções).
   → Ambiguidade bloqueante ⇒ `status: blocked` + "Perguntas em Aberto".
3. Revise o plano com um maintainer.
4. Implemente com a skill `code-implementer` (só com `status: ready`).
   → Código + testes + lint/detekt/testes.
5. Revise com a skill `code-reviewer` (não altera código).
   → Conclusão: APROVADO ou AJUSTES NECESSARIOS.
6. (Opcional, só se pedido) Abra o PR com a skill `open-pull-request`.
```

> `.specs/` é versionado (apesar do prefixo `.`, não é rascunho oculto). Não use `specs/` nem `docs/specs/` para entrada de tarefas.

## 9. Skills de Automação

| Skill | Quando usar |
|---|---|
| `code-planner` | Planejar qualquer tarefa não trivial, antes de codar. |
| `code-implementer` | Implementar a partir de `.specs/<feature-id>/tasks.md` com `status: ready`. |
| `code-reviewer` | Revisar antes de commit/PR. |
| `android-architecture` | Dúvidas sobre onde criar arquivos, camadas, padrões e visibilidade. |
| `open-pull-request` | **Só quando explicitamente invocada.** |

Skills de domínio já existentes em `.agents/skills/` (ex.: `feature-architecture`, `wear-architecture`, `camerax`, `navigation-3`) continuam válidas; em caso de conflito, prevalece `android-architecture` para este repositório.

## 10. Proibições Globais

- Não versionar segredos: `.env`, `google-services.json`, `*.keystore`, `*.jks`, `key.properties`, tokens ou chaves de API.
- Não usar `README.md` como fonte de verdade.
- Não commitar código sem testes.
- Não ignorar lint/detekt sem justificativa explícita (nem `@Suppress` para silenciar finding).
- Não alterar contratos públicos sem registro no release e bump de versão quando aplicável.
- Não executar `rm -rf`, `git reset --hard`, `git push -f` ou reescrita de histórico sem confirmação explícita.
- Não carregar `open-pull-request` automaticamente.
- Não versionar `.specs/` como rascunho oculto: os planos SDD são públicos no repositório, apesar do prefixo `.`.

## 11. Dependências Externas

Versões centrais em `gradle/libs.versions.toml`. Principais:

| Dependência | Uso | Referenciada em |
|---|---|---|
| Kotlin (2.4.x) | Linguagem | `gradle/libs.versions.toml` |
| Jetpack Compose / Material 3 | UI | `core:theme`, `core:ui`, `feature:*/presentation` |
| Navigation 3 | Navegação tipada | `core:navigation`, `feature:*/navigation` |
| Hilt | Injeção de dependência | `di/` de cada módulo |
| Firebase (Auth, Firestore, Analytics, Crashlytics, Performance) | Backend, telemetria | `core:auth`, `core:data`, `core:analytics` |
| Credential Manager / Google ID | Login | `core:auth`, `feature:auth` |
| Google Play Billing | Assinatura | `core:billing`, `feature:subscription` |
| OpenRouter / Gemini | Assistente de IA | `core:ai`, `feature:ai-agent`, `feature:chat` |
| Detekt, Kover, Konsist | Qualidade e cobertura | `buildSrc/config/detekt`, `buildSrc` |

## 12. Contexto do Projeto

- **Owner / autor:** Bruno Carvalho (`apm.yml`).
- **Plataforma:** Android (minSdk 28, targetSdk 37) + Wear OS.
- **Versão atual:** `2.2.1` (`app/build.gradle.kts`), `versionCode` 14.
- **Build e release:** Gradle + GitHub Actions; pipeline em `.github/PIPELINE.md`; distribuição via Play Console e Firebase App Distribution (testers internos).
- **Licença:** proprietária, todos os direitos reservados (`LICENSE`).
- **Documentação de arquitetura:** `architecture/01-05` (visão geral, regras de negócio, modelos de domínio, integrações externas, decisões de arquitetura AD-0xx) está em `architecture/`. Ela é uma leitura parcial do código: cada regra cita o arquivo de origem, e o que não foi verificado está marcado no próprio documento. Quando algo não estiver documentado, cite o código diretamente e registre a lacuna em "Perguntas em Aberto". `docs/` não é usado para isso, pois é o site publicado no GitHub Pages.
