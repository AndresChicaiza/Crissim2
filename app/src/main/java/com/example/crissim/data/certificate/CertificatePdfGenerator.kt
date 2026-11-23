package com.example.crissim.data.certificate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import com.example.crissim.domain.model.Certificate
import com.example.crissim.domain.model.Level
import com.example.crissim.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Generador de PDF para certificados CrisisSim.
 * Crea un PDF visualmente atractivo con datos del certificado, QR y firma.
 */
object CertificatePdfGenerator {

    private const val TAG = "CertificatePdfGenerator"
    private const val PAGE_WIDTH = 595f  // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842f // A4 height in points
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    /**
     * Genera el PDF del certificado y lo guarda en el directorio de caché de la app.
     * Retorna una Uri content:// (via FileProvider) que se puede compartir con otras apps.
     */
    fun generateCertificatePdf(context: Context, certificate: Certificate): Uri? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawCertificate(canvas, certificate, context)

            pdfDocument.finishPage(page)

            // Guardar en cache/certificates/ (expuesto por FileProvider)
            val cacheDir = context.getCacheDir()
            val certificatesDir = File(cacheDir, "certificates")
            if (!certificatesDir.exists()) certificatesDir.mkdirs()

            val fileName = "Certificado_${certificate.level.name}_${certificate.certificateUuid.take(8)}.pdf"
            val file = File(certificatesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Log.d(TAG, "PDF generado: ${file.absolutePath}")

            // Retornar Uri content:// via FileProvider (compatible con Android 7+)
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error generando PDF", e)
            null
        }
    }

    private fun drawCertificate(canvas: Canvas, certificate: Certificate, context: Context) {
        val y = MARGIN
        val centerX = PAGE_WIDTH / 2

        // Paint objects
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20") // Verde oscuro
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A2E")
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A2E")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            strokeWidth = 2f
        }

        var currentY = y + 20f

        // === HEADER: Logo ILC + Title ===
        // Cargar y dibujar logo del ILC
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ilc_logo)
        if (logoBitmap != null) {
            // Escalar logo a 120px de alto manteniendo aspect ratio
            val targetHeight = 120f
            val scale = targetHeight / logoBitmap.height.toFloat()
            val targetWidth = logoBitmap.width * scale
            val scaledBitmap = Bitmap.createScaledBitmap(logoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            
            val logoX = centerX - targetWidth / 2
            canvas.drawBitmap(scaledBitmap, logoX, currentY, null)
            currentY += targetHeight + 15f
        }

        // Draw decorative top line
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        currentY += 20f

        // Title
        canvas.drawText("CERTIFICADO DE COMPETENCIA", centerX, currentY, titlePaint)
        currentY += 40f

        // Subtitle
        canvas.drawText("International Language Center · Yumbo, Valle del Cauca", centerX, currentY, subtitlePaint)
        currentY += 30f

        // Decorative line
        canvas.drawLine(MARGIN + 80, currentY, PAGE_WIDTH - MARGIN - 80, currentY, linePaint)
        currentY += 30f

        // === BODY: "Se certifica que..." ===
        val certifiesText = "Se certifica que"
        canvas.drawText(certifiesText, centerX, currentY, bodyPaint)
        currentY += 30f

        // User name (large, bold)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(certificate.userName, centerX, currentY, namePaint)
        currentY += 35f

        // "ha superado satisfactoriamente el"
        val passedText = "ha superado satisfactoriamente el"
        canvas.drawText(passedText, centerX, currentY, bodyPaint)
        currentY += 25f

        // Level name
        val levelName = getLevelDisplayName(certificate.level)
        val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0") // Azul
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(levelName, centerX, currentY, levelPaint)
        currentY += 35f

        // Score info
        val scoreText = "con un puntaje de ${certificate.score} de ${certificate.maxScore} puntos (${calculatePercentage(certificate.score, certificate.maxScore)}%)"
        canvas.drawText(scoreText, centerX, currentY, bodyPaint)
        currentY += 30f

        // === QR CODE SECTION ===
        currentY += 20f
        val qrSize = 180f
        val qrX = centerX - qrSize / 2

        // Generate QR bitmap
        val qrPayload = certificate.buildQrPayload()
        val qrBitmap = generateQrBitmap(qrPayload, qrSize.toInt())
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrX, currentY, null)
        }
        currentY += qrSize + 10f

        // QR caption
        canvas.drawText("Código QR de verificación", centerX, currentY, smallPaint)
        currentY += 20f

        // === FOOTER: Issue date, certificate number, signature ===
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))
        val issuedDate = dateFormat.format(Date(certificate.issuedAt))

        canvas.drawText("Emitido el $issuedDate", centerX, currentY, bodyPaint)
        currentY += 20f

        canvas.drawText("Certificado N.º ${certificate.certificateUuid.take(12).uppercase()}", centerX, currentY, bodyPaint)
        currentY += 20f

        // Signature line
        canvas.drawLine(MARGIN + 60, currentY, PAGE_WIDTH - MARGIN - 60, currentY, linePaint)
        currentY += 15f
        canvas.drawText("CrisisSim · Firmado digitalmente (ECDSA)", centerX, currentY, smallPaint)
    }

    private fun getLevelDisplayName(level: Level): String = when (level) {
        Level.BASIC -> "Nivel Básico: Evacuación Escolar e Incendios"
        Level.INTERMEDIATE -> "Nivel Intermedio: Sismos"
        Level.ADVANCED -> "Nivel Avanzado: Primeros Auxilios y Liderazgo"
    }

    private fun calculatePercentage(score: Int, maxScore: Int): Int = if (maxScore > 0) (score * 100 / maxScore) else 0

    /**
     * Genera un bitmap con el código QR.
     */
    private fun generateQrBitmap(payload: String, size: Int): Bitmap? {
        return try {
            val hints = ConcurrentHashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            val writer = QRCodeWriter()
            val bitMatrix: BitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            Log.e(TAG, "Error generando QR", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado generando QR", e)
            null
        }
    }
}