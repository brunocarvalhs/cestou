package br.com.brunocarvalhs.howmuch.feature.products.di

import br.com.brunocarvalhs.howmuch.feature.products.data.repository.ProductRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.data.repository.RecipeRepositoryImpl
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
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
}
