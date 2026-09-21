// Plugin de convenção: defaults de test task compartilhados por todos os módulos.
// Aplicado no root via `id("howmuch.test-defaults")`.
//
// Em um monorepo com rollout incremental de testes, é normal um módulo ficar temporariamente
// sem nenhum teste unitário. Sem isso, `testDebugUnitTest` falha com "did not discover any
// tests" assim que qualquer configuração (ex: isIncludeAndroidResources) força a task a rodar
// em vez de ficar NO-SOURCE — o que quebraria a esteira de CI por um motivo alheio a bugs reais.
subprojects {
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests = false
    }
}
