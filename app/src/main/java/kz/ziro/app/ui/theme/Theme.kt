package kz.ziro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF151A2E)
val Parchment = Color(0xFFF6F4EE)
val Gold = Color(0xFFD6A83A)
val AdminAccent = Color(0xFF1C2036)

private val ZiroColorScheme = lightColorScheme(
    primary = Ink,
    secondary = AdminAccent,
    tertiary = Gold,
    background = Parchment,
    surface = Color.White
)

@Composable
fun ZiroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZiroColorScheme,
        content = content
    )
}
