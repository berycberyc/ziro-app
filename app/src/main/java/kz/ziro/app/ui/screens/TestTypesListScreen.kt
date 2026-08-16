package kz.ziro.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var pendingDelete by remember { mutableStateOf<TestType?>(null) }

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
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpen(tt) }
                            ) {
                                Text("${tt.name_kk} / ${tt.name_ru}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    tt.stages.joinToString(" · ") { "${it.subject} (${it.questions})" },
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { pendingDelete = tt }) {
                                Text("Өшіру", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { tt ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Жою керек пе?") },
            text = { Text("${tt.name_kk} / ${tt.name_ru} өшіріледі. Бұл әрекетті болдырмау мүмкін емес.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = tt.id
                    pendingDelete = null
                    if (id != null) {
                        scope.launch {
                            repo.delete(id)
                            reload()
                        }
                    }
                }) {
                    Text("Өшіру", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Бас тарту")
                }
            }
        )
    }
}
