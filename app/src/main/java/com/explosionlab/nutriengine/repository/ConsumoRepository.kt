package com.explosionlab.nutriengine.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Backup local do consumo diário em SharedPreferences.
 *
 * Cada vez que o usuário fecha uma lista na aba Pesquisar, uma [ListaSalva]
 * é criada com ID único. Assim o histórico preserva a granularidade de
 * "refeição por refeição" dentro de um mesmo dia.
 *
 * [AlimentoSalvo] armazena os valores *por 100 g* como base; a quantidade
 * em gramas ([quantidadeG]) é o único campo editável — os macros escalonados
 * são calculados sob demanda via propriedades computadas.
 */
class ConsumoRepository(context: Context) {

    private val TAG   = "ConsumoRepository"
    private val prefs = context.getSharedPreferences("nutricart_consumo", Context.MODE_PRIVATE)

    private val _mudancas = MutableSharedFlow<Unit>(replay = 1)
    val mudancas: SharedFlow<Unit> = _mudancas.asSharedFlow()

    // ── Chaves ────────────────────────────────────────────────────────────────

    private fun keyKcal(data: String)       = "consumo_${data}_kcal"
    private fun keyProteina(data: String)   = "consumo_${data}_proteina"
    private fun keyCarbo(data: String)      = "consumo_${data}_carbo"
    private fun keyGordura(data: String)    = "consumo_${data}_gordura"
    private fun keyAtualizado(data: String) = "consumo_${data}_ts"
    private fun keyListas(data: String)     = "listas_${data}"

    // ── Modelos ───────────────────────────────────────────────────────────────

    /**
     * Alimento persistido com valores nutricionais *por 100 g*.
     * Alterar [quantidadeG] recalcula automaticamente kcal, proteínas, etc.
     */
    data class AlimentoSalvo(
        val id:                  String,
        val descricao:           String,
        val categoria:           String,
        val quantidadeG:         Double,
        val kcalPer100g:         Double,
        val proteinasPer100g:    Double,
        val carboidratosPer100g: Double,
        val gordurasPer100g:     Double,
    ) {
        private val fator: Double        get() = quantidadeG / 100.0
        val kcal:         Double         get() = kcalPer100g         * fator
        val proteinas:    Double         get() = proteinasPer100g    * fator
        val carboidratos: Double         get() = carboidratosPer100g * fator
        val gorduras:     Double         get() = gordurasPer100g     * fator

        fun comQuantidade(g: Double) = copy(quantidadeG = g.coerceAtLeast(0.1))
    }

    /**
     * Uma lista salva — equivale a uma refeição ou lanche.
     * Cada [ListaSalva] tem ID único e horário de registro para exibição.
     */
    data class ListaSalva(
        val id:        String,
        val timestamp: Long,
        val horaTexto: String,
        val alimentos: List<AlimentoSalvo>,
    ) {
        val totalKcal:         Double get() = alimentos.sumOf { it.kcal }
        val totalProteinas:    Double get() = alimentos.sumOf { it.proteinas }
        val totalCarboidratos: Double get() = alimentos.sumOf { it.carboidratos }
        val totalGorduras:     Double get() = alimentos.sumOf { it.gorduras }
    }

    data class ConsumoLocal(
        val data:         String,
        val kcal:         Double,
        val proteinaG:    Double,
        val carboG:       Double,
        val gorduraG:     Double,
        val atualizadoEm: Long,
    )

    data class ConsumoCompleto(
        val consumo: ConsumoLocal,
        val listas:  List<ListaSalva>,
    )

    // ── Listas — escrita ──────────────────────────────────────────────────────

    fun salvarListaDeAlimentos(
        data:      String = LocalDate.now().toString(),
        alimentos: List<AlimentoSalvo>,
    ) {
        if (alimentos.isEmpty()) return
        val hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val nova  = ListaSalva(
            id        = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            horaTexto = hora,
            alimentos = alimentos,
        )
        val existentes = carregarListas(data).toMutableList()
        existentes.add(nova)
        persistirListas(data, existentes)
        recalcularTotais(data, existentes)
        Log.d(TAG, "Lista salva — $data $hora: ${alimentos.size} itens")
    }

    // ── Listas — edição ───────────────────────────────────────────────────────

    fun editarAlimento(
        data:            String,
        listaId:         String,
        alimentoIndex:   Int,
        novaQuantidadeG: Double,
    ) {
        val listas   = carregarListas(data).toMutableList()
        val listaIdx = listas.indexOfFirst { it.id == listaId }
        if (listaIdx < 0 || alimentoIndex !in listas[listaIdx].alimentos.indices) return

        val novosAlimentos = listas[listaIdx].alimentos.toMutableList()
        novosAlimentos[alimentoIndex] = novosAlimentos[alimentoIndex].comQuantidade(novaQuantidadeG)
        listas[listaIdx] = listas[listaIdx].copy(alimentos = novosAlimentos)

        persistirListas(data, listas)
        recalcularTotais(data, listas)
        _mudancas.tryEmit(Unit)
    }

    fun removerAlimento(
        data:          String,
        listaId:       String,
        alimentoIndex: Int,
    ) {
        val listas   = carregarListas(data).toMutableList()
        val listaIdx = listas.indexOfFirst { it.id == listaId }
        if (listaIdx < 0 || alimentoIndex !in listas[listaIdx].alimentos.indices) return

        val novosAlimentos = listas[listaIdx].alimentos.toMutableList()
        novosAlimentos.removeAt(alimentoIndex)

        if (novosAlimentos.isEmpty()) listas.removeAt(listaIdx)
        else listas[listaIdx] = listas[listaIdx].copy(alimentos = novosAlimentos)

        persistirListas(data, listas)
        recalcularTotais(data, listas)
        _mudancas.tryEmit(Unit)
    }

    fun removerLista(data: String, listaId: String) {
        val listas = carregarListas(data).toMutableList()
        listas.removeAll { it.id == listaId }
        persistirListas(data, listas)
        recalcularTotais(data, listas)
        _mudancas.tryEmit(Unit)
    }

    // ── Listas — leitura ──────────────────────────────────────────────────────

    fun carregarListas(data: String = LocalDate.now().toString()): List<ListaSalva> {
        val json = prefs.getString(keyListas(data), null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj          = array.getJSONObject(i)
                val alimentosArr = obj.getJSONArray("alimentos")
                ListaSalva(
                    id        = obj.getString("id"),
                    timestamp = obj.getLong("timestamp"),
                    horaTexto = obj.optString("horaTexto", ""),
                    alimentos = (0 until alimentosArr.length()).map { j ->
                        val a = alimentosArr.getJSONObject(j)
                        AlimentoSalvo(
                            id                  = a.getString("id"),
                            descricao           = a.getString("descricao"),
                            categoria           = a.optString("categoria", ""),
                            quantidadeG         = a.optDouble("quantidadeG", 100.0),
                            kcalPer100g         = a.optDouble("kcalPer100g",         a.optDouble("kcal",         0.0)),
                            proteinasPer100g    = a.optDouble("proteinasPer100g",    a.optDouble("proteinas",    0.0)),
                            carboidratosPer100g = a.optDouble("carboidratosPer100g", a.optDouble("carboidratos", 0.0)),
                            gordurasPer100g     = a.optDouble("gordurasPer100g",     a.optDouble("gorduras",     0.0)),
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar listas ($data): ${e.message}")
            emptyList()
        }
    }

    // ── Persistência interna ──────────────────────────────────────────────────

    private fun persistirListas(data: String, listas: List<ListaSalva>) {
        val array = JSONArray()
        listas.forEach { lista ->
            val alimentosArr = JSONArray()
            lista.alimentos.forEach { a ->
                alimentosArr.put(JSONObject().apply {
                    put("id",                  a.id)
                    put("descricao",           a.descricao)
                    put("categoria",           a.categoria)
                    put("quantidadeG",         a.quantidadeG)
                    put("kcalPer100g",         a.kcalPer100g)
                    put("proteinasPer100g",    a.proteinasPer100g)
                    put("carboidratosPer100g", a.carboidratosPer100g)
                    put("gordurasPer100g",     a.gordurasPer100g)
                })
            }
            array.put(JSONObject().apply {
                put("id",        lista.id)
                put("timestamp", lista.timestamp)
                put("horaTexto", lista.horaTexto)
                put("alimentos", alimentosArr)
            })
        }
        prefs.edit().putString(keyListas(data), array.toString()).apply()
    }

    private fun recalcularTotais(data: String, listas: List<ListaSalva>) {
        salvarConsumoLocal(
            data      = data,
            kcal      = listas.sumOf { it.totalKcal },
            proteinaG = listas.sumOf { it.totalProteinas },
            carboG    = listas.sumOf { it.totalCarboidratos },
            gorduraG  = listas.sumOf { it.totalGorduras },
        )
    }

    // ── Consumo agregado ──────────────────────────────────────────────────────

    fun salvarConsumoLocal(
        data:      String = LocalDate.now().toString(),
        kcal:      Double,
        proteinaG: Double,
        carboG:    Double,
        gorduraG:  Double,
    ) {
        prefs.edit()
            .putFloat(keyKcal(data),      kcal.toFloat())
            .putFloat(keyProteina(data),  proteinaG.toFloat())
            .putFloat(keyCarbo(data),     carboG.toFloat())
            .putFloat(keyGordura(data),   gorduraG.toFloat())
            .putLong(keyAtualizado(data), System.currentTimeMillis())
            .apply()
        _mudancas.tryEmit(Unit)
    }

    fun acumularConsumoLocal(
        data:      String = LocalDate.now().toString(),
        kcal:      Double,
        proteinaG: Double,
        carboG:    Double,
        gorduraG:  Double,
    ) {
        val atual = carregarConsumoLocal(data)
        salvarConsumoLocal(
            data      = data,
            kcal      = atual.kcal      + kcal,
            proteinaG = atual.proteinaG + proteinaG,
            carboG    = atual.carboG    + carboG,
            gorduraG  = atual.gorduraG  + gorduraG,
        )
    }

    fun carregarConsumoLocal(data: String = LocalDate.now().toString()): ConsumoLocal =
        ConsumoLocal(
            data         = data,
            kcal         = prefs.getFloat(keyKcal(data),     0f).toDouble(),
            proteinaG    = prefs.getFloat(keyProteina(data), 0f).toDouble(),
            carboG       = prefs.getFloat(keyCarbo(data),    0f).toDouble(),
            gorduraG     = prefs.getFloat(keyGordura(data),  0f).toDouble(),
            atualizadoEm = prefs.getLong(keyAtualizado(data), 0L),
        )

    fun temRegistroLocal(data: String = LocalDate.now().toString()): Boolean =
        prefs.getLong(keyAtualizado(data), 0L) > 0L

    // ── Histórico ─────────────────────────────────────────────────────────────

    fun lerHistorico7Dias(): List<ConsumoLocal> {
        val hoje = LocalDate.now()
        return (6 downTo 0).map { diasAtras ->
            carregarConsumoLocal(hoje.minusDays(diasAtras.toLong()).toString())
        }
    }

    fun lerHistoricoCompleto7Dias(): List<ConsumoCompleto> {
        val hoje = LocalDate.now()
        return (6 downTo 0).map { diasAtras ->
            val data = hoje.minusDays(diasAtras.toLong()).toString()
            ConsumoCompleto(
                consumo = carregarConsumoLocal(data),
                listas  = carregarListas(data),
            )
        }
    }
}