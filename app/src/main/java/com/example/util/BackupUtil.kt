package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ParsedBackup(
    val cards: List<CreditCardEntity>,
    val expenses: List<ExpenseEntity>,
    val photosCount: Int,
    val logosCount: Int = 0,
    val preferences: Map<String, Any?> = emptyMap(),
    val rememberedLogos: Map<String, String> = emptyMap()
)

object BackupUtil {
    private const val TAG = "BackupUtil"
    private const val METADATA_ENTRY_NAME = "backup_metadata.json"
    private const val LEGACY_METADATA_NAME = "metadata.json"
    private const val RECEIPTS_DIR_ENTRY = "receipts/"
    private const val LOGOS_DIR_ENTRY = "logos/"

    /**
     * Exporta el respaldo completo (tarjetas, consumos con todos sus datos,
     * configuración completa de la app, fotos de comprobantes y fotos de logotipos)
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

            // 1. Exportar configuración completa de la aplicación (SharedPreferences seguras y fechas personalizadas)
            val prefs = context.getSharedPreferences("creditia_secure_prefs", Context.MODE_PRIVATE)
            val prefsObj = JSONObject()
            val customDatesObj = JSONObject()
            for ((key, value) in prefs.all) {
                when (value) {
                    is Boolean -> prefsObj.put(key, value)
                    is Int -> prefsObj.put(key, value)
                    is Long -> prefsObj.put(key, value)
                    is Float -> prefsObj.put(key, value.toDouble())
                    is Double -> prefsObj.put(key, value)
                    is String -> {
                        prefsObj.put(key, value)
                        if (key.startsWith("custom_cycle_range_") || key.startsWith("custom_cut_date_") || key.startsWith("custom_payment_limit_")) {
                            customDatesObj.put(key, value)
                        }
                    }
                    else -> if (value != null) prefsObj.put(key, value.toString())
                }
            }
            root.put("preferences", prefsObj)
            root.put("custom_dates", customDatesObj)
            val selCardId = prefs.getLong("selected_card_id", -1L)
            if (selCardId > 0) root.put("selected_card_id", selCardId)
            val selPeriod = prefs.getString("selected_period", "") ?: ""
            if (selPeriod.isNotBlank()) root.put("selected_period", selPeriod)

            // 2. Exportar catálogo de logotipos recordados
            val logoPrefs = context.getSharedPreferences("creditia_establishment_logos", Context.MODE_PRIVATE)
            val logoPrefsObj = JSONObject()
            for ((key, value) in logoPrefs.all) {
                if (value is String && value.isNotBlank()) {
                    logoPrefsObj.put(key, value)
                }
            }
            root.put("remembered_logos", logoPrefsObj)

            // 3. Exportar tarjetas de crédito
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

            // 4. Exportar consumos y recolectar archivos a empacar
            val receiptsToPack = mutableSetOf<File>()
            val logosToPack = mutableSetOf<File>()
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
                obj.put("transactionType", e.transactionType)
                obj.put("transactionGroupId", e.transactionGroupId ?: "")
                obj.put("transactionGroupName", e.transactionGroupName ?: "")
                obj.put("timestamp", e.timestamp)

                // Recolectar comprobantes
                val receiptList = e.getReceiptUriList()
                val relativeReceiptNames = receiptList.mapNotNull { uriPath ->
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
                obj.put("receiptFileNames", relativeReceiptNames.joinToString("|"))
                obj.put("receiptUri", e.receiptUri ?: "")

                // Recolectar logotipos asociados al consumo
                val logoUrl = e.establishmentLogoUrl ?: ""
                val relativeLogoNames = if (logoUrl.isNotBlank()) {
                    logoUrl.split("|").map { it.trim() }.filter { it.isNotBlank() }.map { part ->
                        if (part.startsWith("http://") || part.startsWith("https://")) {
                            part
                        } else {
                            val cleanPath = part.removePrefix("file://")
                            val file = File(cleanPath)
                            if (file.exists() && file.isFile) {
                                logosToPack.add(file)
                                file.name
                            } else {
                                val fallbackFile = File(LogoStorageUtil.getLogosDir(context), file.name)
                                if (fallbackFile.exists() && fallbackFile.isFile) {
                                    logosToPack.add(fallbackFile)
                                    fallbackFile.name
                                } else {
                                    file.name
                                }
                            }
                        }
                    }.joinToString("|")
                } else ""
                obj.put("logoFileNames", relativeLogoNames)
                obj.put("establishmentLogoUrl", logoUrl)

                expensesArray.put(obj)
            }
            root.put("expenses", expensesArray)

            // Recolectar logos referenciados en remembered_logos
            for ((_, v) in logoPrefs.all) {
                if (v is String && v.isNotBlank() && !v.startsWith("http://") && !v.startsWith("https://")) {
                    val clean = v.removePrefix("file://")
                    val f = File(clean)
                    if (f.exists() && f.isFile) {
                        logosToPack.add(f)
                    } else {
                        val fb = File(LogoStorageUtil.getLogosDir(context), f.name)
                        if (fb.exists() && fb.isFile) logosToPack.add(fb)
                    }
                }
            }

            // Asegurar que todos los comprobantes y logos en los directorios internos de la app se incluyan
            val allDirReceipts = ReceiptStorageUtil.getReceiptsDir(context).listFiles()?.filter { it.isFile } ?: emptyList()
            receiptsToPack.addAll(allDirReceipts)

            val allDirLogos = LogoStorageUtil.getAllLogoFiles(context)
            logosToPack.addAll(allDirLogos)

            // 5. Escribir metadatos JSON
            zipOut.putNextEntry(ZipEntry(METADATA_ENTRY_NAME))
            val jsonBytes = root.toString(2).toByteArray(Charsets.UTF_8)
            zipOut.write(jsonBytes)
            zipOut.closeEntry()

            // 6. Escribir fotos de comprobantes en receipts/
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

            // 7. Escribir fotos de logotipos en logos/
            for (logoFile in logosToPack) {
                try {
                    val entry = ZipEntry(LOGOS_DIR_ENTRY + logoFile.name)
                    zipOut.putNextEntry(entry)
                    FileInputStream(logoFile).use { fis ->
                        fis.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                } catch (e: Exception) {
                    Log.e(TAG, "Error empacando logo ${logoFile.name}: ${e.message}")
                }
            }

            zipOut.finish()
            zipOut.flush()
        } finally {
            zipOut.close()
        }
    }

    /**
     * Parsea y extrae el respaldo, detectando automáticamente si es un ZIP completo
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
        val targetLogosDir = LogoStorageUtil.getLogosDir(context)
        if (!targetLogosDir.exists()) {
            targetLogosDir.mkdirs()
        }

        var jsonContent: String? = null
        var photosCount = 0
        var logosCount = 0

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
                } else if ((normalizedName.startsWith(LOGOS_DIR_ENTRY) || normalizedName.contains("logos/")) && !entry.isDirectory) {
                    val fileName = File(normalizedName).name
                    if (fileName.isNotBlank()) {
                        val destFile = File(targetLogosDir, fileName)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        logosCount++
                    }
                }
            }
        }

        if (jsonContent.isNullOrBlank()) {
            throw IllegalArgumentException("El archivo ZIP no contiene el archivo de metadatos válido.")
        }

        return parseJsonMetadata(context, jsonContent, photosCount, logosCount)
    }

    private fun parseJsonBackup(context: Context, jsonText: String): ParsedBackup {
        return parseJsonMetadata(context, jsonText, 0, 0)
    }

    private fun parseJsonMetadata(
        context: Context,
        jsonContent: String,
        extractedPhotosCount: Int,
        extractedLogosCount: Int = 0
    ): ParsedBackup {
        val root = JSONObject(jsonContent)
        val cardsArray = root.optJSONArray("cards") ?: JSONArray()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        val receiptsDir = ReceiptStorageUtil.getReceiptsDir(context)
        val logosDir = LogoStorageUtil.getLogosDir(context)

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
            val effectiveReceiptNames = if (receiptFileNames.isNotBlank()) receiptFileNames else rawReceiptUri

            val finalReceiptUri = if (effectiveReceiptNames.isNotBlank()) {
                effectiveReceiptNames.split("|")
                    .map { it.trim().removePrefix("file://") }
                    .filter { it.isNotBlank() }
                    .map { rawName ->
                        val localFile = File(receiptsDir, File(rawName).name)
                        if (localFile.exists()) localFile.absolutePath else rawName
                    }
                    .joinToString("|")
                    .ifBlank { null }
            } else null

            // Reconstruir establishmentLogoUrl apuntando a los logos extraídos
            val logoFileNames = obj.optString("logoFileNames", "")
            val rawLogoUrl = obj.optString("establishmentLogoUrl", "")
            val effectiveLogoNames = if (logoFileNames.isNotBlank()) logoFileNames else rawLogoUrl

            val finalLogoUrl = if (effectiveLogoNames.isNotBlank()) {
                effectiveLogoNames.split("|")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { rawVal ->
                        if (rawVal.startsWith("http://") || rawVal.startsWith("https://")) {
                            rawVal
                        } else {
                            val cleanName = File(rawVal.removePrefix("file://")).name
                            val localLogo = File(logosDir, cleanName)
                            if (localLogo.exists()) {
                                localLogo.absolutePath
                            } else {
                                val origFile = File(rawVal.removePrefix("file://"))
                                if (origFile.exists()) origFile.absolutePath else localLogo.absolutePath
                            }
                        }
                    }
                    .joinToString("|")
                    .ifBlank { null }
            } else null

            val deferredGroupId = obj.optString("deferredGroupId", "").ifBlank { null }
            val transactionType = obj.optString("transactionType", ExpenseEntity.TYPE_EXPENSE).ifBlank { ExpenseEntity.TYPE_EXPENSE }
            val transactionGroupId = obj.optString("transactionGroupId", "").ifBlank { null }
            val transactionGroupName = obj.optString("transactionGroupName", "").ifBlank { null }

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
                transactionType = transactionType,
                transactionGroupId = transactionGroupId,
                transactionGroupName = transactionGroupName,
                establishmentLogoUrl = finalLogoUrl,
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

        val customDatesObj = root.optJSONObject("custom_dates")
        if (customDatesObj != null) {
            val cdKeys = customDatesObj.keys()
            while (cdKeys.hasNext()) {
                val k = cdKeys.next()
                parsedPreferences[k] = customDatesObj.optString(k)
            }
        }

        if (root.has("selected_card_id")) {
            parsedPreferences["selected_card_id"] = root.optLong("selected_card_id")
        }
        if (root.has("selected_period")) {
            parsedPreferences["selected_period"] = root.optString("selected_period")
        }

        val rememberedLogosObj = root.optJSONObject("remembered_logos")
        val parsedRememberedLogos = mutableMapOf<String, String>()
        if (rememberedLogosObj != null) {
            val keys = rememberedLogosObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rawVal = rememberedLogosObj.optString(key, "")
                if (rawVal.isNotBlank()) {
                    if (rawVal.startsWith("http://") || rawVal.startsWith("https://")) {
                        parsedRememberedLogos[key] = rawVal
                    } else {
                        val fileName = File(rawVal.removePrefix("file://")).name
                        val localLogo = File(logosDir, fileName)
                        parsedRememberedLogos[key] = if (localLogo.exists()) localLogo.absolutePath else localLogo.absolutePath
                    }
                }
            }
        }

        return ParsedBackup(
            cards = parsedCards,
            expenses = parsedExpenses,
            photosCount = extractedPhotosCount,
            logosCount = extractedLogosCount,
            preferences = parsedPreferences,
            rememberedLogos = parsedRememberedLogos
        )
    }
}
