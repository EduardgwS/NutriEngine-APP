package com.explosionlab.nutriengine.features.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.model.DicaMacro
import com.explosionlab.nutriengine.core.model.Objetivo
import com.explosionlab.nutriengine.core.model.RecomendacaoReceita
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

//Consumo dos macronutrientes do dia
data class MacroState(
    val proteinaConsumida:    Double = 0.0,
    val carboConsumido:       Double = 0.0,
    val gorduraConsumida:     Double = 0.0,
    val proteinaMeta:         Double = 0.0,
    val carboMeta:            Double = 0.0,
    val gorduraMeta:          Double = 0.0,
) {
    val proteinaGap:  Double get() = (proteinaMeta  - proteinaConsumida).coerceAtLeast(0.0)
    val carboGap:     Double get() = (carboMeta     - carboConsumido).coerceAtLeast(0.0)
    val gorduraGap:   Double get() = (gorduraMeta   - gorduraConsumida).coerceAtLeast(0.0)

    val maiorDeficit: Int get() {
        if (proteinaMeta <= 0) return -1
        val pctProt  = proteinaGap  / proteinaMeta
        val pctCarbo = carboGap     / carboMeta.coerceAtLeast(1.0)
        val pctGord  = gorduraGap   / gorduraMeta.coerceAtLeast(1.0)
        val max      = maxOf(pctProt, pctCarbo, pctGord)
        if (max < 0.10) return -1   // menos de 10% de gap → tudo ok
        return when (max) {
            pctProt  -> 0
            pctCarbo -> 1
            else     -> 2
        }
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val healthRepo     = HealthConnectRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)

    private val _caloriasHoje         = MutableStateFlow(0.0)
    private val _caloriasRecomendadas = MutableStateFlow(0)
    private val _recomendacaoReceita  = MutableStateFlow<RecomendacaoReceita?>(null)
    private val _macroState           = MutableStateFlow(MacroState())
    private val _dicaMacro            = MutableStateFlow<DicaMacro?>(null)

    val caloriasHoje:         StateFlow<Double>               = _caloriasHoje.asStateFlow()
    val caloriasRecomendadas: StateFlow<Int>                  = _caloriasRecomendadas.asStateFlow()
    val recomendacaoReceita:  StateFlow<RecomendacaoReceita?> = _recomendacaoReceita.asStateFlow()
    val macroState:           StateFlow<MacroState>           = _macroState.asStateFlow()
    val dicaMacro:            StateFlow<DicaMacro?>           = _dicaMacro.asStateFlow()

    init {
        carregarCalorias()
        observarMudancas()
    }

    fun recarregarCaloriasHome() = carregarCalorias()

    private fun carregarCalorias() {
        viewModelScope.launch {
            try {
                val hoje   = LocalDate.now()
                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepository.carregarNome()
                )


                var kcalHoje  = 0.0
                var protHoje  = 0.0
                var carboHoje = 0.0
                var gordHoje  = 0.0

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val hc = healthRepo.lerNutricaoDia(hoje)
                    if (hc.calorias > 0) {
                        kcalHoje  = hc.calorias
                        protHoje  = hc.proteinas
                        carboHoje = hc.carboidratos
                        gordHoje  = hc.gorduras
                        consumoRepo.acumularConsumoLocal(
                            data      = hoje.toString(),
                            kcal      = hc.calorias,
                            proteinaG = hc.proteinas,
                            carboG    = hc.carboidratos,
                            gorduraG  = hc.gorduras,
                        )
                    }
                }

                if (kcalHoje <= 0) {
                    val local = consumoRepo.carregarConsumoLocal(hoje.toString())
                    kcalHoje  = local.kcal
                    protHoje  = local.proteinaG
                    carboHoje = local.carboG
                    gordHoje  = local.gorduraG
                }

                //Meta dos Macronutrientes
                val kcalMeta = perfil.caloriasRecomendadas.toDouble()
                val (pCarbo, pProt, pGord) = when (perfil.objetivo) {
                    Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
                    Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
                    Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
                }

                _caloriasHoje.value         = kcalHoje
                _caloriasRecomendadas.value = perfil.caloriasRecomendadas

                val novoMacroState = MacroState(
                    proteinaConsumida = protHoje,
                    carboConsumido    = carboHoje,
                    gorduraConsumida  = gordHoje,
                    proteinaMeta      = kcalMeta * pProt  / 4.0,
                    carboMeta         = kcalMeta * pCarbo / 4.0,
                    gorduraMeta       = kcalMeta * pGord  / 9.0,
                )
                _macroState.value = novoMacroState

                //Busca da receita do dia
                viewModelScope.launch {
                    try {
                        val response = NetworkModule.api.getReceitaDoDia(perfil.objetivo.name)
                        val receita = response.receita
                        if (receita.titulo != null && receita.descricao != null) {
                            _recomendacaoReceita.value = receita
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Erro ao buscar receita: ${e.message}")
                    }
                }

                viewModelScope.launch {
                    try {
                        val response = NetworkModule.api.getDicaMacro(
                            maiorDeficit      = novoMacroState.maiorDeficit,
                            proteinaConsumida = novoMacroState.proteinaConsumida
                        )
                        _dicaMacro.value = response.dica
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Erro ao buscar dica macro: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao carregar calorias: ${e.message}")
            }
        }
    }

    private fun observarMudancas() {
        viewModelScope.launch {
            consumoRepo.mudancas.collect {
                Log.d("HomeViewModel", "Consumo atualizado — recarregando calorias.")
                carregarCalorias()
            }
        }
    }
}