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
import kz.ziro.app.data.TestType
import kz.ziro.app.omr.SheetQrData
import kz.ziro.app.pdf.AnswerSheetPdfGenerator
import java.io.File

@Composable
fun AnswerSheetScreen(testType: TestType, onBack: () -> Unit, onScanSheet: () -> Unit) {
    val context = LocalContext.current
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "kz.ziro.app.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Бөлісу"))
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }

        Text(
            "${testType.name_kk} / ${testType.name_ru}",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))
        Text(
            testType.stages.joinToString(" · ") { "${it.subject} (${it.questions})" },
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                loading = true
                try {
                    generatedFile = AnswerSheetPdfGenerator.generate(context, testType)
                } catch (e: Exception) {
                    Toast.makeText(context, "Қате: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Бос парақ жасау (PDF)")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                loading = true
                try {
                    val fakeData = SheetQrData(
                        testTypeId = testType.id ?: "",
                        studentName = "Тест Оқушы",
                        classroom = "101",
                        variant = "A"
                    )
                    generatedFile = AnswerSheetPdfGenerator.generateWithQr(context, testType, fakeData)
                } catch (e: Exception) {
                    Toast.makeText(context, "Қате: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сынақ парағы (QR-мен, тест үшін)")
        }

        generatedFile?.let { file ->
            Spacer(Modifier.height(12.dp))
            Text("Дайын: ${file.name}", fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            Button(onClick = { shareFile(file) }, modifier = Modifier.fillMaxWidth()) {
                Text("Бөлісу (WhatsApp және т.б.)")
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onScanSheet, modifier = Modifier.fillMaxWidth()) {
            Text("Толтырылған парақты сканерлеу (сынақ)")
        }
    }
}
