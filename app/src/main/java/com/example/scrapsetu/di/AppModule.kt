package com.example.scrapsetu.di

import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.GroqRepository
import com.example.scrapsetu.data.repo.ListingRepository
import com.example.scrapsetu.data.repo.MatchRepository
import com.example.scrapsetu.data.repo.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAuthRepository(): AuthRepository = AuthRepository()

    @Provides @Singleton
    fun provideListingRepository(): ListingRepository = ListingRepository()

    @Provides @Singleton
    fun provideMatchRepository(): MatchRepository = MatchRepository()

    @Provides @Singleton
    fun provideGroqRepository(): GroqRepository = GroqRepository()

    @Provides @Singleton
    fun provideStorageRepository(): StorageRepository = StorageRepository()
}