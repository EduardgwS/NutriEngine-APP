package com.explosionlab.nutriengine.core.common

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
import com.explosionlab.nutriengine.core.designsystem.components.NutriTab

enum class TemaApp { CLARO, ESCURO, SISTEMA }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo   = AuthRepository(application)
    private val perfilRepo = PerfilRepository(application)
    private val prefs      = application.getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)

    //Temas

    var tema by mutableStateOf(
        TemaApp.valueOf(prefs.getString("tema_app", TemaApp.SISTEMA.name) ?: TemaApp.SISTEMA.name)
    )
        private set

    fun atualizarTema(novoTema: TemaApp) {
        tema = novoTema
        prefs.edit { putString("tema_app", novoTema.name) }
    }

    //Sessão

    val telaInicial: String get() = when {
        !authRepo.estaLogado()       -> "login"
        !perfilRepo.perfilCompleto() -> "hc_intro"
        else                         -> NutriTab.INICIO.rota
    }

    fun perfilCompleto(): Boolean = perfilRepo.perfilCompleto()

    fun logout() {
        authRepo.limparToken()
    }

}
