# Task: Concluir a migração para o Gradle 9 (resíduos e validação)

- **Slug:** gradle-9-migration
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-13
- **Status:** ready
- **Versão-alvo:** 2.6.0 (sem bump de `versionName`; chore de build, o versionName segue o processo de release)
- **Tipo de mudança:** chore
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

- O pedido foi "migrar para o Gradle 9". A auditoria mostrou que a migração **já foi feita no wrapper**: `gradle/wrapper/gradle-wrapper.properties:3` aponta para `gradle-9.9.0-bin.zip`, resultado do bump do Dependabot em #279 (`CHANGELOG.md:8`). O AGP já está em 9.5.1 (`gradle/libs.versions.toml:2`).
- Restam resíduos e um aviso de deprecação. Este plano fecha esses resíduos e valida a build com Gradle 9.9.0.
- Verificação feita nesta sessão:
  - `./gradlew --version` → Gradle 9.9.0, launcher JDK 21.0.7.
  - `./gradlew help --warning-mode all` → `BUILD SUCCESSFUL`, com 2 deprecações, ambas originadas no convention plugin Kover (`buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:8`): `Configuration.setVisible(boolean)` (remoção no Gradle 11) e `ReportingExtension.file(String)` (remoção no Gradle 10).
  - `./gradlew build --dry-run` → `BUILD SUCCESSFUL`, mesmo resumo de deprecação.
- Pedido original: `.specs/tasks.txt` não contém entrada para esta tarefa. A entrada veio do pedido direto do usuário (`/code-planner crie um plano para migrar para o gradle 9`).

## 2. Escopo

- **Inclui:**
  - T1: eliminar as 2 deprecações do Gradle 9.9 emitidas na configuração (Kover).
  - T2: remover a chave morta `gradle-version` de `.github/pipeline-config.yaml`.
  - T3: validar a build completa (lint, detekt, testes, cobertura, assemble) com Gradle 9.9.0 sem nenhum aviso de deprecação do Gradle.
  - T4 (decisão pendente, não executar sem aprovação): alinhamento de JDK entre CI e Gradle daemon (ver §9).
- **Exclui:**
  - Trocar a versão do Gradle (já é 9.9.0).
  - Trocar a versão do AGP ou do Kotlin.
  - Ativar configuration cache (sugerido como fase posterior, ver §8).
  - Remover ou corrigir o módulo `baselineprofile/` órfão (ver §9).
  - Elevar `compileOptions` de Java 11 para 17.
  - Editar `CHANGELOG.md` ou `versionName`, conforme a regra do processo de release.

## 3. Impacto Arquitetural

- Camada afetada: nenhuma camada de código de produção. O impacto é em build (Gradle/buildSrc) e CI (`.github/`).
- Padrão aplicado: não se aplica (infraestrutura de build).
- Arquivos a alterar:
  - `buildSrc/build.gradle.kts:13` (versão do Kover, hoje `0.9.11`), se houver versão mais nova compatível com Gradle 9.9.0 e Kotlin 2.4.21.
  - `.github/pipeline-config.yaml:32` (remover `gradle-version: "8.3.1"`).
- Arquivos que **não** mudam: `gradle/wrapper/gradle-wrapper.properties`, `libs.versions.toml`, `settings.gradle.kts`, `app/build.gradle.kts`.
- Diagrama de dependência de build (atual):

```
gradle-wrapper (9.9.0) ──► Gradle 9.9.0 launcher (JDK 21 local / 17 CI)
        │
        ├─ buildSrc (convenções howmuch.*) ── kover 0.9.11 ──► deprecações (T1)
        │                                  └─ detekt 1.23.8
        ├─ settings.gradle.kts (foojay, 27 módulos incluídos)
        └─ CI: setup-env (JDK 17) ◄── pipeline-config.yaml
                    gradle-version: 8.3.1 (sem consumidor) ──► remover (T2)
```

- Injeção/comunicação: não se aplica.

## 4. Contratos e Interfaces

- Nenhum modelo, repositório, use case, intent, estado, `NavKey` ou contrato de IA/remote-config muda.
- Nenhuma API pública entre módulos muda. Impacto público: **none**.
- Sem novo endpoint, coleção, evento de Analytics ou remote-config.

## 5. Regras de Negócio Aplicáveis

Regras de build que este plano precisa preservar:

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Gate de cobertura total de 85% | Mantido, sem alteração de limite | `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:157` |
| 2 | Gate de cobertura de layout de 85% | Mantido | `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:167` |
| 3 | Detekt usa baseline por módulo | Mantido | `buildSrc/src/main/kotlin/howmuch.detekt.gradle.kts:47` |
| 4 | Testes sem casos descobertos não falham | Mantido | `buildSrc/src/main/kotlin/howmuch.test-defaults.gradle.kts:10` |
| 5 | CI lê `java-version` de `pipeline-config.yaml` (usado em `tests.yml`, `build.yml`, `release.yml`) | Mantido. Só a chave `gradle-version` sai | `.github/workflows/tests.yml:92`, `.github/pipeline-config.yaml:31` |
| 6 | `gradle-version` não tem consumidor no repositório | Remover a chave | `.github/pipeline-config.yaml:32` (busca em `.github/` sem ocorrências além da própria linha) |
| 7 | `.github/PIPELINE.md` não documenta a versão do Gradle | Sem alteração no PIPELINE.md | busca em `.github/PIPELINE.md` sem ocorrências |

## 6. Critérios de Aceitação

- **Performance:** nenhum impacto em runtime do app. Build: `./gradlew help` deve manter tempo de configuração equivalente ao atual (~4s após cache, medido em `scratchpad/gradle9-warnings.log`). Sem I/O duplicado nem tasks novas.
- **Feature toggles:** não se aplica.
- **Observabilidade:** não se aplica.
- **Compatibilidade:**
  - Gradle 9.9.0 sem nenhum aviso de deprecação do próprio Gradle na configuração: `./gradlew help --warning-mode all` sem linhas `deprecated` do Gradle.
  - Resumo `Deprecated Gradle features were used in this build` ausente em `./gradlew build --dry-run`.
  - `minSdk` 28 e `targetSdk`/`compileSdk` 37 inalterados.
  - Sem mudança em artefatos publicados (APK/AAB).

## 7. Plano de Testes

- **Unitários:** não há código novo. Os testes existentes são a regressão.
- **Instrumentados / UI:** não se aplica.
- **Validação de build (obrigatória):**
  1. `./gradlew --version` → Gradle 9.9.0.
  2. `./gradlew help --warning-mode all` → sem deprecação do Gradle (esperado após T1).
  3. `./gradlew build --dry-run` → `BUILD SUCCESSFUL`, sem resumo de deprecação.
  4. `./gradlew lintDebug`.
  5. `./gradlew detekt`.
  6. `./gradlew testDebug`.
  7. `./gradlew koverVerify koverVerifyLayout` (manter 85%; se a versão do Kover mudar, comparar o percentual antes/depois).
  8. `./gradlew :app:assembleDebug :wear:assembleDebug` (valida os dois targets distribuíveis do pipeline).
  9. Não executar `clean` (regra de `agp-9-upgrade`).
- **Lint/Detekt/Cobertura:** comandos acima.
- **CI:** o PR precisa passar no job de build e no de lint. Lembrar que duas falhas recentes em `develop` não são do Gradle (ver §8, riscos).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Upgrade do Kover altera o cálculo de cobertura e derruba o gate de 85% | média | alto | Medir antes/depois com `koverVerify`; se cair, não subir a versão e registrar a deprecação como dívida upstream |
| Deprecação vem de dentro do plugin Kover e não há versão corrigida | média | baixo | Registrar em "Perguntas em Aberto" e aceitar o aviso até o próximo release do plugin |
| Erro intermitente `Dagger/MissingBinding` (KSP2) no job de lint, já documentado | baixa-média | médio | Já existe mitigação de heap em `pipeline-config.yaml:35`. Run de referência: `43432076327` (13/11, 13:20). Reexecutar antes de concluir que é regressão |
| Falha de upload do Wear no Firebase App Distribution | alta (ocorreu na run `43481430059`, 13/11 18:46) | baixo para este plano | Não é do Gradle: o build chegou ao deploy. Tratar em issue separada |
| Ao remover `gradle-version`, alguma ferramenta externa lê a chave | baixa | baixo | Busca no repositório não encontrou consumidores. Validar com o maintainer no review |
| Daemon JVM 21 (`gradle/gradle-daemon-jvm.properties:12`) e CI com JDK 17 (`setup-env/action.yml:8`) podem gerar provisionamento de JDK no CI, com tempo extra | média | baixo | Ver T4 (§9). Não alterar nesta entrega |

## 9. Perguntas em Aberto

Nenhuma pergunta é bloqueante. A tarefa está pronta para implementação. As decisões abaixo têm default definido.

- [ ] **Não bloqueante:** o pedido "migrar para o Gradle 9" já está atendido no wrapper. **Default adotado:** tratar a tarefa como fechamento de resíduos (T1–T3). Confirmar com o maintainer se havia outro alvo (ex.: configuration cache obrigatório).
- [x] **Dívida upstream (T1), registrada na revisão:** as 2 deprecações do Gradle 9.9 (`Configuration.setVisible` e `ReportingExtension.file`) saem de dentro do plugin Kover (`kotlinx.kover.gradle.plugin.appliers.PrepareKover`, via `asBucket`), acionado por `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:8`. A versão 0.9.11 é a última publicada no Maven Central (verificado em 2026-11-13). **Resolução:** sem alteração no repositório. Critério de §6 ("zero avisos do Gradle") fica pendente até nova versão do Kover; reavaliar a cada atualização do plugin.
- [ ] **Não bloqueante (T4):** qual JDK o CI deve usar? `pipeline-config.yaml:31` diz 17; `gradle/gradle-daemon-jvm.properties:12` diz 21, e o JDK local é 21.0.7. **Default:** não alterar nesta entrega. **Recomendação:** alinhar CI em 21 em um PR separado, para evitar o provisionamento do daemon.
- [ ] **Não bloqueante:** o módulo `baselineprofile/` não está incluído em `settings.gradle.kts` (não há `include` para ele), mas o arquivo ainda existe. Ele usa `alias(libs.plugins.kotlin.android)` (`baselineprofile/build.gradle.kts:3`), `kotlinOptions` (`:16-17`) e aliases que não existem no catálogo (`android.test`, `baselineprofile`, `uiautomator`; `gradle/libs.versions.toml` não os define). **Default:** não tocar. **Recomendação:** remover ou mover para fora do repositório em issue separada, porque qualquer `include` futuro quebra com AGP 9.
- [ ] **Não bloqueante:** `compileOptions` continua em Java 11 (`app/build.gradle.kts:77-78`, `wear/build.gradle.kts:66-67`, `buildSrc/src/main/kotlin/howmuch.android.library.gradle.kts:27-28`). Não é exigência do Gradle 9. **Default:** não tocar.
- [ ] **Não bloqueante (fase posterior):** configuration cache não está ativo (`gradle.properties` não tem `org.gradle.configuration-cache`). **Default:** fora deste plano. Abrir spike separado.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito (apenas plano, pseudocódigo e diagrama).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Impacto público marcado como `none`, sem símbolo público alterado.
- [x] Não se aplica: nenhum endpoint, toggle ou observabilidade nova.
- [x] Não se aplica: nenhum cache novo.
- [x] Perguntas em aberto listadas, todas não bloqueantes, com default definido.
- [x] Não tocar `CHANGELOG.md` nem `versionName` (processo de release).

---

## Sequência de execução (para o code-implementer)

1. T1: atualizar Kover em `buildSrc/build.gradle.kts:13` para a versão mais recente compatível (verificar no Maven Central `org.jetbrains.kotlinx:kover-gradle-plugin`). Rodar `./gradlew help --warning-mode all` e `./gradlew koverVerify koverVerifyLayout`. Se houver regressão de cobertura ou o aviso persistir, reverter e registrar em §9.
2. T2: remover `gradle-version: "8.3.1"` de `.github/pipeline-config.yaml:32`. Manter `java-version`.
3. T3: rodar o bloco de validação da §7 completo.
4. Commit em Conventional Commits, por exemplo `chore(build): remove deprecações do Kover no Gradle 9.9 e chave morta de gradle-version`, com trailer `Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>`.
5. T4 e os demais itens de §9 ficam para PRs separados, após decisão do maintainer.
