package com.explosionlab.nutriengine.features.market

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
import com.explosionlab.nutriengine.core.model.Objetivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo     = AuthRepository(application)
    private val perfilRepo   = PerfilRepository(application)
    private val consumoRepo  = ConsumoRepository(application)
    private val mercadoRepo  = MercadoRepository(authRepo)

    private val prefs = application.getSharedPreferences("mercado_prefs", Context.MODE_PRIVATE)
    private val lastUpdateKey = "last_mercado_update"
    private var isFirstLoadInSession = true

    private val _recomendacoes = MutableStateFlow<List<RecomendacaoProduto>>(emptyList())
    private val _parceiros     = MutableStateFlow<List<Parceiro>>(emptyList())
    private val _carregando    = MutableStateFlow(false)
    private val _erro          = MutableStateFlow("")

    val recomendacoes: StateFlow<List<RecomendacaoProduto>> = _recomendacoes.asStateFlow()
    val parceiros:     StateFlow<List<Parceiro>>            = _parceiros.asStateFlow()
    val carregando:    StateFlow<Boolean>                   = _carregando.asStateFlow()
    val erro:          StateFlow<String>                    = _erro.asStateFlow()

    init {
        carregarDados()
    }

    fun carregarDados(force: Boolean = false) {
        val agora      = System.currentTimeMillis()
        val cincoHoras = 5 * 60 * 60 * 1000L
        val lastUpdate = prefs.getLong(lastUpdateKey, 0L)

        val deveAtualizar = force || isFirstLoadInSession || (agora - lastUpdate > cincoHoras)

        if (!deveAtualizar && _recomendacoes.value.isNotEmpty()) {
            return
        }

        isFirstLoadInSession = false

        viewModelScope.launch {
            _carregando.value = true
            _erro.value       = ""
            try {
                val jobParceiros = launch {
                    _parceiros.value = mercadoRepo.listarParceiros()
                }

                val jobRecomendacoes = launch {
                    val necessidades = identificarNecessidades()
                    _recomendacoes.value = mercadoRepo.buscarRecomendacoes(
                        necessidades = necessidades
                    )
                }

                jobParceiros.join()
                jobRecomendacoes.join()

                prefs.edit { putLong(lastUpdateKey, System.currentTimeMillis()) }

            } catch (e: Exception) {
                Log.e("MarketViewModel", "Erro: ${e.message}")
                _erro.value = "Não foi possível carregar as recomendações."
            } finally {
                _carregando.value = false
            }
        }
    }

    fun abrirProduto(urlCompra: String, context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, urlCompra.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e("MarketViewModel", "Erro ao abrir URL: ${e.message}")
        }
    }

    //Identificação de necessidades com base no perfil e consumo do dia

    private suspend fun identificarNecessidades(): List<String> {
        val perfil = perfilRepo.carregarPerfil(
            nomeGoogleFallback = authRepo.carregarNome()
        )
        val consumoHoje = consumoRepo.carregarConsumoLocal(LocalDate.now().toString())

        //Cálculo local de metas
        val kcalMeta = perfil.caloriasRecomendadas.toDouble()
        val (pCarbo, pProt, pGord) = when (perfil.objetivo) {
            Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
            Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
            Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
        }

        val protMeta  = kcalMeta * pProt  / 4.0
        val carboMeta = kcalMeta * pCarbo / 4.0
        val gordMeta  = kcalMeta * pGord  / 9.0

        val necessidades = mutableListOf<String>()

        //Cria uma necessidade se faltar +15% de uma meta
        if ((protMeta  - consumoHoje.proteinaG) > protMeta  * 0.15) necessidades.add("ALTA_PROTEINA")
        if ((carboMeta - consumoHoje.carboG)    > carboMeta * 0.15) necessidades.add("CARBOIDRATO")
        if ((gordMeta  - consumoHoje.gorduraG)  > gordMeta  * 0.15) necessidades.add("GORDURA_BOA")

        // Objetivo como tag genérica
        necessidades.add(perfil.objetivo.name)

        return necessidades
    }
}
