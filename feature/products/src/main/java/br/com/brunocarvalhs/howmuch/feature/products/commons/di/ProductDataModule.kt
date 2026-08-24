package br.com.brunocarvalhs.howmuch.feature.products.commons.di

import br.com.brunocarvalhs.howmuch.feature.products.app.data.repository.CommonProductRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.app.data.repository.ProductRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.app.data.repository.RecipeRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.CommonProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.RecipeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProductDataModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindCommonProductRepository(impl: CommonProductRepositoryImpl): CommonProductRepository
}
