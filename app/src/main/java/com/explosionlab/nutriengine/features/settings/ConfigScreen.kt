package com.explosionlab.nutriengine.features.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.core.common.AppViewModel
import com.explosionlab.nutriengine.core.common.TemaApp
import com.explosionlab.nutriengine.core.designsystem.NutriGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onVoltar:                 () -> Unit,
    onEditarPerfil:           () -> Unit,
    onConectarHealthConnect: () -> Unit,
    appViewModel:             AppViewModel,
    viewModel:                ConfiguracoesViewModel = viewModel(),
) {
    val state   = viewModel.state
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val version  = remember {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName
    }

    //Permissão

    val notificacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida -> viewModel.onResultadoPermissaoNotificacao(concedida) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NutriGreen,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            //Aparencia
            GrupoTitulo("Aparência")

            SeletorTema(
                temaSelecionado = appViewModel.tema,
                onSelecionar    = { appViewModel.atualizarTema(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            //Perfil
            GrupoTitulo("Perfil")

            ConfigItem(
                icone     = Icons.Default.Person,
                titulo    = "Editar perfil",
                descricao = "Alterar nome, peso, altura e objetivo",
                onClick   = onEditarPerfil,
                trailing  = {
                    Icon(Icons.Default.ChevronRight, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            //Notificações
            GrupoTitulo("Notificações")

            ConfigItem(
                icone     = if (state.notificacoesAtivas) Icons.Default.Notifications
                            else Icons.Default.NotificationsOff,
                titulo    = "Notificações",
                descricao = if (state.notificacoesAtivas) "Notificações ativadas"
                            else "Notificações desativadas",
                trailing = {
                    Switch(
                        checked         = state.notificacoesAtivas,
                        onCheckedChange = { ativar ->
                            if (ativar) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onResultadoPermissaoNotificacao(true)
                                }
                            } else {
                                viewModel.desativarNotificacoes()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = Color.White,
                            checkedTrackColor  = NutriGreen,
                        ),
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            //Integrações
            GrupoTitulo("Integrações")

            val hcCor = if (state.healthConnectConectado) NutriGreen else MaterialTheme.colorScheme.onSurfaceVariant
            ConfigItem(
                icone     = Icons.Default.Favorite,
                titulo    = "Health Connect",
                descricao = if (state.healthConnectConectado) "Conectado e sincronizando"
                            else "Sincronize seus dados de saúde",
                containerColor = if (state.healthConnectConectado) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor   = if (state.healthConnectConectado) NutriGreen else MaterialTheme.colorScheme.onSurface,
                iconeColor     = hcCor,
                onClick   = { if (!state.healthConnectConectado) onConectarHealthConnect() },
                trailing  = {
                    Icon(
                        imageVector = if (state.healthConnectConectado) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = hcCor,
                        modifier = Modifier.size(if (state.healthConnectConectado) 20.dp else 18.dp)
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Versão $version",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

//Tema
@Composable
private fun SeletorTema(
    temaSelecionado: TemaApp,
    onSelecionar:    (TemaApp) -> Unit,
) {
    data class OpcaoTema(val tema: TemaApp, val label: String, val icone: ImageVector)

    val opcoes = listOf(
        OpcaoTema(TemaApp.CLARO,   "Claro",     Icons.Default.LightMode),
        OpcaoTema(TemaApp.ESCURO,  "Escuro",    Icons.Default.DarkMode),
        OpcaoTema(TemaApp.SISTEMA, "Automático", Icons.Default.SettingsBrightness),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {

            Text("Tema", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Escolha como o app deve aparecer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                opcoes.forEach { opcao ->
                    val selecionado = temaSelecionado == opcao.tema
                    OutlinedButton(
                        onClick  = { onSelecionar(opcao.tema) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selecionado) NutriGreen else Color.Transparent,
                            contentColor   = if (selecionado) Color.White
                            else             MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            width = if (selecionado) 0.dp else 1.dp,
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(opcao.icone, null, modifier = Modifier.size(18.dp))
                            Text(opcao.label, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

//Extra
@Composable
private fun GrupoTitulo(texto: String) {
    Text(
        texto,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color      = NutriGreen,
        modifier   = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ConfigItem(
    icone:          ImageVector,
    titulo:         String,
    descricao:      String,
    modifier:       Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor:   Color = MaterialTheme.colorScheme.onSurface,
    iconeColor:     Color = NutriGreen,
    onClick:        (() -> Unit)? = null,
    trailing:       @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        onClick  = onClick ?: {},
        enabled  = onClick != null
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icone, null, tint = iconeColor, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = contentColor)
                Text(descricao, style = MaterialTheme.typography.bodySmall,
                    color = if (containerColor == MaterialTheme.colorScheme.surfaceVariant)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else contentColor.copy(alpha = 0.8f))
            }
            trailing?.invoke()
        }
    }
}
