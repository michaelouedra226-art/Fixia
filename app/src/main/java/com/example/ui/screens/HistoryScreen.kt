package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DiagnosticEntity
import com.example.ui.theme.*
import com.example.ui.viewmodels.FixiaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCompare: (Long, Long) -> Unit
) {
    val allDiagnostics by viewModel.allDiagnostics.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "favorites", "active", "resolved", "urgency"
    var isCompareMode by remember { mutableStateOf(false) }
    val selectedForCompare = remember { mutableStateListOf<Long>() }

    val filteredList = remember(allDiagnostics, searchQuery, selectedFilter) {
        allDiagnostics.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.titreProbleme.contains(searchQuery, ignoreCase = true) ||
                    item.summary.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "favorites" -> item.isFavorite
                "active" -> !item.isResolved
                "resolved" -> item.isResolved
                "critique" -> item.urgence.equals("critique", ignoreCase = true)
                "eleve" -> item.urgence.equals("eleve", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isCompareMode = !isCompareMode
                            selectedForCompare.clear()
                        },
                        modifier = Modifier.testTag("toggle_compare_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Mode Comparaison",
                            tint = if (isCompareMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isCompareMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedForCompare.size} / 2 sélectionnés pour comparaison",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                if (selectedForCompare.size == 2) {
                                    onNavigateToCompare(selectedForCompare[0], selectedForCompare[1])
                                }
                            },
                            enabled = selectedForCompare.size == 2,
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.testTag("launch_compare_button")
                        ) {
                            Text("Comparer")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("history_search_input"),
                placeholder = { Text("Rechercher un diagnostic...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                singleLine = true
            )

            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("Tous (${allDiagnostics.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "favorites",
                        onClick = { selectedFilter = "favorites" },
                        label = { Text("Favoris") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "active",
                        onClick = { selectedFilter = "active" },
                        label = { Text("En cours") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "resolved",
                        onClick = { selectedFilter = "resolved" },
                        label = { Text("Résolus") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "critique",
                        onClick = { selectedFilter = "critique" },
                        label = { Text("Critiques") }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun diagnostic ne correspond aux critères.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { entity ->
                        val isSelected = selectedForCompare.contains(entity.id)

                        Card(
                            onClick = {
                                if (isCompareMode) {
                                    if (isSelected) selectedForCompare.remove(entity.id)
                                    else if (selectedForCompare.size < 2) selectedForCompare.add(entity.id)
                                } else {
                                    onNavigateToDetail(entity.id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isCompareMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked && selectedForCompare.size < 2) selectedForCompare.add(entity.id)
                                            else selectedForCompare.remove(entity.id)
                                        }
                                    )
                                }

                                val urgencyColor = when (entity.urgence.lowercase()) {
                                    "critique" -> UrgenceCritique
                                    "eleve" -> UrgenceEleve
                                    "moyen" -> UrgenceMoyen
                                    else -> UrgenceFaible
                                }

                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(urgencyColor)
                                )

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(entity.titreProbleme, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (entity.isFavorite) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = UrgenceMoyen, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Text(
                                        text = entity.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(entity.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteDiagnostic(entity.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
