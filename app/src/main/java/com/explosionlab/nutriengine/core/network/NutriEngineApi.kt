package com.explosionlab.nutriengine.core.network

import com.explosionlab.nutriengine.core.model.DicaMacroResponse
import com.explosionlab.nutriengine.core.model.ReceitaResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NutriEngineApi {
    @GET("api/receita-do-dia")
    suspend fun getReceitaDoDia(
        @Query("objetivo") objetivo: String
    ): ReceitaResponse

    @GET("api/dicas-macrocard")
    suspend fun getDicaMacro(
        @Query("maior_deficit") maiorDeficit: Int,
        @Query("proteina_consumida") proteinaConsumida: Double
    ): DicaMacroResponse
}
