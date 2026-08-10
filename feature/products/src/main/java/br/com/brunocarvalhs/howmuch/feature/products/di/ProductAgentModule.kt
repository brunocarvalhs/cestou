package br.com.brunocarvalhs.howmuch.feature.products.di

import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.PriceComparisonUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductListSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.RecipeAddToListUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.RecipeSearchUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProductAgentModule {

    @Binds
    @IntoSet
    abstract fun bindProductSave(useCase: ProductSaveUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindProductSearch(useCase: ProductSearchUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindProductListSave(useCase: ProductListSaveUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindRecipeSearch(useCase: RecipeSearchUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindRecipeAddToList(useCase: RecipeAddToListUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindPriceComparison(useCase: PriceComparisonUseCase): AgentActionUseCase<*>
}
