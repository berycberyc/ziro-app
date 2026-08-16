package kz.ziro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kz.ziro.app.data.Registration
import kz.ziro.app.data.TestType
import kz.ziro.app.omr.AnalysisResult
import kz.ziro.app.omr.BubbleSheetAnalyzer

@Composable
fun CaptureAnswerSheetScreen(
    testType: TestType,
    registration: Registration? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Камера дайындалуда...") }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Артқа") }
        Text(
            "${testType.name_kk}" + (registration?.students?.full_name?.let { " — $it" } ?: ""),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(8.dp))

        if (result == null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (hasPermission) {
                    CapturePreview(
                        onImageCaptureReady = {
                            imageCapture = it
                            status = "Дайын. Түсіруге болады."
                        },
                        onError = { status = "Камера қатесі: $it" }
                    )
                } else {
                    Text("Камераға рұқсат керек")
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val capture = imageCapture
                    if (capture == null) {
                        status = "Камера әлі дайын емес, күте тұрыңыз."
                        return@Button
                    }
                    analyzing = true
                    status = "Түсірілуде..."
                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                status = "Сурет алынды, талдау басталды..."
                                try {
                                    val bytes = ByteArray(image.planes[0].buffer.remaining())
                                    image.planes[0].buffer.get(bytes)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    image.close()

                                    if (bitmap == null) {
                                        status = "Қате: сурет декодталмады."
                                        analyzing = false
                                        return
                                    }

                                    val analysis = BubbleSheetAnalyzer.analyze(bitmap, testType, pageNumber = 1)
                                    if (!analysis.cornersFound) {
                                        status = "Бұрыштық белгілер табылмады. Жарықты және бұрышты түзетіп, қайта түсіріңіз."
                                        analyzing = false
                                        return
                                    }
                                    status = "Дайын."
                                    result = analysis
                                } catch (e: Exception) {
                                    status = "Талдау қатесі: ${e.message ?: e.javaClass.simpleName}"
                                } finally {
                                    analyzing = false
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                status = "Түсіру қатесі: ${exception.message}"
                                analyzing = false
                            }
                        }
                    )
                },
                enabled = !analyzing && imageCapture != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (analyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Түсіру")
                }
            }
        } else {
            ResultList(result = result!!, onRetake = { result = null; status = "Дайын. Түсіруге болады." })
        }
    }
}

@Composable
private fun CapturePreview(
    onImageCaptureReady: (ImageCapture) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder().build()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                    )
                    onImageCaptureReady(capture)
                } catch (e: Exception) {
                    onError(e.message ?: e.javaClass.simpleName)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ResultList(result: AnalysisResult, onRetake: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Танылды: ${result.answers.count { it.detectedLabel != null }} / ${result.answers.size}",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Күмәнді: ${result.answers.count { !it.confident }}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(result.answers) { ans ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${ans.subject} №${ans.questionNumber}", fontSize = 12.sp)
                    Text(
                        ans.detectedLabel ?: "—",
                        fontSize = 12.sp,
                        color = if (!ans.confident) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
            Text("Қайта түсіру")
        }
    }
}
