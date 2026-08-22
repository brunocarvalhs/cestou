package br.com.brunocarvalhs.howmuch.core.ai.di

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.service.AiAgentFactoryImpl
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindAiAgentFactory(impl: AiAgentFactoryImpl): AiAgentFactory

    companion object {
        @Provides
        fun provideAiAgentSession(authService: AuthService): AiAgentSession {
            val userId = authService.currentUser?.id
            return AiAgentSession(
                userId = userId,
                locale = Locale.getDefault()
            )
        }
    }
}
