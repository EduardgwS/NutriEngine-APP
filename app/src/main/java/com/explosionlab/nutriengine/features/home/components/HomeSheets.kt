package com.explosionlab.nutriengine.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.explosionlab.nutriengine.core.designsystem.NutriGreen
import com.explosionlab.nutriengine.core.model.RecomendacaoReceita
import com.explosionlab.nutriengine.features.health.DadoCard
import com.explosionlab.nutriengine.features.market.RecomendacaoProduto

@Composable
fun ReceitaDetalheSheet(receita: RecomendacaoReceita, onFechar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetHeader(titulo = receita.titulo.orEmpty(), onFechar = onFechar)

        Text(receita.descricao.orEmpty(), style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()
        Text("Ingredientes", fontWeight = FontWeight.Bold)
        receita.ingredientes?.forEach { ingrediente ->
            Text("• $ingrediente", style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider()
        Text("Modo de fazer", fontWeight = FontWeight.Bold)
        receita.modoPreparo?.forEachIndexed { index, passo ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${index + 1}.",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(passo, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ProdutoDetalheSheet(
    produto: RecomendacaoProduto,
    onFechar: () -> Unit,
    onComprar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    produto.marca,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onFechar) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        if (produto.imagemUrl.isNotBlank()) {
            AsyncImage(
                model = produto.imagemUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Fit,
            )
        }

        if (produto.motivo.isNotBlank()) {
            Surface(shape = RoundedCornerShape(8.dp), color = NutriGreen.copy(alpha = 0.1f)) {
                Text(
                    produto.motivo,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = NutriGreen,
                )
            }
        }

        HorizontalDivider()
        Text(
            "Informação nutricional (%.0fg)".format(produto.quantidadeG),
            style = MaterialTheme.typography.labelLarge,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DadoCard("Energia", "%.0f kcal".format(produto.kcal), Modifier.weight(1f), Icons.Default.Bolt)
            DadoCard("Proteína", "%.1f g".format(produto.proteinas), Modifier.weight(1f), Icons.Default.KebabDining)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DadoCard("Carbo", "%.1f g".format(produto.carboidratos), Modifier.weight(1f), Icons.Default.BakeryDining)
            DadoCard("Gordura", "%.1f g".format(produto.gorduras), Modifier.weight(1f), Icons.Default.WaterDrop)
        }

        Button(
            onClick = onComprar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriGreen),
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver no mercado", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SheetHeader(titulo: String, onFechar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titulo,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onFechar) {
            Icon(Icons.Default.Close, contentDescription = null)
        }
    }
}
