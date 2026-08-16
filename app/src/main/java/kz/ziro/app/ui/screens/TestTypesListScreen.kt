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
import kz.ziro.app.data.TestType
import kz.ziro.app.data.TestTypeRepository

@Composable
fun TestTypesListScreen(onBack: () -> Unit, onCreateNew: () -> Unit, onOpen: (TestType) -> Unit) {
    val repo = remember { TestTypeRepository() }
    val scope = rememberCoroutineScope()
    var testTypes by remember { mutableStateOf<List<TestType>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            testTypes = repo.getAll()
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }
        Text("Тест түрлері", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(Modifier.height(16.dp))

        Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
            Text("+ Жаңа тест түрі")
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (testTypes.isEmpty()) {
            Text("Әзірге тест түрі жоқ.")
        } else {
            LazyColumn {
                items(testTypes) { tt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onOpen(tt) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${tt.name_kk} / ${tt.name_ru}", fontWeight = FontWeight.SemiBold)
                            Text(
                                tt.stages.joinToString(" · ") { "${it.subject} (${it.questions})" },
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
