package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ParsedBackup(
    val cards: List<CreditCardEntity>,
    val expenses: List<ExpenseEntity>,
    val photosCount: Int,
    val preferences: Map<String, Any?> = emptyMap()
)

object BackupUtil {
    private const val TAG = "BackupUtil"
    private const val METADATA_ENTRY_NAME = "backup_metadata.json"
    private const val LEGACY_METADATA_NAME = "metadata.json"
    private const val RECEIPTS_DIR_ENTRY = "receipts/"

    /**
     * Exporta el respaldo completo (tarjetas, consumos con todos sus datos,
     * configuración completa de la app y los archivos de fotos de comprobantes)
     * en un archivo ZIP estándar.
     */
    fun exportFullBackup(
        context: Context,
        cards: List<CreditCardEntity>,
        expenses: List<ExpenseEntity>,
        outputStream: OutputStream
    ) {
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))
        try {
            val root = JSONObject()
            root.put("app", "Creditia")
            root.put("version", 2)
            root.put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            root.put("cards_count", cards.size)
            root.put("expenses_count", expenses.size)

            // Exportar configuración completa de la aplicación (SharedPreferences)
            val prefs = context.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)
            val prefsObj = JSONObject()
            for ((key, value) in prefs.all) {
                when (value) {
                    is Boolean -> prefsObj.put(key, value)
                    is Int -> prefsObj.put(key, value)
                    is Long -> prefsObj.put(key, value)
                    is Float -> prefsObj.put(key, value.toDouble())
                    is Double -> prefsObj.put(key, value)
                    is String -> prefsObj.put(key, value)
                    else -> if (value != null) prefsObj.put(key, value.toString())
                }
            }
            root.put("preferences", prefsObj)

            val cardsArray = JSONArray()
            for (c in cards) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("bankName", c.bankName)
                obj.put("cardName", c.cardName)
                obj.put("network", c.network)
                obj.put("tier", c.tier)
                obj.put("last4", c.last4)
                obj.put("cutDay", c.cutDay)
                obj.put("payDay", c.payDay)
                obj.put("payBusinessDays", c.payBusinessDays)
                obj.put("cardColorTheme", c.cardColorTheme)
                obj.put("creditLimit", c.creditLimit)
                obj.put("isDefault", c.isDefault)
                cardsArray.put(obj)
            }
            root.put("cards", cardsArray)

            val receiptsToPack = mutableSetOf<File>()
            val expensesArray = JSONArray()

            for (e in expenses) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("cardId", e.cardId)
                obj.put("sequenceNumber", e.sequenceNumber)
                obj.put("amount", e.amount)
                obj.put("establishment", e.establishment)
                obj.put("description", e.description)
                obj.put("dateString", e.dateString)
                obj.put("billingPeriod", e.billingPeriod)
                obj.put("deferredGroupId", e.deferredGroupId ?: "")
                obj.put("timestamp", e.timestamp)

                val receiptList = e.getReceiptUriList()
                val relativeNames = receiptList.mapNotNull { uriPath ->
                    val cleanPath = uriPath.removePrefix("file://")
                    val file = File(cleanPath)
                    if (file.exists() && file.isFile) {
                        receiptsToPack.add(file)
                        file.name
                    } else {
                        val fallbackFile = File(ReceiptStorageUtil.getReceiptsDir(context), file.name)
                        if (fallbackFile.exists() && fallbackFile.isFile) {
                            receiptsToPack.add(fallbackFile)
                            fallbackFile.name
                        } else null
                    }
                }
                obj.put("receiptFileNames", relativeNames.joinToString("|"))
                expensesArray.put(obj)
            }
            root.put("expenses", expensesArray)

            // Asegurar que todos los comprobantes locales en el directorio de la app se incluyan
            val allDirReceipts = ReceiptStorageUtil.getReceiptsDir(context).listFiles()?.filter { it.isFile } ?: emptyList()
            receiptsToPack.addAll(allDirReceipts)

            // 1. Escribir metadatos JSON
            zipOut.putNextEntry(ZipEntry(METADATA_ENTRY_NAME))
            val jsonBytes = root.toString(2).toByteArray(Charsets.UTF_8)
            zipOut.write(jsonBytes)
            zipOut.closeEntry()

            // 2. Escribir fotos de comprobantes en receipts/
            for (receiptFile in receiptsToPack) {
                try {
                    val entry = ZipEntry(RECEIPTS_DIR_ENTRY + receiptFile.name)
                    zipOut.putNextEntry(entry)
                    FileInputStream(receiptFile).use { fis ->
                        fis.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                } catch (e: Exception) {
                    Log.e(TAG, "Error empacando foto ${receiptFile.name}: ${e.message}")
                }
            }

            zipOut.finish()
            zipOut.flush()
        } finally {
            zipOut.close()
        }
    }

    /**
     * Parsea y extrae el respaldo, detectando automáticamente si es un ZIP con fotos
     * o un archivo JSON convencional de versiones anteriores.
     */
    fun parseBackup(
        context: Context,
        inputStream: InputStream
    ): ParsedBackup {
        val tempFile = File.createTempFile("creditia_restore_", ".tmp", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }

            val isZip = try {
                val header = ByteArray(4)
                FileInputStream(tempFile).use { fis ->
                    val read = fis.read(header)
                    read >= 4 &&
                            header[0] == 0x50.toByte() &&
                            header[1] == 0x4B.toByte()
                }
            } catch (e: Exception) {
                false
            }

            return if (isZip) {
                parseZipBackup(context, tempFile)
            } else {
                val jsonText = tempFile.readText(Charsets.UTF_8)
                parseJsonBackup(context, jsonText)
            }
        } finally {
            try {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo eliminar archivo temporal: ${e.message}")
            }
        }
    }

    private fun parseZipBackup(context: Context, zipFileOnDisk: File): ParsedBackup {
        val targetReceiptsDir = ReceiptStorageUtil.getReceiptsDir(context)
        if (!targetReceiptsDir.exists()) {
            targetReceiptsDir.mkdirs()
        }
        var jsonContent: String? = null
        var photosCount = 0

        java.util.zip.ZipFile(zipFileOnDisk).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val normalizedName = entry.name.replace('\\', '/')
                val isJson = normalizedName == METADATA_ENTRY_NAME ||
                        normalizedName == LEGACY_METADATA_NAME ||
                        normalizedName.endsWith(".json")

                if (isJson) {
                    zip.getInputStream(entry).use { input ->
                        jsonContent = input.bufferedReader(Charsets.UTF_8).readText()
                    }
                } else if ((normalizedName.startsWith(RECEIPTS_DIR_ENTRY) || normalizedName.contains("receipts/")) && !entry.isDirectory) {
                    val fileName = File(normalizedName).name
                    if (fileName.isNotBlank()) {
                        val destFile = File(targetReceiptsDir, fileName)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        photosCount++
                    }
                }
            }
        }

        if (jsonContent.isNullOrBlank()) {
            throw IllegalArgumentException("El archivo ZIP no contiene el archivo de metadatos válido.")
        }

        return parseJsonMetadata(context, jsonContent, photosCount)
    }

    private fun parseJsonBackup(context: Context, jsonText: String): ParsedBackup {
        return parseJsonMetadata(context, jsonText, 0)
    }

    private fun parseJsonMetadata(context: Context, jsonContent: String, extractedPhotosCount: Int): ParsedBackup {
        val root = JSONObject(jsonContent)
        val cardsArray = root.optJSONArray("cards") ?: JSONArray()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        val receiptsDir = ReceiptStorageUtil.getReceiptsDir(context)

        val parsedCards = mutableListOf<CreditCardEntity>()
        for (i in 0 until cardsArray.length()) {
            val obj = cardsArray.getJSONObject(i)
            val card = CreditCardEntity(
                id = obj.optLong("id", 0L),
                bankName = obj.optString("bankName", "BANCO"),
                cardName = obj.optString("cardName", "TARJETA"),
                network = obj.optString("network", "VISA"),
                tier = obj.optString("tier", "CLASSIC"),
                last4 = obj.optString("last4", "0000"),
                cutDay = obj.optInt("cutDay", 24),
                payDay = obj.optInt("payDay", 14),
                payBusinessDays = obj.optInt("payBusinessDays", 15),
                cardColorTheme = obj.optString("cardColorTheme", "blue"),
                creditLimit = obj.optDouble("creditLimit", 4000.0),
                isDefault = obj.optBoolean("isDefault", false)
            )
            parsedCards.add(card)
        }

        val parsedExpenses = mutableListOf<ExpenseEntity>()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)

            // Reconstruir receiptUri apuntando a las fotos extraídas
            val receiptFileNames = obj.optString("receiptFileNames", "")
            val rawReceiptUri = obj.optString("receiptUri", "")
            val effectiveNames = if (receiptFileNames.isNotBlank()) receiptFileNames else rawReceiptUri

            val finalReceiptUri = if (effectiveNames.isNotBlank()) {
                effectiveNames.split("|")
                    .map { it.trim().removePrefix("file://") }
                    .filter { it.isNotBlank() }
                    .map { rawName ->
                        val localFile = File(receiptsDir, File(rawName).name)
                        if (localFile.exists()) localFile.absolutePath else rawName
                    }
                    .joinToString("|")
                    .ifBlank { null }
            } else null

            val deferredGroupId = obj.optString("deferredGroupId", "").ifBlank { null }

            val expense = ExpenseEntity(
                id = obj.optLong("id", 0L),
                cardId = obj.optLong("cardId", 0L),
                sequenceNumber = obj.optInt("sequenceNumber", i + 1),
                amount = obj.optDouble("amount", 0.0),
                establishment = obj.optString("establishment", "Consumo General"),
                description = obj.optString("description", "Consumo"),
                dateString = obj.optString("dateString", "2026/10/01"),
                billingPeriod = obj.optString("billingPeriod", "Octubre 2026"),
                receiptUri = finalReceiptUri,
                deferredGroupId = deferredGroupId,
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
            parsedExpenses.add(expense)
        }

        val prefsObj = root.optJSONObject("preferences") ?: root.optJSONObject("config")
        val parsedPreferences = mutableMapOf<String, Any?>()
        if (prefsObj != null) {
            val keys = prefsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                parsedPreferences[key] = prefsObj.get(key)
            }
        }

        return ParsedBackup(
            cards = parsedCards,
            expenses = parsedExpenses,
            photosCount = extractedPhotosCount,
            preferences = parsedPreferences
        )
    }
}
