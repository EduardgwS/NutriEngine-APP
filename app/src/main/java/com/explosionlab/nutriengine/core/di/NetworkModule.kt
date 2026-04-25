package com.explosionlab.nutriengine.core.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://nutriengine.explosionlab.com/"
    private val _servidorDisponivel = MutableStateFlow(true)
    val servidorDisponivel = _servidorDisponivel.asStateFlow()

    private val serverStatusInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val response = chain.proceed(request)

            // Verifica especificamente o domínio do servidor do app
            if (request.url.host == "nutriengine.explosionlab.com") {
                // Servidor está disponível se não retornar erro de servidor (5xx)
                _servidorDisponivel.value = response.code < 500
            }

            response
        } catch (e: IOException) {
            // Se falhar a conexão com o domínio específico
            if (request.url.host == "nutriengine.explosionlab.com") {
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
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: com.explosionlab.nutriengine.core.network.NutriEngineApi by lazy {
        retrofit.create(com.explosionlab.nutriengine.core.network.NutriEngineApi::class.java)
    }
}