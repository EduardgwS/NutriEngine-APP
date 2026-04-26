package com.explosionlab.nutriengine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import com.explosionlab.nutriengine.core.common.AppViewModel
import com.explosionlab.nutriengine.core.common.TemaApp
import com.explosionlab.nutriengine.core.designsystem.NutriEngineTheme
import com.explosionlab.nutriengine.core.navigation.NutriNavGraph
import com.explosionlab.nutriengine.core.notifications.NotificationScheduler
import com.explosionlab.nutriengine.core.notifications.NutriNotificationManager
import com.explosionlab.nutriengine.features.home.HomeViewModel
import com.explosionlab.nutriengine.features.market.MarketViewModel
import com.explosionlab.nutriengine.features.megumi.MegumiViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cria os canais de notificação
        NutriNotificationManager.criarCanais(this)

        // Garante que as notificações estejam agendadas se houver permissão e se estiverem ativas no aplicativo
        val prefs = getSharedPreferences("nutriengine_prefs", MODE_PRIVATE)
        val ativadasNoApp = prefs.getBoolean("notificacoes_ativas", true)

        if (ativadasNoApp && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        ) {
            NotificationScheduler.ativar(this, ExistingPeriodicWorkPolicy.REPLACE)
        }

        setContent {
            val appViewModel:     AppViewModel     = viewModel()
            val homeViewModel:    HomeViewModel    = viewModel()
            val megumiViewModel:  MegumiViewModel  = viewModel()
            val marketViewModel: MarketViewModel = viewModel()

            val darkTheme = when (appViewModel.tema) {
                TemaApp.ESCURO  -> true
                TemaApp.CLARO   -> false
                TemaApp.SISTEMA -> isSystemInDarkTheme()
            }

            NutriEngineTheme(darkTheme = darkTheme) {
                NutriNavGraph(
                    appViewModel     = appViewModel,
                    homeViewModel    = homeViewModel,
                    megumiViewModel  = megumiViewModel,
                    marketViewModel = marketViewModel,
                )
            }
        }
    }
}