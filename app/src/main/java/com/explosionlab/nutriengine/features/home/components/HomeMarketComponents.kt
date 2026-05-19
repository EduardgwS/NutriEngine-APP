package com.explosionlab.nutriengine.features.home.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.explosionlab.nutriengine.core.designsystem.NutriGreen
import com.explosionlab.nutriengine.features.market.Parceiro
import com.explosionlab.nutriengine.features.market.RecomendacaoProduto

@Composable
fun SecaoMercado(
    recomendacoes: List<RecomendacaoProduto>,
    parceiros: List<Parceiro>,
    carregando: Boolean,
    onVerProduto: (RecomendacaoProduto) -> Unit,
    onAtualizar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CabecalhoMercado(carregando = carregando, onAtualizar = onAtualizar)

        when {
            carregando -> CarregandoIndicador()
            recomendacoes.isEmpty() -> MensagemVazia()
            else -> ListaRecomendacoes(recomendacoes = recomendacoes, onVerProduto = onVerProduto)
        }

        if (parceiros.isNotEmpty()) {
            ListaParceiros(parceiros = parceiros)
        }
    }
}

@Composable
private fun CabecalhoMercado(carregando: Boolean, onAtualizar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = NutriGreen, modifier = Modifier.size(18.dp))
            Text("Compras recomendadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onAtualizar,
            enabled = !carregando,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CarregandoIndicador() {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = NutriGreen, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun MensagemVazia() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            "Nenhuma recomendação disponível.",
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ListaRecomendacoes(
    recomendacoes: List<RecomendacaoProduto>,
    onVerProduto: (RecomendacaoProduto) -> Unit,
) {
    recomendacoes.firstOrNull()?.motivo?.takeIf { it.isNotBlank() }?.let { motivo ->
        Text(
            motivo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        recomendacoes.forEach { produto ->
            CardProduto(produto = produto, onClick = { onVerProduto(produto) })
        }
    }
}

@Composable
private fun ListaParceiros(parceiros: List<Parceiro>) {
    val context = LocalContext.current

    Text("Mercados parceiros", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        parceiros.forEach { parceiro ->
            AssistChip(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, parceiro.siteUrl.toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                label = { Text(parceiro.nome, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun CardProduto(produto: RecomendacaoProduto, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.width(148.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            ImagemProduto(produto = produto, context = context)
            InfoProduto(produto = produto)
        }
    }
}

@Composable
private fun ImagemProduto(produto: RecomendacaoProduto, context: android.content.Context) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (produto.imagemUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(produto.imagemUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        produto.precoAntigo?.let { precoAntigo ->
            val desconto = ((1.0 - produto.precoAtual / precoAntigo) * 100).toInt()
            DescontoBadge(desconto = desconto, modifier = Modifier.align(Alignment.TopStart))
        }
    }
}

@Composable
private fun DescontoBadge(desconto: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            "-$desconto%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun InfoProduto(produto: RecomendacaoProduto) {
    Column(Modifier.padding(10.dp)) {
        Text(
            produto.nome,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${produto.marca} · %.0fg".format(produto.quantidadeG),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))

        produto.precoAntigo?.let { precoAntigo ->
            Text(
                "R$ %.2f".format(precoAntigo).replace(".", ","),
                style = MaterialTheme.typography.labelSmall,
                textDecoration = TextDecoration.LineThrough,
            )
        }

        Text(
            "R$ %.2f".format(produto.precoAtual).replace(".", ","),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = NutriGreen,
        )
    }
}
