package br.com.brunocarvalhs.howmuch.feature.ai_agent.di

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.AiAgentFactoryImpl
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiAgentModule {

    @Binds
    @Singleton
    abstract fun bindAiAgentFactory(impl: AiAgentFactoryImpl): AiAgentFactory

    @Binds
    @Singleton
    abstract fun bindAiSession(impl: AiAgentSession): AiSession

    companion object {
        @Provides
        @Singleton
        fun provideAiAgentSession(authService: AuthService): AiAgentSession {
            val userId = authService.currentUser?.id
            return AiAgentSession(
                userId = userId,
                locale = Locale.getDefault()
            )
        }

        @Provides
        @Singleton
        fun provideAgentRegistry(): AgentRegistry = AgentRegistry
    }
}
