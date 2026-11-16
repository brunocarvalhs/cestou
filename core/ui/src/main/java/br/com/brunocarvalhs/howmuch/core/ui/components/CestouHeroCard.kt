package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalBlue
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNight

/**
 * Card de destaque do canvas: raio 20 dp, padding 20 dp, texto branco sobre azul ou azul-noite.
 * O conteúdo herda [LocalContentColor] branco, então textos simples não precisam de cor própria.
 */
@Composable
fun CestouHeroCard(
    variant: CestouHeroVariant,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val container = when (variant) {
        CestouHeroVariant.Blue -> SubtotalBlue
        CestouHeroVariant.Night -> SubtotalNight
    }
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(container, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Preview
@Composable
private fun CestouHeroCardPreview() {
    CestouHeroCard(variant = CestouHeroVariant.Blue) {
        Text("Compra do mês")
    }
}
