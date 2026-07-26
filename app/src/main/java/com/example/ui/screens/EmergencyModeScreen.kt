package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodels.FixiaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyModeScreen(
    viewModel: FixiaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedEmergencyType by remember { mutableStateOf("Électrique") }
    var userDetails by remember { mutableStateOf("") }

    val emergencyPlan by viewModel.emergencyPlan.collectAsState()
    val isGeneratingPlan by viewModel.isGeneratingEmergencyPlan.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()

    var generatedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val contactPhone = remember { viewModel.getEmergencyContactPhone() }
    val contactName = remember { viewModel.getEmergencyContactName() }

    LaunchedEffect(selectedEmergencyType) {
        viewModel.generateEmergencyPlan(selectedEmergencyType, userDetails)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Text("MODE URGENCE VITALE", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB71C1C))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High Visibility Emergency Call Panel
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "APPEL D'URGENCE IMMÉDIAT",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 112 Secours
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Secours 112", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Contact d'urgence
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(contactName.take(12), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Share emergency SMS / Text
                        OutlinedButton(
                            onClick = {
                                val text = "URGENCE FIXIA: Danger $selectedEmergencyType détecté. Besoin d'aide immédiate à mon domicile."
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Partager l'Alerte D'Urgence"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Partager Alerte & SMS de Secours", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Danger Type Selector
            item {
                Text(
                    "Sélectionner le type de danger :",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                val dangerTypes = listOf("Électrique", "Gaz / Fuite", "Dégât des Eaux", "Structure / Effondrement", "Infiltration / Fumée")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dangerTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedEmergencyType == type,
                            onClick = { selectedEmergencyType = type },
                            label = { Text(type, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF5350),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF333333),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            // Priority Actions Generated by Gemini
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CONSIGNES DE SÉCURITÉ IMMÉDIATES",
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (isGeneratingPlan) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Red, strokeWidth = 2.dp)
                            }
                        }

                        emergencyPlan?.actionsPrioritaires?.forEach { step ->
                            Surface(
                                color = Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = Color.Red,
                                        shape = CircleShape,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${step.etape}", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(step.action, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (step.conseil.isNotBlank()) {
                                            Text(step.conseil, color = Color.LightGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (emergencyPlan?.consigneSecurite?.isNotBlank() == true) {
                            Text(
                                text = "⚠️ ${emergencyPlan?.consigneSecurite}",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Visual Safety Schema / Image Generation with Gemini
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "GÉNÉRATION D'ILLUSTRATION DE SÉCURITÉ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            "Générez un schéma visuel montrant les gestes de coupure et mise en sécurité pour $selectedEmergencyType.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                imageError = null
                                val prompt = "Schéma technique épuré et pédagogique de sécurité domestique : Couper le courant / gaz / eau en cas d'urgence $selectedEmergencyType. Style illustration claire avec flèches d'action."
                                viewModel.generateImage(
                                    prompt = prompt,
                                    onSuccess = { bmp -> generatedImageBitmap = bmp },
                                    onError = { err -> imageError = err }
                                )
                            },
                            enabled = !isGeneratingImage,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingImage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Génération Gemini 2.5 Flash Image...")
                            } else {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Générer Schéma Visuel de Sécurité")
                            }
                        }

                        imageError?.let { err ->
                            Text(err, color = Color.Red, fontSize = 12.sp)
                        }

                        generatedImageBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Schéma de sécurité généré",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            // Exit Emergency Mode Button
            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White)
                ) {
                    Text("Quitter le Mode Urgence", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
