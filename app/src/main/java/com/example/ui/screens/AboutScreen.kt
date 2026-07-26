package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("À propos", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HomeRepairService,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fixia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Version 2.0 (Finale)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Publié le 26 juillet 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Engagement Vie Privée & Sécurité", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text("• Strictement personnel — Aucune monétisation, aucune publicité, aucun compte utilisateur obligatoire.", style = MaterialTheme.typography.bodySmall)
                        Text("• Zéro collecte de données externes : vos diagnostics restent 100% locaux dans votre base de données Room chiffrée.", style = MaterialTheme.typography.bodySmall)
                        Text("• Vos clés d'API sont stockées exclusivement via EncryptedSharedPreferences (Jetpack Security).", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Technologies & Architecture", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text("• 100% Native Android (Kotlin + Jetpack Compose + Material 3)", style = MaterialTheme.typography.bodySmall)
                        Text("• Google Gemini API (Analyse multimodale : vision, audio, texte)", style = MaterialTheme.typography.bodySmall)
                        Text("• Clean Architecture + MVVM + Room Local DB + CameraX", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
