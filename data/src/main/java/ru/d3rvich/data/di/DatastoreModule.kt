package ru.d3rvich.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.d3rvich.datastore.JetGamesPreferencesDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Provides
    @Singleton
    fun providePreferencesDatastore(@ApplicationContext context: Context): JetGamesPreferencesDataStore {
        return JetGamesPreferencesDataStore(context = context)
    }
}