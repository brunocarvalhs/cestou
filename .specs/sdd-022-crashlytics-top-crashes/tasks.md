# Task: Corrigir os 3 crashes abertos no Crashlytics (últimos 7 dias)

- **Slug:** crashlytics-top-crashes
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-01
- **Status:** ready
- **Versão-alvo:** 2.3.1 (patch)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Pedido do usuário: corrigir os crashes abertos reportados pelo Firebase Crashlytics, filtro "Abertos", últimos 7 dias (26/out–01/nov/2026), ordenados por `eventCount`
(console: `crashlytics/app/android:br.com.brunocarvalhs.howmuch/issues?time=7d&state=open&tag=all&sort=eventCount&types=crash`).

O console lista **3 problemas abertos**, todos com 3 eventos cada:

| # | Issue | Eventos/Usuários | Versões | Tag |
|---|---|---|---|---|
| A | `CartGraphKt.cartDestination$lambda$0` — `IllegalStateException: You cannot access the NavBackStackEntry's ViewModels after the NavBackStackEntry is destroyed.` | 3/3 | 2.2.0–2.3.0 | falhas recorrentes |
| B | `Navigator.getNavController` — `IllegalStateException: Navigator not bound to a NavController` | 3/1 | 2.2.0–2.2.0 | falhas no início da sessão |
| C | `JobScheduler34.forNamespace` — `NoSuchMethodError` | 3/1 | 2.2.0–2.3.0 | — |

Investigação por item, com stack trace completo lido no console (IDs de issue: `564afa05e8745a558202f8e9f684782a`, `7b8dafbcbcb664d9d0c8883a589e6e12`, `7c080f7cbf38dfe61fdbdd0f0710a58a`):

### A — `CartGraphKt.cartDestination$lambda$0` (ainda ativo em 2.3.0 — **requer correção**)

Stack trace (evento mais recente, Samsung SM-S721B, Android 16, app 2.3.0 (15), 31/out/2026 20:48:18):
```
Fatal Exception: java.lang.IllegalStateException
You cannot access the NavBackStackEntry's ViewModels after the NavBackStackEntry is destroyed.
  androidx.navigation.NavBackStackEntry.getViewModelStore (NavBackStackEntry.kt:179)
  androidx.lifecycle.viewmodel.compose.ViewModelKt.viewModel (ViewModel.kt:1)
  br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartGraphKt.cartDestination$lambda$0 (CartGraph.kt:339)
  androidx.compose.runtime.internal.ComposableLambdaImpl.invoke (ComposableLambda.kt:143)
```
O número de linha do relatório (`CartGraph.kt:339`) não bate com o arquivo atual (242 linhas) porque o arquivo foi reestruturado depois (`ef35b273`, `58367891`, `97533540`, `7630d7e9` — pacotes padronizados, componentes movidos para `core:ui`). O padrão de código que causa o crash continua presente, hoje em `CartGraph.kt:67-70`:
```kotlin
val parentEntry = remember(backStackEntry) {
    navigator.navController.getBackStackEntry<CartFlow>()   // CartGraph.kt:68
}
val viewModel: CartViewModel = hiltViewModel(parentEntry)   // CartGraph.kt:70
```
`parentEntry` é capturado uma vez dentro de `remember(backStackEntry)`. Quando o usuário sai do carrinho (`onBack = { navigator.goBack() }`, `CartGraph.kt:89`), o `NavHost` remove tanto a entrada de `CartDestination` quanto a do `CartFlow` pai (já que `CartDestination` é a `startDestination` do grafo aninhado). Se a composable recompuser durante a transição de saída — por exemplo porque `gateViewModel.isPro` (`CartGraph.kt:75`) ou `uiState` (`CartGraph.kt:72`) emitem um novo valor nesse intervalo — `remember(backStackEntry)` não reexecuta (a chave `backStackEntry` não mudou), mas a chamada `hiltViewModel(parentEntry)` roda a cada recomposição, fora de qualquer guarda de ciclo de vida, e `NavBackStackEntry.getViewModelStore()` lança assim que `parentEntry.lifecycle.currentState == DESTROYED`.

O mesmo padrão — `remember(backStackEntry) { navigator.navController.getBackStackEntry<CartFlow>() }` seguido de `hiltViewModel(parentEntry)` sem guarda — se repete **3 outras vezes no mesmo arquivo**, todas igualmente vulneráveis:
- `finishPurchaseDestination` — `CartGraph.kt:110-113`
- `productHistoryDestination` — `CartGraph.kt:205-210`
- `shareOptionsDestination` — `CartGraph.kt:229-232`

Confirmado por busca no repositório: `navController.getBackStackEntry` só aparece em `CartGraph.kt` (e no teste `core/navigation/.../NavigatorTest.kt`), ou seja, o risco está isolado a este arquivo.

### B — `Navigator.getNavController` (**já corrigido** — nenhuma ação de código necessária)

Stack trace do evento (Motorola Moto G15, app **2.2.0 (13)**, 30/out/2026 16:17:36, "1 segundo" após início de sessão):
```
Fatal Exception: java.lang.IllegalStateException: Navigator not bound to a NavController
  br.com.brunocarvalhs.howmuch.core.navigation.Navigator.getNavController (Navigator.kt:14)
  br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartGraphKt.cartDestination$lambda$0 (CartGraph.kt:60)
  ...
  br.com.brunocarvalhs.howmuch.MainActivity.CestouApp$lambda$4$18$0 (MainActivity.java:187)
```
Este é exatamente o item **C-01** já descrito e corrigido em `.specs/sdd-004-navigator-deeplink/tasks.md` (status: `implemented`), via os commits:
- `b8bb264a` (2026-10-21) — "fix: bind Navigator synchronously to avoid NavController race crash"
- `7dbd5cff` (2026-10-27) — "fix(navigation): fix Navigator crash and lost invites from deep links"

Ambos os commits entraram **antes** do corte da release 2.3.0 (`15308c36`, 2026-10-28). Os 3 eventos do Crashlytics são **todos** da versão 2.2.0 (13) — build anterior ao fix — e não há nenhum evento em 2.2.1 ou 2.3.0. O código atual de `Navigator.kt` (`core/navigation/src/main/java/.../Navigator.kt:40-44`) já documenta a corrida resolvida (comentário explicando a checagem de identidade em `unbind`), e `navigate()`/`goBack()` (linhas 52-74) já usam `navControllerOrNull` em vez da propriedade que lança.

**Conclusão: não é um crash ativo na versão atual.** O item segue listado como "Aberto" no console apenas porque ninguém o fechou manualmente lá — ação fora do escopo de código (ver §9).

### C — `JobScheduler34.forNamespace` (ainda ativo em 2.3.0 — **requer correção**)

Stack trace (evento mais recente, **Pixel 8 Pro**, app 2.3.0 (15), 29/out/2026 02:13:08):
```
Fatal Exception: java.lang.RuntimeException
Unable to get provider androidx.startup.InitializationProvider: fl1: java.lang.NoSuchMethodError: No virtual method
forNamespace(Ljava/lang/String;)Landroid/app/job/JobScheduler; in class Landroid/app/job/JobScheduler; or its super
classes (declaration of 'android.app.job.JobScheduler' appears in /system/framework/framework.jar)
  android.app.ActivityThread.installProvider (ActivityThread.java:7462)
  ...
Caused by java.lang.NoSuchMethodError: ...
  androidx.work.impl.background.systemjob.JobScheduler34.forNamespace (JobScheduler34.kt:47)
  androidx.work.impl.background.systemjob.JobSchedulerExtKt.getWmJobScheduler (JobSchedulerExt.kt:40)
  androidx.work.impl.background.systemjob.SystemJobScheduler.<init> (SystemJobScheduler.kt:80)
  androidx.work.impl.Schedulers.createBestAvailableBackgroundScheduler (Schedulers.kt:256)
  androidx.work.impl.WorkManagerImpl.initialize (WorkManagerImpl.java:214)
  androidx.work.WorkManager$Companion.initialize (WorkManager.java:203)
  androidx.work.WorkManagerInitializer.create (WorkManagerInitializer.java:39)
  androidx.startup.AppInitializer.doInitialize (AppInitializer.java:180)
  androidx.startup.InitializationProvider.onCreate (InitializationProvider.java:49)
  android.content.ContentProvider.attachInfo (ContentProvider.java:2451)
```
`androidx.work:work-runtime-ktx` (`gradle/libs.versions.toml:85`, versão `2.12.0`) é dependência real e usada — não é código morto: `feature/settings/.../data/worker/ShoppingReminderScheduler.kt` e `ShoppingReminderWorker.kt` implementam o lembrete diário de listas pendentes (`feat: implement the shopping reminder notification`, commit `9958fc05`, documentado em `architecture/04-integracoes-externas.md:73-75`).

`app/src/main/AndroidManifest.xml:63-67` mantém o `<provider android:name="androidx.startup.InitializationProvider" ... tools:node="merge">` **sem remover** o `WorkManagerInitializer` padrão. Isso faz o `WorkManager` ser inicializado automaticamente via `ContentProvider.attachInfo`, **antes de `Application.onCreate()`** — ou seja, antes de qualquer `try/catch` que o app possa rodar. Neste aparelho (Pixel 8 Pro, provavelmente build beta/QPR de Android 14+ que reporta `SDK_INT >= 34` mas cujo framework não expõe `JobScheduler.forNamespace`), a inicialização automática do `SystemJobScheduler` do WorkManager lança `NoSuchMethodError` de dentro do `ContentProvider`, derrubando o processo **no início do app, para 100% dos usuários afetados**, independente de usarem lembretes ou não. Não há `Configuration.Provider` customizado nem inicialização manual hoje — `ShoppingReminderScheduler.sync()` (linha 28) apenas chama `WorkManager.getInstance(context)`, assumindo que a inicialização automática já ocorreu.

Isto é um bug conhecido de incompatibilidade `androidx.work` + build específico de framework (não corrigível só pelo app alterando lógica de negócio); a mitigação padrão é desligar a inicialização automática e assumir controle manual e defensivo da inicialização do WorkManager.

## 2. Escopo

**Inclui:**
- **A** — Em `feature/cart/.../navigation/CartGraph.kt`, adicionar guarda de ciclo de vida antes de usar `parentEntry` (`hiltViewModel(parentEntry)`) nas 4 composables que seguem o padrão `remember(backStackEntry) { navigator.navController.getBackStackEntry<CartFlow>() }`: `cartDestination`, `finishPurchaseDestination`, `productHistoryDestination`, `shareOptionsDestination`. A composable deve encerrar a composição (ex.: `return@composable`/`return@dialog`) sem acessar `parentEntry` quando `backStackEntry.lifecycle.currentState` (ou `parentEntry.lifecycle.currentState`) já for `Lifecycle.State.DESTROYED`.
- **C** — Desligar a inicialização automática do WorkManager (remover o `WorkManagerInitializer` do `androidx.startup.InitializationProvider` em `app/src/main/AndroidManifest.xml`) e assumir inicialização manual e defensiva (try/catch) em `CestouApplication.onCreate()` (`app/src/main/java/.../CestouApplication.kt`). Blindar também o ponto de consumo em `ShoppingReminderScheduler.sync()` (`feature/settings/.../data/worker/ShoppingReminderScheduler.kt:28`) contra `WorkManager` indisponível.
- Testes unitários novos/atualizados para A e C (ver §7).
- Validação de que **B** já está corrigido em código (nenhuma mudança de produção) — registrar a conclusão aqui e recomendar fechamento manual do issue no console (§9).

**Exclui:**
- Qualquer mudança em `Navigator.kt` (issue B já resolvida; `navigate`/`goBack`/`unbind` não são tocados).
- Migração de `ShoppingReminderWorker` para `@HiltWorker`/`HiltWorkerFactory`/`Configuration.Provider` do Hilt — o commit original (`9958fc05`) optou deliberadamente por `EntryPointAccessors` para manter a inicialização padrão simples; a correção de C preserva essa decisão, apenas substituindo "automática via ContentProvider" por "manual e defensiva em `onCreate`", sem introduzir `HiltWorkerFactory`.
- Novas funcionalidades de lembrete ou mudança de UX da tela de notificações.
- Qualquer outro item do backlog de `.specs/sdd-001-auditoria-geral/tasks.md` ou de `.specs/sdd-005-crash-hardening/tasks.md` (C-02 a C-06) — fora do escopo deste plano, que é limitado aos 3 issues abertos no Crashlytics.

## 3. Impacto Arquitetural

- Módulos/camadas afetados:
  - `feature:cart` → `navigation/` (Clean Arch + MVI; nenhuma mudança em `domain`/`data`/`presentation`).
  - `:app` → `CestouApplication.kt` (entrypoint) e `AndroidManifest.xml`.
  - `feature:settings` → `data/worker/ShoppingReminderScheduler.kt`.
- Nenhum arquivo novo necessário.
- Nenhuma mudança de injeção Hilt (sem novos módulos `di/`).

```
Fluxo A (antes → depois)

composable<CartDestination> { backStackEntry }
        │
        ▼
remember(backStackEntry) { navController.getBackStackEntry<CartFlow>() }
        │
        ▼
   [ANTES] hiltViewModel(parentEntry)  ──► NavBackStackEntry.getViewModelStore()
                                             ├─ lifecycle == DESTROYED ──► IllegalStateException (CRASH)
                                             └─ lifecycle != DESTROYED ──► ok
   [DEPOIS] if (parentEntry.lifecycle.currentState == DESTROYED) return@composable
            hiltViewModel(parentEntry)  ──► sempre com lifecycle válido


Fluxo C (antes → depois)

Zygote/ActivityThread
   │
   ▼
ContentProvider.attachInfo (androidx.startup.InitializationProvider)
   │
   ▼
[ANTES] WorkManagerInitializer.create() ──► WorkManager.initialize() ──► SystemJobScheduler.<init>()
                                                                             └─ JobScheduler34.forNamespace() ──► NoSuchMethodError (CRASH, antes do onCreate)
[DEPOIS] WorkManagerInitializer removido do manifest (tools:node="remove")
   │
   ▼
Application.onCreate()
   │
   ▼
try { WorkManager.initialize(this, Configuration.Builder().build()) }
catch (t: Throwable) { Timber.e(t, ...); /* feature de lembrete fica indisponível, app não cai */ }
   │
   ▼
ShoppingReminderScheduler.sync() ──► try { WorkManager.getInstance(context) } catch (e: Exception) { return }
```

## 4. Contratos e Interfaces

Nenhum contrato público novo ou alterado. Todas as assinaturas abaixo são `internal`/privadas, sem impacto em consumidores de outros módulos.

- `CartGraph.kt` (privado ao módulo `feature:cart`, já `internal`/`private fun`): sem mudança de assinatura; apenas adição de uma checagem de guarda no corpo de cada composable afetada.
- `CestouApplication.onCreate()`: sem mudança de assinatura (`override fun onCreate()` já existe); apenas corpo adicional.
- `ShoppingReminderScheduler.sync(settings: AppSettings)`: assinatura pública da interface `ReminderScheduler` (`core:domain`) **não muda** — apenas o corpo interno ganha uma guarda defensiva.
- `AndroidManifest.xml`: remoção de um nó `<meta-data>` dentro do `<provider>` já existente — não é contrato de código, mas registrar aqui por alterar comportamento de inicialização do processo (critério do Fluxo Obrigatório, §6 do AGENTS.md, aplicável por segurança mesmo não sendo um "parâmetro público" em sentido estrito de Kotlin).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | `cartDestination`/`finishPurchaseDestination`/`productHistoryDestination`/`shareOptionsDestination` sempre chamam `hiltViewModel(parentEntry)` assim que recompostas, mesmo que a entry pai já tenha sido destruída. | As 4 composables encerram a composição sem acessar `parentEntry`/`hiltViewModel` quando `backStackEntry`/`parentEntry` já estão `DESTROYED`. | `CartGraph.kt:67-70,110-113,205-210,229-232` |
| 2 | `WorkManager` é inicializado automaticamente via `ContentProvider` (`androidx.startup.InitializationProvider`), antes de `Application.onCreate()`, sem proteção contra `NoSuchMethodError` de frameworks incompatíveis. | `WorkManagerInitializer` é removido do manifest; `WorkManager.initialize()` passa a rodar manualmente em `CestouApplication.onCreate()`, protegido por `try/catch`. Em caso de falha, o app continua normalmente e o lembrete diário fica indisponível nesse aparelho. | `app/src/main/AndroidManifest.xml:63-67`; `CestouApplication.kt:15-18` |
| 3 | `ShoppingReminderScheduler.sync()` assume `WorkManager.getInstance(context)` sempre disponível. | `sync()` captura falha de `WorkManager` indisponível e retorna silenciosamente (sem crash), preservando o restante do fluxo de configurações. | `ShoppingReminderScheduler.kt:28` |
| 4 | Issue "Navigator not bound to a NavController" (B) listado como aberto no Crashlytics. | Sem mudança de código — comportamento já corrigido por `b8bb264a`/`7dbd5cff` (`.specs/sdd-004-navigator-deeplink/tasks.md`), confirmado pelos eventos restritos à versão 2.2.0. | `core/navigation/.../Navigator.kt:23-24,40-44,52-74` |

## 6. Critérios de Aceitação

- **Performance:** as guardas adicionadas em A são leituras de `Lifecycle.State` (sem alocação, sem I/O); não introduzem recomposição extra nem efeitos colaterais. A inicialização defensiva do WorkManager em C roda uma única vez no `onCreate`, fora da hot path de composição.
- **Threading/concorrência:** `WorkManager.initialize()` em `onCreate()` é síncrono, como já é o comportamento padrão do inicializador automático — sem mudança de thread.
- **Feature toggles:** não aplicável (nenhum dos 3 itens é controlado por remote-config).
- **Observabilidade:** a falha de inicialização do WorkManager em C deve ser registrada via `Timber.e` (log local) **e** reportada como evento não fatal ao Crashlytics (confirmar API disponível em `core:analytics` antes de implementar — não logar dados pessoais, conforme AGENTS.md §5).
- **Compatibilidade:** `minSdk 28` — a guarda de `Lifecycle.State` em A já é parte de `androidx.lifecycle`, disponível em todo o range suportado. A mudança em C é especialmente relevante para dispositivos com `SDK_INT >= 34` cujo framework não implementa `JobScheduler.forNamespace` (caso observado em Pixel 8 Pro); dispositivos abaixo de API 34 não são afetados pelo bug, mas a inicialização manual deve continuar funcionando normalmente neles.
- **Wear OS:** módulo `:wear` não depende de `CartGraph.kt`, `CestouApplication.kt` nem `androidx.work` — sem impacto.

## 7. Plano de Testes

**Unitários:**
- `core/navigation` ou `feature:cart` (novo teste, local a definir na implementação): compor `cartDestination`/demais destinations com um `NavBackStackEntry` cujo `lifecycle.currentState` é forçado para `DESTROYED` (fake/mock) e verificar que `hiltViewModel`/`parentEntry` **não** são acessados (sem exceção lançada) — cenário negativo espelhando o crash A.
- Caso positivo: `lifecycle.currentState` em estado válido (`RESUMED`/`STARTED`) — comportamento inalterado, `parentEntry` acessado normalmente.
- `CestouApplication`/novo helper de inicialização do WorkManager: teste que `WorkManager.initialize()` lançando (`mockk` simulando `Throwable`) é capturado e não propaga; teste que a inicialização bem-sucedida não é afetada.
- `ShoppingReminderScheduler`: teste cobrindo `WorkManager.getInstance(context)` lançando `IllegalStateException` (não inicializado) → `sync()` retorna sem lançar; teste do caminho feliz já existente não deve regredir.

**Instrumentados / UI:**
- Não obrigatório para este fix (crashes de timing/lifecycle são difíceis de reproduzir de forma determinística em Maestro); se o maintainer preferir, um teste Maestro de "entrar e sair do carrinho rapidamente múltiplas vezes" pode ser adicionado como regressão exploratória, usando `testTag` (AGENTS.md §5), mas não é bloqueante para este plano.

**Lint/Detekt/Cobertura:**
- `./gradlew lintDebug`
- `./gradlew detekt`
- `./gradlew testDebug`
- `./gradlew koverVerify koverVerifyLayout` (mínimo 85%)

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Guarda de lifecycle em A esconder um caso legítimo de navegação (ex.: `parentEntry` temporariamente `CREATED` mas ainda válido) e a tela some sem conteúdo. | baixa | média | Guardar apenas contra `DESTROYED` (não contra `CREATED`/`STARTED`), que é o único estado em que `getViewModelStore()` de fato lança; testes unitários cobrindo os dois lados do limite. |
| Capturar `Throwable` amplamente em `CestouApplication.onCreate()` pode mascarar outros erros reais de inicialização do WorkManager, não apenas o `NoSuchMethodError` do OEM. | média | baixa | Logar a exceção completa (classe + mensagem, sem dados pessoais) via Timber + evento não fatal no Crashlytics, para que qualquer novo tipo de falha continue visível no console, só não derrube o app. |
| Remover o `WorkManagerInitializer` padrão e inicializar manualmente pode mudar timing de inicialização em caminhos não testados (ex.: processos de teste instrumentado que dependem do auto-init). | baixa | média | Rodar `connectedDebugAndroidTest` dos módulos afetados (`:app`, `:feature:settings`) antes de abrir o PR; `./gradlew testDebug` cobre os testes unitários novos. |
| Issue B ser reaberto no Crashlytics por outro usuário ainda em 2.2.0/2.2.1 após este plano ser concluído. | baixa | baixa | Nenhuma ação de código adicional necessária — monitorar; se reaparecer em 2.3.0+, tratar como regressão nova (fora deste plano). |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma pergunta bloqueante — `status: ready`.

Não bloqueantes (confirmar com o maintainer durante a implementação/review):
- [ ] Confirmar com o maintainer se deve fechar manualmente o issue B ("Navigator not bound to a NavController") no console do Crashlytics, já que o código já está corrigido desde antes da release 2.3.0.
- [ ] Confirmar em `core:analytics` o nome do método/API já existente para reportar exceção não fatal (ex.: `FirebaseCrashlytics.recordException`) a ser usado na falha defensiva de inicialização do WorkManager (C), para manter o nome canônico de evento consistente com o resto do app.
- [ ] Decidir se a captura em `CestouApplication.onCreate()` deve ser `catch (t: Throwable)` (mais amplo, cobre qualquer incompatibilidade de framework) ou restrita a `NoSuchMethodError`/`NoClassDefFoundError` (mais estreito, não esconde outros bugs de configuração do WorkManager). Recomendação deste plano: `Throwable`, pelo precedente de robustez já adotado em `.specs/sdd-005-crash-hardening/tasks.md` para falhas de origem externa/SO.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas, ver §3).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" — N/A, nenhum símbolo público muda (todos os pontos tocados são `internal`/privados).
- [x] Novo endpoint/remote-config: N/A — nenhum endpoint ou flag novo.
- [x] Novo cache: N/A — nenhum cache novo introduzido.
- [x] Perguntas em aberto exauridas — nenhuma bloqueante; 3 não bloqueantes registradas em §9 para confirmação durante a implementação.
