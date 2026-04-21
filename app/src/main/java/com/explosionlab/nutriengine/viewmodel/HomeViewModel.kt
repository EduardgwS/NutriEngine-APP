package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val healthRepo     = HealthRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)

    private val _caloriasHoje         = MutableStateFlow(0.0)
    private val _caloriasRecomendadas = MutableStateFlow(0)
    private val _recomendacaoReceita  = MutableStateFlow<RecomendacaoReceita?>(null)

    val caloriasHoje:         StateFlow<Double>               = _caloriasHoje.asStateFlow()
    val caloriasRecomendadas: StateFlow<Int>                  = _caloriasRecomendadas.asStateFlow()
    val recomendacaoReceita:  StateFlow<RecomendacaoReceita?> = _recomendacaoReceita.asStateFlow()

    init {
        carregarCalorias()
        observarMudancas()
    }

    fun recarregarCaloriasHome() = carregarCalorias()

    private fun carregarCalorias() {
        viewModelScope.launch {
            try {
                val hoje = LocalDate.now()
                var kcalParaExibir = 0.0

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val nutricaoHC = healthRepo.lerNutricaoDia(hoje)
                    if (nutricaoHC.calorias > 0) {
                        kcalParaExibir = nutricaoHC.calorias
                        consumoRepo.salvarConsumoLocal(
                            data      = hoje.toString(),
                            kcal      = nutricaoHC.calorias,
                            proteinaG = nutricaoHC.proteinas,
                            carboG    = nutricaoHC.carboidratos,
                            gorduraG  = nutricaoHC.gorduras,
                        )
                    }
                }

                if (kcalParaExibir <= 0) {
                    kcalParaExibir = consumoRepo.carregarConsumoLocal(hoje.toString()).kcal
                }

                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepository.carregarNome()
                )

                _caloriasHoje.value         = kcalParaExibir
                _caloriasRecomendadas.value = perfil.caloriasRecomendadas
                _recomendacaoReceita.value  = escolherReceitaDoDia(perfil.objetivo)

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