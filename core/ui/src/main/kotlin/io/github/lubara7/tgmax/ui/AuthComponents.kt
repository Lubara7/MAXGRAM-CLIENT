package io.github.lubara7.tgmax.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(88.dp)) {
        drawCircle(primary)
        val path = Path().apply {
            moveTo(size.width * .20f, size.height * .47f)
            lineTo(size.width * .78f, size.height * .25f)
            lineTo(size.width * .64f, size.height * .76f)
            lineTo(size.width * .45f, size.height * .58f)
            lineTo(size.width * .35f, size.height * .69f)
            lineTo(size.width * .36f, size.height * .53f)
            close()
        }
        drawPath(path, Color.White)
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(text) }
}
