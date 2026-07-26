package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.models.DiagnosticEntity
import com.example.data.models.DiagnosticResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun generateAndSavePdf(
        context: Context,
        diagnostic: DiagnosticEntity,
        onSuccess: (Uri) -> Unit = {}
    ) {
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val parsedResponse = try {
                moshi.adapter(DiagnosticResponse::class.java).fromJson(diagnostic.rawJsonResponse)
            } catch (e: Exception) {
                null
            }

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1C1B1F")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#49454F")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }

            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#2563EB")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#1C1B1F")
                textSize = 11f
                typeface = Typeface.DEFAULT
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
            }

            var y = 40f

            // Header Banner
            canvas.drawRect(20f, y, 575f, y + 60f, headerBgPaint)
            canvas.drawText("FIXIA - Rapport Diagnostic Expert", 35f, y + 28f, titlePaint)
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(diagnostic.timestamp))
            canvas.drawText("Généré le : $dateStr", 35f, y + 48f, subtitlePaint)
            y += 80f

            // Main Info
            canvas.drawText("Problème : ${diagnostic.titreProbleme}", 35f, y, titlePaint)
            y += 24f
            canvas.drawText("Niveau d'urgence : ${diagnostic.urgence.uppercase()} | Confiance IA : ${(diagnostic.confidenceScore * 100).toInt()}%", 35f, y, bodyPaint)
            y += 20f
            canvas.drawText("Recommandation : ${diagnostic.recommendation.uppercase()}", 35f, y, bodyPaint)
            y += 35f

            // Summary
            canvas.drawText("Résumé Exécutif", 35f, y, sectionTitlePaint)
            y += 18f
            val summaryLines = wrapText(diagnostic.summary, 80)
            summaryLines.forEach { line ->
                canvas.drawText(line, 35f, y, bodyPaint)
                y += 15f
            }
            y += 15f

            // Causes
            parsedResponse?.causes?.takeIf { it.isNotEmpty() }?.let { causes ->
                canvas.drawText("Causes probables identifiées", 35f, y, sectionTitlePaint)
                y += 18f
                causes.forEach { cause ->
                    val causeLine = "• ${cause.cause} (Probabilité : ${(cause.probabilite * 100).toInt()}%)"
                    canvas.drawText(causeLine, 35f, y, bodyPaint)
                    y += 15f
                    val explanationLines = wrapText("  ${cause.explication}", 78)
                    explanationLines.forEach { eline ->
                        canvas.drawText(eline, 35f, y, bodyPaint)
                        y += 14f
                    }
                    y += 5f
                }
                y += 15f
            }

            // DIY Plan
            parsedResponse?.planDiy?.takeIf { it.isNotEmpty() }?.let { steps ->
                canvas.drawText("Plan d'action DIY recommandé", 35f, y, sectionTitlePaint)
                y += 18f
                steps.forEach { step ->
                    canvas.drawText("Étape ${step.etape} : ${step.titre} (${step.tempsEstimeMinutes} min)", 35f, y, bodyPaint)
                    y += 15f
                    val descLines = wrapText("  ${step.description}", 78)
                    descLines.forEach { dline ->
                        canvas.drawText(dline, 35f, y, bodyPaint)
                        y += 14f
                    }
                    if (step.conseilSecurite.isNotBlank()) {
                        canvas.drawText("  Sécurité : ${step.conseilSecurite}", 35f, y, subtitlePaint)
                        y += 14f
                    }
                    y += 6f
                }
                y += 15f
            }

            // Pro estimation
            parsedResponse?.estimationPrixProfessionnel?.let { pro ->
                canvas.drawText("Estimation Professionnel : ${pro.min.toInt()} - ${pro.max.toInt()} ${pro.devise}", 35f, y, sectionTitlePaint)
                y += 25f
            }

            // User Notes
            if (diagnostic.userNote.isNotBlank()) {
                canvas.drawText("Notes personnelles : ${diagnostic.userNote}", 35f, y, bodyPaint)
                y += 20f
            }

            // Footer
            canvas.drawText("Fixia - Assistant de Diagnostic Domestique Intelligent", 35f, 810f, subtitlePaint)

            document.finishPage(page)

            val safeTitle = diagnostic.titreProbleme.replace("[^a-zA-Z0-9]".toRegex(), "_").take(25)
            val fileName = "Fixia_Diagnostic_${SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())}_$safeTitle.pdf"

            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, fileName)
                FileOutputStream(pdfFile).use { outputStream ->
                    document.writeTo(outputStream)
                }
                savedUri = Uri.fromFile(pdfFile)
            }

            document.close()

            if (savedUri != null) {
                Toast.makeText(context, "PDF téléchargé avec succès !", Toast.LENGTH_LONG).show()
                onSuccess(savedUri)
            } else {
                Toast.makeText(context, "Échec de l'enregistrement du PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur génération PDF : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfUri(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Ouvrir le PDF avec..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Aucune application de lecture PDF disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 <= maxCharsPerLine) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
