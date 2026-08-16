package kz.ziro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.ziro.app.data.SessionRepository
import kz.ziro.app.data.TestSession

@Composable
fun SessionPickerScreen(onBack: () -> Unit, onSelect: (TestSession) -> Unit) {
    val repo = remember { SessionRepository() }
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<TestSession>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            sessions = repo.getAll()
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }
        Text("Сессияны таңдаңыз", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (sessions.isEmpty()) {
            Text("Әзірге сессия жоқ.")
        } else {
            LazyColumn {
                items(sessions) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onSelect(s) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${s.title_kk} / ${s.title_ru}", fontWeight = FontWeight.SemiBold)
                            Text(s.session_date ?: "", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
