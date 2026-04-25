package com.explosionlab.nutriengine.features.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel


class HealthConnectIntroViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepo = HealthRepository(application)

    val hcDisponivel: Boolean = healthRepo.isDisponivel()
    val permissions           = healthRepo.permissions
}
