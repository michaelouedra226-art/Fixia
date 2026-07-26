package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.models.DiagnosticResponse
import com.example.ui.theme.UrgenceMoyen
import com.example.ui.viewmodels.FixiaViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ProArtisan(
    val name: String,
    val category: String,
    val rating: Double,
    val distanceKm: Double,
    val phone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProModeScreen(
    diagnosticId: Long,
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allDiagnostics by viewModel.allDiagnostics.collectAsState()
    val entity = allDiagnostics.find { it.id == diagnosticId }

    if (entity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Option Professionnelle") },
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

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "Localisation activée — Recherche des artisans à proximité", Toast.LENGTH_SHORT).show()
        }
    }

    val sampleArtisans = remember {
        listOf(
            ProArtisan("Artisan Dépannage Express", "Plomberie & Chauffage", 4.9, 1.2, "+33123456789"),
            ProArtisan("Bâtiment & Rénovation Pro", "Électricité & Structure", 4.8, 2.5, "+33198765432"),
            ProArtisan("SOS Fuite & Infiltration", "Plomberie & Étanchéité", 4.7, 3.8, "+33145678901"),
            ProArtisan("Atelier Électroménager", "Réparation Électroménager", 4.6, 4.1, "+33189012345")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intervention Professionnelle", fontWeight = FontWeight.Bold) },
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
            // Price & Duration Estimation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Euro, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Estimation du Coût d'Intervention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        val minP = parsedResponse.estimationPrixProfessionnel.min.toInt()
                        val maxP = parsedResponse.estimationPrixProfessionnel.max.toInt()
                        val currency = parsedResponse.estimationPrixProfessionnel.devise

                        Text(
                            text = "$minP € à $maxP $currency",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Durée estimée : ${parsedResponse.tempsInterventionEstime}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Location Permission Action
            item {
                Button(
                    onClick = {
                        if (!hasLocationPermission) {
                            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            // Search via Maps intent
                            val gmmIntentUri = Uri.parse("geo:0,0?q=artisan+depannage")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("locate_pro_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasLocationPermission) "Ouvrir la carte des artisans" else "Trouver un pro près de moi")
                }
            }

            // Recommended Artisans List
            item {
                Text("Artisans Référencés à Proximité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(sampleArtisans) { pro ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.White)
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(pro.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(pro.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = UrgenceMoyen, modifier = Modifier.size(14.dp))
                                Text("${pro.rating} (${pro.distanceKm} km)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pro.phone}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Phone, contentDescription = "Appeler", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Legal Disclaimer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Text("Avertissement Légal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Fixia est un outil d'orientation et d'estimation basé sur l'IA Gemini. L'application ne fournit aucun contrat de prestation et ne saurait être tenue responsable des travaux effectués par un tiers ou par l'utilisateur.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
