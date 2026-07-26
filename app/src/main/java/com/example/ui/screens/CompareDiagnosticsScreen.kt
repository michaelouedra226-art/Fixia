package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.DiagnosticResponse
import com.example.ui.viewmodels.FixiaViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareDiagnosticsScreen(
    id1: Long,
    id2: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val allDiagnostics by viewModel.allDiagnostics.collectAsState()
    val item1 = allDiagnostics.find { it.id == id1 }
    val item2 = allDiagnostics.find { it.id == id2 }

    if (item1 == null || item2 == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Comparaison") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Veuillez sélectionner deux diagnostics valides.")
            }
        }
        return
    }

    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val parsed1 = remember(item1.rawJsonResponse) {
        try { moshi.adapter(DiagnosticResponse::class.java).fromJson(item1.rawJsonResponse) } catch (e: Exception) { null }
    } ?: DiagnosticResponse()

    val parsed2 = remember(item2.rawJsonResponse) {
        try { moshi.adapter(DiagnosticResponse::class.java).fromJson(item2.rawJsonResponse) } catch (e: Exception) { null }
    } ?: DiagnosticResponse()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparaison de Diagnostics", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Column 1
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Diagnostic #1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(parsed1.titreProbleme, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                Text("Urgence ${item1.urgence.uppercase()}", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("Confiance: ${(parsed1.scoreConfianceGlobal * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Divider()
                            Text("Résumé:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(parsed1.resume, style = MaterialTheme.typography.bodySmall)
                            Divider()
                            Text("Prix Pro:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("${parsed1.estimationPrixProfessionnel.min.toInt()} € - ${parsed1.estimationPrixProfessionnel.max.toInt()} €", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Column 2
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Diagnostic #2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text(parsed2.titreProbleme, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                                Text("Urgence ${item2.urgence.uppercase()}", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("Confiance: ${(parsed2.scoreConfianceGlobal * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Divider()
                            Text("Résumé:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(parsed2.resume, style = MaterialTheme.typography.bodySmall)
                            Divider()
                            Text("Prix Pro:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("${parsed2.estimationPrixProfessionnel.min.toInt()} € - ${parsed2.estimationPrixProfessionnel.max.toInt()} €", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
