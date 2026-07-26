package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DiagnosticEntity
import com.example.ui.theme.UrgenceCritique
import com.example.ui.theme.UrgenceEleve
import com.example.ui.theme.UrgenceFaible
import com.example.ui.theme.UrgenceMoyen
import com.example.ui.viewmodels.FixiaViewModel
import com.example.utils.PdfExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedProblemTimelineScreen(
    suiviId: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNewControl: (Long) -> Unit
) {
    val context = LocalContext.current
    val suivi by viewModel.diagnosticRepo.getProblemeSuiviById(suiviId).collectAsState(initial = null)
    val linkedDiagnostics by viewModel.diagnosticRepo.getDiagnosticsForSuivi(suiviId).collectAsState(initial = emptyList())

    val currentSuivi = suivi

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentSuivi?.titre ?: "Suivi du problème",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            linkedDiagnostics.firstOrNull()?.let { firstDiag ->
                                PdfExportHelper.generateAndSavePdf(context, firstDiag)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Télécharger le PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToNewControl(suiviId) },
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("Nouveau contrôle") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.testTag("new_control_fab")
            )
        }
    ) { padding ->
        if (currentSuivi == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header status card
                item {
                    val statusBg = when (currentSuivi.statut) {
                        "RESOLU" -> UrgenceFaible.copy(alpha = 0.15f)
                        "EMPIRE" -> UrgenceCritique.copy(alpha = 0.15f)
                        "SOUS_SURVEILLANCE" -> UrgenceMoyen.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    }

                    val statusTint = when (currentSuivi.statut) {
                        "RESOLU" -> UrgenceFaible
                        "EMPIRE" -> UrgenceCritique
                        "SOUS_SURVEILLANCE" -> UrgenceMoyen
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, statusTint.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = statusBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = statusTint,
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = currentSuivi.statut.replace("_", " "),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Text(
                                    text = "Créé le ${SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(currentSuivi.dateCreation))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = currentSuivi.titre,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = statusTint)
                                Text(
                                    text = "${linkedDiagnostics.size} contrôle(s) dans la timeline",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Chronologie des diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (linkedDiagnostics.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun contrôle enregistré pour le moment.")
                            }
                        }
                    }
                } else {
                    items(linkedDiagnostics) { entity ->
                        TimelineItemCard(
                            entity = entity,
                            onClick = { onNavigateToDetail(entity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItemCard(
    entity: DiagnosticEntity,
    onClick: () -> Unit
) {
    val urgencyColor = when (entity.urgence.lowercase()) {
        "critique" -> UrgenceCritique
        "eleve" -> UrgenceEleve
        "moyen" -> UrgenceMoyen
        else -> UrgenceFaible
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(urgencyColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.FRANCE).format(Date(entity.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = urgencyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = entity.urgence.uppercase(),
                            color = urgencyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = entity.titreProbleme,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = entity.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (entity.beforeAfterType == "APRES") {
                    Surface(
                        color = UrgenceFaible.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Compare, contentDescription = null, tint = UrgenceFaible, modifier = Modifier.size(14.dp))
                            Text("Contrôle Avant / Après", style = MaterialTheme.typography.labelSmall, color = UrgenceFaible, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
