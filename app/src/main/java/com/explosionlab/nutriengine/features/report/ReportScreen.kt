@file:Suppress("AssignedValueIsNeverRead")

package com.explosionlab.nutriengine.features.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.core.designsystem.ErrorRed
import com.explosionlab.nutriengine.core.designsystem.InfoBlue
import com.explosionlab.nutriengine.core.designsystem.NutriGreen
import com.explosionlab.nutriengine.core.designsystem.WarningOrange
import com.explosionlab.nutriengine.core.model.Objetivo
import com.explosionlab.nutriengine.core.model.Perfil
import com.explosionlab.nutriengine.features.health.DadoCard
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import com.explosionlab.nutriengine.features.health.SecaoTitulo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JTimeTextStyle

@Composable
fun RelatorioScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: RelatorioViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recarregarRelatorio()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (state.carregando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NutriGreen)
            }
            return@Box
        }

        val p = state.perfil ?: return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Relatório", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            PerfilCard(p)

            SecaoTitulo("Dados físicos")
            DadosFisicosRow(p)

            if (p.imc > 0) {
                SecaoTitulo("Índice de Massa Corporal (IMC)")
                ImcCard(p)
            }

            if (p.gastoEnergeticoTotal > 0) {
                SecaoTitulo("Meta calórica diária")
                MetaCaloricaCard(p)
                DistribuicaoMacrosCard(p)
            }

            state.nutricaoExternaHoje?.let { externa ->
                SecaoTitulo("Importado de outros apps")
                NutricaoExternaCard(externa)
            }

            SecaoTitulo("Resumo da semana")
            if (state.historico7Dias.any { it.kcal > 0 }) {
                GraficoSemanal(historico = state.historico7Dias, caloriasRecomendadas = p.caloriasRecomendadas)
            } else {
                EmptyStateCard("Nenhum consumo registrado ainda.\nCrie uma lista na aba Pesquisar para começar.")
            }

            HistoricoAlimentosSection(
                historico = state.historicoCompleto7Dias,
                onEditar = viewModel::editarAlimento,
                onRemover = viewModel::removerAlimento,
                onRemoverLista = viewModel::removerLista,
            )

            if (p.peso == 0.0 && p.altura == 0.0) {
                EmptyStateCard("Complete seu perfil para ver seus dados aqui.")
            }
        }
    }
}

@Composable
private fun PerfilCard(p: Perfil) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NutriGreen),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(p.nome.ifBlank { "Usuário" }, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(p.objetivo.label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            Text("${p.sexo.label} · ${p.idade} anos · ${p.nivelAtividade.label}",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun DadosFisicosRow(p: Perfil) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DadoCard("Peso", if (p.peso > 0) "%.1f kg".format(p.peso) else "—", Modifier.weight(1f))
        DadoCard("Altura", if (p.altura > 0) "%.2f m".format(p.altura) else "—", Modifier.weight(1f))
        DadoCard("Idade", if (p.idade > 0) "${p.idade} anos" else "—", Modifier.weight(1f))
    }
}

@Composable
private fun ImcCard(p: Perfil) {
    val cor = imcCor(p.imc)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("%.1f".format(p.imc), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = cor)
                Text(p.imcDescricao, color = cor, fontWeight = FontWeight.Bold)
            }
            ImcEscala()
        }
    }
}

@Composable
private fun MetaCaloricaCard(p: Perfil) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Contas utilizadas no cálculo de calorias diárias:", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinhaCalculo("TMB (Mifflin-St Jeor)", "%.0f kcal/dia".format(p.tmb), "Calorias em repouso absoluto")
            LinhaCalculo("Fator de atividade", "× %.3f".format(p.nivelAtividade.fator), p.nivelAtividade.label)
            HorizontalDivider()
            LinhaCalculo("GET (manutenção)", "${p.gastoEnergeticoTotal} kcal/dia",
                "TMB × fator — para manter o peso atual", destaque = true,
                corValor = MaterialTheme.colorScheme.onSurface)
            if (p.ajusteKcal != 0) {
                val sinal = if (p.ajusteKcal > 0) "+" else ""
                val corAjuste = if (p.ajusteKcal > 0) NutriGreen else InfoBlue
                val textoNota = when (p.objetivo) {
                    Objetivo.PERDER_PESO -> "Déficit para ≈ 0,5 kg/semana"
                    Objetivo.GANHAR_MUSCULOS -> "Superávit para ganho muscular"
                    else -> ""
                }
                LinhaCalculo("Ajuste (${p.objetivo.label})", "$sinal${p.ajusteKcal} kcal",
                    textoNota, corValor = corAjuste)
                HorizontalDivider()
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("A meta diária ideal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${p.caloriasRecomendadas} kcal", fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold, color = NutriGreen)
            }
        }
    }
}

@Composable
private fun DistribuicaoMacrosCard(p: Perfil) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Distribuição sugerida dos macronutrientes", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val kcal = p.caloriasRecomendadas.toDouble()
            val (pCarbo, pProt, pGord) = calcularDistribuicaoMacros(p.objetivo)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroCard("Carbo", "%.0fg".format(kcal * pCarbo / 4), "%.0f%%".format(pCarbo * 100), Modifier.weight(1f), icon = Icons.Default.BakeryDining)
                MacroCard("Proteína", "%.0fg".format(kcal * pProt / 4), "%.0f%%".format(pProt * 100), Modifier.weight(1f), icon = Icons.Default.KebabDining)
                MacroCard("Gordura", "%.0fg".format(kcal * pGord / 9), "%.0f%%".format(pGord * 100), Modifier.weight(1f), icon = Icons.Default.WaterDrop)
            }
            Text("Proporções otimizadas para: ${p.objetivo.label.lowercase()}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyStateCard(texto: String) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(texto, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HistoricoAlimentosSection(
    historico: List<ConsumoRepository.ConsumoCompleto>,
    onEditar: (data: String, listaId: String, alimentoId: String, novaQuantidadeG: Double) -> Unit,
    onRemover: (data: String, listaId: String, alimentoId: String) -> Unit,
    onRemoverLista: (data: String, listaId: String) -> Unit,
) {
    val diasComListas = historico.filter { it.listas.isNotEmpty() }
    var modoEdicao by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Histórico de Alimentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (diasComListas.isNotEmpty()) {
            FilledTonalButton(
                onClick = { modoEdicao = !modoEdicao },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (modoEdicao) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (modoEdicao) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                AnimatedContent(
                    targetState = modoEdicao,
                    transitionSpec = {
                        (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    },
                    label = "btn_edicao"
                ) { editing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (editing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (editing) "Concluir" else "Editar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (diasComListas.isEmpty()) {
        EmptyStateCard("Nenhum alimento registrado nos últimos 7 dias.\nUse a aba Pesquisar para adicionar refeições.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        diasComListas.forEach { consumo ->
            key(consumo.consumo.data) {
                DiaCard(consumo, modoEdicao, onEditar, onRemover, onRemoverLista)
            }
        }
    }
}

@Composable
private fun DiaCard(
    consumoCompleto: ConsumoRepository.ConsumoCompleto,
    modoEdicao: Boolean,
    onEditar: (String, String, String, Double) -> Unit,
    onRemover: (String, String, String) -> Unit,
    onRemoverLista: (String, String) -> Unit,
) {
    var diaExpandido by remember(consumoCompleto.consumo.data) {
        mutableStateOf(consumoCompleto.consumo.data == LocalDate.now().toString())
    }
    val rotacao by animateFloatAsState(if (diaExpandido) 180f else 0f, label = "dia")

    val data = LocalDate.parse(consumoCompleto.consumo.data)
    val ehHoje = data == LocalDate.now()
    val ehOntem = data == LocalDate.now().minusDays(1)
    val nomeDia = when {
        ehHoje -> "Hoje"
        ehOntem -> "Ontem"
        else -> data.dayOfWeek.getDisplayName(JTimeTextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() }
    }
    val dataFormatada = data.format(DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR")))
    val totalKcalDia = consumoCompleto.listas.sumOf { it.totalKcal }
    val nListas = consumoCompleto.listas.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (ehHoje) NutriGreen.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(nomeDia, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = if (ehHoje) NutriGreen else MaterialTheme.colorScheme.onSurface)
                        Text(dataFormatada, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$nListas ${if (nListas == 1) "lista" else "listas"} · %.0f kcal total".format(totalKcalDia),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { diaExpandido = !diaExpandido }) {
                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(rotacao))
                }
            }

            AnimatedVisibility(
                visible = diaExpandido,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    consumoCompleto.listas.forEach { lista ->
                        key(lista.id) {
                            ListaCard(consumoCompleto.consumo.data, lista, modoEdicao, onEditar, onRemover, onRemoverLista)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaCard(
    data: String,
    lista: ConsumoRepository.ListaSalva,
    modoEdicao: Boolean,
    onEditar: (String, String, String, Double) -> Unit,
    onRemover: (String, String, String) -> Unit,
    onRemoverLista: (String, String) -> Unit,
) {
    var expandida by remember(lista.id) { mutableStateOf(true) }
    var confirmarEx by remember { mutableStateOf(false) }
    val rotacao by animateFloatAsState(if (expandida) 180f else 0f, label = "lista")

    if (confirmarEx) {
        AlertDialog(
            onDismissRequest = { confirmarEx = false },
            title = { Text("Excluir lista?") },
            text = { Text("Todos os ${lista.alimentos.size} itens desta lista serão removidos.") },
            confirmButton = {
                TextButton(onClick = { onRemoverLista(data, lista.id); confirmarEx = false }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEx = false }) { Text("Cancelar") } },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = NutriGreen, modifier = Modifier.size(14.dp))
                        Text(lista.horaTexto.ifBlank { "—" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = NutriGreen)
                    }
                    Text("${lista.alimentos.size} itens · %.0f kcal".format(lista.totalKcal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    if (modoEdicao) {
                        IconButton(onClick = { confirmarEx = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    }
                    IconButton(onClick = { expandida = !expandida }) { Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(rotacao)) }
                }
            }

            AnimatedVisibility(
                visible = expandida,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                    lista.alimentos.forEach { alimento ->
                        key(alimento.id) {
                            AlimentoEditavelRow(alimento, modoEdicao, { g -> onEditar(data, lista.id, alimento.id, g) }, { onRemover(data, lista.id, alimento.id) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MacroChipPequeno("P: %.0fg".format(lista.totalProteinas), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MacroChipPequeno("C: %.0fg".format(lista.totalCarboidratos), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        MacroChipPequeno("G: %.0fg".format(lista.totalGorduras), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlimentoEditavelRow(
    alimento: ConsumoRepository.AlimentoSalvo,
    modoEdicao: Boolean,
    onSalvarGramas: (Double) -> Unit,
    onRemover: () -> Unit,
) {
    var editando by remember { mutableStateOf(false) }
    var gramasInput by remember(alimento.quantidadeG) { mutableStateOf("%.0f".format(alimento.quantidadeG)) }

    val bgColor by animateColorAsState(
        targetValue = if (modoEdicao) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent,
        label = "alimento_bg"
    )

    if (editando) {
        val gramasValidas = gramasInput.replace(",", ".").toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { editando = false },
            title = { Text("Editar quantidade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alimento.descricao, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = gramasInput,
                        onValueChange = { if (it.length <= 6) gramasInput = it },
                        label = { Text("Gramas") },
                        suffix = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = gramasValidas == null || gramasValidas <= 0
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { gramasValidas?.let(onSalvarGramas); editando = false },
                    enabled = gramasValidas != null && gramasValidas > 0
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editando = false }) { Text("Cancelar") } },
        )
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        onClick = { if (modoEdicao) editando = true },
        enabled = modoEdicao
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = NutriGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    "%.0fg".format(alimento.quantidadeG),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NutriGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alimento.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    "%.0f kcal".format(alimento.kcal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (modoEdicao) {
                IconButton(
                    onClick = { editando = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onRemover,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroChipPequeno(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = cor.copy(alpha = 0.10f)) {
        Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), maxLines = 1)
    }
}

@Composable
fun GraficoSemanal(historico: List<ConsumoRepository.ConsumoLocal>, caloriasRecomendadas: Int) {
    val maxKcal = maxOf(historico.maxOf { it.kcal }, caloriasRecomendadas.toDouble(), 1.0)
    val barColor = NutriGreen
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val metaColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val density = LocalDensity.current

    val totalSemana = historico.sumOf { it.kcal }
    val mediaSemana = if (historico.isNotEmpty()) totalSemana / historico.size else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Desempenho Semanal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Média: %.0f kcal".format(mediaSemana),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Total: %.0f kcal".format(totalSemana),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendaItem(barColor, "Consumo")
                if (caloriasRecomendadas > 0) LegendaItem(metaColor, "Meta")
            }

            Spacer(Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val n = historico.size
                val barAreaW = size.width
                val barAreaH = size.height - with(density) { 28.dp.toPx() }
                val slotW = barAreaW / n
                val barW = slotW * 0.55f
                val gap = (slotW - barW) / 2f
                val cornerR = with(density) { 4.dp.toPx() }

                //Meta diária
                if (caloriasRecomendadas > 0) {
                    val metaY = barAreaH - (barAreaH * (caloriasRecomendadas / maxKcal).toFloat()).coerceIn(0f, barAreaH)
                    drawLine(
                        color = metaColor,
                        start = Offset(0f, metaY),
                        end = Offset(size.width, metaY),
                        strokeWidth = with(density) { 1.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                historico.forEachIndexed { i, dia ->
                    val x = i * slotW + gap
                    val ratio = (dia.kcal / maxKcal).toFloat().coerceIn(0f, 1f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barW, barAreaH),
                        cornerRadius = CornerRadius(cornerR)
                    )
                    //Barra de progresso
                    if (dia.kcal > 0) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, barAreaH - (barAreaH * ratio)),
                            size = Size(barW, barAreaH * ratio),
                            cornerRadius = CornerRadius(cornerR)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                historico.forEach { dia ->
                    val d = LocalDate.parse(dia.data)
                    val nome = d.dayOfWeek.getDisplayName(JTimeTextStyle.NARROW, Locale.forLanguageTag("pt-BR"))
                        .replaceFirstChar { it.uppercase() }
                    Text(
                        nome,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendaItem(cor: Color, rotulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = MaterialTheme.shapes.extraSmall, color = cor) {}
        Spacer(Modifier.width(4.dp))
        Text(rotulo, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun LinhaCalculo(rotulo: String, valor: String, nota: String = "", destaque: Boolean = false, corValor: Color = NutriGreen) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(rotulo, style = if (destaque) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, fontWeight = if (destaque) FontWeight.SemiBold else FontWeight.Normal)
            if (nota.isNotEmpty()) Text(nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(valor, style = if (destaque) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal, color = corValor)
    }
}

@Composable
fun MacroCard(nome: String, gramas: String, porcento: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Icon(icon, null, tint = NutriGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(nome, style = MaterialTheme.typography.labelSmall)
            }
            Text(gramas, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(porcento, style = MaterialTheme.typography.labelSmall, color = NutriGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NutricaoExternaCard(nutricao: HealthConnectRepository.NutricaoDiaria) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = com.explosionlab.nutriengine.R.drawable.ic_health_connect),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sincronizado via Health Connect",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Estes dados foram importados de outros aplicativos:",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DestaqueNutriente("Calorias", "%.0f kcal".format(nutricao.calorias))
                DestaqueNutriente("Proteínas", "%.1f g".format(nutricao.proteinas))
                DestaqueNutriente("Carbos", "%.1f g".format(nutricao.carboidratos))
                DestaqueNutriente("Gorduras", "%.1f g".format(nutricao.gorduras))
            }
            if (nutricao.fontes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Fontes: ${nutricao.fontes.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DestaqueNutriente(label: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

fun imcCor(imc: Double): Color = when {
    imc < 18.5 -> InfoBlue
    imc < 25.0 -> NutriGreen
    imc < 30.0 -> WarningOrange
    else -> ErrorRed
}

private fun calcularDistribuicaoMacros(objetivo: Objetivo): Triple<Double, Double, Double> = when (objetivo) {
    Objetivo.GANHAR_MUSCULOS -> Triple(0.45, 0.30, 0.25)
    Objetivo.PERDER_PESO -> Triple(0.40, 0.35, 0.25)
    Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
}

@Composable
fun ImcEscala() {
    Column(horizontalAlignment = Alignment.End) {
        Text("< 18.5", color = InfoBlue, style = MaterialTheme.typography.labelSmall)
        Text("18.5–24.9", color = NutriGreen, style = MaterialTheme.typography.labelSmall)
        Text("25–29.9", color = WarningOrange, style = MaterialTheme.typography.labelSmall)
        Text("≥ 30", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
    }
}
