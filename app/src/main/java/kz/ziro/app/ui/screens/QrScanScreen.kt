package kz.ziro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@Composable
fun QrScanScreen(onBack: () -> Unit, onScanned: (String) -> Unit) {
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

    var status by remember { mutableStateOf("Іске қосылуда...") }
    var framesAnalyzed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Артқа") }
            Spacer(Modifier.weight(1f))
        }

        Text(
            "QR-кодты камераға көрсетіңіз",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            "$status (кадр: $framesAnalyzed)",
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (hasPermission) {
                CameraPreviewWithQrDetection(
                    onQrDetected = onScanned,
                    onStatus = { status = it },
                    onFrameAnalyzed = { framesAnalyzed++ }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Камераға рұқсат керек")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Рұқсат беру")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithQrDetection(
    onQrDetected: (String) -> Unit,
    onStatus: (String) -> Unit,
    onFrameAnalyzed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var alreadyReported by remember { mutableStateOf(false) }

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

                    val scannerOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val scanner = BarcodeScanning.getClient(scannerOptions)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        onFrameAnalyzed()
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !alreadyReported) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstOrNull()?.rawValue
                                    if (value != null && !alreadyReported) {
                                        alreadyReported = true
                                        onStatus("Табылды!")
                                        onQrDetected(value)
                                    } else {
                                        onStatus("Іздеуде...")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    onStatus("Қате: ${e.message}")
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    onStatus("Камера іске қосылды")
                } catch (e: Exception) {
                    onStatus("Камера қатесі: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
