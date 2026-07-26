package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiagnosticResponse(
    @Json(name = "urgence") val urgence: String = "faible", // "faible", "moyen", "eleve", "critique"
    @Json(name = "titre_probleme") val titreProbleme: String = "Problème non spécifié",
    @Json(name = "resume") val resume: String = "",
    @Json(name = "score_confiance_global") val scoreConfianceGlobal: Double = 0.85,
    @Json(name = "causes") val causes: List<DiagnosticCause> = emptyList(),
    @Json(name = "recommandation_principale") val recommandationPrincipale: String = "diy", // "diy", "professionnel", "surveillance"
    @Json(name = "niveau_diy") val niveauDiy: String = "debutant", // "debutant", "intermediaire", "avance"
    @Json(name = "plan_diy") val planDiy: List<DiyStep> = emptyList(),
    @Json(name = "estimation_prix_professionnel") val estimationPrixProfessionnel: ProEstimation = ProEstimation(),
    @Json(name = "temps_intervention_estime") val tempsInterventionEstime: String = "1 à 2 heures",
    @Json(name = "questions_clarification") val questionsClarification: List<String> = emptyList(),
    @Json(name = "avertissements") val avertissements: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DiagnosticCause(
    @Json(name = "cause") val cause: String = "",
    @Json(name = "probabilite") val probabilite: Double = 0.5,
    @Json(name = "explication") val explication: String = ""
)

@JsonClass(generateAdapter = true)
data class DiyStep(
    @Json(name = "etape") val etape: Int = 1,
    @Json(name = "titre") val titre: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "materiel_necessaire") val materielNecessaire: List<String> = emptyList(),
    @Json(name = "temps_estime_minutes") val tempsEstimeMinutes: Int = 15,
    @Json(name = "conseil_securite") val conseilSecurite: String = ""
)

@JsonClass(generateAdapter = true)
data class ProEstimation(
    @Json(name = "min") val min: Double = 50.0,
    @Json(name = "max") val max: Double = 150.0,
    @Json(name = "devise") val devise: String = "EUR"
)

enum class MediaType {
    PHOTO, VIDEO, AUDIO, TEXT
}

data class LocalMediaItem(
    val uriString: String,
    val mediaType: MediaType,
    val fileName: String = "Fichier"
)

@Entity(tableName = "diagnostics")
data class DiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val titreProbleme: String,
    val urgence: String, // "faible", "moyen", "eleve", "critique"
    val summary: String,
    val confidenceScore: Double,
    val recommendation: String, // "diy", "professionnel", "surveillance"
    val rawJsonResponse: String,
    val mediaUrisJson: String, // JSON array string of media URI paths
    val userNote: String = "",
    val isResolved: Boolean = false,
    val isFavorite: Boolean = false,
    val completedStepIndicesJson: String = "[]", // JSON array of completed DIY step numbers
    val userDescription: String = "",
    val hasReminder: Boolean = false,
    val reminderTimestamp: Long = 0L,
    val problemeSuiviId: Long? = null,
    val isPendingAnalysis: Boolean = false,
    val isExpressMode: Boolean = false,
    val beforeAfterType: String? = null, // "AVANT" or "APRES"
    val beforeDiagnosticId: Long? = null,
    val roomId: Long? = null,
    val zoneId: Long? = null
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "autre", // "cuisine", "sdb", "salon", "chambre", "garage", "exterieur", "combles"
    val iconName: String = "home",
    val floor: Int = 0,
    val description: String = ""
)

@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    val name: String,
    val equipmentName: String = ""
)

@Entity(tableName = "problemes_suivis")
data class ProblemeSuiviEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titre: String,
    val dateCreation: Long = System.currentTimeMillis(),
    val statut: String = "EN_COURS", // "EN_COURS", "SOUS_SURVEILLANCE", "RESOLU", "EMPIRE"
    val urgenceActuelle: String = "moyen",
    val diagnosticInitialId: Long = 0L,
    val derniereMiseAJour: Long = System.currentTimeMillis(),
    val roomId: Long? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val diagnosticId: Long,
    val sender: String, // "user" or "gemini"
    val message: String,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "connaissances_personnelles")
data class ConnaissancePersonnelleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val problemType: String,
    val solutionType: String, // "DIY" or "Pro"
    val materialsUsed: String = "",
    val timeSpentMinutes: Int = 0,
    val estimatedSavings: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ComparisonResponse(
    @Json(name = "evolution_gravite") val evolutionGravite: String = "Stabilité", // "Amélioration", "Stabilité", "Dégradation"
    @Json(name = "progression_regression") val progressionRegression: String = "Problème stable",
    @Json(name = "nouvelle_urgence_recommandee") val nouvelleUrgenceRecommandee: String = "moyen",
    @Json(name = "commentaire_evolution") val commentaireEvolution: String = ""
)

@JsonClass(generateAdapter = true)
data class BeforeAfterResponse(
    @Json(name = "verdict") val verdict: String = "RESOLU", // "RESOLU", "PARTIELLEMENT_RESOLU", "NON_RESOLU", "EMPIRE"
    @Json(name = "explication_detaillee") val explicationDetaillee: String = "",
    @Json(name = "nouveau_score_confiance") val nouveauScoreConfiance: Double = 0.9,
    @Json(name = "recommandation") val recommandation: String = "Réparation terminée avec succès."
)

@JsonClass(generateAdapter = true)
data class LiveArZone(
    @Json(name = "label") val label: String = "Anomalie détectée",
    @Json(name = "severity") val severity: String = "orange", // "red", "orange", "yellow"
    @Json(name = "box_2d") val box2d: List<Int> = listOf(200, 200, 800, 800), // ymin, xmin, ymax, xmax (0-1000)
    @Json(name = "description") val description: String = ""
)

@JsonClass(generateAdapter = true)
data class LiveArAnalysisResponse(
    @Json(name = "overlay_zones") val overlayZones: List<LiveArZone> = emptyList(),
    @Json(name = "is_danger_immediat") val isDangerImmediat: Boolean = false,
    @Json(name = "titre_detection") val titreDetection: String = "Analyse en cours",
    @Json(name = "niveau_urgence") val niveauUrgence: String = "faible"
)

@JsonClass(generateAdapter = true)
data class EmergencyActionStep(
    @Json(name = "etape") val etape: Int = 1,
    @Json(name = "action") val action: String = "",
    @Json(name = "conseil") val conseil: String = "",
    @Json(name = "is_critical") val isCritical: Boolean = true
)

@JsonClass(generateAdapter = true)
data class EmergencyPlanResponse(
    @Json(name = "titre") val titre: String = "Urgence Domestique",
    @Json(name = "type_danger") val typeDanger: String = "Général",
    @Json(name = "actions_prioritaires") val actionsPrioritaires: List<EmergencyActionStep> = emptyList(),
    @Json(name = "consigne_securite") val consigneSecurite: String = ""
)

