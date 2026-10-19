package com.chughtai.criterion.di

import android.content.Context
import com.chughtai.criterion.core.content.QuranContentDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun ktor(okHttp: OkHttpClient): HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttp }
        install(ContentNegotiation) { json() }
    }

    @Provides @Singleton
    fun contentDb(@ApplicationContext ctx: Context): QuranContentDb = QuranContentDb.build(ctx)
}
