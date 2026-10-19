package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

object OfflineReceiptAnalyzer {

    /**
     * Analiza un comprobante de forma 100% OFFLINE (sin conexión a internet ni consumo de APIs)
     * utilizando el reconocedor óptico de caracteres (OCR) de Google ML Kit en el dispositivo.
     */
    suspend fun analyzeReceiptOffline(
        bitmap: Bitmap
    ): Result<ReceiptAnalysisResult> = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        val parsedData = parseReceiptText(visionText.text)
                        continuation.resume(Result.success(parsedData))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    } finally {
                        recognizer.close()
                    }
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    continuation.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Extrae de forma inteligente monto, fecha, establecimiento y descripción a partir del texto del ticket.
     */
    fun parseReceiptText(fullText: String): ReceiptAnalysisResult {
        if (fullText.isBlank()) {
            return ReceiptAnalysisResult(0.0, "", "", "Sin texto detectado en comprobante")
        }

        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extraer Establecimiento (Generalmente en las primeras líneas no-ruido)
        val establishment = extractEstablishment(lines)

        // 2. Extraer Monto Total
        val amount = extractTotalAmount(lines, fullText)

        // 3. Extraer Fecha
        val dateString = extractDate(lines, fullText)

        // 4. Descripción sugerida
        val description = if (establishment.isNotBlank()) {
            "Consumo en $establishment (Reconocimiento Offline)"
        } else {
            "Consumo registrado con escáner offline"
        }

        return ReceiptAnalysisResult(
            amount = amount,
            establishment = establishment,
            dateString = dateString,
            description = description
        )
    }

    private fun extractEstablishment(lines: List<String>): String {
        val noiseWords = listOf(
            "BOLETA", "FACTURA", "TICKET", "COMPROBANTE", "ELECTRONICA", "ELECTRONICO",
            "RUC", "R.U.C.", "R.U.C", "DNI", "D.N.I.", "CLIENTE", "DIRECCION", "DIREC",
            "FECHA", "HORA", "SERIE", "NUMERO", "TELEF", "TEL", "CAJA", "CAJERO",
            "BIENVENIDO", "GRACIAS", "AUTORIZACION", "SUNAT", "VALOR", "IGV", "TOTAL"
        )

        for (line in lines.take(6)) {
            val upper = line.uppercase(Locale.ROOT)
            val isNoise = noiseWords.any { upper.startsWith(it) || upper.contains("RUC") || upper.contains("R.U.C") }
            val hasLetters = line.any { it.isLetter() }
            val containsOnlyNumbersOrSymbols = line.all { !it.isLetter() }

            if (!isNoise && hasLetters && !containsOnlyNumbersOrSymbols && line.length in 3..40) {
                // Limpiar caracteres extraños iniciales o finales
                return line.replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "").trim()
            }
        }

        return lines.firstOrNull { it.any { c -> c.isLetter() } }?.take(30) ?: "Comercio"
    }

    private fun extractTotalAmount(lines: List<String>, fullText: String): Double {
        // Palabras clave de total prioritarias
        val totalKeywords = listOf(
            "TOTAL A PAGAR", "TOTAL PAGAR", "TOTAL NETO", "TOTAL IMPORTE",
            "IMPORTE TOTAL", "MONTO TOTAL", "TOTAL VENTA", "TOTAL:", "TOTAL S/",
            "TOTAL $", "TOTAL", "PAGAR", "PAGO", "IMPORTE"
        )

        val decimalPattern = Pattern.compile("(\\d{1,6}[.,]\\d{2})")

        // Prioridad 1: Buscar en líneas que contengan palabras clave de TOTAL
        for (keyword in totalKeywords) {
            for (i in lines.indices.reversed()) {
                val line = lines[i].uppercase(Locale.ROOT)
                if (line.contains(keyword)) {
                    val matcher = decimalPattern.matcher(line)
                    var lastMatch: String? = null
                    while (matcher.find()) {
                        lastMatch = matcher.group(1)
                    }
                    if (lastMatch != null) {
                        val parsed = sanitizeNumber(lastMatch)
                        if (parsed > 0.0) return parsed
                    }

                    // Si no está en la misma línea, revisar la línea inmediatamente posterior
                    if (i + 1 < lines.size) {
                        val nextMatcher = decimalPattern.matcher(lines[i + 1])
                        if (nextMatcher.find()) {
                            val parsed = sanitizeNumber(nextMatcher.group(1))
                            if (parsed > 0.0) return parsed
                        }
                    }
                }
            }
        }

        // Prioridad 2: Buscar montos precedidos por símbolo de moneda S/ o $
        val currencyPattern = Pattern.compile("(?:S/\\.?|\\$|USD)\\s*([0-9]{1,6}[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
        val currMatcher = currencyPattern.matcher(fullText)
        val currencyAmounts = mutableListOf<Double>()
        while (currMatcher.find()) {
            val amountStr = currMatcher.group(1)
            val parsed = sanitizeNumber(amountStr)
            if (parsed > 0.0) currencyAmounts.add(parsed)
        }
        if (currencyAmounts.isNotEmpty()) {
            // Generalmente el total es el mayor monto con símbolo de moneda
            return currencyAmounts.maxOrNull() ?: 0.0
        }

        // Prioridad 3: Buscar todos los números con formato decimal y tomar el mayor monto razonable (< 20000)
        val allDecimalMatcher = decimalPattern.matcher(fullText)
        val allAmounts = mutableListOf<Double>()
        while (allDecimalMatcher.find()) {
            val parsed = sanitizeNumber(allDecimalMatcher.group(1))
            if (parsed in 0.5..30000.0) {
                allAmounts.add(parsed)
            }
        }

        return allAmounts.maxOrNull() ?: 0.0
    }

    private fun extractDate(lines: List<String>, fullText: String): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // 1. Formato YYYY/MM/DD o YYYY-MM-DD
        val ymdPattern = Pattern.compile("\\b(202[0-9])[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])\\b")
        val ymdMatcher = ymdPattern.matcher(fullText)
        if (ymdMatcher.find()) {
            val y = ymdMatcher.group(1)!!.toInt()
            val m = ymdMatcher.group(2)!!.toInt()
            val d = ymdMatcher.group(3)!!.toInt()
            return String.format(Locale.US, "%04d/%02d/%02d", y, m, d)
        }

        // 2. Formato DD/MM/YYYY o DD-MM-YYYY
        val dmyPattern = Pattern.compile("\\b(0?[1-9]|[12][0-9]|3[01])[/-](0?[1-9]|1[0-2])[/-](202[0-9])\\b")
        val dmyMatcher = dmyPattern.matcher(fullText)
        if (dmyMatcher.find()) {
            val d = dmyMatcher.group(1)!!.toInt()
            val m = dmyMatcher.group(2)!!.toInt()
            val y = dmyMatcher.group(3)!!.toInt()
            return String.format(Locale.US, "%04d/%02d/%02d", y, m, d)
        }

        // 3. Formato DD/MM/YY (ej. 19/10/26)
        val dmyShortPattern = Pattern.compile("\\b(0?[1-9]|[12][0-9]|3[01])[/-](0?[1-9]|1[0-2])[/-](2[0-9])\\b")
        val dmyShortMatcher = dmyShortPattern.matcher(fullText)
        if (dmyShortMatcher.find()) {
            val d = dmyShortMatcher.group(1)!!.toInt()
            val m = dmyShortMatcher.group(2)!!.toInt()
            val y = 2000 + dmyShortMatcher.group(3)!!.toInt()
            return String.format(Locale.US, "%04d/%02d/%02d", y, m, d)
        }

        // Si no se encuentra fecha en el ticket, retornar fecha actual
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d/%02d/%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun sanitizeNumber(raw: String): Double {
        return try {
            val normalized = raw.replace(',', '.')
            normalized.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
