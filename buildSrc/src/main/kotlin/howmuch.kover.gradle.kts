// Plugin de convenção: mantém toda a configuração da cobertura de testes fora do
// build.gradle.kts raiz. Aplicado no root via `id("howmuch.kover")`.
//
// Cobertura agregada de todos os módulos com código de produção. baselineprofile e o
// entry point do :app ficam de fora do gate: são exercitados por Macrobenchmark/androidTest,
// não por unit test — ver .maestro/ e o módulo baselineprofile para esses fluxos.
plugins {
    id("org.jetbrains.kotlinx.kover")
}

// Cada módulo agregado precisa do plugin aplicado para publicar a variant "kover" que o
// root consome via `kover(project(":x"))` abaixo — sem isso a resolução falha por falta
// de variant compatível.
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    // Variante "layout": mesmos testes JVM da debug, reportada só sobre telas/componentes Compose
    // (ver reports.variant("layout") abaixo). optional = módulos sem build type debug só pulam.
    extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
        currentProject { createVariant("layout") { add("debug", optional = true) } }
    }
}

val LAYOUT_CLASSES = listOf(
    "*.presentation.screen.*",
    "*.presentation.wear.screen.*",
    "*.presentation.components.*",
    "br.com.brunocarvalhs.howmuch.core.ui.components.*",
    "br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragDropUtilsKt*"
)

kover {
    currentProject { createVariant("layout") {} }
}

dependencies {
    kover(project(":app"))
    kover(project(":core:common"))
    kover(project(":core:domain"))
    kover(project(":core:theme"))
    kover(project(":core:ui"))
    kover(project(":core:navigation"))
    kover(project(":core:auth"))
    kover(project(":core:data"))
    kover(project(":core:ai"))
    kover(project(":core:remote-config"))
    kover(project(":core:analytics"))
    kover(project(":feature:shopping"))
    kover(project(":feature:products"))
    kover(project(":feature:settings"))
    kover(project(":feature:auth"))
    kover(project(":feature:profile"))
    kover(project(":feature:chat"))
    kover(project(":feature:cart"))
    kover(project(":feature:ai-agent"))
}

kover {
    reports {
        filters {
            excludes {
                // Código gerado (Hilt, Room, Moshi, KSP) e boilerplate sem lógica própria.
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*_Impl",
                    "*_Impl\$*",
                    "*.BuildConfig",
                    "*.databinding.*",
                    "*Hilt_*",
                    "dagger.hilt.internal.*",
                    "hilt_aggregated_deps.*"
                )
                annotatedBy("dagger.Module", "dagger.hilt.InstallIn")
                // Funções @Preview só existem para o Android Studio; não são código do app nem testadas.
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens",
                    "androidx.wear.compose.ui.tooling.preview.WearPreviewDevices"
                )
                // Módulos de DI: apenas @Binds/@Provides declarativos, sem ramificação lógica.
                classes("*.di.*")
                // Tokens de tema (cores, tipografia, shapes): configuração declarativa do Compose.
                classes("br.com.brunocarvalhs.howmuch.core.theme.*")
                // NavGraphBuilder de cada feature (navigation.*Graph): só wiring de composable<X> { ... }
                // sem ramificação própria.
                classes("*.navigation.*Graph*")
                // Wrappers finos sobre APIs estáticas do Android (Play Services Wearable Data
                // Layer, AppFunctionService do SO) — mocká-las de forma significativa exige
                // Robolectric/instrumented test, não apenas JVM unit test; a maior parte das
                // linhas aqui é log em branches de erro de framework, não lógica própria.
                classes(
                    "br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncServiceImpl*",
                    "br.com.brunocarvalhs.howmuch.appfunctions.*"
                )
                // WorkManager.getInstance(context) é resolvido estaticamente pelo próprio
                // WorkManagerImpl (não interceptável de forma confiável via mockk em JVM puro,
                // exige WorkManagerTestInitHelper/instrumentado); ShoppingReminderWorker some com
                // NotificationManager/ActivityCompat/EntryPoints, mesma categoria dos wrappers
                // estáticos acima.
                classes(
                    "*.ShoppingReminderScheduler*",
                    "*.ShoppingReminderWorker*"
                )
                // Provedores de IA que constroem o cliente do SDK (GenerativeModel / Ktor
                // HttpClient) como campo privado inline em vez de injetado — não testável sem
                // um refactor de DI (fora de escopo aqui; considerar injetar o client depois).
                classes(
                    "*.GeminiAiAgent*",
                    "*.OpenRouterAiAgent*"
                )
                // Ponto de entrada do app: Activity que só monta o Compose root (setContent)
                // e faz roteamento de deep link, e a Application que só registra o Hilt/Timber
                // no boot — mesma categoria de presentation.screen, sem teste de layout dedicado.
                classes(
                    "br.com.brunocarvalhs.howmuch.MainActivity*",
                    "br.com.brunocarvalhs.howmuch.CestouApplication"
                )
                // androidx.startup Initializer implementations: thin bootstrap calling static
                // Firebase/Timber SDK setup on app start, no branching logic of our own worth
                // unit-testing (would just be mocking the SDK's own static initializers).
                classes("*.initializer.*")
                // ML Kit / CameraX wrappers (OCR, image analysis) — need instrumentation, not
                // unit-testable in plain JVM tests without a real Android environment.
                classes(
                    "*.MlKitImageAnalyzerService*",
                    "*.ProductImageTextRecognizer*"
                )
                // *InitializerImpl classes: by project convention (see feature-layer whitelist)
                // these only wire NavGraphBuilder.composable<X> { ... } blocks per feature, same
                // category as the already-excluded *.navigation.*Graph* wiring.
                classes("*InitializerImpl*")
            }
        }
        // Gate geral (koverVerify): lógica, sem as telas/componentes Compose — essas têm gate próprio.
        total {
            filtersAppend { excludes { classes(LAYOUT_CLASSES) } }
            verify {
                rule {
                    minBound(80)
                }
            }
        }
        // Gate de layout (koverVerifyLayout): só telas/componentes Compose, cobertos por testes de
        // layout Robolectric (JVM). Testes instrumentados (androidTest) não entram no Kover.
        variant("layout") {
            filtersAppend { includes { classes(LAYOUT_CLASSES) } }
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}
