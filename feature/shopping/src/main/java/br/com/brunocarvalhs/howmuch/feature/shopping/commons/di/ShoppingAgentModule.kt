package br.com.brunocarvalhs.howmuch.feature.shopping.commons.di

import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingCreateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingDeleteUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetAllUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetDetailsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingMultiDeleteUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingReopenUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingUpdateUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShoppingAgentModule {

    @Binds
    @IntoSet
    abstract fun bindShoppingCreate(useCase: ShoppingCreateUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingUpdate(useCase: ShoppingUpdateUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingDelete(useCase: ShoppingDeleteUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingGetAll(useCase: ShoppingGetAllUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingGetById(useCase: ShoppingGetByIdUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingMultiDelete(useCase: ShoppingMultiDeleteUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingReopen(useCase: ShoppingReopenUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingClearPurchased(useCase: ShoppingClearPurchasedUseCase): AgentActionUseCase<*>

    @Binds
    @IntoSet
    abstract fun bindShoppingGetDetails(useCase: ShoppingGetDetailsUseCase): AgentActionUseCase<*>
}
