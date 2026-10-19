package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class ReceiptAnalysisResult(
    val amount: Double,
    val dateString: String,
    val establishment: String,
    val description: String
)

sealed class GeminiTestResult {
    data class Success(val message: String, val latencyMs: Long) : GeminiTestResult()
    data class Error(val message: String) : GeminiTestResult()
}

object GeminiService {

    val AVAILABLE_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-flash-latest",
        "gemini-3.7-flash"
    )

    const val DEFAULT_MODEL = "gemini-3.5-flash"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun sanitizeModelName(modelName: String): String {
        val trimmed = modelName.trim()
        val withoutPrefix = trimmed.removePrefix("models/").trim()
        return if (withoutPrefix.isBlank()) DEFAULT_MODEL else withoutPrefix
    }

    /**
     * Consulta a Google AI Studio la lista de modelos habilitados para la API Key dada
     */
    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La clave de API no puede estar vacía."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey&pageSize=100"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = extractErrorMessage(response.code, responseBody, "models.list")
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(responseBody)
                val modelsArray = json.optJSONArray("models") ?: JSONArray()
                val list = mutableListOf<String>()

                for (i in 0 until modelsArray.length()) {
                    val item = modelsArray.optJSONObject(i) ?: continue
                    val name = item.optString("name", "").removePrefix("models/").trim()
                    val methods = item.optJSONArray("supportedGenerationMethods")
                    var supportsGenerate = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") {
                                supportsGenerate = true
                                break
                            }
                        }
                    }
                    if (supportsGenerate && name.isNotBlank()) {
                        list.add(name)
                    }
                }

                // Priorizamos modelos flash y pro
                val prioritized = list.filter { it.contains("flash", ignoreCase = true) || it.contains("pro", ignoreCase = true) }
                Result.success(if (prioritized.isNotEmpty()) prioritized else list)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(apiKey: String, modelName: String): GeminiTestResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext GeminiTestResult.Error("La clave de API no puede estar vacía.")
        }

        val cleanModel = sanitizeModelName(modelName)
        val startTime = System.currentTimeMillis()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$trimmedKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Responde únicamente con la palabra: OK")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val latency = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    // Si el modelo seleccionado da 404 o no está disponible, consultamos los modelos habilitados
                    if (response.code == 404 || responseBody.contains("not found", ignoreCase = true)) {
                        val availableResult = fetchAvailableModels(trimmedKey)
                        val models = availableResult.getOrNull()?.filter { it.contains("flash", ignoreCase = true) }
                        val modelsHint = if (!models.isNullOrEmpty()) {
                            "\n\nModelos recomendados en tu cuenta:\n• " + models.take(5).joinToString("\n• ") +
                                    "\n\nTe sugerimos seleccionar 'gemini-3.5-flash'."
                        } else {
                            ""
                        }
                        return@withContext GeminiTestResult.Error(
                            "El modelo '$cleanModel' no está disponible para tu API Key.$modelsHint"
                        )
                    }

                    val errorMessage = extractErrorMessage(response.code, responseBody, cleanModel)
                    return@withContext GeminiTestResult.Error(errorMessage)
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                if (text.isNotBlank()) {
                    GeminiTestResult.Success(
                        message = "Conexión exitosa con $cleanModel ($latency ms)",
                        latencyMs = latency
                    )
                } else {
                    GeminiTestResult.Error("El modelo respondió sin contenido.")
                }
            }
        } catch (e: Throwable) {
            val errorMsg = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                e.message?.contains("ConnectException", ignoreCase = true) == true ->
                    "Sin conexión a internet. Verifica tu red móvil o WiFi."
                else -> "Error de prueba: ${e.localizedMessage ?: e.message ?: "Desconocido"}"
            }
            GeminiTestResult.Error(errorMsg)
        }
    }

    suspend fun analyzeReceipt(
        apiKey: String,
        modelName: String,
        bitmap: Bitmap
    ): Result<ReceiptAnalysisResult> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No se ha configurado la API Key de Gemini en Ajustes."))
        }

        val primaryModel = sanitizeModelName(modelName)
        val firstAttempt = executeReceiptAnalysis(trimmedKey, primaryModel, bitmap)

        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        // Si el modelo principal falló por no estar disponible (404), intentamos con gemini-3.5-flash
        val err = firstAttempt.exceptionOrNull()
        val errMsg = err?.message ?: ""
        if ((errMsg.contains("404") || errMsg.contains("no está disponible", ignoreCase = true) || errMsg.contains("not found", ignoreCase = true)) && primaryModel != "gemini-3.5-flash") {
            val fallbackAttempt = executeReceiptAnalysis(trimmedKey, "gemini-3.5-flash", bitmap)
            if (fallbackAttempt.isSuccess) {
                return@withContext fallbackAttempt
            }
        }

        firstAttempt
    }

    private fun executeReceiptAnalysis(
        apiKey: String,
        modelName: String,
        bitmap: Bitmap
    ): Result<ReceiptAnalysisResult> {
        return try {
            val cleanModel = sanitizeModelName(modelName)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                Eres un asistente contable experto. Analiza la siguiente imagen de un comprobante de pago, factura, ticket de caja o recibo comercial.
                Extrae exactamente estos campos en formato JSON estricto:
                {
                  "monto": 0.0,
                  "fecha": "YYYY/MM/DD",
                  "establecimiento": "Nombre del comercio o establecimiento",
                  "descripcion": "Breve descripción de la compra o categoría principal"
                }

                Reglas de extracción:
                1. "monto": Debe ser el valor total final pagado en formato numérico decimal (ej: 18.50). Si tiene símbolo de moneda quítalo.
                2. "fecha": La fecha del comprobante en formato estricto YYYY/MM/DD (año de 4 dígitos, mes de 2 dígitos, día de 2 dígitos). Si no hay año legible, asume el año 2026. Si no hay fecha legible, deja cadena vacía "".
                3. "establecimiento": El nombre de la tienda, restaurante, comercio o emisor del comprobante.
                4. "descripcion": Un resumen conciso (máximo 8 palabras) de los artículos comprados o el concepto (ej: 'Almuerzo de trabajo', 'Medicamentos', 'Supermercado').
                5. Responde SOLAMENTE con el objeto JSON válido.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMessage = extractErrorMessage(response.code, responseBody, cleanModel)
                    return Result.failure(Exception(errorMessage))
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                val cleanJson = rawText
                    .replace("```json", "", ignoreCase = true)
                    .replace("```", "")
                    .trim()

                val jsonObject = JSONObject(cleanJson)
                val amount = jsonObject.optDouble("monto", 0.0)
                val dateString = jsonObject.optString("fecha", "")
                val establishment = jsonObject.optString("establecimiento", "")
                val description = jsonObject.optString("descripcion", "")

                Result.success(
                    ReceiptAnalysisResult(
                        amount = if (amount.isNaN()) 0.0 else amount,
                        dateString = dateString,
                        establishment = establishment,
                        description = description
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(statusCode: Int, responseBody: String, modelName: String): String {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val apiMsg = errorObj?.optString("message", "") ?: ""

            when {
                statusCode == 400 || statusCode == 403 || apiMsg.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "API Key no válida. Revisa la clave ingresada de Google AI Studio. (Detalle: $apiMsg)"
                statusCode == 429 || apiMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                    "Cuota de peticiones excedida para el modelo $modelName. (Detalle: $apiMsg)"
                statusCode == 404 || apiMsg.contains("not found", ignoreCase = true) ->
                    "El modelo '$modelName' no está disponible para tu API Key. (Detalle: $apiMsg)"
                apiMsg.isNotBlank() -> apiMsg
                else -> "Error del servidor Gemini (HTTP $statusCode)"
            }
        } catch (e: Exception) {
            "Error HTTP $statusCode al conectar con Gemini."
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun loadSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxSide = max(options.outWidth, options.outHeight)
            if (maxSide > maxDimension) {
                while ((maxSide / (inSampleSize * 2)) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }
}
