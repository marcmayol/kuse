package com.marcm.cadencia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.marcm.cadencia.R
import com.marcm.cadencia.ui.theme.Accent

/** La marca sola (anillo abierto con la rama), en el rojo de Kuse. */
@Composable
fun AppLogo(size: Dp = 56.dp, tint: Color = Accent, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_kuse_mark),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * Wordmark: redondeada, ligera y con tracking amplio (0.12em). Nunca en negrita —
 * si algún día se añade Quicksand como recurso de fuente, sólo hay que cambiar
 * [wordmarkFamily].
 */
private val wordmarkFamily = FontFamily.SansSerif

val WordmarkStyle = TextStyle(
    fontFamily = wordmarkFamily,
    fontWeight = FontWeight.Light,
    letterSpacing = 0.12.em
)

@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = "Kuse",
        style = WordmarkStyle.copy(fontSize = fontSize, color = color),
        modifier = modifier
    )
}

/** Marca + wordmark en fila, para cabeceras y onboarding. */
@Composable
fun AppLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 34.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(size = markSize)
        Wordmark(fontSize = fontSize)
    }
}
