package com.explosionlab.nutriengine.features.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
import com.explosionlab.nutriengine.core.model.Perfil
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RelatorioUiState(
    val perfil:                 Perfil?                              = null,
    val historico7Dias:         List<ConsumoRepository.ConsumoLocal>    = emptyList(),
    val historicoCompleto7Dias: List<ConsumoRepository.ConsumoCompleto> = emptyList(),
    val nutricaoExternaHoje:    HealthConnectRepository.NutricaoDiaria? = null,
    val carregando:             Boolean                              = true,
)

class RelatorioViewModel(application: Application) : AndroidViewModel(application) {

    private val context     = application.applicationContext
    private val authRepo    = AuthRepository(application)
    private val perfilRepo  = PerfilRepository(application)
    private val healthRepo  = HealthConnectRepository(application)
    private val consumoRepo = ConsumoRepository(application)

    private val _state = MutableStateFlow(RelatorioUiState())
    val state: StateFlow<RelatorioUiState> = _state.asStateFlow()

    init {
        carregarDados()
        observarMudancas()
    }

    //Carregar

    fun recarregarRelatorio() = carregarDados(silencioso = true)

    private fun carregarDados(silencioso: Boolean = false) {
        viewModelScope.launch {
            if (!silencioso) {
                _state.value = _state.value.copy(carregando = true)
            }
            try {
                val (pesoHC, alturaHC) = sincronizarHealthConnect()

                val nutricaoHC = if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val myPackage = context.packageName
                    val todos = healthRepo.lerNutricaoHoje()
                    // Filtra apenas se houver fontes diferentes da nossa
                    if (todos.fontes.any { it != myPackage }) {
                        val proprio = healthRepo.lerNutricaoPropriaDia(LocalDate.now())
                        HealthConnectRepository.NutricaoDiaria(
                            calorias = todos.calorias - proprio.calorias,
                            carboidratos = todos.carboidratos - proprio.carboidratos,
                            proteinas = todos.proteinas - proprio.proteinas,
                            gorduras = todos.gorduras - proprio.gorduras,
                            fontes = todos.fontes.filter { it != myPackage }.toSet()
                        )
                    } else null
                } else null

                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepo.carregarNome(),
                    pesoOverride       = pesoHC,
                    alturaOverride     = alturaHC,
                )

                _state.value = _state.value.copy(
                    perfil                 = perfil,
                    historico7Dias         = consumoRepo.lerHistorico7Dias(),
                    historicoCompleto7Dias = consumoRepo.lerHistoricoCompleto7Dias(),
                    nutricaoExternaHoje    = nutricaoHC,
                    carregando             = false,
                )

            } catch (_: Exception) {
                tratarErroNoCarregamento()
            }
        }
    }

    private suspend fun sincronizarHealthConnect(): Pair<Double?, Double?> {
        if (!healthRepo.isDisponivel() || !healthRepo.temPermissoes()) return null to null

        val pesoHC   = healthRepo.lerUltimoPeso()
        val alturaHC = healthRepo.lerUltimaAltura()

        var pesoOverride:   Double? = null
        var alturaOverride: Double? = null

        if (pesoHC != null || alturaHC != null) {
            val pesoAtual   = pesoHC   ?: perfilRepo.carregarPeso()
            val alturaAtual = alturaHC ?: perfilRepo.carregarAltura()
            perfilRepo.salvarMedidas(pesoAtual, alturaAtual)
            pesoOverride   = pesoAtual
            alturaOverride = alturaAtual
        }

        val nutricao = healthRepo.lerNutricaoHoje()
        if (nutricao.calorias > 0 || nutricao.proteinas > 0 ||
            nutricao.carboidratos > 0 || nutricao.gorduras > 0
        ) {
            consumoRepo.salvarConsumoLocal(
                data      = LocalDate.now().toString(),
                kcal      = nutricao.calorias,
                proteinaG = nutricao.proteinas,
                carboG    = nutricao.carboidratos,
                gorduraG  = nutricao.gorduras,
            )
        }
        return pesoOverride to alturaOverride
    }

    private suspend fun tratarErroNoCarregamento() {
        val perfilFallback = runCatching {
            perfilRepo.carregarPerfil(nomeGoogleFallback = authRepo.carregarNome())
        }.getOrNull()

        _state.value = _state.value.copy(
            perfil                 = perfilFallback,
            historico7Dias         = runCatching { consumoRepo.lerHistorico7Dias() }.getOrElse { emptyList() },
            historicoCompleto7Dias = runCatching { consumoRepo.lerHistoricoCompleto7Dias() }.getOrElse { emptyList() },
            carregando             = false,
        )
    }

    private fun observarMudancas() {
        viewModelScope.launch {
            consumoRepo.mudancas.collect {
                carregarDados(silencioso = true)
            }
        }
    }

    //Edição
    fun editarAlimento(
        data:            String,
        listaId:         String,
        alimentoId:      String,
        novaQuantidadeG: Double,
    ) {
        viewModelScope.launch {
            consumoRepo.editarAlimento(data, listaId, alimentoId, novaQuantidadeG)
        }
    }

    fun removerAlimento(
        data:          String,
        listaId:       String,
        alimentoId:    String,
    ) {
        viewModelScope.launch {
            consumoRepo.removerAlimento(data, listaId, alimentoId)
        }
    }

    fun removerLista(data: String, listaId: String) {
        viewModelScope.launch {
            consumoRepo.removerLista(data, listaId)
        }
    }
}
