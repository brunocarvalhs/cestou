package br.com.brunocarvalhs.howmuch.core.navigation

import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen

/** Converte a tela do catálogo de analytics no tipo que o [FeatureNavGraph] expõe. */
fun AnalyticsScreen.toScreenName(): ScreenName = ScreenName(id = id, screenClass = screenClass)
