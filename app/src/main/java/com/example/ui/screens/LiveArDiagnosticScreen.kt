package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.models.LiveArAnalysisResponse
import com.example.data.models.LiveArZone
import com.example.ui.viewmodels.FixiaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LiveArDiagnosticScreen(
    viewModel: FixiaViewModel,
    onBack: () -> Unit,
    onNavigateToNewDiagnostic: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Diagnostic Live AR", fontWeight = FontWeight.Bold)
                        LiveBadge()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToEmergency,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Mode Urgence")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraArContent(
                    viewModel = viewModel,
                    onNavigateToNewDiagnostic = onNavigateToNewDiagnostic,
                    onNavigateToEmergency = onNavigateToEmergency
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Permission caméra requise pour l'analyse AR en temps réel.",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() }
                        ) {
                            Text("Accorder l'accès caméra")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = Color.Red.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
            Text("DIRECT AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CameraArContent(
    viewModel: FixiaViewModel,
    onNavigateToNewDiagnostic: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val liveArResult by viewModel.liveArResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingLiveAr.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    var latestBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Periodic analysis loop
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(2000)
            latestBitmap?.let { bmp ->
                viewModel.analyzeLiveArFrame(bmp)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (!isPaused) {
                        val bitmap = imageProxy.toBitmapRotated()
                        latestBitmap = bitmap
                    }
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay AR bounding boxes & labels
        liveArResult?.let { result ->
            ArOverlayCanvas(
                analysisResponse = result,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Status Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = liveArResult?.titreDetection ?: "Analyse du flux en cours...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isAnalyzing) "Balayage Gemini Vision..." else "Prêt à détecter les anomalies",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // Danger Alert Banner
        AnimatedVisibility(
            visible = liveArResult?.isDangerImmediat == true,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Surface(
                color = Color.Red,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("DANGER IMMÉDIAT DÉTECTÉ !", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Button(
                        onClick = onNavigateToEmergency,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red)
                    ) {
                        Text("Activer le Mode Urgence Vitale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bottom Controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isPaused) Color.DarkGray else MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Figer/Reprendre",
                            tint = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            isPaused = true
                            onNavigateToNewDiagnostic()
                        },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Diagnostiquer l'Image", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Changer Caméra", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArOverlayCanvas(
    analysisResponse: LiveArAnalysisResponse,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        analysisResponse.overlayZones.forEach { zone ->
            val box = zone.box2d
            if (box.size == 4) {
                val ymin = (box[0] / 1000f) * height
                val xmin = (box[1] / 1000f) * width
                val ymax = (box[2] / 1000f) * height
                val xmax = (box[3] / 1000f) * width

                val rectWidth = xmax - xmin
                val rectHeight = ymax - ymin

                val boxColor = when (zone.severity.lowercase()) {
                    "red", "critique" -> Color.Red
                    "orange", "eleve" -> Color(0xFFFF9800)
                    else -> Color(0xFFFFEB3B)
                }

                // Draw bounding box
                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(xmin, ymin),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(width = 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                // Fill semi transparent
                drawRoundRect(
                    color = boxColor.copy(alpha = 0.15f),
                    topLeft = Offset(xmin, ymin),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
            }
        }
    }

    // Floating text labels on top of Canvas
    Box(modifier = modifier) {
        analysisResponse.overlayZones.forEach { zone ->
            val box = zone.box2d
            if (box.size == 4) {
                val boxColor = when (zone.severity.lowercase()) {
                    "red", "critique" -> Color.Red
                    "orange", "eleve" -> Color(0xFFFF9800)
                    else -> Color(0xFFFFEB3B)
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .border(1.5.dp, boxColor, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(boxColor)
                        )
                        Column {
                            Text(zone.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (zone.description.isNotBlank()) {
                                Text(zone.description, color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ImageProxy.toBitmapRotated(): Bitmap {
    val bitmap = toBitmap()
    val rotationDegrees = imageInfo.rotationDegrees
    if (rotationDegrees == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
