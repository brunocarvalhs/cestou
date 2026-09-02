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
                // Módulos de DI: apenas @Binds/@Provides declarativos, sem ramificação lógica.
                classes("*.di.*")
                // Tokens de tema (cores, tipografia, shapes): configuração declarativa do Compose.
                classes("br.com.brunocarvalhs.howmuch.core.theme.*")
                // Composables de tela/componente ainda sem teste de layout dedicado via Robolectric
                // (rollout incremental cobre as telas principais nesta rodada; o restante fica para
                // uma próxima passada). ViewModels e lógica de apresentação continuam cobertos
                // normalmente — só a função @Composable em si fica de fora aqui. Inclui as telas
                // Wear (presentation.wear.screen) e os NavGraphBuilder de cada feature
                // (navigation.*Graph), que são só wiring de composable<X> { ... } sem ramificação
                // própria — mesma categoria das telas, sem teste de layout dedicado ainda.
                classes(
                    "*.presentation.screen.*",
                    "*.presentation.wear.screen.*",
                    "*.presentation.components.*",
                    "br.com.brunocarvalhs.howmuch.core.ui.components.*",
                    "*.navigation.*Graph*"
                )
                // Wrappers finos sobre APIs estáticas do Android (Play Services Wearable Data
                // Layer, AppFunctionService do SO) — mocká-las de forma significativa exige
                // Robolectric/instrumented test, não apenas JVM unit test; a maior parte das
                // linhas aqui é log em branches de erro de framework, não lógica própria.
                classes(
                    "br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncServiceImpl*",
                    "br.com.brunocarvalhs.howmuch.appfunctions.*"
                )
                // Provedores de IA que constroem o cliente do SDK (GenerativeModel / Ktor
                // HttpClient) como campo privado inline em vez de injetado — não testável sem
                // um refactor de DI (fora de escopo aqui; considerar injetar o client depois).
                classes(
                    "*.GeminiAiAgent",
                    "*.OpenRouterAiAgent"
                )
                // Composables puros de drag-and-drop (mesma categoria de presentation.screen/
                // components acima); DragTargetInfo (o state holder) continua coberto por teste.
                classes("br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragDropUtilsKt*")
            }
        }
        total {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}
