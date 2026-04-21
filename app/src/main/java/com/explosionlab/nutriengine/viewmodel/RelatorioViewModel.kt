package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.Perfil
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.launch

data class RelatorioUiState(
    val perfil:                 Perfil?                              = null,
    val historico7Dias:         List<ConsumoRepository.ConsumoLocal>    = emptyList(),
    val historicoCompleto7Dias: List<ConsumoRepository.ConsumoCompleto> = emptyList(),
    val carregando:             Boolean                              = true,
)

class RelatorioViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo    = AuthRepository(application)
    private val perfilRepo  = PerfilRepository(application)
    private val healthRepo  = HealthRepository(application)
    private val consumoRepo = ConsumoRepository(application)

    var state by mutableStateOf(RelatorioUiState()); private set

    init {
        carregarDados()
    }

    // ── Carregamento ──────────────────────────────────────────────────────────

    fun carregarDados() {
        viewModelScope.launch {
            state = state.copy(carregando = true)
            try {
                var pesoOverride:   Double? = null
                var alturaOverride: Double? = null

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val pesoHC   = healthRepo.lerUltimoPeso()
                    val alturaHC = healthRepo.lerUltimaAltura()

                    if (pesoHC != null || alturaHC != null) {
                        val pesoAtual   = pesoHC   ?: perfilRepo.carregarPeso()
                        val alturaAtual = alturaHC ?: perfilRepo.carregarAltura()
                        perfilRepo.salvarMedidas(pesoAtual, alturaAtual)
                        pesoOverride   = pesoAtual
                        alturaOverride = alturaAtual
                    }

                    val nutricao = healthRepo.lerNutricaoHoje()
                    if (nutricao.calorias > 0 || nutricao.proteinas > 0
                        || nutricao.carboidratos > 0 || nutricao.gorduras > 0
                    ) {
                        consumoRepo.salvarConsumoLocal(
                            data      = java.time.LocalDate.now().toString(),
                            kcal      = nutricao.calorias,
                            proteinaG = nutricao.proteinas,
                            carboG    = nutricao.carboidratos,
                            gorduraG  = nutricao.gorduras,
                        )
                    }
                }

                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepo.carregarNome(),
                    pesoOverride       = pesoOverride,
                    alturaOverride     = alturaOverride,
                )

                state = state.copy(
                    perfil                 = perfil,
                    historico7Dias         = consumoRepo.lerHistorico7Dias(),
                    historicoCompleto7Dias = consumoRepo.lerHistoricoCompleto7Dias(),
                    carregando             = false,
                )

            } catch (e: Exception) {
                val perfilFallback    = runCatching {
                    perfilRepo.carregarPerfil(nomeGoogleFallback = authRepo.carregarNome())
                }.getOrNull()
                state = state.copy(
                    perfil                 = perfilFallback,
                    historico7Dias         = runCatching { consumoRepo.lerHistorico7Dias() }.getOrElse { emptyList() },
                    historicoCompleto7Dias = runCatching { consumoRepo.lerHistoricoCompleto7Dias() }.getOrElse { emptyList() },
                    carregando             = false,
                )
            }
        }
    }

    // ── Edição — rápida (sem loading indicator) ───────────────────────────────

    private fun recarregarHistorico() {
        state = state.copy(
            historico7Dias         = consumoRepo.lerHistorico7Dias(),
            historicoCompleto7Dias = consumoRepo.lerHistoricoCompleto7Dias(),
        )
    }

    fun editarAlimento(
        data:            String,
        listaId:         String,
        alimentoIndex:   Int,
        novaQuantidadeG: Double,
    ) {
        consumoRepo.editarAlimento(data, listaId, alimentoIndex, novaQuantidadeG)
        recarregarHistorico()
    }

    fun removerAlimento(
        data:          String,
        listaId:       String,
        alimentoIndex: Int,
    ) {
        consumoRepo.removerAlimento(data, listaId, alimentoIndex)
        recarregarHistorico()
    }

    fun removerLista(data: String, listaId: String) {
        consumoRepo.removerLista(data, listaId)
        recarregarHistorico()
    }
}