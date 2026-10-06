package br.com.brunocarvalhs.howmuch.feature.products.di

import br.com.brunocarvalhs.howmuch.core.domain.repository.ProductReader
import br.com.brunocarvalhs.howmuch.core.domain.services.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.products.data.repository.CommonProductRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.data.repository.ProductRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.data.repository.RecipeRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShareShoppingUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductDataModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    // AD-011: same singleton also satisfies the segregated read-only port that
    // feature/shopping and feature/cart depend on instead of the full ProductRepository.
    @Binds
    @Singleton
    abstract fun bindProductReader(impl: ProductRepositoryImpl): ProductReader

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindCommonProductRepository(impl: CommonProductRepositoryImpl): CommonProductRepository

    @Binds
    abstract fun bindShareShoppingUseCase(impl: ShareShoppingUseCaseImpl): ShareShoppingUseCase
}
