package br.com.brunocarvalhs.data.di

import android.content.Context
import br.com.brunocarvalhs.data.repository.ShoppingCartRepositoryImpl
import br.com.brunocarvalhs.data.services.CartLocalStorage
import br.com.brunocarvalhs.data.services.DataStorageService
import br.com.brunocarvalhs.data.services.RealtimeService
import br.com.brunocarvalhs.domain.repository.ShoppingCartRepository
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.services.IDataStorageService
import br.com.brunocarvalhs.domain.useCases.AddProductUseCase
import br.com.brunocarvalhs.domain.useCases.CreateShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.DeleteCartOfHistoryUseCase
import br.com.brunocarvalhs.domain.useCases.DeleteShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.EditProductUseCase
import br.com.brunocarvalhs.domain.useCases.EnterShoppingCartWithTokenUseCase
import br.com.brunocarvalhs.domain.useCases.FinalizePurchaseUseCase
import br.com.brunocarvalhs.domain.useCases.GetHistoryCartUseCase
import br.com.brunocarvalhs.domain.useCases.ObserveShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.RemoveProductFromCartUseCase
import br.com.brunocarvalhs.domain.useCases.ShareShoppingCartUseCase
import br.com.brunocarvalhs.domain.useCases.UpdateShoppingCartUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModuleDI {

    @Provides
    @Singleton
    fun provideRealtimeService(): RealtimeService {
        return RealtimeService()
    }

    @Provides
    @Singleton
    fun provideDatabaseService(@ApplicationContext context: Context): IDataStorageService {
        return DataStorageService(context = context)
    }

    @Provides
    @Singleton
    fun provideCartLocalStorage(dataStorageService: IDataStorageService): ICartLocalStorage {
        return CartLocalStorage(dataStorageService = dataStorageService)
    }

    @Provides
    fun provideShoppingCartRepository(service: RealtimeService): ShoppingCartRepository {
        return ShoppingCartRepositoryImpl(service)
    }

    @Provides
    fun provideAddProductToCartUseCase(repository: ShoppingCartRepository): AddProductUseCase {
        return AddProductUseCase(repository = repository)
    }

    @Provides
    fun provideCreateShoppingCartUseCase(repository: ShoppingCartRepository): CreateShoppingCartUseCase {
        return CreateShoppingCartUseCase(repository = repository)
    }

    @Provides
    fun provideDeleteShoppingCartUseCase(repository: ShoppingCartRepository): DeleteShoppingCartUseCase {
        return DeleteShoppingCartUseCase(repository = repository)
    }

    @Provides
    fun provideEditProductUseCase(repository: ShoppingCartRepository): EditProductUseCase {
        return EditProductUseCase(repository = repository)
    }

    @Provides
    fun provideEnterShoppingCartWithTokenUseCase(
        repository: ShoppingCartRepository
    ): EnterShoppingCartWithTokenUseCase {
        return EnterShoppingCartWithTokenUseCase(repository = repository)
    }

    @Provides
    fun provideRemoveProductFromCartUseCase(repository: ShoppingCartRepository): RemoveProductFromCartUseCase {
        return RemoveProductFromCartUseCase(repository = repository)
    }

    @Provides
    fun provideShareShoppingCartUseCase(repository: ShoppingCartRepository): ShareShoppingCartUseCase {
        return ShareShoppingCartUseCase(repository = repository)
    }

    @Provides
    fun provideUpdateShoppingCartUseCase(repository: ShoppingCartRepository): UpdateShoppingCartUseCase {
        return UpdateShoppingCartUseCase(repository = repository)
    }

    @Provides
    fun provideObserveShoppingCartUseCase(repository: ShoppingCartRepository): ObserveShoppingCartUseCase {
        return ObserveShoppingCartUseCase(repository)
    }

    @Provides
    fun provideGetHistoryCartUseCase(cartLocalStorage: ICartLocalStorage): GetHistoryCartUseCase {
        return GetHistoryCartUseCase(cartLocalStorage)
    }

    @Provides
    fun provideFinalizePurchaseUseCase(
        repository: ShoppingCartRepository,
        cartLocalStorage: ICartLocalStorage
    ): FinalizePurchaseUseCase {
        return FinalizePurchaseUseCase(
            shoppingCartRepository = repository,
            localStorage = cartLocalStorage
        )
    }

    @Provides
    fun provideDeleteCartOfHistoryUseCase(cartLocalStorage: ICartLocalStorage): DeleteCartOfHistoryUseCase {
        return DeleteCartOfHistoryUseCase(cartLocalStorage)
    }
}
