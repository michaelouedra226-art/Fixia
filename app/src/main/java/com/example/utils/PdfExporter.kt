package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.models.DiagnosticEntity
import com.example.data.models.DiagnosticResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndSharePdf(context: Context, entity: DiagnosticEntity) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val parsed = try {
            moshi.adapter(DiagnosticResponse::class.java).fromJson(entity.rawJsonResponse)
        } catch (e: Exception) {
            null
        } ?: return

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 11f
        }

        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 40f
        val margin = 40f

        // Header
        canvas.drawText("FIXIA — Rapport de Diagnostic Domestique", margin, y, titlePaint)
        y += 24f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val dateStr = dateFormat.format(Date(entity.timestamp))
        canvas.drawText("Date : $dateStr | ID : #${entity.id}", margin, y, bodyPaint)
        y += 30f

        // Problem Title
        canvas.drawText("Problème : ${entity.titreProbleme}", margin, y, subtitlePaint)
        y += 20f

        // Urgency & Confidence
        val urgencyText = "Urgence : ${entity.urgence.uppercase(Locale.FRANCE)}  |  Score de confiance : ${(entity.confidenceScore * 100).toInt()}%"
        canvas.drawText(urgencyText, margin, y, boldBodyPaint)
        y += 25f

        // Executive summary
        canvas.drawText("Résumé :", margin, y, subtitlePaint)
        y += 18f
        val summaryLines = wrapText(parsed.resume, 75)
        for (line in summaryLines) {
            canvas.drawText(line, margin, y, bodyPaint)
            y += 15f
        }
        y += 15f

        // Causes
        if (parsed.causes.isNotEmpty()) {
            canvas.drawText("Causes Probables :", margin, y, subtitlePaint)
            y += 18f
            for (cause in parsed.causes) {
                val cLine = "• ${cause.cause} (${(cause.probabilite * 100).toInt()}%)"
                canvas.drawText(cLine, margin, y, boldBodyPaint)
                y += 15f
                val expLines = wrapText(cause.explication, 70)
                for (exp in expLines) {
                    canvas.drawText("   $exp", margin, y, bodyPaint)
                    y += 14f
                }
                y += 6f
            }
            y += 10f
        }

        // Recommendation & Plan DIY
        canvas.drawText("Recommandation : ${parsed.recommandationPrincipale.uppercase()}", margin, y, subtitlePaint)
        y += 20f

        if (parsed.planDiy.isNotEmpty()) {
            canvas.drawText("Plan d'Action DIY (${parsed.niveauDiy}) :", margin, y, boldBodyPaint)
            y += 18f
            for (step in parsed.planDiy.take(5)) {
                val stepHeader = "Étape ${step.etape} : ${step.titre}"
                canvas.drawText(stepHeader, margin, y, boldBodyPaint)
                y += 15f
                val descLines = wrapText(step.description, 70)
                for (dl in descLines) {
                    canvas.drawText("   $dl", margin, y, bodyPaint)
                    y += 14f
                }
                if (step.conseilSecurite.isNotBlank()) {
                    canvas.drawText("   [Sécurité] ${step.conseilSecurite}", margin, y, bodyPaint)
                    y += 14f
                }
                y += 6f
            }
        }

        pdfDocument.finishPage(page)

        // Save PDF file
        val pdfFile = File(context.cacheDir, "Fixia_Diagnostic_${entity.id}.pdf")
        try {
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            // Share intent
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le rapport PDF Fixia"))
        } catch (e: Exception) {
            e.printStackTrace()
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
