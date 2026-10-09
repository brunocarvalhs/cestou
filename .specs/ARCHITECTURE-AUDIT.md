# Auditoria de arquitetura — Cestou (How-Much)

Data: 2026-10-09 · Base: `develop` @ `14ec68e3` · Objetivo: organizar o projeto para manutenção,
expansão e atualizações — renomear, refatorar e ajustar estruturas.

Respeita as decisões já registradas em `STATE.md`: AD-005 (feature plana `data/ di/ domain/
navigation/ presentation/`, implementação `internal`), AD-011 (contrato em `core`, superset na
feature) e a decisão do G10 (**sem módulos Gradle novos**; compartilhar via `core/domain`,
`core/data`, `core/ui`). Nenhuma proposta abaixo cria módulo novo.

## Números

| Métrica | Valor |
|---|---|
| Módulos | 1 app, 1 wear, 11 core, 9 feature |
| Código de produção | ~29 mil linhas Kotlin (`src/main`) |
| Maior feature | `feature/shopping` (4.399 linhas), `feature/products` (3.698), `feature/cart` (3.368) |
| Cobertura geral (gate, sem layout) | 86,1% |
| Cobertura de layout (sem previews) | 38,1% — 1.956 / 5.135 linhas |
| Problemas escondidos na baseline do detekt | 134 (57 `MagicNumber`, 29 `ImportOrdering`, 14 `MaximumLineLength`) |
| Dependências entre módulos declaradas sem nenhum import | 27 (fora `app`/`wear`, que agregam o grafo do Hilt) |

## Achados, por prioridade

### P1 — Estrutura e acoplamento (afeta tudo o que vem depois)

**1. O grafo de módulos declarado não é o real.** 27 dependências `implementation(project(...))`
em `core/*` e `feature/*` não têm um único import. Exemplos: `feature/cart` → `ai-agent`,
`settings`, `core:auth`, `core:data`; `feature/products` → `chat`, `settings`, `core:auth`,
`core:data`; `feature/shopping` → `chat`, `core:auth`, `core:data`; `feature/chat` → `ai-agent`,
`core:ui`; `feature/ai-agent` → `settings`. Efeito: build mais lento (mais recompilação em
cascata) e um mapa de acoplamento falso, que esconde o acoplamento verdadeiro.
→ Remover, compilando módulo a módulo. Risco baixo.

**2. Acoplamento real entre features** (o que sobra depois do item 1):

| Quem importa | O quê | Destino proposto |
|---|---|---|
| `cart` | `products.R` (7 arquivos usam strings do products) | strings para o próprio `cart` ou `core/ui` |
| `cart` | `Suggestions`, `ShareOptionsBottomSheet` (componentes) | `core/ui/components` |
| `cart`, `shopping` | `ProductsUseCase`, `SortProductsUseCase`, `ShoppingClearPurchasedUseCase` | contrato em `core/domain` (mesmo padrão do AD-011) |
| `cart` | `chat.domain.entity.ChatMessage` | `core/domain/model` |
| `cart` | rotas `ProductPickerRoute`, `EditShopping` | ficam como entrada de navegação "sancionada", como `settings.navigation.Settings` (G10) |
| `auth` | `settings.domain.usecase.UpdateLanguageUseCase` | contrato em `core/domain` |

Depois disso, `cart` deixa de depender de `products`/`chat` e o grafo fica: feature → core, com
exceção só de rotas de navegação.

**3. `core:common` virou depósito.** Contém inicialização de Firebase, AppCheck, Crashlytics,
sincronização com Wear (Play Services Wearable), Timber, extensões de `Context` — e quase todo
módulo depende dele. Consequência mais visível: `core:domain` depende de `core:common` (e
portanto de Firebase e Wear) **só para usar `AppException`**.
→ Mover `AppException` para `core/domain`; mover inicializadores Firebase/Crashlytics/AppCheck
para `core/data` (onde o Firebase já mora) e a sincronização Wear para `core/data`. `core:common`
fica com utilitários puros. Resultado: `core:domain` sem nenhuma dependência Android.

**4. Domínio com Android.** 7 arquivos de `domain/` importam Android:
- `shopping/ShoppingCreateUseCase`, `ShoppingDuplicateUseCase`, `ShoppingJoinUseCase` usam
  `Context` para buscar textos (título padrão da lista, sufixo "cópia", texto de notificação);
- `products/ShareShoppingUseCaseImpl` abre a tela de compartilhamento (`Intent`, `startActivity`)
  com o título "Compartilhar lista" fixo em português;
- `auth/AuthConfigUseCase` monta a configuração do FirebaseUI (SDK de interface) dentro do domínio;
- `products/ProductAnalyzeImageUseCase` recebe `Bitmap`; `chat/ChatMessage` usa `@Stable` do Compose.
→ Textos padrão entram como parâmetro (resolvidos na apresentação; `UiText` já existe em
`core/ui`); o compartilhamento vai para a apresentação; a configuração do FirebaseUI vai para `di`/`data`. Torna o domínio
testável sem Robolectric.

### P2 — Nomes e convenções

**5. "Initializer" quer dizer duas coisas.** `core/common/initializer/*`,
`core/auth/initializer/AuthInitializer` e `core/remote-config/.../RemoteConfigInitializer` são
`androidx.startup.Initializer` (boot do app). `core/navigation/FeatureInitializer` e
`feature/*/XInitializer` registram grafos de navegação. Existem **dois `AuthInitializer`** com
papéis diferentes.
Além disso, cada feature tem 3 peças para isso: interface `CartInitializer`, `CartInitializerImpl`
e o binding — mas o app só consome `Set<FeatureInitializer>` (multibinding), então a interface por
feature não serve para nada.
→ Renomear `FeatureInitializer` → `FeatureNavGraph`; cada feature fica com uma classe
(`CartNavGraph`) ligada direto com `@IntoSet`. Apaga 8 interfaces.

**6. Pacotes fora do padrão do AD-005.**
- modelos de domínio: `domain/model` (auth, products) × `domain/entity` (chat) → `domain/model`
- mapeamento: `data/mapper` (chat, shopping) × `data/extensions` (products) → `data/mapper`
- `presentation/event` só no products → dentro de `presentation/state` ou `intent`
- `navigation/mobile` + `navigation/wear` só no shopping → `navigation/` + `navigation/wear` como profile
- `feature/ai-agent` usa `src/main/kotlin`; todos os outros `src/main/java` → `src/main/java`

**7. Nomes que não dizem o que a classe é.**
- `ProductsUseCase` é um CRUD inteiro (`invoke`, `save`, `update`, `delete`, `move`) — é uma
  fachada de repositório, não um caso de uso → dividir ou renomear (`ProductCatalog`/repositório).
- `FormProduct` → `ProductForm` (padrão do resto: `ProductPhotoForm`, `EditShoppingContent`).
- `*Manager` em `core/data` (`NetworkManager`, `FirebaseFirestoreManager`, `CryptoManager`) →
  nomes pelo papel (`FirestoreClient`, `Encryptor`...).
- `ShareShoppingUseCaseImpl` — interface com uma implementação; mantida pelo AD-011 (porta entre
  features), revisar depois do item 2.
- **Não recomendado:** renomear o pacote `br.com.brunocarvalhs.howmuch` para "cestou". Custo alto
  (todos os arquivos, deep links, Firebase, `applicationId` do Play não pode mudar) e ganho só
  cosmético.

**8. AD-005 ("tudo `internal`") pouco cumprido em algumas features:** products 33 declarações
públicas de 135, shopping 22 de 94, chat 10 de 27, ai-agent 11 de 20. Boa parte some sozinha
depois do item 2 (o que é público hoje é o que outras features importam).

### P3 — Build, qualidade e higiene

**9. `build.gradle.kts` repetido em 19 módulos.** Cada um repete `compileSdk`, `minSdk`,
`JavaVersion.VERSION_11`, `testInstrumentationRunner`, plugins Compose/Hilt e o mesmo bloco de
dependências de teste. O `buildSrc` só tem convenções de detekt, kover e test-defaults.
→ Plugins de convenção `howmuch.android.library`, `howmuch.android.feature` (Compose + Hilt +
navegação + testes). Cada módulo fica com ~10 linhas. Atualizar SDK/Java vira uma linha.

**10. 20 cópias do teste de arquitetura Konsist**, uma por módulo, cada uma varrendo o projeto
inteiro, e todas checando só o primeiro nível de pacote.
→ Um único teste de arquitetura com as regras que importam: camadas (domain não importa
data/presentation/Android), feature não importa feature (exceto `navigation`), sufixos de nome
(`*UseCase` em `domain/usecase`, `*ViewModel` em `presentation/viewmodel`), `internal` em feature.
É o que impede a bagunça de voltar depois da refatoração.

**11. Baseline do detekt com 134 itens.** 43 são formatação com correção automática
(`ImportOrdering`, `MaximumLineLength`, `FinalNewline`...). `MagicNumber` (57) é a maior dívida.
→ Corrigir a formatação de uma vez; zerar `MagicNumber` por módulo.

**12. Código morto.** 7 use cases sem nenhum uso: `CommonProductAddUseCase`,
`CommonProductGetAllUseCase`, `CommonProductRemoveUseCase`, `GetProductSuggestionsUseCase`,
`GetQuestionSuggestionsUseCase`, `ProductProcessMessageUseCase`, `SharedUseCase`; mais strings
órfãs. (O conjunto morto de UI do products — QuickAdd/Search/Suggestions, ~1.000 linhas — já foi
removido na branch `test/layout-coverage-80`.)

**13. Kover não mede `core:billing` nem `feature:subscription`** — ficam fora dos dois gates.

**14. Higiene do repositório.** 14 worktrees antigas em `.claude/worktrees` (2,4 GB); 18 arquivos
de `.idea/` versionados (`gradle.xml` aparece modificado o tempo todo); textos fixos em
`Text("...")` em 20 lugares do código de produção.

## Plano de execução

Ordem pensada para cada fase deixar a seguinte mais barata e para que nenhum teste novo seja
reescrito por uma renomeação posterior. Uma PR por item numerado.

| Fase | Itens | O que muda | Risco |
|---|---|---|---|
| 0 — Limpeza | 1, 12, 13, 14 | remove deps falsas, código morto, worktrees, `.idea`; Kover passa a medir billing/subscription | baixo |
| 1 — Rede de segurança | 9, 10, 11 (formatação) | plugins de convenção; teste de arquitetura único (regras que ainda falham entram como pendência documentada) | baixo |
| 2 — Desacoplar | 3, 4, 2 | `core:common` enxuto, domínio sem Android, contratos compartilhados em `core/domain` | médio |
| 3 — Renomear | 5, 6, 7, 8 | `FeatureNavGraph`, pacotes padronizados, nomes de classes | médio (muitos arquivos, mudança mecânica) |
| 4 — Cobertura de layout | — | testes de layout até 80% e gate `koverVerifyLayout` no CI (retoma `test/layout-coverage-80`) | baixo |

Cada fase termina com `./gradlew assembleDebug testDebugUnitTest koverVerify detekt` verde e o
teste de arquitetura (fase 1) passando com as regras daquela fase ligadas.
