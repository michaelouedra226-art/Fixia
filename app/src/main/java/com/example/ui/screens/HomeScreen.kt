package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.DiagnosticEntity
import com.example.ui.theme.*
import com.example.ui.viewmodels.FixiaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FixiaViewModel,
    onNavigateToNewDiagnostic: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToTrackedProblems: () -> Unit = {},
    onNavigateToKnowledgeStats: () -> Unit = {}
) {

    val context = LocalContext.current
    val activity = context as? Activity

    // Double back to exit state
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            activity?.finishAffinity()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Appuyez encore une fois pour quitter", Toast.LENGTH_SHORT).show()
        }
    }

    val recentDiagnostics by viewModel.recentDiagnostics.collectAsState()
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    val activeSuivis by viewModel.activeProblemesSuivis.collectAsState()
    val totalCount by viewModel.diagnosticCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HomeRepairService,
                                    contentDescription = "Fixia Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Fixia",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Text(
                                text = "Assistant Diagnostic Domestique",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewDiagnostic,
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White) },
                text = {
                    Text(
                        text = "NOUVEAU DIAGNOSTIC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("new_diagnostic_fab")
                    .padding(bottom = 8.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.fixia_hero_banner_1785084409995),
                            contentDescription = "Fixia Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "IA MULTIMODALE GEMINI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Diagnostiquez vos problèmes en secondes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Photo, vidéo, son ou texte — Fixia vous guide pas à pas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "Rechercher un diagnostic...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "TOTAL",
                        value = totalCount.toString(),
                        icon = Icons.Default.Assessment,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "URGENCES",
                        value = activeEmergencies.size.toString(),
                        icon = Icons.Default.Warning,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = UrgenceEleve,
                        borderColor = UrgenceEleve.copy(alpha = 0.25f)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "SUIVIS",
                        value = activeSuivis.size.toString(),
                        icon = Icons.Default.Timeline,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                }
            }

            // Quick Actions: Mode Suivi & Base Connaissances
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = onNavigateToTrackedProblems,
                        modifier = Modifier.weight(1f).testTag("tracked_problems_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Problèmes Suivis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${activeSuivis.size} en cours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        onClick = onNavigateToKnowledgeStats,
                        modifier = Modifier.weight(1f).testTag("knowledge_stats_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Column {
                                Text("Gains & Mémoire", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Économies DIY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }


            // Section 1: Urgences en cours (Active emergencies)
            if (activeEmergencies.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Urgences en cours",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = UrgenceEleve.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "${activeEmergencies.size} ACTIVE${if (activeEmergencies.size > 1) "S" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UrgenceEleve,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeEmergencies) { entity ->
                                EmergencyCard(
                                    entity = entity,
                                    onClick = { onNavigateToDetail(entity.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Derniers diagnostics
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Derniers diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text(
                            text = "VOIR TOUT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val filteredRecent = if (searchQuery.isBlank()) recentDiagnostics else {
                recentDiagnostics.filter {
                    it.titreProbleme.contains(searchQuery, ignoreCase = true) ||
                            it.summary.contains(searchQuery, ignoreCase = true)
                }
            }

            if (filteredRecent.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPasteSearch,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (totalCount == 0) "Aucun diagnostic enregistré" else "Aucun résultat trouvé",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (totalCount == 0) {
                                Button(
                                    onClick = onNavigateToNewDiagnostic,
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lancer mon premier diagnostic")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredRecent) { entity ->
                    DiagnosticItemCard(
                        entity = entity,
                        onClick = { onNavigateToDetail(entity.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun EmergencyCard(
    entity: DiagnosticEntity,
    onClick: () -> Unit
) {
    val urgencyColor = when (entity.urgence.lowercase()) {
        "critique" -> UrgenceCritique
        "eleve" -> UrgenceEleve
        else -> UrgenceMoyen
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = urgencyColor.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = urgencyColor,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = entity.urgence.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${(entity.confidenceScore * 100).toInt()}% Confiance",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = urgencyColor
                )
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(entity.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DiagnosticItemCard(
    entity: DiagnosticEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when (entity.urgence.lowercase()) {
        "critique" -> UrgenceCritique
        "eleve" -> UrgenceEleve
        "moyen" -> UrgenceMoyen
        else -> UrgenceFaible
    }

    val iconVector = when {
        entity.mediaUrisJson.contains("audio", ignoreCase = true) -> Icons.Default.Mic
        entity.mediaUrisJson.contains("video", ignoreCase = true) -> Icons.Default.Videocam
        entity.mediaUrisJson.contains("image", ignoreCase = true) || entity.mediaUrisJson.length > 5 -> Icons.Default.PhotoCamera
        else -> Icons.Default.Handyman
    }

    val iconBg = when {
        entity.isResolved -> UrgenceFaible.copy(alpha = 0.12f)
        entity.urgence.lowercase() == "critique" || entity.urgence.lowercase() == "eleve" -> UrgenceEleve.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }

    val iconTint = when {
        entity.isResolved -> UrgenceFaible
        entity.urgence.lowercase() == "critique" || entity.urgence.lowercase() == "eleve" -> UrgenceEleve
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

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
                        text = entity.titreProbleme,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (entity.isResolved) {
                        Surface(
                            color = UrgenceFaible.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "RÉSOLU",
                                style = MaterialTheme.typography.labelSmall,
                                color = UrgenceFaible,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = entity.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.FRANCE).format(Date(entity.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(entity.confidenceScore * 100).toInt()}% Confiance",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint,
                        fontWeight = FontWeight.SemiBold
                    )
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
