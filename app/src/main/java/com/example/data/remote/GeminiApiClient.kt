package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.models.DiagnosticResponse
import com.example.data.models.LocalMediaItem
import com.example.data.models.MediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(DiagnosticResponse::class.java)

    private val systemPrompt = """
        Tu es un expert diagnostiqueur en bâtiment, plomberie, électricité, humidité, structure et électroménager avec 25 ans d’expérience terrain.

        Tu analyses des photos, vidéos et/ou enregistrements audio de problèmes domestiques.

        Tu dois répondre UNIQUEMENT en JSON valide strict selon ce schéma exact :

        {
          "urgence": "faible",
          "titre_probleme": "string",
          "resume": "string",
          "score_confiance_global": 0.85,
          "causes": [
            {
              "cause": "string",
              "probabilite": 0.8,
              "explication": "string"
            }
          ],
          "recommandation_principale": "diy",
          "niveau_diy": "debutant",
          "plan_diy": [
            {
              "etape": 1,
              "titre": "string",
              "description": "string",
              "materiel_necessaire": ["string"],
              "temps_estime_minutes": 15,
              "conseil_securite": "string"
            }
          ],
          "estimation_prix_professionnel": {
            "min": 50,
            "max": 150,
            "devise": "EUR"
          },
          "temps_intervention_estime": "1 à 2 heures",
          "questions_clarification": ["string"],
          "avertissements": ["string"]
        }

        Note sur 'urgence' : doit être l'un de : "faible", "moyen", "eleve", "critique".
        Note sur 'recommandation_principale' : doit être l'un de : "diy", "professionnel", "surveillance".
        Note sur 'niveau_diy' : doit être l'un de : "debutant", "intermediaire", "avance".

        Sois précis, prudent et privilégie toujours la sécurité des personnes. Si les informations sont insuffisantes, utilise le champ questions_clarification.
    """.trimIndent()

    private val comparisonAdapter = moshi.adapter(com.example.data.models.ComparisonResponse::class.java)
    private val beforeAfterAdapter = moshi.adapter(com.example.data.models.BeforeAfterResponse::class.java)
    private val liveArAdapter = moshi.adapter(com.example.data.models.LiveArAnalysisResponse::class.java)
    private val emergencyAdapter = moshi.adapter(com.example.data.models.EmergencyPlanResponse::class.java)

    suspend fun analyzeDiagnostic(
        apiKey: String,
        qualityMode: String,
        mediaItems: List<LocalMediaItem>,
        userDescription: String,
        personalKnowledgeContext: String = ""
    ): Result<DiagnosticResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Clé API Gemini manquante. Veuillez la configurer dans les Paramètres."))
        }

        val model = if (qualityMode == "pro") "gemini-2.5-pro" else "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val partsArray = JSONArray()

            // 1. Convert user description if present
            val promptText = StringBuilder("Description fournie par l'utilisateur : ")
            if (userDescription.isNotBlank()) {
                promptText.append(userDescription)
            } else {
                promptText.append("Aucune description textuelle. Analyse les fichiers multimédias joints.")
            }
            if (personalKnowledgeContext.isNotBlank()) {
                promptText.append("\n\nConnaissances personnelles de l'utilisateur :\n").append(personalKnowledgeContext)
            }
            promptText.append("\nFournis un diagnostic complet selon le format JSON requis.")

            partsArray.put(JSONObject().put("text", promptText.toString()))

            // 2. Add media items as inlineData
            for (item in mediaItems) {
                val mediaData = readUriAsBase64(item.uriString, item.mediaType)
                if (mediaData != null) {
                    val mimeType = when (item.mediaType) {
                        MediaType.PHOTO -> "image/jpeg"
                        MediaType.VIDEO -> "video/mp4"
                        MediaType.AUDIO -> "audio/mp3"
                        MediaType.TEXT -> "text/plain"
                    }
                    val inlineObj = JSONObject()
                        .put("mimeType", mimeType)
                        .put("data", mediaData)
                    partsArray.put(JSONObject().put("inlineData", inlineObj))
                }
            }

            val rootObj = JSONObject()
            val contentObj = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentObj)
            rootObj.put("contents", contentsArray)

            val fullSystemPrompt = if (personalKnowledgeContext.isNotBlank()) {
                "$systemPrompt\n\nPrends en compte le contexte suivant issu des résolutions précédentes de l'utilisateur :\n$personalKnowledgeContext"
            } else {
                systemPrompt
            }

            val systemInstruction = JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", fullSystemPrompt))
            )
            rootObj.put("systemInstruction", systemInstruction)

            val generationConfig = JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.25)
            rootObj.put("generationConfig", generationConfig)

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erreur API Gemini (${response.code}) : $responseBodyString"))
            }

            val jsonRes = JSONObject(responseBodyString)
            val candidates = jsonRes.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("Réponse Gemini vide ou bloquée par les filtres de sécurité."))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawJsonText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (rawJsonText.isBlank()) {
                return@withContext Result.failure(Exception("Impossible d'extraire la réponse JSON de Gemini."))
            }

            val cleanedJson = rawJsonText
                .replace("^```json".toRegex(), "")
                .replace("^```".toRegex(), "")
                .replace("```$".toRegex(), "")
                .trim()

            val parsedResponse = responseAdapter.fromJson(cleanedJson)
                ?: return@withContext Result.failure(Exception("Échec du parsing du schéma JSON de la réponse."))

            Result.success(parsedResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeComparison(
        apiKey: String,
        oldDiagnosticSummary: String,
        newMediaItems: List<LocalMediaItem>,
        newDescription: String
    ): Result<com.example.data.models.ComparisonResponse> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        try {
            val prompt = """
                Ancien diagnostic : $oldDiagnosticSummary
                Nouveau contrôle effectué par l'utilisateur : $newDescription
                
                Analyse l'évolution de la situation.
                Réponds UNIQUEMENT en JSON strict avec ce schéma :
                {
                  "evolution_gravite": "Amélioration" | "Stabilité" | "Dégradation",
                  "progression_regression": "Explication courte",
                  "nouvelle_urgence_recommandee": "faible" | "moyen" | "eleve" | "critique",
                  "commentaire_evolution": "Commentaire détaillé d'évolution"
                }
            """.trimIndent()

            val partsArray = JSONArray().put(JSONObject().put("text", prompt))
            for (item in newMediaItems) {
                val mediaData = readUriAsBase64(item.uriString, item.mediaType)
                if (mediaData != null) {
                    val mimeType = when (item.mediaType) {
                        MediaType.PHOTO -> "image/jpeg"
                        MediaType.VIDEO -> "video/mp4"
                        MediaType.AUDIO -> "audio/mp3"
                        MediaType.TEXT -> "text/plain"
                    }
                    partsArray.put(JSONObject().put("inlineData", JSONObject().put("mimeType", mimeType).put("data", mediaData)))
                }
            }

            val rootObj = JSONObject()
            rootObj.put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
            rootObj.put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val rawJsonText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
            val cleanedJson = rawJsonText.replace("^```json".toRegex(), "").replace("^```".toRegex(), "").replace("```$".toRegex(), "").trim()

            val res = comparisonAdapter.fromJson(cleanedJson) ?: com.example.data.models.ComparisonResponse()
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeBeforeAfter(
        apiKey: String,
        beforeSummary: String,
        afterMediaItems: List<LocalMediaItem>,
        afterDescription: String
    ): Result<com.example.data.models.BeforeAfterResponse> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        try {
            val prompt = """
                Problème AVANT : $beforeSummary
                Observations APRÈS la réparation : $afterDescription
                
                Analyse les médias APRÈS et détermine si la réparation a fonctionné.
                Réponds UNIQUEMENT au format JSON strict :
                {
                  "verdict": "RESOLU" | "PARTIELLEMENT_RESOLU" | "NON_RESOLU" | "EMPIRE",
                  "explication_detaillee": "Explication claire",
                  "nouveau_score_confiance": 0.95,
                  "recommandation": "Consigne ou suite à donner"
                }
            """.trimIndent()

            val partsArray = JSONArray().put(JSONObject().put("text", prompt))
            for (item in afterMediaItems) {
                val mediaData = readUriAsBase64(item.uriString, item.mediaType)
                if (mediaData != null) {
                    val mimeType = when (item.mediaType) {
                        MediaType.PHOTO -> "image/jpeg"
                        MediaType.VIDEO -> "video/mp4"
                        MediaType.AUDIO -> "audio/mp3"
                        MediaType.TEXT -> "text/plain"
                    }
                    partsArray.put(JSONObject().put("inlineData", JSONObject().put("mimeType", mimeType).put("data", mediaData)))
                }
            }

            val rootObj = JSONObject()
            rootObj.put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
            rootObj.put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val rawJsonText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
            val cleanedJson = rawJsonText.replace("^```json".toRegex(), "").replace("^```".toRegex(), "").replace("```$".toRegex(), "").trim()

            val res = beforeAfterAdapter.fromJson(cleanedJson) ?: com.example.data.models.BeforeAfterResponse()
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendClarificationMessage(
        apiKey: String,
        diagnosticSummary: String,
        chatHistory: List<com.example.data.models.ChatMessageEntity>,
        userMessage: String,
        mediaItem: LocalMediaItem?
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        try {
            val historyBuilder = StringBuilder("Contexte du diagnostic : $diagnosticSummary\n\nHistorique de discussion :\n")
            chatHistory.forEach {
                historyBuilder.append("${if (it.sender == "user") "Utilisateur" else "Assistant Gemini"}: ${it.message}\n")
            }
            historyBuilder.append("Nouveau message utilisateur : $userMessage")

            val partsArray = JSONArray().put(JSONObject().put("text", historyBuilder.toString()))
            if (mediaItem != null) {
                val mediaData = readUriAsBase64(mediaItem.uriString, mediaItem.mediaType)
                if (mediaData != null) {
                    val mimeType = when (mediaItem.mediaType) {
                        MediaType.PHOTO -> "image/jpeg"
                        MediaType.VIDEO -> "video/mp4"
                        MediaType.AUDIO -> "audio/mp3"
                        MediaType.TEXT -> "text/plain"
                    }
                    partsArray.put(JSONObject().put("inlineData", JSONObject().put("mimeType", mimeType).put("data", mediaData)))
                }
            }

            val rootObj = JSONObject()
            rootObj.put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val replyText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "Merci pour ces précisions."
            Result.success(replyText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun testApiKeyConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Clé API vide"))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        try {
            val rootObj = JSONObject()
            val contentObj = JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", "Réponds simplement 'OK'."))
            )
            rootObj.put("contents", JSONArray().put(contentObj))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Connexion Gemini réussie !")
            } else {
                Result.failure(Exception("Erreur ${response.code}: ${response.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askStepHelp(
        apiKey: String,
        problemTitle: String,
        stepTitle: String,
        stepDescription: String,
        userQuestion: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        try {
            val prompt = """
                Problème : $problemTitle
                Étape : $stepTitle - $stepDescription
                Question de l'utilisateur : $userQuestion
                
                En tant qu'expert bricolage domestique, donne une explication claire, encourageante et ultra-sécurisée pour aider l'utilisateur à réaliser cette étape spécifique.
            """.trimIndent()

            val rootObj = JSONObject()
            val contentObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            rootObj.put("contents", JSONArray().put(contentObj))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "Pas de réponse"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateImage(
        apiKey: String,
        prompt: String,
        aspectRatio: String = "1:1"
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Clé API Gemini manquante. Veuillez la configurer dans les paramètres."))
        }

        val model = "gemini-2.5-flash-image"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val rootObj = JSONObject()
            val contentObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            rootObj.put("contents", JSONArray().put(contentObj))

            val genConfig = JSONObject()
                .put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                .put("imageConfig", JSONObject().put("aspectRatio", aspectRatio))
            rootObj.put("generationConfig", genConfig)

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erreur de génération d'image (${response.code}) : $resStr"))
            }

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("Aucune image générée par Gemini."))
            }

            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val base64Data = inlineData.optString("data")
                        if (base64Data.isNotBlank()) {
                            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            if (bitmap != null) {
                                return@withContext Result.success(bitmap)
                            }
                        }
                    }
                }
            }

            Result.failure(Exception("Format d'image non trouvé dans la réponse."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeLiveArFrame(
        apiKey: String,
        bitmap: Bitmap
    ): Result<com.example.data.models.LiveArAnalysisResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Clé API manquante."))
        }

        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
                Analyse cette image de diagnostic domestique instantané pour superposition Réalité Augmentée (Live AR).
                Identifie les zones d'anomalies (eau, électricité, fissure, surchauffe, usure) avec leurs coordonnées normalisées (0 à 1000).
                
                Réponds STRICTEMENT en JSON selon ce schéma :
                {
                  "overlay_zones": [
                    {
                      "label": "Titre court du problème",
                      "severity": "red" | "orange" | "yellow",
                      "box_2d": [ymin, xmin, ymax, xmax],
                      "description": "Explication rapide"
                    }
                  ],
                  "is_danger_immediat": false,
                  "titre_detection": "Analyse rapide",
                  "niveau_urgence": "faible" | "moyen" | "eleve" | "critique"
                }
            """.trimIndent()

            val partsArray = JSONArray()
                .put(JSONObject().put("text", prompt))
                .put(JSONObject().put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", base64Image)))

            val rootObj = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erreur AR: $resStr"))
            }

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val rawJsonText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
            val cleanedJson = rawJsonText.replace("^```json".toRegex(), "").replace("^```".toRegex(), "").replace("```$".toRegex(), "").trim()

            val parsed = liveArAdapter.fromJson(cleanedJson) ?: com.example.data.models.LiveArAnalysisResponse()
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateEmergencyPlan(
        apiKey: String,
        emergencyType: String,
        description: String
    ): Result<com.example.data.models.EmergencyPlanResponse> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val prompt = """
                URGENCE VITALE / DANGER IMMÉDIAT DÉTECTÉ.
                Type de danger : $emergencyType
                Description : $description
                
                Génère un plan de mise en sécurité d'urgence numéro un, ultra-clair, direct et concis pour protéger les personnes et le logement immédiatement.
                
                Réponds STRICTEMENT en JSON avec ce schéma :
                {
                  "titre": "Alerte de Sécurité Immédiate",
                  "type_danger": "Électrique / Gaz / Inondation / Structure",
                  "actions_prioritaires": [
                    {
                      "etape": 1,
                      "action": "Action 1 immédiate (ex: Couper le disjoncteur général)",
                      "conseil": "Conseil de sécurité vitale",
                      "is_critical": true
                    }
                  ],
                  "consigne_securite": "Message de prudence"
                }
            """.trimIndent()

            val rootObj = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val rawJsonText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
            val cleanedJson = rawJsonText.replace("^```json".toRegex(), "").replace("^```".toRegex(), "").replace("```$".toRegex(), "").trim()

            val parsed = emergencyAdapter.fromJson(cleanedJson) ?: com.example.data.models.EmergencyPlanResponse()
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateHouseMaintenancePlan(
        apiKey: String,
        roomSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val prompt = """
                Tu es l'assistant Jumeau Numérique de la maison Fixia.
                Voici l'état actuel des pièces et des équipements enregistrés :
                $roomSummary
                
                Rédige un Plan de Maintenance Préventive personnalisé pour le logement (actions recommandées à 1 mois, 3 mois, et conseils de prévention saisonnière).
                Formatte ta réponse en texte structuré avec des titres clairs et des conseils pratiques.
            """.trimIndent()

            val rootObj = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))

            val body = rootObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""

            if (!response.isSuccessful) return@withContext Result.failure(Exception(resStr))

            val candidates = JSONObject(resStr).optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "Plan indisponible"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readUriAsBase64(uriString: String, mediaType: MediaType): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                if (mediaType == MediaType.PHOTO) {
                    // Compress image bitmap to optimize payload size
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val outputStream = ByteArrayOutputStream()
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                } else {
                    val bytes = stream.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
