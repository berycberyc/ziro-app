package kz.ziro.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kz.ziro.app.data.Registration
import kz.ziro.app.data.RegistrationRepository
import kz.ziro.app.data.TestSession
import kz.ziro.app.data.TestType
import kz.ziro.app.data.TestTypeRepository
import kz.ziro.app.pdf.AnswerSheetPdfGenerator
import java.io.File

@Composable
fun GenerateSheetsScreen(session: TestSession, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val regRepo = remember { RegistrationRepository() }
    val testTypeRepo = remember { TestTypeRepository() }

    var loading by remember { mutableStateOf(true) }
    var registrations by remember { mutableStateOf<List<Registration>>(emptyList()) }
    var missingDataCount by remember { mutableStateOf(0) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var generating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(session.id) {
        scope.launch {
            val all = regRepo.getForSession(session.id)
            registrations = all.filter { it.classroom != null && it.test_variant != null }
            missingDataCount = all.size - registrations.size
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }
        Text(
            "${session.title_kk} / ${session.title_ru}",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            Text("Дайын оқушылар: ${registrations.size}")
            if (missingDataCount > 0) {
                Text(
                    "Аудитория/нұсқа толтырылмаған: $missingDataCount (сайтта Excel арқылы толтырыңыз)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    generating = true
                    errorMessage = ""
                    scope.launch {
                        try {
                            val typeCache = mutableMapOf<String, TestType>()
                            val items = registrations.mapNotNull { reg ->
                                val tt = typeCache.getOrPut(reg.test_type_id) {
                                    testTypeRepo.getById(reg.test_type_id) ?: return@mapNotNull null
                                }
                                reg to tt
                            }
                            if (items.isEmpty()) {
                                errorMessage = "Дайын оқушы табылмады."
                            } else {
                                generatedFile = AnswerSheetPdfGenerator.generateBatch(
                                    context, session.title_ru, items
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = "Қате: ${e.message}"
                        } finally {
                            generating = false
                        }
                    }
                },
                enabled = !generating && registrations.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (generating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Жауап парақтарын дайындау")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            generatedFile?.let { file ->
                Spacer(Modifier.height(12.dp))
                Text("Дайын: ${file.name}", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val uri = FileProvider.getUriForFile(context, "kz.ziro.app.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Бөлісу"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Бөлісу (WhatsApp және т.б.)")
                }
            }
        }
    }
}
