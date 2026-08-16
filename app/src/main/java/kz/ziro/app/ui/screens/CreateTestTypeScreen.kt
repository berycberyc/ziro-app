package kz.ziro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.ziro.app.data.Stage
import kz.ziro.app.data.TestType
import kz.ziro.app.data.TestTypeRepository

private data class StageInput(
    var subject: String = "",
    var questions: String = "",
    var minutes: String = "",
    var format: String = "abcd"
)

@Composable
fun CreateTestTypeScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val repo = remember { TestTypeRepository() }
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var nameKk by remember { mutableStateOf("") }
    var nameRu by remember { mutableStateOf("") }
    var scoringScheme by remember { mutableStateOf("simple") }
    var stages by remember { mutableStateOf(listOf(StageInput())) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    val scoringOptions = listOf(
        "simple" to "Қарапайым (+1)",
        "penalty" to "Айыппұлмен (+4/-1/0)",
        "difficulty" to "Күрделілік бойынша",
        "adaptive" to "Бейімделген"
    )

    fun handleSave() {
        error = false
        loading = true
        scope.launch {
            val stagesJson = stages.map {
                Stage(
                    subject = it.subject,
                    questions = it.questions.toIntOrNull() ?: 0,
                    minutes = it.minutes.toIntOrNull() ?: 0,
                    format = it.format
                )
            }
            val success = repo.create(
                TestType(
                    code = code.uppercase(),
                    name_kk = nameKk,
                    name_ru = nameRu,
                    stages = stagesJson,
                    scoring_scheme = scoringScheme
                )
            )
            loading = false
            if (success) onSaved() else error = true
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Артқа") }
            Text("Тест түрін құру", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Код (мыс. TEXSCHOOL)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nameKk,
                onValueChange = { nameKk = it },
                label = { Text("Атауы (қазақша)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nameRu,
                onValueChange = { nameRu = it },
                label = { Text("Название (русский)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Кезеңдер", fontWeight = FontWeight.SemiBold)
        }

        itemsIndexed(stages) { index, stage ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = stage.subject,
                        onValueChange = { newVal ->
                            stages = stages.toMutableList().also {
                                it[index] = it[index].copy(subject = newVal)
                            }
                        },
                        label = { Text("Пән") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stage.questions,
                            onValueChange = { newVal ->
                                stages = stages.toMutableList().also {
                                    it[index] = it[index].copy(questions = newVal)
                                }
                            },
                            label = { Text("Сұрақ саны") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stage.minutes,
                            onValueChange = { newVal ->
                                stages = stages.toMutableList().also {
                                    it[index] = it[index].copy(minutes = newVal)
                                }
                            },
                            label = { Text("Минут") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = stage.format == "abcd",
                            onClick = {
                                stages = stages.toMutableList().also {
                                    it[index] = it[index].copy(format = "abcd")
                                }
                            },
                            label = { Text("АВСД") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = stage.format == "number",
                            onClick = {
                                stages = stages.toMutableList().also {
                                    it[index] = it[index].copy(format = "number")
                                }
                            },
                            label = { Text("Сан") }
                        )
                        Spacer(Modifier.weight(1f))
                        if (stages.size > 1) {
                            TextButton(onClick = {
                                stages = stages.toMutableList().also { it.removeAt(index) }
                            }) {
                                Text("✕ Өшіру")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { stages = stages + StageInput() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Кезең қосу")
            }

            Spacer(Modifier.height(16.dp))
            Text("Ұпай санау тәсілі", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            scoringOptions.forEach { (value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = scoringScheme == value,
                        onClick = { scoringScheme = value }
                    )
                    Text(label, fontSize = 14.sp)
                }
            }

            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("Қате шықты, қайта көріңіз.", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { handleSave() },
                enabled = !loading && code.isNotBlank() && nameKk.isNotBlank() && nameRu.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Сақтау")
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
