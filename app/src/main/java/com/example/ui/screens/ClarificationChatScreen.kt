package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatMessageEntity
import com.example.data.models.LocalMediaItem
import com.example.data.models.MediaType
import com.example.ui.viewmodels.FixiaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClarificationChatScreen(
    diagnosticId: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val diagnostic by viewModel.diagnosticRepo.getDiagnosticById(diagnosticId).collectAsState(initial = null)
    val chatMessages by viewModel.diagnosticRepo.getChatMessages(diagnosticId).collectAsState(initial = emptyList())
    val isChatThinking by viewModel.isChatThinking.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var selectedChatMedia by remember { mutableStateOf<LocalMediaItem?>(null) }
    val listState = rememberLazyListState()

    // Speech-to-Text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                textInput = if (textInput.isBlank()) spokenText else "$textInput $spokenText"
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedChatMedia = LocalMediaItem(
                uriString = it.toString(),
                mediaType = MediaType.PHOTO,
                fileName = "PhotoPrécision.jpg"
            )
        }
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size, isChatThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Assistant Clarification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = diagnostic?.titreProbleme ?: "Diagnostic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Selected media preview
                    selectedChatMedia?.let { media ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(media.fileName, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { selectedChatMedia = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Input bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Ajouter photo", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour dicter vos précisions...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Dictée vocale non supportée", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictée vocale", tint = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Répondre à l'assistant...") },
                            modifier = Modifier.weight(1f).testTag("chat_input_text"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        IconButton(
                            onClick = {
                                val text = textInput.trim()
                                val media = selectedChatMedia
                                textInput = ""
                                selectedChatMedia = null
                                viewModel.sendChatMessage(diagnosticId, text, media)
                            },
                            enabled = !isChatThinking && (textInput.isNotBlank() || selectedChatMedia != null),
                            modifier = Modifier.testTag("send_chat_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Clarification requirement banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Donnez plus de détails ou de photos à Gemini pour lever les doutes et affiner la précision du diagnostic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Text("Avez-vous remarqué une odeur, un bruit suspect, ou la date de première apparition du problème ?", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(chatMessages) { message ->
                        ChatMessageItem(message = message)
                    }
                }

                if (isChatThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Gemini analyse vos réponses...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!message.mediaUri.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                        Text("Média joint", style = MaterialTheme.typography.labelSmall, color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }

                if (message.message.isNotBlank()) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
