package com.explosionlab.nutriengine.core.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.Period
import java.util.UUID


data class Mensagem(
    val texto:     String,
    val ehUsuario: Boolean,
    val id:        String = UUID.randomUUID().toString()
)


data class Alimento(
    val id:           String,
    val descricao:    String,
    val categoria:    String,
    val kcal:         Double,
    val proteinas:    Double,
    val carboidratos: Double,
    val gorduras:     Double,
)


data class IdentificacaoResult(
    val nome:   String,
    val gramas: Double? = null,
)


data class RecomendacaoReceita(
    @SerializedName("titulo")
    val titulo:       String? = null,
    @SerializedName("descricao")
    val descricao:    String? = null,
    @SerializedName("ingredientes")
    val ingredientes: List<String>? = null,
    @SerializedName("modo_preparo")
    val modoPreparo:  List<String>? = null,
)

data class ReceitaResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("receita")
    val receita: RecomendacaoReceita
)

data class DicaMacro(
    @SerializedName("icone")
    val icone:  String,
    @SerializedName("titulo")
    val titulo: String,
    @SerializedName("corpo")
    val corpo:  String
)

data class DicaMacroResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("dica")
    val dica:   DicaMacro
)


data class ResultadoLogin(

    val sucesso:  Boolean,
    val nome:     String = "",
    val mensagem: String = ""
)


enum class Objetivo(val label: String) {
    GANHAR_MUSCULOS      ("Ganhar Músculos"),
    PERDER_PESO          ("Perder Peso"),
    MELHORAR_ALIMENTACAO ("Melhorar a Alimentação"),
}

enum class Sexo(val label: String) {
    MASCULINO("Masculino"),
    FEMININO ("Feminino"),
    OUTRO    ("Outro"),
}

enum class NivelAtividade(
    val label:     String,
    val descricao: String,
    val fator:     Double
) {
    SEDENTARIO(
        label     = "Sedentário",
        descricao = "Pouco ou nenhum exercício",
        fator     = 1.2
    ),
    LEVEMENTE_ATIVO(
        label     = "Levemente ativo",
        descricao = "Exercício leve 1–3 dias/semana",
        fator     = 1.375
    ),
    MODERADAMENTE_ATIVO(
        label     = "Moderadamente ativo",
        descricao = "Exercício moderado 3–5 dias/semana",
        fator     = 1.55
    ),
    MUITO_ATIVO(
        label     = "Muito ativo",
        descricao = "Exercício intenso 6–7 dias/semana",
        fator     = 1.725
    ),
    EXTREMAMENTE_ATIVO(
        label     = "Extremamente ativo",
        descricao = "Treino pesado diário ou trabalho físico intenso",
        fator     = 1.9
    ),
}

data class Perfil(
    val nome:           String         = "",
    val altura:         Double         = 0.0,
    val peso:           Double         = 0.0,
    val dataNascimento: LocalDate?     = null,
    val sexo:           Sexo           = Sexo.MASCULINO,
    val objetivo:       Objetivo       = Objetivo.MELHORAR_ALIMENTACAO,
    val nivelAtividade: NivelAtividade = NivelAtividade.SEDENTARIO,
) {
    val idade: Int get() = dataNascimento
        ?.let { Period.between(it, LocalDate.now()).years }
        ?: 0

    val imc: Double get() = if (altura > 0) peso / (altura * altura) else 0.0

    val imcDescricao: String get() = when {
        imc < 18.5 -> "Palito"
        imc < 25.0 -> "Normal"
        imc < 30.0 -> "Ficando Gordo"
        else       -> "Majin Boo"
    }

    /** Taxa Metabólica Basal — calorias em repouso absoluto (Mifflin-St Jeor). */
    val tmb: Double get() {
        if (peso <= 0 || altura <= 0 || idade <= 0) return 0.0
        return when (sexo) {
            Sexo.MASCULINO -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade + 5.0
            Sexo.FEMININO  -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade - 161.0
            Sexo.OUTRO     -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade - 78.0
        }
    }

    /** Gasto Energético Total — calorias para manter o peso atual (TMB × fator de atividade). */
    val gastoEnergeticoTotal: Int get() =
        if (tmb > 0) (tmb * nivelAtividade.fator).toInt() else 0

    /** Calorias diárias recomendadas com base no objetivo. */
    val caloriasRecomendadas: Int get() = when {
        gastoEnergeticoTotal <= 0               -> 0
        objetivo == Objetivo.PERDER_PESO        -> (gastoEnergeticoTotal - 500).coerceAtLeast(1200)
        objetivo == Objetivo.GANHAR_MUSCULOS    -> gastoEnergeticoTotal + 300
        else                                    -> gastoEnergeticoTotal
    }

    /** Diferença entre a meta e o GET (positivo = superávit, negativo = déficit). */
    val ajusteKcal: Int get() = caloriasRecomendadas - gastoEnergeticoTotal
}
