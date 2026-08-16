package kz.ziro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.ziro.app.data.AuthRepository

@Composable
fun HomeScreen(role: String, onLoggedOut: () -> Unit) {
    val authRepository = androidx.compose.runtime.remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    val roleLabel = if (role == "admin") "Әкімші" else "Мұғалім"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Сәлем!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("$roleLabel ретінде кірдіңіз.", fontSize = 15.sp)

        Text(
            "Тест түрін құру, PDF генерациясы және сканерлеу бөлімдері жақын арада осында пайда болады.",
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    authRepository.logout()
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Шығу")
        }
    }
}
