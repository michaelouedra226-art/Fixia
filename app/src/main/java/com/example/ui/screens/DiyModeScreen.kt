package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DiagnosticResponse
import com.example.data.models.DiyStep
import com.example.ui.theme.UrgenceFaible
import com.example.ui.theme.UrgenceMoyen
import com.example.ui.viewmodels.FixiaViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiyModeScreen(
    diagnosticId: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val allDiagnostics by viewModel.allDiagnostics.collectAsState()
    val entity = allDiagnostics.find { it.id == diagnosticId }

    if (entity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mode DIY") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Diagnostic introuvable.")
            }
        }
        return
    }

    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val parsedResponse = remember(entity.rawJsonResponse) {
        try {
            moshi.adapter(DiagnosticResponse::class.java).fromJson(entity.rawJsonResponse)
        } catch (e: Exception) {
            null
        }
    } ?: DiagnosticResponse()

    // Parse completed step indices from JSON
    val completedIndices = remember(entity.completedStepIndicesJson) {
        val set = mutableSetOf<Int>()
        try {
            val jsonArr = JSONArray(entity.completedStepIndicesJson)
            for (i in 0 until jsonArr.length()) {
                set.add(jsonArr.getInt(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        set
    }

    val totalSteps = parsedResponse.planDiy.size
    val completedCount = completedIndices.size
    val progress = if (totalSteps > 0) completedCount.toFloat() / totalSteps.toFloat() else 0f

    var activeHelpStep by remember { mutableStateOf<DiyStep?>(null) }
    var helpQuestionInput by remember { mutableStateOf("") }

    val stepHelpText by viewModel.stepHelpText.collectAsState()
    val isAskingStepHelp by viewModel.isAskingStepHelp.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Plan d'Action DIY", fontWeight = FontWeight.Bold)
                        Text(
                            "Niveau : ${parsedResponse.niveauDiy.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
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
            // Progress Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Progression de la réparation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("$completedCount / $totalSteps étapes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = UrgenceFaible,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        if (completedCount == totalSteps && totalSteps > 0) {
                            Surface(
                                color = UrgenceFaible.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = UrgenceFaible)
                                    Text("Bravo ! Toutes les étapes ont été complétées.", style = MaterialTheme.typography.labelMedium, color = UrgenceFaible, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Steps List
            itemsIndexed(parsedResponse.planDiy) { index, step ->
                val isCompleted = completedIndices.contains(index)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCompleted) UrgenceFaible else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Text("${step.etape}", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = step.titre,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { checked ->
                                    val newSet = completedIndices.toMutableSet()
                                    if (checked) newSet.add(index) else newSet.remove(index)
                                    viewModel.updateCompletedSteps(entity.id, newSet)
                                },
                                modifier = Modifier.testTag("step_checkbox_$index")
                            )
                        }

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Tools & Time
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("${step.tempsEstimeMinutes} min", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (step.materielNecessaire.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Hardware, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(step.materielNecessaire.joinToString(", "), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }

                        if (step.conseilSecurite.isNotBlank()) {
                            Surface(
                                color = UrgenceMoyen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = UrgenceMoyen, modifier = Modifier.size(18.dp))
                                    Text("Sécurité : ${step.conseilSecurite}", style = MaterialTheme.typography.labelSmall, color = UrgenceMoyen)
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                activeHelpStep = step
                                helpQuestionInput = ""
                                viewModel.clearStepHelp()
                            },
                            modifier = Modifier.align(Alignment.End).testTag("step_help_button_$index")
                        ) {
                            Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("J'ai besoin d'aide sur cette étape")
                        }
                    }
                }
            }
        }

        // Step Assistance Dialog with Gemini
        if (activeHelpStep != null) {
            val step = activeHelpStep!!
            AlertDialog(
                onDismissRequest = { activeHelpStep = null },
                title = { Text("Aide Gemini - Étape ${step.etape}") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(step.titre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        OutlinedTextField(
                            value = helpQuestionInput,
                            onValueChange = { helpQuestionInput = it },
                            placeholder = { Text("Posez votre question (ex: Quel outil utiliser si je n'ai pas la bonne clé ?)") },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (isAskingStepHelp) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Gemini prépare votre réponse...", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (stepHelpText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stepHelpText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.askStepHelp(
                                problemTitle = entity.titreProbleme,
                                stepTitle = step.titre,
                                stepDesc = step.description,
                                question = helpQuestionInput
                            )
                        },
                        enabled = helpQuestionInput.isNotBlank() && !isAskingStepHelp
                    ) {
                        Text("Demander à l'IA")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeHelpStep = null }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}
