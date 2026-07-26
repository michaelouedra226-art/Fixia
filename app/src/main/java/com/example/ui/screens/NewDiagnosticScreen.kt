package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LocalMediaItem
import com.example.data.models.MediaType
import com.example.ui.theme.*
import com.example.ui.viewmodels.FixiaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDiagnosticScreen(
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAudioRecorder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAnalysisSuccess: (Long) -> Unit
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    val userDescription by viewModel.userDescription.collectAsState()
    val mediaItems = viewModel.selectedMediaItems

    // SAF Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addMediaItem(LocalMediaItem(it.toString(), MediaType.PHOTO, "Photo importée"))
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addMediaItem(LocalMediaItem(it.toString(), MediaType.VIDEO, "Vidéo importée"))
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addMediaItem(LocalMediaItem(it.toString(), MediaType.AUDIO, "Audio importé"))
        }
    }

    var showPickerChoiceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau Diagnostic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Section 1: Capture Source Selector
                item {
                    Text(
                        text = "1. Capturer ou ajouter un média",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SourceActionTile(
                            modifier = Modifier.weight(1f),
                            title = "Photo / Vidéo",
                            subtitle = "Caméra",
                            icon = Icons.Default.CameraAlt,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToCamera
                        )
                        SourceActionTile(
                            modifier = Modifier.weight(1f),
                            title = "Son / Bruit",
                            subtitle = "Enregistrer",
                            icon = Icons.Default.Mic,
                            color = UrgenceFaible,
                            onClick = onNavigateToAudioRecorder
                        )
                        SourceActionTile(
                            modifier = Modifier.weight(1f),
                            title = "Fichier",
                            subtitle = "Importer",
                            icon = Icons.Default.FolderOpen,
                            color = UrgenceMoyen,
                            onClick = { showPickerChoiceDialog = true }
                        )
                    }
                }

                // Attached Media Items
                if (mediaItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Médias ajoutés (${mediaItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(mediaItems) { item ->
                                MediaChipItem(
                                    item = item,
                                    onRemove = { viewModel.removeMediaItem(item) }
                                )
                            }
                        }
                    }
                }

                // Option: Mode Express vs Normal & Link to Tracked Problem
                item {
                    val isExpress by viewModel.isExpressMode.collectAsState()
                    val activeSuivis by viewModel.activeProblemesSuivis.collectAsState()
                    val selectedSuiviId by viewModel.selectedSuiviId.collectAsState()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("Diagnostic Express (< 4 sec)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("Synthèse rapide avec Gemini Flash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = isExpress,
                                    onCheckedChange = { viewModel.isExpressMode.value = it },
                                    modifier = Modifier.testTag("express_mode_switch")
                                )
                            }

                            if (activeSuivis.isNotEmpty()) {
                                Divider()
                                Text("Lier à un problème suivi existant :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedSuiviId == null,
                                            onClick = { viewModel.selectedSuiviId.value = null },
                                            label = { Text("Nouveau problème") }
                                        )
                                    }
                                    items(activeSuivis) { suivi ->
                                        FilterChip(
                                            selected = selectedSuiviId == suivi.id,
                                            onClick = { viewModel.selectedSuiviId.value = suivi.id },
                                            label = { Text(suivi.titre) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Optional Description

                item {
                    Text(
                        text = "2. Description du problème (optionnel)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userDescription,
                        onValueChange = { viewModel.userDescription.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("description_input"),
                        placeholder = {
                            Text("Décrivez les symptômes (ex: fuite d'eau sous le lavabo depuis hier, bruit de grincement dans la chaudière, odeur de brûlé...)")
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Info banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "L'analyse IA examine la texture, les odeurs (si décrites), l'acoustique du son et les structures visuelles pour déterminer l'urgence et le plan DIY.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Launch Analysis Button
            val canLaunch = mediaItems.isNotEmpty() || userDescription.isNotBlank()

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.runAnalysis(onSuccess = onAnalysisSuccess) },
                        enabled = canLaunch && !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("launch_analysis_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyse Gemini en cours...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lancer l'analyse IA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // File Picker Modal Dialog
            if (showPickerChoiceDialog) {
                AlertDialog(
                    onDismissRequest = { showPickerChoiceDialog = false },
                    title = { Text("Importer un fichier") },
                    text = { Text("Quel type de média souhaitez-vous importer depuis votre appareil ?") },
                    confirmButton = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showPickerChoiceDialog = false
                                    photoPickerLauncher.launch("image/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importer une Photo")
                            }

                            OutlinedButton(
                                onClick = {
                                    showPickerChoiceDialog = false
                                    videoPickerLauncher.launch("video/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importer une Vidéo")
                            }

                            OutlinedButton(
                                onClick = {
                                    showPickerChoiceDialog = false
                                    audioPickerLauncher.launch("audio/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importer un Fichier Audio")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPickerChoiceDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Error Dialog
            if (analysisError != null) {
                AlertDialog(
                    onDismissRequest = { },
                    icon = { Icon(Icons.Default.Error, contentDescription = null, tint = UrgenceCritique) },
                    title = { Text("Erreur d'analyse IA") },
                    text = { Text(analysisError ?: "Une erreur est survenue.") },
                    confirmButton = {
                        Button(onClick = onNavigateToSettings) {
                            Text("Ouvrir les Paramètres")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearNewDiagnosticForm() }) {
                            Text("Fermer")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SourceActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MediaChipItem(
    item: LocalMediaItem,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val icon = when (item.mediaType) {
                MediaType.PHOTO -> Icons.Default.Image
                MediaType.VIDEO -> Icons.Default.Videocam
                MediaType.AUDIO -> Icons.Default.Audiotrack
                MediaType.TEXT -> Icons.Default.TextFields
            }
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(text = item.fileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Supprimer", modifier = Modifier.size(16.dp))
            }
        }
    }
}
