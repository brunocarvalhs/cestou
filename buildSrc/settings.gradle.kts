// Permite ao buildSrc ler o catálogo de versões do projeto: versões de plugin declaradas uma vez só.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
