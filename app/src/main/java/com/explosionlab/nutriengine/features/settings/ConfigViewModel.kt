package com.explosionlab.nutriengine.features.settings

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.notifications.NotificationScheduler
import kotlinx.coroutines.launch

data class ConfiguracoesUiState(
    val notificacoesAtivas:  Boolean = false,
    val healthConnectConectado: Boolean = false,
)

class ConfiguracoesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs      = application.getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)
    private val healthRepo = com.explosionlab.nutriengine.features.health.HealthConnectRepository(application)

    var state by mutableStateOf(ConfiguracoesUiState()); private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val temPermissaoSistemica = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val ativadasNoApp = prefs.getBoolean("notificacoes_ativas", true)
            val hcConectado = healthRepo.temPermissoes()

            state = state.copy(
                notificacoesAtivas = temPermissaoSistemica && ativadasNoApp,
                healthConnectConectado = hcConectado,
            )
        }
    }

    //Notificações

    fun onResultadoPermissaoNotificacao(concedida: Boolean) {
        state = state.copy(notificacoesAtivas = concedida)
        prefs.edit { putBoolean("notificacoes_ativas", concedida) }

        val app = getApplication<Application>()
        if (concedida) NotificationScheduler.ativar(app)
        else           NotificationScheduler.desativar(app)
    }

    fun desativarNotificacoes() {
        prefs.edit { putBoolean("notificacoes_ativas", false) }
        state = state.copy(notificacoesAtivas = false)
        NotificationScheduler.desativar(getApplication())
    }
}