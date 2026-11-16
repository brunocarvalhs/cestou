package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalCoral
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNight

private const val DARK_LUMINANCE_THRESHOLD = 0.5f

@Composable
fun CestouButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    style: CestouButtonStyle = CestouButtonStyle.Primary,
    /** Cor explícita sobrepõe a cor da [style]; chamadas antigas continuam compilando. */
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val label: @Composable RowScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)
    val shape = RoundedCornerShape(14.dp)

    if (style == CestouButtonStyle.Outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor ?: MaterialTheme.colorScheme.primary
            ),
            content = label
        )
        return
    }

    val (defaultContainer, defaultContent) = styleColors(style)
    val container = containerColor ?: defaultContainer
    val content = contentColor ?: defaultContent
    Button(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.5f),
            disabledContentColor = content.copy(alpha = 0.5f)
        ),
        content = label
    )
}

@Composable
private fun styleColors(style: CestouButtonStyle): Pair<Color, Color> = when (style) {
    CestouButtonStyle.Primary -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    CestouButtonStyle.Strong -> {
        val isDark = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE_THRESHOLD
        if (isDark) Color.White to SubtotalNight else SubtotalNight to Color.White
    }
    CestouButtonStyle.Accent -> SubtotalCoral to SubtotalNight
    // Outlined é tratado no CestouButton; este ramo só existe para o `when` ser exaustivo.
    CestouButtonStyle.Outlined -> Color.Transparent to MaterialTheme.colorScheme.primary
}

@Preview
@Composable
private fun CestouButtonPreview() {
    CestouButton(
        text = "Começar",
        onClick = {},
        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
    )
}
