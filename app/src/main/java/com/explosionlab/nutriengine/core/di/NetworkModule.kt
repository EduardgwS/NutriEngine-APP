package com.explosionlab.nutriengine.core.di

import com.explosionlab.nutriengine.core.network.NutriEngineApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkModule {
    const val BACKEND_URL = "https://nutriengine.explosionlab.com"
    private val host = BACKEND_URL.removePrefix("https://")
    private val _servidorDisponivel = MutableStateFlow(true)
    val servidorDisponivel = _servidorDisponivel.asStateFlow()

    private val serverStatusInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val response = chain.proceed(request)

            //Verifica especificamente se o domínio do aplicativo está no ar
            if (request.url.host == host) {
                _servidorDisponivel.value = response.code < 500
            }

            response
        } catch (e: IOException) {
            if (request.url.host == host) {
                _servidorDisponivel.value = false
            }
            throw e
        }
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(serverStatusInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: NutriEngineApi by lazy {
        retrofit.create(NutriEngineApi::class.java)
    }
}