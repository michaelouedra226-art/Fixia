package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.models.LocalMediaItem
import com.example.data.models.MediaType
import com.example.ui.viewmodels.FixiaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Appareil Photo") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Autorisation Caméra Requise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Fixia a besoin d'accéder à la caméra pour prendre en photo ou vidéo le problème domestique à diagnostiquer.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la Caméra")
                }
            }
        }
        return
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    val imageCapture = remember { ImageCapture.Builder().setFlashMode(flashMode).build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraSelector, flashMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                imageCapture.flashMode = flashMode
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Framing Target Box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        )

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_back_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    Icon(icon, contentDescription = "Flash", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Changer Caméra", tint = Color.White)
                }
            }
        }

        // Bottom Capture Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cadrez le problème domestique (fuite, fissure, composant)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Capture Shutter Button
            Surface(
                onClick = {
                    val photoFile = File(
                        context.cacheDir,
                        "FIXIA_PHOTO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val savedUri = Uri.fromFile(photoFile)
                                capturedPhotoUri = savedUri
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, "Erreur photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(76.dp)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("shutter_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }

        // Confirmation Preview Modal Dialog
        if (capturedPhotoUri != null) {
            AlertDialog(
                onDismissRequest = { capturedPhotoUri = null },
                title = { Text("Photo capturée !") },
                text = { Text("Voulez-vous ajouter cette photo à votre diagnostic Fixia ?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addMediaItem(
                                LocalMediaItem(
                                    uriString = capturedPhotoUri.toString(),
                                    mediaType = MediaType.PHOTO,
                                    fileName = "Photo du problème"
                                )
                            )
                            capturedPhotoUri = null
                            onNavigateBack()
                        }
                    ) {
                        Text("Utiliser cette photo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { capturedPhotoUri = null }) {
                        Text("Reprendre")
                    }
                }
            )
        }
    }
}
