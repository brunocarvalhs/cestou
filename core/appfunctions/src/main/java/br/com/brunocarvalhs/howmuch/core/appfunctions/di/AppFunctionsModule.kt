package br.com.brunocarvalhs.howmuch.core.appfunctions.di

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppFunctionsModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
}
