package kz.ziro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanResultScreen(rawValue: String, onScanAgain: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }

        Text("QR оқылды", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                rawValue,
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Бұл QR-дан оқылған шикі мәтін. Оқушыны/аудиторияны тану логикасы келесі қадамда қосылады.",
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Қайта сканерлеу")
        }
    }
}
