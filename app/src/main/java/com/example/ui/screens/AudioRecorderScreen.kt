package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.models.LocalMediaItem
import com.example.data.models.MediaType
import com.example.ui.theme.UrgenceFaible
import com.example.ui.theme.UrgenceCritique
import com.example.ui.viewmodels.FixiaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (!hasMicPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Enregistreur de Bruit") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(64.dp), tint = UrgenceFaible)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Permission Micro Requise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fixia analyse les sons et bruits anormaux (grincements de chaudière, sifflements de canalisation, grésillements électriques).", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Autoriser le Micro")
                }
            }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var recordSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordSeconds++
            }
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enregistrer un Son", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Analyse Acoustique", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Rapprochez votre téléphone de la source sonore (canalisation, moteur, chaudière, tableau électrique) et enregistrez 10 à 30 secondes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Central Animated Mic Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .background(
                            if (isRecording) UrgenceCritique.copy(alpha = 0.2f) else UrgenceFaible.copy(alpha = 0.15f),
                            CircleShape
                        )
                ) {
                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                // Start recording
                                val file = File(
                                    context.cacheDir,
                                    "FIXIA_AUDIO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp3"
                                )
                                try {
                                    val recorder = MediaRecorder().apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    mediaRecorder = recorder
                                    recordedFile = file
                                    isRecording = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erreur enregistreur : ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Stop recording
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                isRecording = false
                            }
                        },
                        modifier = Modifier
                            .size(90.dp)
                            .background(if (isRecording) UrgenceCritique else UrgenceFaible, CircleShape)
                            .testTag("audio_record_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Arrêter" else "Enregistrer",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = if (isRecording) "Enregistrement en cours..." else if (recordedFile != null) "Son enregistré !" else "Appuyez pour commencer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isRecording || recordSeconds > 0) {
                    val minutes = recordSeconds / 60
                    val seconds = recordSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Playback & Use Buttons
            if (recordedFile != null && !isRecording) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isPlaying) {
                                try {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(recordedFile!!.absolutePath)
                                        prepare()
                                        start()
                                        setOnCompletionListener {
                                            isPlaying = false
                                        }
                                    }
                                    isPlaying = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPlaying) "Pause" else "Écouter l'enregistrement")
                    }

                    Button(
                        onClick = {
                            viewModel.addMediaItem(
                                LocalMediaItem(
                                    uriString = Uri.fromFile(recordedFile).toString(),
                                    mediaType = MediaType.AUDIO,
                                    fileName = "Enregistrement Audio (${recordSeconds}s)"
                                )
                            )
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Utiliser ce son pour le diagnostic")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}
