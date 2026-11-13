package br.com.brunocarvalhs.howmuch.core.ads.di

import br.com.brunocarvalhs.howmuch.core.ads.service.AdMobAiInterstitialAds
import br.com.brunocarvalhs.howmuch.core.ui.contract.AiInterstitialHook
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAiInterstitialHook(impl: AdMobAiInterstitialAds): AiInterstitialHook
}
