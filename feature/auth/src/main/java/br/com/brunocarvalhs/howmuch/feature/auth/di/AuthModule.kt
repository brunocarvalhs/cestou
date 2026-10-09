package br.com.brunocarvalhs.howmuch.feature.auth.di

import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.feature.auth.AuthNavGraph
import br.com.brunocarvalhs.howmuch.feature.auth.data.repository.EmailAuthRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {

    @Binds
    @IntoSet
    abstract fun bindAuthNavGraph(impl: AuthNavGraph): FeatureNavGraph

    @Binds
    abstract fun bindEmailAuthRepository(impl: EmailAuthRepositoryImpl): EmailAuthRepository
}
