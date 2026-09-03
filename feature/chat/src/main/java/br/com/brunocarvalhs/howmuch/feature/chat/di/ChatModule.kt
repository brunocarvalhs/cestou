package br.com.brunocarvalhs.howmuch.feature.chat.di

import br.com.brunocarvalhs.howmuch.feature.chat.ChatInitializer
import br.com.brunocarvalhs.howmuch.feature.chat.ChatInitializerImpl
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatModule {

    @Binds
    @IntoSet
    abstract fun bindChatInitializer(impl: ChatInitializerImpl): FeatureInitializer
}
