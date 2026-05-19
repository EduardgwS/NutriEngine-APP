package com.explosionlab.nutriengine.core.data.repository

import android.content.Context
import androidx.core.content.edit
import com.explosionlab.nutriengine.core.model.NivelAtividade
import com.explosionlab.nutriengine.core.model.Objetivo
import com.explosionlab.nutriengine.core.model.Perfil
import com.explosionlab.nutriengine.core.model.Sexo
import java.time.LocalDate

//Armazenamento dos dados do usuário
class PerfilRepository(private val context: Context) {

    private val prefsName = "nutriengine_prefs"

    companion object {
        const val KEY_PERFIL_COMPLETO  = "perfil_completo"
        const val KEY_NOME             = "perfil_nome"
        const val KEY_DATA_NASCIMENTO  = "perfil_data_nascimento"   // ISO "YYYY-MM-DD"
        const val KEY_SEXO             = "perfil_sexo"
        const val KEY_OBJETIVO         = "perfil_objetivo"
        const val KEY_NIVEL_ATIVIDADE  = "perfil_nivel_atividade"
        const val KEY_PESO             = "perfil_peso"              // Double em kg
        const val KEY_ALTURA           = "perfil_altura"            // Double em metros
    }

    private fun prefs() = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun perfilCompleto(): Boolean = prefs().getBoolean(KEY_PERFIL_COMPLETO, false)


    fun carregarNome(fallback: String = ""): String =
        prefs().getString(KEY_NOME, null)?.takeIf { it.isNotBlank() } ?: fallback


    fun carregarPeso(): Double   = Double.fromBits(prefs().getLong(KEY_PESO,   0L))
    fun carregarAltura(): Double = Double.fromBits(prefs().getLong(KEY_ALTURA, 0L))

    fun salvarMedidas(peso: Double, altura: Double) {
        prefs().edit {
            putLong(KEY_PESO, peso.toBits())
                .putLong(KEY_ALTURA, altura.toBits())
        }
    }

    // Perfil

    fun salvarPerfil(
        nome:           String,
        dataNascimento: LocalDate,
        sexo: Sexo,
        objetivo: Objetivo,
        nivelAtividade: NivelAtividade,
        peso:           Double,
        altura:         Double,
    ) {
        prefs().edit {
            putString(KEY_NOME, nome.trim())
                .putString(KEY_DATA_NASCIMENTO, dataNascimento.toString())
                .putString(KEY_SEXO, sexo.name)
                .putString(KEY_OBJETIVO, objetivo.name)
                .putString(KEY_NIVEL_ATIVIDADE, nivelAtividade.name)
                .putLong(KEY_PESO, peso.toBits())
                .putLong(KEY_ALTURA, altura.toBits())
                .putBoolean(KEY_PERFIL_COMPLETO, true)
        }
    }

    fun carregarPerfil(
        nomeGoogleFallback: String  = "",
        pesoOverride:       Double? = null,
        alturaOverride:     Double? = null,
    ): Perfil {
        val p       = prefs()
        val dataStr = p.getString(KEY_DATA_NASCIMENTO, null)

        val pesoLocal   = Double.fromBits(p.getLong(KEY_PESO,   0L))
        val alturaLocal = Double.fromBits(p.getLong(KEY_ALTURA, 0L))

        return Perfil(
            nome = carregarNome(fallback = nomeGoogleFallback),
            peso = pesoOverride ?: pesoLocal,
            altura = alturaOverride ?: alturaLocal,
            dataNascimento = dataStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            sexo = Sexo.valueOf(
                p.getString(KEY_SEXO, Sexo.MASCULINO.name) ?: Sexo.MASCULINO.name
            ),
            objetivo = Objetivo.valueOf(
                p.getString(KEY_OBJETIVO, Objetivo.MELHORAR_ALIMENTACAO.name)
                    ?: Objetivo.MELHORAR_ALIMENTACAO.name
            ),
            nivelAtividade = NivelAtividade.valueOf(
                p.getString(KEY_NIVEL_ATIVIDADE, NivelAtividade.SEDENTARIO.name)
                    ?: NivelAtividade.SEDENTARIO.name
            ),
        )
    }
}