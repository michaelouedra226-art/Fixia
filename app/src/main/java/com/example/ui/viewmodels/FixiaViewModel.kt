package com.example.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.models.DiagnosticEntity
import com.example.data.models.LocalMediaItem
import com.example.data.remote.GeminiApiClient
import com.example.data.repository.DiagnosticRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FixiaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val geminiClient = GeminiApiClient(application)
    val settingsRepo = SettingsRepository(application)
    val diagnosticRepo = DiagnosticRepository(db.diagnosticDao(), db.roomDao(), geminiClient, settingsRepo)

    val allDiagnostics: StateFlow<List<DiagnosticEntity>> = diagnosticRepo.allDiagnostics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEmergencies: StateFlow<List<DiagnosticEntity>> = diagnosticRepo.activeEmergencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDiagnostics: StateFlow<List<DiagnosticEntity>> = diagnosticRepo.recentDiagnostics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diagnosticCount: StateFlow<Int> = diagnosticRepo.diagnosticCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allRooms: StateFlow<List<com.example.data.models.RoomEntity>> = diagnosticRepo.allRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themePreference: StateFlow<String> = settingsRepo.themeFlow
    val qualityMode: StateFlow<String> = settingsRepo.qualityFlow

    // New diagnostic state
    val selectedMediaItems = mutableStateListOf<LocalMediaItem>()
    val userDescription = MutableStateFlow("")

    // Analysis progress state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Test connection state
    private val _testConnectionResult = MutableStateFlow<Result<String>?>(null)
    val testConnectionResult: StateFlow<Result<String>?> = _testConnectionResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    // Step help state
    private val _stepHelpText = MutableStateFlow<String?>(null)
    val stepHelpText: StateFlow<String?> = _stepHelpText.asStateFlow()

    private val _isAskingStepHelp = MutableStateFlow(false)
    val isAskingStepHelp: StateFlow<Boolean> = _isAskingStepHelp.asStateFlow()

    fun addMediaItem(item: LocalMediaItem) {
        selectedMediaItems.add(item)
    }

    fun removeMediaItem(item: LocalMediaItem) {
        selectedMediaItems.remove(item)
    }

    fun clearNewDiagnosticForm() {
        selectedMediaItems.clear()
        userDescription.value = ""
        _analysisError.value = null
    }

    val activeProblemesSuivis: StateFlow<List<com.example.data.models.ProblemeSuiviEntity>> = diagnosticRepo.activeProblemesSuivis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProblemesSuivis: StateFlow<List<com.example.data.models.ProblemeSuiviEntity>> = diagnosticRepo.allProblemesSuivis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConnaissances: StateFlow<List<com.example.data.models.ConnaissancePersonnelleEntity>> = diagnosticRepo.allConnaissances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Options for linking diagnostic to tracked problem
    val selectedSuiviId = MutableStateFlow<Long?>(null)
    val isExpressMode = MutableStateFlow(false)

    // Chat thinking state
    private val _isChatThinking = MutableStateFlow(false)
    val isChatThinking: StateFlow<Boolean> = _isChatThinking.asStateFlow()

    // Before/After action state
    private val _isProcessingBeforeAfter = MutableStateFlow(false)
    val isProcessingBeforeAfter: StateFlow<Boolean> = _isProcessingBeforeAfter.asStateFlow()

    fun runAnalysis(onSuccess: (Long) -> Unit) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _analysisError.value = null

        viewModelScope.launch {
            val result = diagnosticRepo.runNewDiagnostic(
                mediaItems = selectedMediaItems.toList(),
                userDescription = userDescription.value,
                problemeSuiviId = selectedSuiviId.value,
                isExpress = isExpressMode.value
            )
            _isAnalyzing.value = false
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                clearNewDiagnosticForm()
                onSuccess(newId)
            } else {
                _analysisError.value = result.exceptionOrNull()?.message ?: "Erreur d'analyse."
            }
        }
    }

    fun createSuiviForDiagnostic(diagnosticId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val suiviId = diagnosticRepo.createSuiviForDiagnostic(diagnosticId)
            onCreated(suiviId)
        }
    }

    fun runComparisonCheckup(suiviId: Long, mediaItems: List<LocalMediaItem>, desc: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val res = diagnosticRepo.runComparisonCheckup(suiviId, mediaItems, desc)
            _isAnalyzing.value = false
            if (res.isSuccess) {
                onSuccess(res.getOrThrow())
            }
        }
    }

    fun runBeforeAfterCheck(diagnosticId: Long, afterMedia: List<LocalMediaItem>, afterDesc: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isProcessingBeforeAfter.value = true
            val res = diagnosticRepo.runBeforeAfterCheck(diagnosticId, afterMedia, afterDesc)
            _isProcessingBeforeAfter.value = false
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun sendChatMessage(diagnosticId: Long, msg: String, media: LocalMediaItem? = null) {
        if (msg.isBlank() && media == null) return
        viewModelScope.launch {
            _isChatThinking.value = true
            diagnosticRepo.sendChatMessage(diagnosticId, msg, media)
            _isChatThinking.value = false
        }
    }

    fun saveConnaissancePersonnelle(
        problemType: String,
        solutionType: String,
        materialsUsed: String,
        timeSpentMinutes: Int,
        estimatedSavings: Double
    ) {
        viewModelScope.launch {
            diagnosticRepo.saveConnaissancePersonnelle(
                problemType, solutionType, materialsUsed, timeSpentMinutes, estimatedSavings
            )
        }
    }


    fun testApiKey(key: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionResult.value = null
            val res = geminiClient.testApiKeyConnection(key)
            _testConnectionResult.value = res
            _isTestingConnection.value = false
        }
    }

    fun askStepHelp(problemTitle: String, stepTitle: String, stepDesc: String, question: String) {
        viewModelScope.launch {
            _isAskingStepHelp.value = true
            _stepHelpText.value = null
            val key = settingsRepo.getGeminiApiKey()
            val res = geminiClient.askStepHelp(key, problemTitle, stepTitle, stepDesc, question)
            _isAskingStepHelp.value = false
            _stepHelpText.value = res.getOrDefault("Impossible d'obtenir une réponse.")
        }
    }

    fun clearStepHelp() {
        _stepHelpText.value = null
    }

    fun updateNote(id: Long, note: String) {
        viewModelScope.launch { diagnosticRepo.updateNote(id, note) }
    }

    fun toggleResolved(id: Long) {
        viewModelScope.launch { diagnosticRepo.toggleResolved(id) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { diagnosticRepo.toggleFavorite(id) }
    }

    fun updateCompletedSteps(id: Long, completedIndices: Set<Int>) {
        viewModelScope.launch { diagnosticRepo.updateCompletedSteps(id, completedIndices) }
    }

    fun deleteDiagnostic(id: Long) {
        viewModelScope.launch { diagnosticRepo.deleteDiagnostic(id) }
    }

    fun clearAllDiagnostics() {
        viewModelScope.launch { diagnosticRepo.clearAllDiagnostics() }
    }

    // --- Rooms & Zones (Jumeau Numérique / Digital Twin) ---
    fun insertRoom(name: String, type: String, floor: Int, desc: String, onInserted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val room = com.example.data.models.RoomEntity(
                name = name,
                type = type,
                floor = floor,
                description = desc,
                iconName = when(type.lowercase()) {
                    "cuisine" -> "kitchen"
                    "sdb", "salle de bain" -> "bathtub"
                    "salon", "séjour" -> "weekend"
                    "chambre" -> "bed"
                    "garage" -> "garage"
                    "exterieur", "jardin" -> "park"
                    else -> "home"
                }
            )
            val id = diagnosticRepo.insertRoom(room)
            onInserted(id)
        }
    }

    fun deleteRoom(id: Long) {
        viewModelScope.launch { diagnosticRepo.deleteRoom(id) }
    }

    fun insertZone(roomId: Long, name: String, equipment: String) {
        viewModelScope.launch {
            val zone = com.example.data.models.ZoneEntity(roomId = roomId, name = name, equipmentName = equipment)
            diagnosticRepo.insertZone(zone)
        }
    }

    fun deleteZone(id: Long) {
        viewModelScope.launch { diagnosticRepo.deleteZone(id) }
    }

    fun getZonesForRoom(roomId: Long): StateFlow<List<com.example.data.models.ZoneEntity>> {
        return diagnosticRepo.getZonesForRoom(roomId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getDiagnosticsForRoom(roomId: Long): StateFlow<List<DiagnosticEntity>> {
        return diagnosticRepo.getDiagnosticsForRoom(roomId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // --- Image Generation (Gemini 2.5 Flash Image) ---
    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        onSuccess: (android.graphics.Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingImage.value = true
            val res = diagnosticRepo.generateImage(prompt, aspectRatio)
            _isGeneratingImage.value = false
            if (res.isSuccess) {
                onSuccess(res.getOrThrow())
            } else {
                onError(res.exceptionOrNull()?.message ?: "Échec de génération de l'image.")
            }
        }
    }

    // --- Live AR Diagnostic ---
    private val _liveArResult = MutableStateFlow<com.example.data.models.LiveArAnalysisResponse?>(null)
    val liveArResult: StateFlow<com.example.data.models.LiveArAnalysisResponse?> = _liveArResult.asStateFlow()

    private val _isAnalyzingLiveAr = MutableStateFlow(false)
    val isAnalyzingLiveAr: StateFlow<Boolean> = _isAnalyzingLiveAr.asStateFlow()

    fun analyzeLiveArFrame(bitmap: android.graphics.Bitmap) {
        if (_isAnalyzingLiveAr.value) return
        viewModelScope.launch {
            _isAnalyzingLiveAr.value = true
            val res = diagnosticRepo.analyzeLiveArFrame(bitmap)
            _isAnalyzingLiveAr.value = false
            if (res.isSuccess) {
                _liveArResult.value = res.getOrThrow()
            }
        }
    }

    fun clearLiveArResult() {
        _liveArResult.value = null
    }

    // --- Emergency Mode ---
    private val _emergencyPlan = MutableStateFlow<com.example.data.models.EmergencyPlanResponse?>(null)
    val emergencyPlan: StateFlow<com.example.data.models.EmergencyPlanResponse?> = _emergencyPlan.asStateFlow()

    private val _isGeneratingEmergencyPlan = MutableStateFlow(false)
    val isGeneratingEmergencyPlan: StateFlow<Boolean> = _isGeneratingEmergencyPlan.asStateFlow()

    fun generateEmergencyPlan(type: String, desc: String) {
        viewModelScope.launch {
            _isGeneratingEmergencyPlan.value = true
            val res = diagnosticRepo.generateEmergencyPlan(type, desc)
            _isGeneratingEmergencyPlan.value = false
            _emergencyPlan.value = res.getOrDefault(com.example.data.models.EmergencyPlanResponse())
        }
    }

    fun getEmergencyContactPhone(): String = settingsRepo.getEmergencyContactPhone()
    fun saveEmergencyContactPhone(phone: String) = settingsRepo.saveEmergencyContactPhone(phone)

    fun getEmergencyContactName(): String = settingsRepo.getEmergencyContactName()
    fun saveEmergencyContactName(name: String) = settingsRepo.saveEmergencyContactName(name)

    // --- House Maintenance Plan ---
    private val _houseMaintenancePlan = MutableStateFlow<String?>(null)
    val houseMaintenancePlan: StateFlow<String?> = _houseMaintenancePlan.asStateFlow()

    private val _isGeneratingMaintenancePlan = MutableStateFlow(false)
    val isGeneratingMaintenancePlan: StateFlow<Boolean> = _isGeneratingMaintenancePlan.asStateFlow()

    fun generateHouseMaintenancePlan(roomSummary: String) {
        viewModelScope.launch {
            _isGeneratingMaintenancePlan.value = true
            val res = diagnosticRepo.generateHouseMaintenancePlan(roomSummary)
            _isGeneratingMaintenancePlan.value = false
            _houseMaintenancePlan.value = res.getOrDefault("Plan de maintenance indisponible.")
        }
    }
}
