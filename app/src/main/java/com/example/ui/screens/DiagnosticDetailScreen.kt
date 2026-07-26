package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DiagnosticResponse
import com.example.ui.theme.*
import com.example.ui.viewmodels.FixiaViewModel
import com.example.utils.PdfExporter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticDetailScreen(
    diagnosticId: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiy: (Long) -> Unit,
    onNavigateToPro: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit = {},
    onNavigateToTrackedTimeline: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val allDiagnostics by viewModel.allDiagnostics.collectAsState()
    val entity = allDiagnostics.find { it.id == diagnosticId }

    var showKnowledgeDialog by remember { mutableStateOf(false) }
    var solutionTypeInput by remember { mutableStateOf("") }
    var materialsInput by remember { mutableStateOf("") }
    var timeSpentInput by remember { mutableStateOf("30") }
    var estimatedSavingsInput by remember { mutableStateOf("80") }

    if (showKnowledgeDialog && entity != null) {
        AlertDialog(
            onDismissRequest = { showKnowledgeDialog = false },
            title = { Text("Félicitations ! Problème Résolu 🎉") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alimentez votre base de connaissances personnelle pour personnaliser vos futurs diagnostics :", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = solutionTypeInput,
                        onValueChange = { solutionTypeInput = it },
                        label = { Text("Solution appliquée (ex: Remplacement du joint)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = materialsInput,
                        onValueChange = { materialsInput = it },
                        label = { Text("Matériaux/Outils utilisés") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeSpentInput,
                            onValueChange = { timeSpentInput = it },
                            label = { Text("Temps (min)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = estimatedSavingsInput,
                            onValueChange = { estimatedSavingsInput = it },
                            label = { Text("Économie (€)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveConnaissancePersonnelle(
                            problemType = entity.titreProbleme,
                            solutionType = solutionTypeInput.ifBlank { "Réparation manuelle DIY" },
                            materialsUsed = materialsInput,
                            timeSpentMinutes = timeSpentInput.toIntOrNull() ?: 30,
                            estimatedSavings = estimatedSavingsInput.toDoubleOrNull() ?: 80.0
                        )
                        viewModel.toggleResolved(entity.id)
                        showKnowledgeDialog = false
                        Toast.makeText(context, "Connaissance enregistrée dans votre mémoire personnelle !", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("Enregistrer & Marquer Résolu")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleResolved(entity.id)
                        showKnowledgeDialog = false
                    }
                ) {
                    Text("Passer sans enregistrer")
                }
            }
        )
    }


    if (entity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Détail Diagnostic") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Diagnostic introuvable.")
            }
        }
        return
    }

    // Parse JSON
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val parsedResponse = remember(entity.rawJsonResponse) {
        try {
            moshi.adapter(DiagnosticResponse::class.java).fromJson(entity.rawJsonResponse)
        } catch (e: Exception) {
            null
        }
    } ?: DiagnosticResponse()

    val urgencyColor = when (entity.urgence.lowercase()) {
        "critique" -> UrgenceCritique
        "eleve" -> UrgenceEleve
        "moyen" -> UrgenceMoyen
        else -> UrgenceFaible
    }

    var showTechnicalDetails by remember { mutableStateOf(false) }
    var userNoteInput by remember(entity.userNote) { mutableStateOf(entity.userNote) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résultat Diagnostic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(entity.id) },
                        modifier = Modifier.testTag("favorite_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (entity.isFavorite) UrgenceMoyen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { PdfExporter.generateAndSharePdf(context, entity) },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = {
                        val shareTxt = "Fixia Diagnostic : ${entity.titreProbleme}\nUrgence : ${entity.urgence.uppercase()}\n\nRésumé : ${parsedResponse.resume}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareTxt)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Partager le diagnostic"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Urgency Badge & Title Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = urgencyColor.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = urgencyColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "URGENCE ${entity.urgence.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Confiance ${(parsedResponse.scoreConfianceGlobal * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = parsedResponse.titreProbleme,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "Diagnostic réalisé le ${SimpleDateFormat("dd MMMM yyyy à HH:mm", Locale.FRANCE).format(Date(entity.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Executive Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Summarize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Résumé Exécutif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = parsedResponse.resume,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Low Confidence Clarification Banner (< 75%) or Chat trigger
            item {
                Card(
                    onClick = { onNavigateToChat(entity.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_chat_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mini-chat de clarification IA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Posez des questions ou apportez des précisions en direct à Gemini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            // Mode Suivi Button
            item {
                OutlinedButton(
                    onClick = {
                        if (entity.problemeSuiviId != null) {
                            onNavigateToTrackedTimeline(entity.problemeSuiviId)
                        } else {
                            viewModel.createSuiviForDiagnostic(entity.id) { newSuiviId ->
                                onNavigateToTrackedTimeline(newSuiviId)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("track_problem_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (entity.problemeSuiviId != null) "Voir la timeline de suivi" else "Ajouter au mode Suivi dans le temps")
                }
            }

            // Causes section

            item {
                Text("Causes Probables", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    parsedResponse.causes.forEach { cause ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cause.cause,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${(cause.probabilite * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { cause.probabilite.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Text(
                                    text = cause.explication,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Mode Navigation Action Tiles (DIY vs PRO)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNavigateToDiy(entity.id) },
                        modifier = Modifier.weight(1f).height(52.dp).testTag("diy_mode_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (parsedResponse.recommandationPrincipale == "diy") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guide DIY Pas-à-Pas")
                    }

                    OutlinedButton(
                        onClick = { onNavigateToPro(entity.id) },
                        modifier = Modifier.weight(1f).height(52.dp).testTag("pro_mode_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Engineering, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Option Artisan / Pro")
                    }
                }
            }

            // Technical Details Accordion
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTechnicalDetails = !showTechnicalDetails },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Détails Techniques & Avertissements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showTechnicalDetails) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Niveau DIY recommandé : ${parsedResponse.niveauDiy.uppercase()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("Temps d'intervention pro estimé : ${parsedResponse.tempsInterventionEstime}", style = MaterialTheme.typography.bodySmall)
                                Text("Avertissements de sécurité :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                parsedResponse.avertissements.forEach { warn ->
                                    Text("⚠️ $warn", style = MaterialTheme.typography.bodySmall, color = UrgenceEleve)
                                }
                            }
                        }
                    }
                }
            }

            // Personal Notes Input
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notes Personnelles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = userNoteInput,
                        onValueChange = {
                            userNoteInput = it
                            viewModel.updateNote(entity.id, it)
                        },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("notes_input"),
                        placeholder = { Text("Ajoutez vos remarques, référence des pièces ou date de réparation...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Mark as Resolved Button
            item {
                Button(
                    onClick = {
                        if (!entity.isResolved) {
                            showKnowledgeDialog = true
                        } else {
                            viewModel.toggleResolved(entity.id)
                            Toast.makeText(context, "Statut réinitialisé", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resolve_toggle_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entity.isResolved) MaterialTheme.colorScheme.surfaceVariant else UrgenceFaible,
                        contentColor = if (entity.isResolved) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                ) {
                    Icon(if (entity.isResolved) Icons.Default.Undo else Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (entity.isResolved) "Marquer comme non résolu" else "Marquer ce problème comme résolu")
                }
            }

        }
    }
}
