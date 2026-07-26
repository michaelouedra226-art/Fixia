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
