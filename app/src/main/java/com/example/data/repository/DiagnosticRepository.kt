package com.example.data.repository

import com.example.data.db.DiagnosticDao
import com.example.data.models.ChatMessageEntity
import com.example.data.models.ConnaissancePersonnelleEntity
import com.example.data.models.DiagnosticEntity
import com.example.data.models.DiagnosticResponse
import com.example.data.models.LocalMediaItem
import com.example.data.models.ProblemeSuiviEntity
import com.example.data.remote.GeminiApiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class DiagnosticRepository(
    private val dao: DiagnosticDao,
    private val geminiClient: GeminiApiClient,
    private val settingsRepository: SettingsRepository
) {
    val allDiagnostics: Flow<List<DiagnosticEntity>> = dao.getAllDiagnostics()
    val activeEmergencies: Flow<List<DiagnosticEntity>> = dao.getActiveEmergencies()
    val recentDiagnostics: Flow<List<DiagnosticEntity>> = dao.getRecentDiagnostics(5)
    val diagnosticCount: Flow<Int> = dao.getDiagnosticCount()

    val allProblemesSuivis: Flow<List<ProblemeSuiviEntity>> = dao.getAllProblemesSuivis()
    val activeProblemesSuivis: Flow<List<ProblemeSuiviEntity>> = dao.getActiveProblemesSuivis()
    val allConnaissances: Flow<List<ConnaissancePersonnelleEntity>> = dao.getAllConnaissances()

    fun getDiagnosticById(id: Long): Flow<DiagnosticEntity?> = dao.getDiagnosticById(id)
    fun getProblemeSuiviById(id: Long): Flow<ProblemeSuiviEntity?> = dao.getProblemeSuiviById(id)
    fun getDiagnosticsForSuivi(suiviId: Long): Flow<List<DiagnosticEntity>> = dao.getDiagnosticsForSuivi(suiviId)
    fun getChatMessages(diagnosticId: Long): Flow<List<ChatMessageEntity>> = dao.getChatMessagesForDiagnostic(diagnosticId)

    suspend fun runNewDiagnostic(
        mediaItems: List<LocalMediaItem>,
        userDescription: String,
        problemeSuiviId: Long? = null,
        isExpress: Boolean = false
    ): Result<Long> {
        val apiKey = settingsRepository.getGeminiApiKey()
        val qualityMode = if (isExpress) "flash" else settingsRepository.getAiQualityMode()

        // Get personal knowledge context
        val recentConnaissances = dao.getRecentConnaissancesSync(3)
        val knowledgeContext = if (recentConnaissances.isNotEmpty()) {
            recentConnaissances.joinToString("\n") {
                "- Problème: ${it.problemType} | Solution: ${it.solutionType} (${it.materialsUsed}) | Gain: ${it.estimatedSavings}€"
            }
        } else ""

        val apiResult = geminiClient.analyzeDiagnostic(
            apiKey = apiKey,
            qualityMode = qualityMode,
            mediaItems = mediaItems,
            userDescription = userDescription,
            personalKnowledgeContext = knowledgeContext
        )

        return if (apiResult.isSuccess) {
            val response = apiResult.getOrThrow()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val rawJson = moshi.adapter(DiagnosticResponse::class.java).toJson(response)

            val mediaUrisJsonArray = JSONArray()
            mediaItems.forEach { mediaUrisJsonArray.put(it.uriString) }

            val entity = DiagnosticEntity(
                titreProbleme = response.titreProbleme,
                urgence = response.urgence,
                summary = response.resume,
                confidenceScore = response.scoreConfianceGlobal,
                recommendation = response.recommandationPrincipale,
                rawJsonResponse = rawJson,
                mediaUrisJson = mediaUrisJsonArray.toString(),
                userDescription = userDescription,
                problemeSuiviId = problemeSuiviId,
                isExpressMode = isExpress
            )

            val newId = dao.insertDiagnostic(entity)

            // If linked to tracked problem, update tracked problem timestamp & current urgency
            if (problemeSuiviId != null) {
                val suivi = dao.getProblemeSuiviByIdSync(problemeSuiviId)
                if (suivi != null) {
                    dao.updateProblemeSuivi(
                        suivi.copy(
                            urgenceActuelle = response.urgence,
                            derniereMiseAJour = System.currentTimeMillis()
                        )
                    )
                }
            }

            Result.success(newId)
        } else {
            // Save offline pending if network/api failed
            val mediaUrisJsonArray = JSONArray()
            mediaItems.forEach { mediaUrisJsonArray.put(it.uriString) }

            val pendingEntity = DiagnosticEntity(
                titreProbleme = if (userDescription.isNotBlank()) userDescription.take(30) else "Diagnostic Hors-Ligne",
                urgence = "moyen",
                summary = "Analyse en attente de connexion réseau.",
                confidenceScore = 0.0,
                recommendation = "surveillance",
                rawJsonResponse = "{}",
                mediaUrisJson = mediaUrisJsonArray.toString(),
                userDescription = userDescription,
                problemeSuiviId = problemeSuiviId,
                isPendingAnalysis = true
            )
            val pendingId = dao.insertDiagnostic(pendingEntity)
            Result.success(pendingId)
        }
    }

    suspend fun createSuiviForDiagnostic(diagnosticId: Long): Long {
        val diagnostic = dao.getDiagnosticByIdSync(diagnosticId) ?: return 0L
        val suivi = ProblemeSuiviEntity(
            titre = diagnostic.titreProbleme,
            statut = "EN_COURS",
            urgenceActuelle = diagnostic.urgence,
            diagnosticInitialId = diagnosticId
        )
        val suiviId = dao.insertProblemeSuivi(suivi)
        dao.updateDiagnostic(diagnostic.copy(problemeSuiviId = suiviId))
        return suiviId
    }

    suspend fun runComparisonCheckup(
        suiviId: Long,
        mediaItems: List<LocalMediaItem>,
        userDescription: String
    ): Result<Long> {
        val apiKey = settingsRepository.getGeminiApiKey()
        val suivi = dao.getProblemeSuiviByIdSync(suiviId)
            ?: return Result.failure(Exception("Problème suivi introuvable."))

        val oldDiagnostics = dao.getDiagnosticByIdSync(suivi.diagnosticInitialId)
        val oldSummary = oldDiagnostics?.summary ?: suivi.titre

        val compResult = geminiClient.analyzeComparison(
            apiKey = apiKey,
            oldDiagnosticSummary = oldSummary,
            newMediaItems = mediaItems,
            newDescription = userDescription
        )

        val comp = compResult.getOrDefault(com.example.data.models.ComparisonResponse())

        val mediaUrisJsonArray = JSONArray()
        mediaItems.forEach { mediaUrisJsonArray.put(it.uriString) }

        val newDiagnostic = DiagnosticEntity(
            titreProbleme = "Contrôle: ${suivi.titre}",
            urgence = comp.nouvelleUrgenceRecommandee,
            summary = "${comp.evolutionGravite} - ${comp.commentaireEvolution}",
            confidenceScore = 0.88,
            recommendation = "surveillance",
            rawJsonResponse = "{}",
            mediaUrisJson = mediaUrisJsonArray.toString(),
            userDescription = userDescription,
            problemeSuiviId = suiviId
        )

        val newId = dao.insertDiagnostic(newDiagnostic)

        val newStatus = when (comp.evolutionGravite.lowercase()) {
            "amélioration" -> "SOUS_SURVEILLANCE"
            "dégradation" -> "EMPIRE"
            else -> suivi.statut
        }

        dao.updateProblemeSuivi(
            suivi.copy(
                statut = newStatus,
                urgenceActuelle = comp.nouvelleUrgenceRecommandee,
                derniereMiseAJour = System.currentTimeMillis()
            )
        )

        return Result.success(newId)
    }

    suspend fun runBeforeAfterCheck(
        diagnosticId: Long,
        afterMediaItems: List<LocalMediaItem>,
        afterDescription: String
    ): Result<com.example.data.models.BeforeAfterResponse> {
        val apiKey = settingsRepository.getGeminiApiKey()
        val beforeDiagnostic = dao.getDiagnosticByIdSync(diagnosticId)
            ?: return Result.failure(Exception("Diagnostic Avant introuvable."))

        val result = geminiClient.analyzeBeforeAfter(
            apiKey = apiKey,
            beforeSummary = beforeDiagnostic.summary,
            afterMediaItems = afterMediaItems,
            afterDescription = afterDescription
        )

        if (result.isSuccess) {
            val ba = result.getOrThrow()
            val mediaUrisJsonArray = JSONArray()
            afterMediaItems.forEach { mediaUrisJsonArray.put(it.uriString) }

            val afterDiagnostic = DiagnosticEntity(
                titreProbleme = "Contrôle Après: ${beforeDiagnostic.titreProbleme}",
                urgence = if (ba.verdict == "RESOLU") "faible" else beforeDiagnostic.urgence,
                summary = "Verdict: ${ba.verdict} - ${ba.explicationDetaillee}",
                confidenceScore = ba.nouveauScoreConfiance,
                recommendation = ba.recommandation,
                rawJsonResponse = "{}",
                mediaUrisJson = mediaUrisJsonArray.toString(),
                userDescription = afterDescription,
                problemeSuiviId = beforeDiagnostic.problemeSuiviId,
                beforeAfterType = "APRES",
                beforeDiagnosticId = diagnosticId,
                isResolved = ba.verdict == "RESOLU"
            )

            val afterId = dao.insertDiagnostic(afterDiagnostic)

            // Update tracked problem if exists
            beforeDiagnostic.problemeSuiviId?.let { suiviId ->
                val suivi = dao.getProblemeSuiviByIdSync(suiviId)
                if (suivi != null) {
                    val finalStatus = when (ba.verdict) {
                        "RESOLU" -> "RESOLU"
                        "EMPIRE" -> "EMPIRE"
                        else -> "SOUS_SURVEILLANCE"
                    }
                    dao.updateProblemeSuivi(
                        suivi.copy(
                            statut = finalStatus,
                            derniereMiseAJour = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        return result
    }

    suspend fun sendChatMessage(
        diagnosticId: Long,
        userMessageText: String,
        mediaItem: LocalMediaItem? = null
    ): Result<String> {
        val apiKey = settingsRepository.getGeminiApiKey()
        val diagnostic = dao.getDiagnosticByIdSync(diagnosticId)
            ?: return Result.failure(Exception("Diagnostic introuvable."))

        // Save user message
        val userMsgEntity = ChatMessageEntity(
            diagnosticId = diagnosticId,
            sender = "user",
            message = userMessageText,
            mediaUri = mediaItem?.uriString
        )
        dao.insertChatMessage(userMsgEntity)

        val history = dao.getChatMessagesForDiagnosticSync(diagnosticId)

        val geminiReplyResult = geminiClient.sendClarificationMessage(
            apiKey = apiKey,
            diagnosticSummary = diagnostic.summary,
            chatHistory = history,
            userMessage = userMessageText,
            mediaItem = mediaItem
        )

        if (geminiReplyResult.isSuccess) {
            val replyText = geminiReplyResult.getOrThrow()
            val geminiMsgEntity = ChatMessageEntity(
                diagnosticId = diagnosticId,
                sender = "gemini",
                message = replyText
            )
            dao.insertChatMessage(geminiMsgEntity)
        }

        return geminiReplyResult
    }

    suspend fun saveConnaissancePersonnelle(
        problemType: String,
        solutionType: String,
        materialsUsed: String,
        timeSpentMinutes: Int,
        estimatedSavings: Double
    ) {
        val entity = ConnaissancePersonnelleEntity(
            problemType = problemType,
            solutionType = solutionType,
            materialsUsed = materialsUsed,
            timeSpentMinutes = timeSpentMinutes,
            estimatedSavings = estimatedSavings
        )
        dao.insertConnaissance(entity)
    }

    suspend fun updateNote(id: Long, note: String) {
        val current = dao.getDiagnosticByIdSync(id) ?: return
        dao.updateDiagnostic(current.copy(userNote = note))
    }

    suspend fun toggleResolved(id: Long) {
        val current = dao.getDiagnosticByIdSync(id) ?: return
        val newResolved = !current.isResolved
        dao.updateDiagnostic(current.copy(isResolved = newResolved))

        // If tracked problem associated, update its status
        current.problemeSuiviId?.let { suiviId ->
            val suivi = dao.getProblemeSuiviByIdSync(suiviId)
            if (suivi != null) {
                dao.updateProblemeSuivi(
                    suivi.copy(
                        statut = if (newResolved) "RESOLU" else "EN_COURS",
                        derniereMiseAJour = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun toggleFavorite(id: Long) {
        val current = dao.getDiagnosticByIdSync(id) ?: return
        dao.updateDiagnostic(current.copy(isFavorite = !current.isFavorite))
    }

    suspend fun updateCompletedSteps(id: Long, completedIndices: Set<Int>) {
        val current = dao.getDiagnosticByIdSync(id) ?: return
        val jsonArray = JSONArray()
        completedIndices.forEach { jsonArray.put(it) }
        dao.updateDiagnostic(current.copy(completedStepIndicesJson = jsonArray.toString()))
    }

    suspend fun deleteDiagnostic(id: Long) {
        dao.deleteDiagnosticById(id)
    }

    suspend fun clearAllDiagnostics() {
        dao.clearAllDiagnostics()
    }
}

