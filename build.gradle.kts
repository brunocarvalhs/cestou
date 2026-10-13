// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Configuração de cobertura, lint estático e demais itens usados pela esteira de CI ficam
// isolados em plugins de convenção sob buildSrc/ (howmuch.*.gradle.kts) para manter este
// arquivo enxuto — veja buildSrc/src/main/kotlin/.
plugins {
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.secrets) apply false
    id("howmuch.kover")
    id("howmuch.detekt")
    id("howmuch.test-defaults")
}
