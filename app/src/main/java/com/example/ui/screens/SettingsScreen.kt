package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.UrgenceFaible
import com.example.ui.theme.UrgenceCritique
import com.example.ui.viewmodels.FixiaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FixiaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    val themeMode by viewModel.themePreference.collectAsState()
    val qualityMode by viewModel.qualityMode.collectAsState()

    val testResult by viewModel.testConnectionResult.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()

    LaunchedEffect(Unit) {
        apiKeyInput = viewModel.settingsRepo.getGeminiApiKey()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Section 1: Security & API Key
            item {
                Text("Clé API Google Gemini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Gestion de la clé d'API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            "Votre clé est stockée localement de façon sécurisée via EncryptedSharedPreferences.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            label = { Text("Clé API Gemini") },
                            singleLine = true,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Afficher/Masquer"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.settingsRepo.saveGeminiApiKey(apiKeyInput)
                                        Toast.makeText(context, "Clé API enregistrée !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_api_key_button")
                            ) {
                                Text("Enregistrer")
                            }

                            OutlinedButton(
                                onClick = { viewModel.testApiKey(apiKeyInput) },
                                enabled = apiKeyInput.isNotBlank() && !isTesting,
                                modifier = Modifier.weight(1f).testTag("test_api_key_button")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Tester la clé")
                                }
                            }
                        }

                        if (testResult != null) {
                            val isSuccess = testResult!!.isSuccess
                            Surface(
                                color = if (isSuccess) UrgenceFaible.copy(alpha = 0.15f) else UrgenceCritique.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) UrgenceFaible else UrgenceCritique
                                    )
                                    Text(
                                        text = if (isSuccess) testResult!!.getOrThrow() else testResult!!.exceptionOrNull()?.message ?: "Échec de connexion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSuccess) UrgenceFaible else UrgenceCritique,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Model Quality Selector
            item {
                Text("Performance de l'IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gemini 2.5 Flash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Diagnostic ultra-rapide (recommandé)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(
                                selected = qualityMode == "flash",
                                onClick = {
                                    coroutineScope.launch { viewModel.settingsRepo.setAiQualityMode("flash") }
                                }
                            )
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gemini 2.5 Pro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Analyse visuelle et technique approfondie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(
                                selected = qualityMode == "pro",
                                onClick = {
                                    coroutineScope.launch { viewModel.settingsRepo.setAiQualityMode("pro") }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Theme Selector
            item {
                Text("Apparence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thème Système", fontWeight = FontWeight.Medium)
                            RadioButton(selected = themeMode == "system", onClick = {
                                coroutineScope.launch { viewModel.settingsRepo.setThemePreference("system") }
                            })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thème Sombre", fontWeight = FontWeight.Medium)
                            RadioButton(selected = themeMode == "dark", onClick = {
                                coroutineScope.launch { viewModel.settingsRepo.setThemePreference("dark") }
                            })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thème Clair", fontWeight = FontWeight.Medium)
                            RadioButton(selected = themeMode == "light", onClick = {
                                coroutineScope.launch { viewModel.settingsRepo.setThemePreference("light") }
                            })
                        }
                    }
                }
            }

            // Section 4: Data & Storage
            item {
                Text("Données et Cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearAllDiagnostics()
                                Toast.makeText(context, "Historique effacé avec succès.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vider l'historique de l'application")
                        }
                    }
                }
            }

            // About Link
            item {
                Card(
                    onClick = onNavigateToAbout,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("about_screen_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("À propos de Fixia", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
