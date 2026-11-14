package br.com.brunocarvalhs.howmuch.di

import br.com.brunocarvalhs.howmuch.deeplink.DeepLinkParser
import br.com.brunocarvalhs.howmuch.deeplink.DeepLinkParserImpl
import br.com.brunocarvalhs.howmuch.session.SessionSync
import br.com.brunocarvalhs.howmuch.session.SessionSyncImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Liga cada helper do MainViewModel à sua implementação (interface -> impl). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeepLinkModule {

    @Binds
    abstract fun bindSessionSync(impl: SessionSyncImpl): SessionSync

    @Binds
    abstract fun bindDeepLinkParser(impl: DeepLinkParserImpl): DeepLinkParser
}
