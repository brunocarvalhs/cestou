package br.com.brunocarvalhs.howmuch.feature.ai_agent.di

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentFactory
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.AgentFactoryImpl
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.GeminiModelFactory
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.GeminiModelFactoryImpl
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.OpenRouterHttpClientFactory
import br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.OpenRouterHttpClientFactoryImpl
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AgentSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AgentModule {

    @Binds
    @Singleton
    abstract fun bindAiAgentFactory(impl: AgentFactoryImpl): AgentFactory

    @Binds
    @Singleton
    abstract fun bindAiSession(impl: AgentSession): AiSession

    @Binds
    @Singleton
    abstract fun bindGeminiModelFactory(impl: GeminiModelFactoryImpl): GeminiModelFactory

    @Binds
    @Singleton
    abstract fun bindOpenRouterHttpClientFactory(impl: OpenRouterHttpClientFactoryImpl): OpenRouterHttpClientFactory

    companion object {
        @Provides
        @Singleton
        fun provideAiAgentSession(authService: AuthService): AgentSession {
            val userId = authService.currentUser?.id
            return AgentSession(
                userId = userId,
                locale = Locale.getDefault()
            )
        }
    }
}
