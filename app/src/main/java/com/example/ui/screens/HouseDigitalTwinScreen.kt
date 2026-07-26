package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.RoomEntity
import com.example.data.models.ZoneEntity
import com.example.ui.viewmodels.FixiaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDigitalTwinScreen(
    viewModel: FixiaViewModel,
    onBack: () -> Unit,
    onNavigateToDiagnosticDetail: (Long) -> Unit
) {
    val rooms by viewModel.allRooms.collectAsState()
    var selectedRoom by remember { mutableStateOf<RoomEntity?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    val maintenancePlan by viewModel.houseMaintenancePlan.collectAsState()
    val isGeneratingPlan by viewModel.isGeneratingMaintenancePlan.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()

    var roomSchemaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var schemaError by remember { mutableStateOf<String?>(null) }

    // Initialize default rooms if list is empty
    LaunchedEffect(rooms) {
        if (rooms.isEmpty()) {
            viewModel.insertRoom("Cuisine", "cuisine", 0, "Équipements, plomberie et électroménager")
            viewModel.insertRoom("Salle de bain", "sdb", 1, "Chauffe-eau, douche, lavabo")
            viewModel.insertRoom("Salon / Séjour", "salon", 0, "Radiateurs, prises, baies vitrées")
            viewModel.insertRoom("Garage / Atelier", "garage", -1, "Tableau électrique principal, tableau d'outils")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jumeau Numérique - Ma Maison", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddRoomDialog = true }) {
                        Icon(Icons.Default.AddHome, contentDescription = "Ajouter une Pièce")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val summary = rooms.joinToString("\n") { "- Pièce: ${it.name} (${it.description})" }
                    viewModel.generateHouseMaintenancePlan(summary)
                },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Plan de Maintenance IA") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Maintenance Plan Banner if generated
            maintenancePlan?.let { plan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Plan Préventif Recommandé", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(plan, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            if (isGeneratingPlan) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("Toutes les Pièces de la Maison", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Room Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomGridCard(
                        room = room,
                        onClick = { selectedRoom = room },
                        onGenerateSchema = {
                            schemaError = null
                            viewModel.generateImage(
                                prompt = "Schéma architectural 3D épuré de la pièce : ${room.name}. Style moderne, lisible, fond clair.",
                                onSuccess = { bmp -> roomSchemaBitmap = bmp },
                                onError = { err -> schemaError = err }
                            )
                        }
                    )
                }
            }

            // Room Schema Bitmap Preview if generated
            roomSchemaBitmap?.let { bmp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Schéma Généré de la Pièce", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { roomSchemaBitmap = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer")
                            }
                        }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Schéma de la pièce",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            schemaError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }

    // Add Room Dialog
    if (showAddRoomDialog) {
        AddRoomDialog(
            onDismiss = { showAddRoomDialog = false },
            onConfirm = { name, type, floor, desc ->
                viewModel.insertRoom(name, type, floor, desc)
                showAddRoomDialog = false
            }
        )
    }

    // Room Detail Bottom Sheet
    selectedRoom?.let { room ->
        RoomDetailBottomSheet(
            room = room,
            viewModel = viewModel,
            onDismiss = { selectedRoom = null },
            onNavigateToDiagnostic = { diagId ->
                selectedRoom = null
                onNavigateToDiagnosticDetail(diagId)
            }
        )
    }
}

@Composable
private fun RoomGridCard(
    room: RoomEntity,
    onClick: () -> Unit,
    onGenerateSchema: () -> Unit
) {
    val icon = when (room.type.lowercase()) {
        "kitchen", "cuisine" -> Icons.Default.Kitchen
        "sdb", "bathtub" -> Icons.Default.Bathtub
        "salon", "weekend" -> Icons.Default.Weekend
        "chambre", "bed" -> Icons.Default.Bed
        "garage" -> Icons.Default.Garage
        "exterieur" -> Icons.Default.Park
        else -> Icons.Default.HomeWork
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                IconButton(onClick = onGenerateSchema) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Schéma IA", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column {
                Text(room.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = when (room.floor) {
                        0 -> "Rez-de-chaussée"
                        -1 -> "Sous-sol"
                        else -> "Étage ${room.floor}"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (room.description.isNotBlank()) {
                Text(
                    text = room.description,
                    maxLines = 2,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun AddRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, floor: Int, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("cuisine") }
    var floorStr by remember { mutableStateOf("0") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une Pièce au Jumeau Numérique") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la pièce (ex: Cuisine)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = floorStr,
                    onValueChange = { floorStr = it },
                    label = { Text("Étage (0 = RDC, 1 = Étage, -1 = Sous-sol)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Équipements & Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, type, floorStr.toIntOrNull() ?: 0, desc)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDetailBottomSheet(
    room: RoomEntity,
    viewModel: FixiaViewModel,
    onDismiss: () -> Unit,
    onNavigateToDiagnostic: (Long) -> Unit
) {
    val zones by viewModel.getZonesForRoom(room.id).collectAsState()
    val roomDiagnostics by viewModel.getDiagnosticsForRoom(room.id).collectAsState()

    var showAddZoneDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(room.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(room.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                IconButton(onClick = { viewModel.deleteRoom(room.id); onDismiss() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer la pièce", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zones & Équipements Clés", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(onClick = { showAddZoneDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter Zone")
                }
            }

            if (zones.isEmpty()) {
                Text("Aucune zone enregistrée (ex: Sous l'évier, Chauffe-eau)", fontSize = 12.sp, color = Color.Gray)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    zones.forEach { zone ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${zone.name} (${zone.equipmentName})") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteZone(zone.id) }
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Diagnostics Liés à cette Pièce", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            if (roomDiagnostics.isEmpty()) {
                Text("Aucun problème signalé dans cette pièce.", fontSize = 13.sp, color = Color.Gray)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(roomDiagnostics, key = { it.id }) { diag ->
                        Card(
                            onClick = { onNavigateToDiagnostic(diag.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(diag.titreProbleme, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddZoneDialog) {
        var zoneName by remember { mutableStateOf("") }
        var equipName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            title = { Text("Ajouter une Zone d'Équipement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        label = { Text("Zone (ex: Sous l'évier)") }
                    )
                    OutlinedTextField(
                        value = equipName,
                        onValueChange = { equipName = it },
                        label = { Text("Équipement (ex: Robinet Mitigeur)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (zoneName.isNotBlank()) {
                        viewModel.insertZone(room.id, zoneName, equipName)
                        showAddZoneDialog = false
                    }
                }) {
                    Text("Ajouter")
                }
            }
        )
    }
}
