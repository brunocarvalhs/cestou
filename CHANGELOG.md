# Changelog

## [2.0.1] - 2026-09-22

* Merge pull request #119 from brunocarvalhs/fix/route-type-r8-keep
* Merge branch 'develop' into fix/route-type-r8-keep
* fix(release): keep RouteType enum from R8 to fix nav crash in 2.0.0
* Merge pull request #112 from brunocarvalhs/docs/beta-feedback-triage-g17
* Merge branch 'develop' into docs/beta-feedback-triage-g17
* Merge pull request #117 from brunocarvalhs/ci/version-skip-doc-ci-only
* Merge branch 'develop' into docs/beta-feedback-triage-g17
* Merge branch 'develop' into ci/version-skip-doc-ci-only
* Merge pull request #118 from brunocarvalhs/develop
* Merge remote-tracking branch 'origin/develop' into ci/version-skip-doc-ci-only
* ci(release): widen no-bump types to match the Angular/semantic-release preset
* ci(release): skip version bump when only doc/ci commits landed
* docs(pm): triage first two Play Console beta feedback comments


## [2.0.0] - 2026-09-21

* Merge pull request #115 from brunocarvalhs/fix/release-user-fraction-escaping
* fix(release): remove broken quote-escaping in the user_fraction read
* Merge pull request #114 from brunocarvalhs/chore/sync-master-with-develop-2
* Merge pull request #113 from brunocarvalhs/fix/release-user-fraction
* fix(release): pass userFraction to the Google Play upload step
* Merge pull request #110 from brunocarvalhs/chore/sync-master-with-develop
* chore: bring PR #111 (docs/store assets) into the master-sync branch
* Merge pull request #111 from brunocarvalhs/docs/sync-specs-and-store-assets
* docs(sdd): sync .specs/, CHANGELOG and README with actual project state
* chore(store): add pt-BR/en-US/es-ES screenshots and release notes
* chore: sync master into develop history
* chore(ci): update APK size baseline to 187516134 bytes
* Merge pull request #109 from brunocarvalhs/feature/compose-previews-responsive-wear
* fix(test): update WelcomeScreen/ViewModel tests for the new constructor and legal notice
* fix(lint): satisfy detekt Filename and UnusedPrivateMember rules
* feat: ajuste de layout e mensagens
* feat(ui): add responsive mobile previews and Wear OS previews to all screens
* chore(ci): update APK size baseline to 187450022 bytes
* Merge pull request #108 from brunocarvalhs/claude/pipeline-review-merge-8qh8yr
* fix(security): install Play Integrity App Check provider in release builds
* chore(ci): update APK size baseline to 187433606 bytes
* Merge pull request #107 from brunocarvalhs/claude/pipeline-review-merge-8qh8yr
* fix(shopping): sort imports in ShoppingGraph to satisfy detekt
* feat(shopping): encode invite QR codes and shares as deep links
* fix(release): sign release builds and stop tracking the keystore
* chore(ci): update APK size baseline to 187433634 bytes
* Merge pull request #87 from brunocarvalhs/fix/welcome-screen-language-and-theme
* fix(settings): correct SettingsRepository import in unit tests
* chore(ci): update APK size baseline to 186776521 bytes
* Merge remote-tracking branch 'origin/develop' into fix/welcome-screen-language-and-theme
* fix(auth): stop asserting on the gated sign-in action in WelcomeScreenTest
* Merge pull request #86 from brunocarvalhs/design/welcome-login-hero-image
* Merge pull request #104 from brunocarvalhs/dependabot/gradle/com.firebaseui-firebase-ui-auth-10.0.0-beta05
* Merge pull request #88 from brunocarvalhs/chore/untrack-google-services-json
* fix(auth): wait for the actions sheet to compose before asserting
* fix(settings): fix import ordering in DeleteAccountUseCaseTest
* fix(auth): wrap long lines in CustomMethodPickerLayout for detekt
* Merge remote-tracking branch 'origin/develop' into design/welcome-login-hero-image
* fix(auth): adapt to firebase-ui-auth 10.0.0-beta05 breaking API changes
* Merge remote-tracking branch 'origin/develop' into dependabot/gradle/com.firebaseui-firebase-ui-auth-10.0.0-beta05
* Merge remote-tracking branch 'origin/develop' into chore/untrack-google-services-json
* Merge pull request #97 from brunocarvalhs/dependabot/github_actions/dependabot/fetch-metadata-3
* fix(auth): resolve detekt and test failures in welcome screen redesign
* Merge remote-tracking branch 'origin/develop' into fix/welcome-screen-language-and-theme
* Merge branch 'develop' into design/welcome-login-hero-image
* build(deps): bump com.firebaseui:firebase-ui-auth
* build(deps): bump org.jetbrains.kotlin:kotlin-reflect (#100)
* build(deps): bump androidx.room:room-ktx from 2.8.4 to 2.8.5 (#99)
* build(deps): bump androidx.room:room-runtime from 2.8.4 to 2.8.5 (#102)
* build(deps): bump androidx.compose:compose-bom (#105)
* build(deps): bump com.google.firebase:firebase-bom (#103)
* build(deps): bump gradle-wrapper from 9.6.1 to 9.7.1 (#95)
* build(deps): bump org.jetbrains.kotlinx:kotlinx-serialization-json (#101)
* build(deps): bump androidx.room:room-compiler from 2.8.4 to 2.8.5 (#98)
* build(deps): bump org.robolectric:robolectric from 4.16.1 to 4.17 (#96)
* build(deps): bump dependabot/fetch-metadata from 2 to 3
* Merge pull request #89 from brunocarvalhs/fix/remove-dead-firebase-buildconfig-fields
* chore(ci): update APK size baseline to 184924393 bytes
* Merge branch 'develop' into design/welcome-login-hero-image
* Merge branch 'develop' into fix/remove-dead-firebase-buildconfig-fields
* Merge pull request #94 from brunocarvalhs/fix/logout-stale-synced-id-race
* fix(ci): resolve real Detekt violations surfaced by the setup-android fix
* fix(ci): relax commitlint header-max-length to 120
* fix(ci): allow debug as a commitlint type
* fix(ci): replace flaky android-actions/setup-android with direct license acceptance
* Merge pull request #93 from brunocarvalhs/feature/ai-chat-fab
* Merge pull request #92 from brunocarvalhs/feature/product-category-picker
* Merge pull request #90 from brunocarvalhs/feature/ai-chat-fab
* feat(products): add smart category picker to Add/Edit Product, collapsible category headers
* Merge pull request #91 from brunocarvalhs/fix/cart-categorization-and-edit-list
* fix(cart): respect categorization toggle and fix broken Edit List action
* feat(chat): add AI chat entry point via FAB, WhatsApp-style UI, and fix broken OpenRouter tool calling
* feat: removendo chat de IA
* feat: shopping item adaptado
* refactor(nav): declare route protection via a RouteProtocol contract
* fix(nav): add a continuous guard against protected routes while signed out
* debug(nav): log Activity lifecycle to test the recreation hypothesis
* debug(auth): add trace logging around sign-out and back-stack changes
* fix(nav): stop NavHost from reacting to isAuthenticated after first frame
* fix(nav): clear the entire back stack on sign-out, not just push Welcome
* fix(auth): close race that lets sign-out enter the app with no user data
* fix(build): drop dead Firebase keys from .env.example
* chore: stop tracking google-services.json
* chore: stop tracking google-services.json
* fix(auth): localize sign-in terms text and provider icons
* feat(auth): redesign welcome screen with language picker and theme fixes
* feat(auth): redesenhar tela de welcome/login com logomarca própria
* build(deps): bump marocchino/sticky-pull-request-comment from 2 to 3 (#61)
* chore(ci): update APK size baseline to 183219553 bytes
* fix(shopping): persist list position on drag-reorder (#67)
* Merge pull request #85 from brunocarvalhs/docs/quality-agent-fix
* Merge branch 'develop' into docs/quality-agent-fix
* build(deps): bump org.robolectric:robolectric from 4.16 to 4.16.1 (#57)
* Merge branch 'develop' into docs/quality-agent-fix
* build(deps): bump com.squareup.okhttp3:logging-interceptor (#52)
* Merge branch 'develop' into docs/quality-agent-fix
* docs(firestore): propose real security rules for every collection (G5) — PROPOSAL, do not deploy from CI (#84)
* docs(sdd): fix android-engineer-quality Maestro handoff that never landed
* build(deps): bump androidx.appcompat:appcompat from 1.7.0 to 1.8.0 (#63)
* ci: auto-merge dependabot PRs for patch/minor updates (#66)
* build(deps): bump actions/setup-node from 4 to 7 (#62)
* build(deps): bump actions/checkout from 4 to 7 (#59)
* build(deps): bump agp from 9.1.1 to 9.4.0 (#56)
* build(deps): bump hiltWork from 1.2.0 to 1.4.0 (#54)
* build(deps): bump androidx.hilt:hilt-navigation-compose (#53)
* build(deps): bump softprops/action-gh-release from 1 to 3 (#51)
* build(deps): bump actions/download-artifact from 4 to 8 (#50)
* build(deps): bump actions/upload-artifact from 4 to 7 (#49)
* Merge pull request #83 from brunocarvalhs/fix/wire-legal-urls
* Merge branch 'develop' into fix/wire-legal-urls
* Merge pull request #81 from brunocarvalhs/docs/beta-readiness-recheck
* Merge branch 'develop' into fix/wire-legal-urls
* Merge branch 'develop' into docs/beta-readiness-recheck
* chore(ci): update APK size baseline to 181662624 bytes
* fix(legal): wire hosted Privacy Policy / Terms of Use URLs (G3)
* Merge branch 'develop' into docs/beta-readiness-recheck
* Merge pull request #82 from brunocarvalhs/fix/openrouter-key-remote-config
* fix(ai-agent): read OpenRouter API key from Remote Config with BuildConfig fallback
* Merge branch 'develop' into docs/beta-readiness-recheck
* docs(sdd): re-check beta readiness against develop
* Merge pull request #80 from brunocarvalhs/docs/wear-qa-agent
* Merge branch 'develop' into docs/wear-qa-agent
* Merge pull request #79 from brunocarvalhs/fix/profile-observe-emission
* Merge branch 'develop' into docs/wear-qa-agent
* Merge branch 'develop' into fix/profile-observe-emission
* Merge pull request #78 from brunocarvalhs/fix/cart-observe-collector-leak
* Merge branch 'develop' into docs/wear-qa-agent
* Merge branch 'develop' into fix/profile-observe-emission
* Merge branch 'develop' into fix/cart-observe-collector-leak
* Merge pull request #76 from brunocarvalhs/docs/wear-agent
* docs(sdd): add Wear OS Maestro QA specialist subagent front
* fix(profile): reconcile Firestore profile emission into ProfileViewModel state
* fix(cart): use flatMapLatest to stop CartViewModel leaking product collectors
* Merge branch 'develop' into docs/wear-agent
* Merge pull request #77 from brunocarvalhs/fix/product-search-debounce
* fix(products): debounce product search and cancel stale requests (G16)
* docs(sdd): add Wear OS specialist subagent front
* chore(ci): update APK size baseline to 181646240 bytes
* Merge pull request #65 from brunocarvalhs/ci/fix-develop-protection-token
* Merge branch 'develop' into ci/fix-develop-protection-token
* Merge pull request #72 from brunocarvalhs/test/jacoco-coverage-baseline
* Merge branch 'develop' into test/jacoco-coverage-baseline
* Merge pull request #71 from brunocarvalhs/feat/beta-analytics-instrumentation
* Merge remote-tracking branch 'origin/develop' into feat/beta-analytics-instrumentation
* Merge branch 'develop' into test/jacoco-coverage-baseline
* Merge pull request #73 from brunocarvalhs/fix/qr-join-scan-debounce
* Merge branch 'develop' into test/jacoco-coverage-baseline
* Merge branch 'develop' into fix/qr-join-scan-debounce
* Merge pull request #69 from brunocarvalhs/fix/gemini-key-remote-config
* Merge branch 'develop' into fix/qr-join-scan-debounce
* Merge branch 'develop' into test/jacoco-coverage-baseline
* Merge branch 'develop' into feat/beta-analytics-instrumentation
* Merge branch 'develop' into fix/gemini-key-remote-config
* ci: run feature/products' new androidTest suite in CI
* docs: document the G11 androidTest addition in the coverage baseline
* test(products): add real-device androidTest regression for G11 (camera executor leak)
* fix(shopping): extract magic number in debounce regression test
* fix(products): wrap long match assertion to satisfy Detekt MaximumLineLength
* fix(analytics): satisfy Detekt ImportOrdering in touched files
* Merge pull request #70 from brunocarvalhs/docs/beta-launch-planning
* fix(ai): extract test stub helpers to file scope to satisfy Detekt
* fix(shopping): debounce QR-code join scans (G13)
* test: measure real Kover baseline, cover zero-tested domain/data paths
* docs(sdd): add architecture and release-engineering subagent fronts
* fix(ai): read Gemini API key from Remote Config with BuildConfig fallback
* docs(analytics): correct BarcodeAnalyzer dead-code claim in ANALYTICS-PLAN
* docs(sdd): beta launch team, priority, analytics, KPIs and task queue
* feat(analytics): instrument beta funnel gaps and document watch plan
* Merge branch 'develop' into ci/fix-develop-protection-token
* Merge pull request #68 from brunocarvalhs/claude/app-launch-action-plan-y614t5
* fix(products): shut down scanner camera executor, log bug audit
* docs(sdd): refresh launch roadmap and fix chat settings a11y gap
* Merge branch 'develop' into ci/fix-develop-protection-token
* Merge pull request #48 from brunocarvalhs/dependabot/gradle/com.google.firebase.crashlytics-3.0.8
* fix(ci): use admin token for direct pushes to now-protected develop
* build(deps): bump com.google.firebase.crashlytics from 3.0.7 to 3.0.8
* Merge pull request #47 from brunocarvalhs/ci/restructure-pipeline-stages
* fix(ci): real-run failures from the pipeline rebuild
* ci: rebuild pipeline around Git Flow, security gates, and full config-driven step names
* ci: group build+deploy by target (app/wear), not by variant
* Merge pull request #46 from brunocarvalhs/ci/group-build-by-variant
* ci: group build+deploy by variant (debug/release), not by target
* Merge pull request #45 from brunocarvalhs/ci/consolidate-build-deploy-jobs
* ci: merge build and deploy-firebase into one job per target/variant
* Merge pull request #44 from brunocarvalhs/fix/app-firebase-app-id
* fix(ci): set app's Firebase app_id explicitly instead of relying on a secret
* Merge pull request #43 from brunocarvalhs/fix/wear-standalone-pairing
* fix(wear): give wear its own applicationId and standalone pairing protocol
* Merge pull request #42 from brunocarvalhs/fix/build-matrix-fail-fast
* fix(ci): don't cancel other build/deploy matrix jobs when one fails
* Merge pull request #41 from brunocarvalhs/fix/wear-firebase-app-id
* fix(ci): stop overriding wear's Firebase app_id with the wrong app
* Merge pull request #40 from brunocarvalhs/ci/multi-target-build-matrix
* Revert "ci: consume brunocarvalhs/mobile-ci-pipeline@v1 instead of local pipeline logic"
* ci: retrigger to test mobile-ci-pipeline access
* ci: consume brunocarvalhs/mobile-ci-pipeline@v1 instead of local pipeline logic
* ci: make pipeline-config.yaml the single source of truth for build targets and checks
* Merge pull request #39 from brunocarvalhs/fix/remove-anonymous-login
* fix: remove anonymous login and stop faking profile identity
* Merge pull request #37 from brunocarvalhs/feat/iaa-quick-add
* Merge pull request #36 from brunocarvalhs/feat/iaa-duplicate-warning
* Merge pull request #35 from brunocarvalhs/feat/iaa-attribution-ui
* Merge pull request #38 from brunocarvalhs/feat/iaa-write-points
* Merge pull request #33 from brunocarvalhs/feat/iaa-history-model
* fix(core-ui): extract Detekt MagicNumber literals in UserAvatars
* fix(products): satisfy Detekt in PR5 (ImportOrdering, line length, TooManyFunctions)
* Merge branch 'feat/iaa-duplicate-warning' into feat/iaa-quick-add
* fix(products): satisfy Detekt ImportOrdering in ProductDuplicateCheckUseCase
* Merge branch 'feat/iaa-write-points' into feat/iaa-duplicate-warning
* Merge branch 'feat/iaa-write-points' into feat/iaa-attribution-ui
* fix(cart): satisfy Detekt (ImportOrdering, MagicNumber, TooManyFunctions)
* fix(cart): satisfy Detekt ImportOrdering in EditItemViewModel/ConfirmItemViewModel
* Merge branch 'feat/iaa-write-points' into feat/iaa-attribution-ui
* Merge branch 'feat/iaa-history-model' into feat/iaa-write-points
* fix(products): extract Detekt MagicNumber timestamp literals in tests
* fix(products): 4 correctness issues from item-add-authorship review
* Merge branch 'feat/iaa-write-points' into feat/iaa-quick-add
* fix(cart): hide attribution avatar for products with no history
* Merge branch 'feat/iaa-write-points' into feat/iaa-attribution-ui
* Merge branch 'feat/iaa-history-model' into feat/iaa-write-points
* fix(products): parse malformed Product.history entries defensively
* Merge branch 'feat/iaa-duplicate-warning' into feat/iaa-quick-add
* feat(products): add Quick Add as the default item entry point (PR5)
* feat(cart,shopping): attribution avatars on item rows + history sheet (PR3)
* feat(products): warn on duplicate active items in the AI add path
* feat(products,cart): wire the three history write points (ADDED/EDITED/PURCHASED)
* feat(products): add Product.history for item add/edit/purchase attribution
* docs: add item-add-authorship spec/design/tasks and persona action plan
* Merge pull request #32 from brunocarvalhs/docs/customer-personas
* docs: add customer-personas skill with real-situation personas
* ci: simplify Firebase App Distribution by removing optional parameters and validations
* Merge pull request #30 from brunocarvalhs/feat/new-layout
* fix: deleteAccount still called the pre-StorageService dataStore API
* fix: restore core/analytics/build.gradle.kts corrupted by a git rename-detection glitch during the merge
* Merge remote-tracking branch 'origin/develop' into feat/new-layout
* Merge pull request #15 from brunocarvalhs/feat/shopping-reminder-notifications
* Merge pull request #14 from brunocarvalhs/feat/shared-storage-service
* Merge pull request #20 from brunocarvalhs/feat/shopping-list-notifications
* fix: exclude ShoppingReminderScheduler/Worker from the coverage gate
* Merge pull request #18 from brunocarvalhs/feat/account-deletion
* Merge remote-tracking branch 'origin/feat/new-layout' into feat/shared-storage-service
* fix: correct import ordering in ShoppingJoinUseCase (Detekt)
* test: add unit tests for DataStoreStorageService to fix the coverage gate
* Merge remote-tracking branch 'origin/feat/new-layout' into feat/account-deletion
* Merge pull request #16 from brunocarvalhs/test/maestro-regression-suite
* fix: update SettingsRepositoryImplTest for the new reminderScheduler dependency
* Merge pull request #21 from brunocarvalhs/docs/hosted-privacy-terms
* Merge pull request #19 from brunocarvalhs/chore/remove-dead-social-auth-stubs
* Merge pull request #22 from brunocarvalhs/docs/store-listing-assets
* Merge pull request #17 from brunocarvalhs/docs/mvp-roadmap
* Merge pull request #31 from brunocarvalhs/feat/shopping-list-reliable-refresh
* fix: correct import ordering in FinishPurchaseViewModel (Detekt)
* fix: extract magic numbers in ShoppingReminderScheduler and add wear POST_NOTIFICATIONS permission
* Merge remote-tracking branch 'origin/feat/new-layout' into feat/account-deletion
* Merge remote-tracking branch 'origin/feat/new-layout' into feat/shared-storage-service
* Merge branch 'feat/new-layout' into docs/store-listing-assets
* Merge branch 'feat/new-layout' into docs/hosted-privacy-terms
* Merge branch 'feat/new-layout' into feat/shopping-list-notifications
* Merge branch 'feat/new-layout' into chore/remove-dead-social-auth-stubs
* Merge branch 'feat/new-layout' into docs/mvp-roadmap
* Merge branch 'feat/new-layout' into test/maestro-regression-suite
* Merge branch 'feat/new-layout' into feat/shopping-reminder-notifications
* Merge pull request #28 from brunocarvalhs/feat/shopping-emoji-picker
* Merge pull request #29 from brunocarvalhs/fix/cart-detail-header-real-data
* Merge pull request #27 from brunocarvalhs/feat/shopping-list-reliable-refresh
* Merge pull request #26 from brunocarvalhs/fix/firestore-shoppingmodel-serializable
* Merge pull request #25 from brunocarvalhs/fix/surface-create-shopping-errors
* Merge remote-tracking branch 'origin/feat/new-layout' into HEAD
* Merge pull request #23 from brunocarvalhs/fix/bottom-nav-fixed-no-flicker
* Merge remote-tracking branch 'origin/feat/new-layout' into HEAD
* Merge pull request #24 from brunocarvalhs/fix/cart-bottombar-nav-inset
* fix: cart detail header shows the real shopping description and emoji
* feat: let users pick an emoji when creating a shopping list
* fix: import ordering in ShoppingScreen (Detekt)
* fix: import ordering in CestouBottomNavigation (Detekt)
* fix: pre-existing Lint errors blocking CI (translations, manifest, locale)
* fix: refresh shopping list reliably on resume and pull-to-refresh
* fix: shopping lists silently vanishing due to missing @Serializable
* fix: pre-existing Lint errors blocking CI (translations, manifest, locale)
* fix: pre-existing Lint errors blocking CI (translations, manifest, locale)
* fix: pre-existing Lint errors blocking CI (translations, manifest, locale)
* build: exclude more framework-entry-point wiring from the Kover gate
* test: add coverage for NetworkManager, FirebaseFirestoreManager, NetworkLogger
* test: add coverage for PairingViewModel and ShoppingDetailViewModel (wear)
* test: add coverage for feature/products data layer
* build: extend Kover exclude filter to non-unit-testable framework wrappers
* test: add coverage for ai-agent service and core/ui utils/entity/dragdrop
* test: add coverage for ShoppingRepositoryImpl and SettingsRepositoryImpl
* build: exclude more framework-entry-point wiring from the Kover gate
* build: exclude more framework-entry-point wiring from the Kover gate
* test: add coverage for NetworkManager, FirebaseFirestoreManager, NetworkLogger
* test: add coverage for NetworkManager, FirebaseFirestoreManager, NetworkLogger
* test: add coverage for PairingViewModel and ShoppingDetailViewModel (wear)
* test: add coverage for PairingViewModel and ShoppingDetailViewModel (wear)
* test: add coverage for feature/products data layer
* test: add coverage for feature/products data layer
* build: extend Kover exclude filter to non-unit-testable framework wrappers
* build: extend Kover exclude filter to non-unit-testable framework wrappers
* test: add coverage for ai-agent service and core/ui utils/entity/dragdrop
* test: add coverage for ai-agent service and core/ui utils/entity/dragdrop
* test: add coverage for ShoppingRepositoryImpl and SettingsRepositoryImpl
* test: add coverage for ShoppingRepositoryImpl and SettingsRepositoryImpl
* build: exclude more framework-entry-point wiring from the Kover gate
* test: add coverage for NetworkManager, FirebaseFirestoreManager, NetworkLogger
* test: add coverage for PairingViewModel and ShoppingDetailViewModel (wear)
* test: add coverage for feature/products data layer
* build: extend Kover exclude filter to non-unit-testable framework wrappers
* test: add coverage for ai-agent service and core/ui utils/entity/dragdrop
* test: add coverage for ShoppingRepositoryImpl and SettingsRepositoryImpl
* chore: trigger CI
* chore: trigger CI
* chore: trigger CI
* fix: use LocalLocale instead of Locale.getDefault() in CestouCategoryHeader
* fix: use LocalLocale instead of Locale.getDefault() in CestouCategoryHeader
* fix: use LocalLocale instead of Locale.getDefault() in CestouCategoryHeader
* fix: use LocalLocale instead of Locale.getDefault() in CestouCategoryHeader
* ci: provision wear/google-services.json in CI setup
* ci: provision wear/google-services.json in CI setup
* ci: provision wear/google-services.json in CI setup
* ci: provision wear/google-services.json in CI setup
* fix: surface shopping list creation failures instead of failing silently
* fix: reserve system navigation bar inset in cart bottom bar
* fix: keep bottom nav mounted and stop it flickering across navigation
* docs: mark Phase 0-2 roadmap items done with their PR numbers
* docs: draft Play Store listing descriptions (en/pt-BR/es)
* docs: draft hosted Privacy Policy and Terms of Use pages
* feat: notify other list members on join and finish
* chore: remove dead Google/Apple sign-in stubs
* feat: add in-app account and data deletion
* docs: add MVP roadmap with gap list and phased plan
* test: add a modular Maestro regression suite
* feat: implement the shopping reminder notification
* feat: add shared StorageService and migrate core:auth to it
* test: adiciona testes de arquitetura Konsist por módulo
* refactor: flatten feature package structures and centralize navigation routes
* feat: refactor navigation routes and implement Wear OS shopping detail
* feat: vinculo de relogio com a conta
* feat: rotas globais
* feat: rota de compartilhar com usuario
* feat: limpando funções não utilizadas
* feat: removendo unidade de medida
* feat: suporte a wear
* feat: suporte a wear
* feat: suporte a wear
* refactor: removendo parte de unit dos produtos
* feat: refatorando products
* feat: refatorando products
* feat: aumento de cobertura de testes
* feat: remote config e analytics
* feat: remote config
* feat: implementando AppFuctions
* ci: preview implementado
* ci: preview implementado
* feat: criando preview do app
* feat: modularizando as funcionalidades
* chore: fix .gitignore to cover module build dirs and stop tracking build artifacts
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: modularizando as funcionalidades
* feat: ajustando o layout para lista de cart
* feat: recriando tela nova
* feat: recriando tela nova
* feat: recriando tela nova
* feat: new layout abordagem
* ci: improve Firebase App Distribution reliability and fix notification logic
* ci: improve Firebase App Distribution reliability and fix notification logic
* ci: rename CONFIG.md to README.md and update artifact names in pipeline config
* ci: rename CONFIG.md to README.md and update artifact names in pipeline config
* ci: simplify yq quoting syntax in github workflows
* ci: simplify yq quoting syntax in github workflows
* ci: update actions versions to v4 and fix yq selectors for hyphenated keys
* ci: update actions versions to v4 and fix yq selectors for hyphenated keys
* ci: implement configurable Java distribution and enhanced Firebase App Distribution
* ci: implement configurable Java distribution and enhanced Firebase App Distribution
* ci: centralizando configuração do pipeline e modernizando templates do repositório
* ci: centralizando configuração do pipeline e modernizando templates do repositório
* ci: refactor pipeline configuration and update GitHub workflows
* ci: refactor pipeline configuration and update GitHub workflows
* ci: migrando autenticação do Firebase App Distribution para Service Account
* ci: migrando autenticação do Firebase App Distribution para Service Account
* ci: update Firebase deployment to use service account credentials
* ci: update Firebase deployment to use service account credentials
* chore: ignore .artifacts directory
* chore: ignore .artifacts directory
* Merge pull request #13 from brunocarvalhs/new-app
* Merge pull request #13 from brunocarvalhs/new-app
* ci: update pipeline configuration and add release keystore
* ci: update pipeline configuration and add release keystore
* Merge pull request #12 from brunocarvalhs/new-app
* Merge pull request #12 from brunocarvalhs/new-app
* test: atualiza ShoppingCreateUseCaseTest com dependência de Context
* test: atualiza ShoppingCreateUseCaseTest com dependência de Context
* feat: implementa internacionalização e refatora filtros de listas de compras
* feat: implementa internacionalização e refatora filtros de listas de compras
* feat: adiciona traduções para ações comuns e descrições de conteúdo em espanhol e português
* feat: adiciona traduções para ações comuns e descrições de conteúdo em espanhol e português
* refactor: improve shopping feature architecture and code consistency
* refactor: improve shopping feature architecture and code consistency
* refactor: migra para jakarta.inject e refatora telas de configurações
* refactor: migra para jakarta.inject e refatora telas de configurações
* refactor: modularize startup initializers and optimize feature architectures
* refactor: modularize startup initializers and optimize feature architectures
* test: refatorando ShoppingMapperTest para utilizar constantes
* test: refatorando ShoppingMapperTest para utilizar constantes
* ci: remove workspace artifacts and perform setup in individual jobs
* ci: remove workspace artifacts and perform setup in individual jobs
* ci: remove workspace artifacts and perform setup in individual jobs
* ci: remove workspace artifacts and perform setup in individual jobs
* refactor: centralize CI/CD configuration and standardize setup environment
* refactor: centralize CI/CD configuration and standardize setup environment
* Revert "ci: consolidate CI workflows and implement DangerJS report parsing"
* Revert "ci: consolidate CI workflows and implement DangerJS report parsing"
* Revert "feat: centralize CI/CD configuration and implement agnostic pipeline framework"
* Revert "feat: centralize CI/CD configuration and implement agnostic pipeline framework"
* feat: centralize CI/CD configuration and implement agnostic pipeline framework
* feat: centralize CI/CD configuration and implement agnostic pipeline framework
* ci: consolidate CI workflows and implement DangerJS report parsing
* ci: consolidate CI workflows and implement DangerJS report parsing
* chore: adiciona logs de sucesso na execução dos steps do Danger
* chore: adiciona logs de sucesso na execução dos steps do Danger
* feat: aprimora automação do Danger JS e refatora verificações
* feat: aprimora automação do Danger JS e refatora verificações
* test: implementando testes unitários e configurando suporte a .env no CI
* test: implementando testes unitários e configurando suporte a .env no CI
* Merge remote-tracking branch 'origin/new-app' into new-app
* Merge remote-tracking branch 'origin/new-app' into new-app
* BREAKING CHANGE: new app
* BREAKING CHANGE: new app
* new app
* new app
* doc: removendo impeditivo de envio para versão interna
* Merge pull request #10 from brunocarvalhs/fix/shared_cart
* feat: nova estrutura de dangerfile
* Revert "feat: incluindo novos steps de qualidade no dangerfile.js"
* feat: incluindo novos steps de qualidade no dangerfile.js
* fix: corrigindo modulação do dangerfile.js
* fix: corrigindo modulação do dangerfile.js
* fix: revertendo problema do github actions
* fix: corrigir problema de flow
* versão 1.3.0
* Merge pull request #3 from brunocarvalhs/develop
* fix: versão 1.0.1
* Merge pull request #2 from brunocarvalhs/fix/data_class_ofuscated
* fix: incluindo keep para evitar quebra de dados no models após ofuscação
* feat: ajuste de build
* feat: assinatura do aplicativo
* feat: assinatura do aplicativo
* feat: removendo wear
* feat: criando release.keystore na esteira
* feat: ajuste de build
* feat: configurando google-service
* Merge branch 'develop' of https://github.com/brunocarvalhs/How-Much into develop
* feat: configurando ofuscação a assinatura do APK
* Criar README.md


## [Unreleased] - 2026-09-21

`master` synced with `develop` (PR #110) — see that PR for the full commit-by-commit list. Summary
of everything since the `[1.4.0]` entry below actually landed on `develop`:

### Added
- **feat**: AI chat FAB with WhatsApp-style UI and OpenRouter tool-calling fixes.
- **feat**: Product attribution (`Product.history`) for add/edit/purchase, with an attribution
  avatar and history bottom sheet in the cart.
- **feat**: Quick Add as the default product entry point; smart category picker with collapsible
  category headers.
- **feat**: Shopping list preview before creation, checkout with a fixed price, purchase-history
  reuse, drag-to-reorder position persisted.
- **feat**: Shopping invites/shares now encode as deep links (QR + code join).
- **feat**: Responsive mobile previews and Wear OS previews added to every Compose screen.
- **feat(wear)**: standalone Wear OS pairing protocol with its own Firebase `applicationId`.

### Changed
- **auth**: removed anonymous login and fake profile identity; redesigned welcome/login screen
  with its own wordmark, language picker, and dark-mode fixes; closed a logout race condition.
- **ai**: Gemini/OpenRouter API keys now read from Firebase Remote Config with a `BuildConfig`
  fallback, so they can be rotated without shipping a release.
- **ci**: pipeline rebuilt around Git Flow and `pipeline-config.yaml`-driven build targets, checks,
  and step order; build + Firebase-deploy jobs merged and grouped by variant; Dependabot auto-merge
  for patch/minor bumps.
- **deps**: ~25 routine dependency bumps (Compose BOM, AGP, Firebase BOM, Room, Kotlin, GitHub
  Actions, etc.).

### Fixed
- **shopping**: debounced QR-code list-join scans; product search debounced with stale-request
  cancellation; cart no longer leaks duplicate product collectors; cart respects the categorization
  toggle and the "Edit List" action works again.
- **settings**: profile screen reconciles the Firestore emission correctly; hosted Privacy
  Policy / Terms of Use URLs wired in.

### Security
- **release**: release keystore stopped being tracked in git; release builds now sign from a
  CI-provided keystore; Play Integrity App Check provider installed in release builds; dead
  Firebase keys dropped from `.env.example`; `google-services.json` stopped being tracked.
- **firestore**: security rules drafted for every collection (join/escalation/notification-forgery
  cases covered) — **proposed only, not yet deployed** to the Firebase console (see AD-009 in
  `.specs/STATE.md`).

### Quality
- **test**: real Kover coverage baseline established; zero-coverage gaps closed in `CloudNetwork`,
  four `products` use cases, and `core/auth.authState`; first library-module `androidTest` added.
- **analytics**: beta funnel instrumented end-to-end.
- **sdd**: beta launch plan, KPIs, persona-based action plan, and Maestro QA coverage notes added
  to `.specs/`.

## [1.4.0] - 2026-08-21

### Changed
- **arch**: Full module refactoring following the Domain-Centric Multi-module Anatomy.
- **arch**: Introduced `FeatureInitializer` pattern for decoupled navigation registration.
- **arch**: Standardized feature module structure with `app/` and `commons/` taxonomy.
- **core**: Renamed `entity` to `model` and `service` to `services` in `:core:domain`.
- **core**: Renamed `mapper` to `extensions` in `:core:data`.
- **sdd**: Initialized Spec-Driven Development (SDD) infrastructure in `.specs/`.
- **sdd**: Reverse-engineered formal specifications for `products`, `shopping`, `settings`, and `ai`.

## [1.3.0] - 2025-10-21

* feat: detekt ajustes
* feat: organizando rotas para cada classe responsável
* feat: importa lista de compra
* fix: processo de captura dos dados e depois compartilhar
* feat: incluindo permissão de assinatura
* feat: novo fluxo de pipeline
* feat: novo fluxo de pipeline
* feat: testando nova pipeline mais performatico
* feat: testando nova pipeline mais performatico
* fix: voltando a versão estavel da esteira
* fix: corrigindo problema de envio para o app Prepare release notes
* fix: incluindo código para captura do changelog
* fix: fluxo de geração de versão automatico


## [1.2.2] - 2025-10-21

* doc: ajuste de validação de fluxo
* doc: ajuste de validação de fluxo
* doc: ajuste de validação de fluxo
* doc: removendo arquivos não usados
* doc: removendo arquivos não usados
* chore(release): Prepare release 1.2.1

## [1.2.1] - 2025-10-21

* fix: adaptação de version automatico

## [1.2.0] - 2025-10-21

* fix: ajuste de script para diretorio do version.sh
* Merge remote-tracking branch 'origin/develop' into develop
* fix: migrando fluxo de versão para debug ao invés de release para seguir fluxo de versão
* Merge pull request #9 from brunocarvalhs/feature/refactor
* feat: implementando tradução para cada idioma suportado
* fix: ajuste de detekt
* feat: fluxo de baseline profiler para melhorar abertura do app
* feat: centralizando calculo de produtos
* feat: criando detalhe do historico
* fix: ajuste do detekt
* fix: modo desenvolvedor para controle de assinatura
* fix: modelo de plano por assinatura
* fix: preview do layout corrigido
* feat: criação de plano para suporte a backend
* fix: corrigindo delay de retry
* fix: removendo crash do componente InputCode
* fix: removendo crash de quantidade de caixas
* Merge pull request #8 from brunocarvalhs/feature/refactor
* feat: processo de gerar release automaticamente
* feat: refazendo observabilidade teste case
* feat: refazendo observabilidade teste case
* fix: removendo regressivo na pipe por bug do emulador
* fix: removendo navController não utilizado
* fix: ajuste de detekt apontados no app
* fix: ajuste de detekt apontados no domain e data
* fix: refatorando o regressivo para emulador
* feat: refatorando o regressivo
* feat: refatorando o regressivo
* feat: ajuste do regressivo
* feat: ajuste do regressivo
* feat: ajuste de bug da atualização de layout
* feat: regressivo criado em esteira
* fix: editor de produtos da lista
* fix: refazendo processo de remover e alterar quantidade do item
* feat: regressivo implementado para lista de compra
* feat: regressivo implementado enter code
* feat: regressivo implementado enter code e shared
* feat: regressivo implementado
* feat: statusbar color
* feat: limit de caixa padrão
* feat: layout responsivo e melhor distribuido
* feat: mudando conceito de layout da lista
* feat: invertendo ordem da lista
* feat: tela de editar produtos
* feat: migrando captura de dados via SavedStateHandle
* feat: refazendo layout para finalizar compra
* feat: removendo imports não usados
* feat: refatorar o usecase e simplificar o viewModel
* feat: mudando de lugar o usecases
* Revert "feat: implementando room"
* Revert "feat: refatorando useCases"
* Revert "feat: cobertura de testes"
* Revert "feat: novo usecases"
* feat: novo usecases
* feat: cobertura de testes
* feat: refatorando useCases
* feat: implementando room
* feat: simplificando parte de history
* feat: formulario para lista de compras
* feat: refatorando mensagem de lista
* feat: refatorando fluxo de compartilhamento de token e lista de compra
* feat: refatorando input code layout
* feat: refatorando input code
* feat: refatorando layout de produtos
* feat: refatorando layout para dividir responsabilidades na parte de produto
* feat: refatorando layout para dividir responsabilidades na parte de finalizar carrinho
* feat: refatorando layout para dividir responsabilidades
* feat: configurando o componente para bottom sheet via navigation
* feat: implementando bottom sheet via navigation
* feat: CONFIG.md renomeando
* feat: README.md criado
* Merge pull request #5 from brunocarvalhs/feature/checkout
* feat: checkout com preço estimado definido
* feat: checkout com preço estimado definido
* feat: checkout com preço estimado definido
* Merge pull request #4 from brunocarvalhs/feature/list-product
* feat: suporte em multiplos idiomas
* feat: suporte em multiplos idiomas
* feat: atualizando texto
* feat: removendo testes de UI sem suporte a Hilt
* feat: ajuste de tela
* feat: isIncludeAndroidResources implementation
* feat: ajuste de teste unitário
* feat: ajuste de preview além do detekt
* fix: ajuste de statusbar
* feat: icones das tabs
* feat: ajuste de detekt
* feat: versão 1.1.0
* feat: salvando lista de compra
* feat: filtro de produtos por lista
* feat: shopping list logica de card
* feat: incluindo fluxo de criar lista de compra
* feat: implementando indicador de card para facilitar identificação de funcionalidade
* feat: implementando detekt e ajustando apontamentos
* feat: implementando detekt e ajustando apontamentos
* feat: ajuste da pipeline
* Merge branch 'master' into develop
* Update release.yml
* Update build.gradle.kts
* Update build.gradle.kts
* Update release.yml

## [1.1.0]
- **feat**: Salvando lista de compra
- **feat**: Filtro de produtos por lista
- **feat**: Lógica de card da lista de compras
- **feat**: Incluindo fluxo de criar lista de compra
- **feat**: Implementando indicador de card para facilitar identificação de funcionalidade
- **feat**: Implementando detekt e ajustando apontamentos
- **feat**: Ajuste da pipeline de release

## [1.0.1]
- **fix**: Incluindo keep para evitar quebra de dados no models após ofuscação
- **feat**: Assinatura do aplicativo e configuração do `release.keystore`
- **feat**: Configurando ofuscação e `google-service`
- **feat**: Track de eventos para performance e analytics
- **feat**: Definição de cores, ícone e inicialização dos SDKs de analytics
- **feat**: Ajustes de layout para eventos nulos e interação do cliente na lista
- **feat**: Criação de cobertura de testes e refatoração de código
- **feat**: Documentando e configurando mínimo SDK
- **feat**: MVP da aplicação
- **chore**: Criação do README.md e commit inicial
