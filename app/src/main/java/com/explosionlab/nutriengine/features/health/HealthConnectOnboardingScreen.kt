package com.explosionlab.nutriengine.features.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.R
import com.explosionlab.nutriengine.core.designsystem.LightGreenContainer
import com.explosionlab.nutriengine.core.designsystem.NutriGreen

@Composable
fun HealthConnectOnboardingScreen(
    onContinuar: () -> Unit,
    viewModel:   HealthConnectOnboardingViewModel = viewModel()
) {
    val permissaoLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        onContinuar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        Box(
            modifier         = Modifier
                .size(100.dp)
                .background(LightGreenContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter            = painterResource(id = R.drawable.ic_health_connect),
                contentDescription = "Ícone Health Connect",
                tint               = Color.Unspecified,
                modifier           = Modifier.size(100.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Conecte ao Health Connect",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Para uma experiência personalizada e sincronizada entre todos seus apps de saúde, conecte ao Health Connect.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        BeneficioItem(
            titulo    = "Peso e altura automáticos",
            descricao = "Seu peso e altura são importados direto do Health Connect, sem precisar digitar."
        )
        BeneficioItem(
            titulo    = "Calorias gastas no dia",
            descricao = "Acompanhe calorias ativas queimadas e veja o balanço real com o que você comeu."
        )
        BeneficioItem(
            titulo    = "Nutrição registrada",
            descricao = "O que você adicionar no app é sincronizado com o Health Connect automaticamente."
        )
        BeneficioItem(
            titulo    = "Seus dados ficam no seu celular",
            descricao = "O Health Connect é local. Nenhum dado de saúde é enviado para servidores externos."
        )

        Spacer(Modifier.height(36.dp))

        // ── Aviso de disponibilidade ───────────────────────────────────────────
        if (!viewModel.hcDisponivel) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Health Connect não está instalado neste dispositivo. Você pode instalá-lo pela Play Store depois.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Botão principal ────────────────────────────────────────────────────
        Button(
            onClick  = {
                if (viewModel.hcDisponivel) permissaoLauncher.launch(viewModel.permissions)
                else onContinuar()
            },
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(28.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = NutriGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text(
                if (viewModel.hcDisponivel) "Conectar com Health Connect" else "Entendido",
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Botão secundário ───────────────────────────────────────────────────
        if (viewModel.hcDisponivel) {
            TextButton(
                onClick  = onContinuar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Continuar sem Health Connect",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Item de benefício ──────────────────────────────────────────────────────────

@Composable
private fun BeneficioItem(titulo: String, descricao: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
