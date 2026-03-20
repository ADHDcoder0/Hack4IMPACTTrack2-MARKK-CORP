package com.example.scrapsetu.di

import android.content.Context
import androidx.room.Room
import com.example.scrapsetu.BuildConfig
import com.example.scrapsetu.data.local.ScrapSetuDatabase
import com.example.scrapsetu.data.local.SessionDao
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import com.example.scrapsetu.data.repo.AuthRepository
import com.example.scrapsetu.data.repo.GroqAnalyticsRepository
import com.example.scrapsetu.data.repo.GroqRepository
import com.example.scrapsetu.data.repo.ListingRepository
import com.example.scrapsetu.data.repo.MasterDataRepository
import com.example.scrapsetu.data.repo.MatchRepository
import com.example.scrapsetu.data.repo.SmartMatchInsightRepository
import com.example.scrapsetu.data.repo.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScrapSetuDatabase =
        Room.databaseBuilder(
            context,
            ScrapSetuDatabase::class.java,
            "scrapsetu.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideSessionDao(db: ScrapSetuDatabase): SessionDao = db.sessionDao()

    @Provides @Singleton
    @Named("groq_api_key")
    fun provideGroqApiKey(): String = BuildConfig.GROQ_API_KEY

    @Provides @Singleton
    @Named("gemini_api_key")
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientProvider.client

    @Provides @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    @Provides @Singleton
    fun provideAuthRepository(sessionDao: SessionDao): AuthRepository = AuthRepository(sessionDao)

    @Provides @Singleton
    fun provideListingRepository(): ListingRepository = ListingRepository()

    @Provides @Singleton
    fun provideMatchRepository(): MatchRepository = MatchRepository()

    @Provides @Singleton
    fun provideGroqRepository(): GroqRepository = GroqRepository()

    @Provides @Singleton
    fun provideStorageRepository(): StorageRepository = StorageRepository()

    @Provides @Singleton
    fun provideMasterDataRepository(): MasterDataRepository = MasterDataRepository()

    @Provides @Singleton
    fun provideSmartMatchInsightRepository(): SmartMatchInsightRepository = SmartMatchInsightRepository()

    @Provides @Singleton
    fun provideGroqAnalyticsRepository(
        supabaseClient: SupabaseClient,
        httpClient: HttpClient,
        @Named("groq_api_key") groqApiKey: String,
        @Named("gemini_api_key") geminiApiKey: String
    ): GroqAnalyticsRepository = GroqAnalyticsRepository(
        supabase = supabaseClient,
        httpClient = httpClient,
        groqApiKey = groqApiKey,
        geminiApiKey = geminiApiKey
    )
}