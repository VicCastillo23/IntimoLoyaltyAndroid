package com.intimocoffee.loyalty.core.di

import android.content.Context
import com.intimocoffee.loyalty.BuildConfig
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import com.intimocoffee.loyalty.core.network.LoyaltyApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
    
    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): SessionDataStore {
        return SessionDataStore(context)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideJson(): Json = json
    
    @Provides
    @Singleton
    fun provideRetrofitProvider(client: OkHttpClient, sessionDataStore: SessionDataStore, jsonInstance: Json): RetrofitProvider {
        return RetrofitProvider(client, sessionDataStore, jsonInstance)
    }
    
    @Provides
    @Singleton
    fun provideLoyaltyApiService(retrofitProvider: RetrofitProvider): LoyaltyApiService {
        return retrofitProvider.getApiService()
    }
}

/**
 * Provides Retrofit instances that can be rebuilt when server config changes.
 */
class RetrofitProvider(
    private val client: OkHttpClient,
    private val sessionDataStore: SessionDataStore,
    private val json: Json
) {
    private var currentBaseUrl: String = ""
    private var apiService: LoyaltyApiService? = null
    
    fun getApiService(): LoyaltyApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService().also { apiService = it }
        }
    }
    
    fun rebuild(): LoyaltyApiService {
        synchronized(this) {
            apiService = null
        }
        return getApiService()
    }
    
    fun getCurrentBaseUrl(): String = currentBaseUrl
    
    private fun buildApiService(): LoyaltyApiService {
        val configured = BuildConfig.LOYALTY_API_BASE_URL.trim()
        val baseUrl = if (configured.endsWith("/")) configured else "$configured/"
        currentBaseUrl = baseUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(LoyaltyApiService::class.java)
    }
}
