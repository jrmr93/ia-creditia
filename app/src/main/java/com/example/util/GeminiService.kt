package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class ReceiptAnalysisResult(
    val amount: Double,
    val dateString: String,
    val establishment: String,
    val description: String
)

data class LogoCandidate(
    val url: String,
    val title: String,
    val domain: String,
    val source: String,
    val thumbnailUrl: String? = null
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
            val openStream: () -> java.io.InputStream? = {
                if (uri.scheme == "file" || uri.scheme == null) {
                    val path = uri.path ?: uri.toString()
                    val f = java.io.File(path)
                    if (f.exists()) java.io.FileInputStream(f) else context.contentResolver.openInputStream(uri)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { stream ->
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

            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }


    private fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    data class BrandResolution(
        val displayName: String,
        val domain: String?,
        val searchQueries: List<String>
    )

    private val KNOWN_BRANDS: Map<String, BrandResolution> = mapOf(
        // Instituciones Públicas y Reguladoras
        "ant" to BrandResolution("ANT - Agencia Nacional de Tránsito", "ant.gob.ec", listOf("ANT Ecuador logo", "Agencia Nacional de Transito Ecuador logo")),
        "sri" to BrandResolution("SRI - Servicio de Rentas Internas", "sri.gob.ec", listOf("SRI Ecuador logo", "Servicio de Rentas Internas logo")),
        "iess" to BrandResolution("IESS", "iess.gob.ec", listOf("IESS Ecuador logo")),
        "cnt" to BrandResolution("CNT", "cnt.com.ec", listOf("CNT Ecuador logo")),
        "bce" to BrandResolution("Banco Central del Ecuador", "bce.fin.ec", listOf("Banco Central del Ecuador logo")),
        "cfn" to BrandResolution("CFN", "cfn.fin.ec", listOf("CFN Ecuador logo")),
        "biess" to BrandResolution("BIESS", "biess.fin.ec", listOf("BIESS Ecuador logo")),
        "atm" to BrandResolution("ATM Guayaquil", "atm.gob.ec", listOf("ATM Guayaquil logo")),
        "cte" to BrandResolution("Comisión de Tránsito del Ecuador", "comisiontransito.gob.ec", listOf("CTE Ecuador logo")),
        "cnel" to BrandResolution("CNEL EP", "cnel.gob.ec", listOf("CNEL EP logo")),
        "eeq" to BrandResolution("Empresa Eléctrica Quito", "eeq.com.ec", listOf("Empresa Electrica Quito logo")),
        "interagua" to BrandResolution("Interagua", "interagua.com.ec", listOf("Interagua Guayaquil logo")),
        "emapag" to BrandResolution("EMAPAG EP", "emapag-ep.gob.ec", listOf("EMAPAG logo")),
        "epmaps" to BrandResolution("EPMAPS Agua de Quito", "aguaquito.gob.ec", listOf("EPMAPS Agua de Quito logo")),
        "registro civil" to BrandResolution("Registro Civil del Ecuador", "registrocivil.gob.ec", listOf("Registro Civil Ecuador logo")),

        // Bancos y Entidades Financieras
        "pichincha" to BrandResolution("Banco Pichincha", "bancopichincha.com", listOf("Banco Pichincha logo", "Banco Pichincha Ecuador logo")),
        "banco pichincha" to BrandResolution("Banco Pichincha", "bancopichincha.com", listOf("Banco Pichincha logo")),
        "guayaquil" to BrandResolution("Banco Guayaquil", "bancoguayaquil.com", listOf("Banco Guayaquil logo")),
        "banco guayaquil" to BrandResolution("Banco Guayaquil", "bancoguayaquil.com", listOf("Banco Guayaquil logo")),
        "pacifico" to BrandResolution("Banco del Pacífico", "bancodelpacifico.com", listOf("Banco del Pacifico logo")),
        "banco del pacifico" to BrandResolution("Banco del Pacífico", "bancodelpacifico.com", listOf("Banco del Pacifico logo")),
        "produbanco" to BrandResolution("Produbanco", "produbanco.com.ec", listOf("Produbanco logo")),
        "bolivariano" to BrandResolution("Banco Bolivariano", "bolivariano.com", listOf("Banco Bolivariano logo")),
        "banco bolivariano" to BrandResolution("Banco Bolivariano", "bolivariano.com", listOf("Banco Bolivariano logo")),
        "internacional" to BrandResolution("Banco Internacional", "bancointernacional.com.ec", listOf("Banco Internacional Ecuador logo")),
        "banco internacional" to BrandResolution("Banco Internacional", "bancointernacional.com.ec", listOf("Banco Internacional Ecuador logo")),
        "austro" to BrandResolution("Banco del Austro", "bancodelaustro.com", listOf("Banco del Austro logo")),
        "banco del austro" to BrandResolution("Banco del Austro", "bancodelaustro.com", listOf("Banco del Austro logo")),
        "solidario" to BrandResolution("Banco Solidario", "banco-solidario.com", listOf("Banco Solidario Ecuador logo")),
        "banco solidario" to BrandResolution("Banco Solidario", "banco-solidario.com", listOf("Banco Solidario Ecuador logo")),
        "cooperativa jep" to BrandResolution("Cooperativa JEP", "coopjep.fin.ec", listOf("Cooperativa JEP logo")),
        "jep" to BrandResolution("Cooperativa JEP", "coopjep.fin.ec", listOf("Cooperativa JEP logo")),
        "diners" to BrandResolution("Diners Club", "dinersclub.com.ec", listOf("Diners Club Ecuador logo")),
        "deuna" to BrandResolution("DeUna", "deuna.com.ec", listOf("DeUna Banco Pichincha logo")),
        "payphone" to BrandResolution("Payphone", "livepayphone.com", listOf("Payphone Ecuador logo")),

        // Supermercados y Tiendas de Consumo
        "supermaxi" to BrandResolution("Supermaxi", "supermaxi.com", listOf("Supermaxi logo", "Supermaxi Ecuador logo")),
        "megamaxi" to BrandResolution("Megamaxi", "supermaxi.com", listOf("Megamaxi logo")),
        "favorita" to BrandResolution("Corporación Favorita", "supermaxi.com", listOf("Corporacion Favorita logo", "Supermaxi logo")),
        "aki" to BrandResolution("Supermercados Akí", "aki.com.ec", listOf("Supermercados Aki Ecuador logo")),
        "gran aki" to BrandResolution("Gran Akí", "aki.com.ec", listOf("Gran Aki Ecuador logo")),
        "super aki" to BrandResolution("Súper Akí", "aki.com.ec", listOf("Super Aki Ecuador logo")),
        "tia" to BrandResolution("Almacenes Tía", "tia.com.ec", listOf("Almacenes Tia Ecuador logo", "Tia logo png")),
        "almacenes tia" to BrandResolution("Almacenes Tía", "tia.com.ec", listOf("Almacenes Tia Ecuador logo")),
        "tuti" to BrandResolution("Tuti", "tuti.com.ec", listOf("Tuti Supermercados Ecuador logo")),
        "coral" to BrandResolution("Coral Hipermercados", "coralph.com", listOf("Coral Hipermercados logo")),
        "coralph" to BrandResolution("Coral Hipermercados", "coralph.com", listOf("Coral Hipermercados logo")),
        "comisariato" to BrandResolution("Mi Comisariato", "micomisariato.com", listOf("Mi Comisariato logo", "Corporacion El Rosado logo")),
        "mi comisariato" to BrandResolution("Mi Comisariato", "micomisariato.com", listOf("Mi Comisariato logo")),
        "ferrisariato" to BrandResolution("Ferrisariato", "ferrisariato.com", listOf("Ferrisariato logo")),
        "santamaria" to BrandResolution("Supermercados Santa María", "santamaria.ec", listOf("Supermercados Santa Maria Ecuador logo")),
        "santa maria" to BrandResolution("Supermercados Santa María", "santamaria.ec", listOf("Supermercados Santa Maria Ecuador logo")),
        "hipervalle" to BrandResolution("Hipervalle", "hipervalle.com.ec", listOf("Hipervalle logo")),

        // Farmacias y Cuidado Personal
        "fybeca" to BrandResolution("Fybeca", "fybeca.com", listOf("Fybeca logo", "Farmacias Fybeca logo")),
        "sana sana" to BrandResolution("Farmacias Sana Sana", "sanasana.com.ec", listOf("Farmacias Sana Sana logo", "Sana Sana Ecuador logo")),
        "cruz azul" to BrandResolution("Farmacias Cruz Azul", "farmaciascruzazul.ec", listOf("Farmacias Cruz Azul Ecuador logo")),
        "farmacias economicas" to BrandResolution("Farmacias Económicas", "farmaciaseconomicas.com.ec", listOf("Farmacias Economicas Ecuador logo")),
        "economicas" to BrandResolution("Farmacias Económicas", "farmaciaseconomicas.com.ec", listOf("Farmacias Economicas Ecuador logo")),
        "medicity" to BrandResolution("Farmacias Medicity", "farmaciasmedicity.com", listOf("Farmacias Medicity logo")),
        "farmacias mia" to BrandResolution("Farmacias Mía", "farmaciasmia.com", listOf("Farmacias Mia Ecuador logo")),
        "dipaso" to BrandResolution("Dipaso", "dipaso.com.ec", listOf("Dipaso Ecuador logo")),

        // Moda, Hogar y Tiendas Departamentales
        "de prati" to BrandResolution("De Prati", "deprati.com.ec", listOf("De Prati logo")),
        "deprati" to BrandResolution("De Prati", "deprati.com.ec", listOf("De Prati logo")),
        "etafashion" to BrandResolution("Etafashion", "etafashion.com", listOf("Etafashion logo")),
        "eta fashion" to BrandResolution("Etafashion", "etafashion.com", listOf("Etafashion logo")),
        "marathon" to BrandResolution("Marathon Sports", "marathonsports.ec", listOf("Marathon Sports logo")),
        "marathon sports" to BrandResolution("Marathon Sports", "marathonsports.ec", listOf("Marathon Sports logo")),
        "sukasa" to BrandResolution("Sukasa", "sukasa.com", listOf("Sukasa Ecuador logo")),
        "pycca" to BrandResolution("Pycca", "pycca.com", listOf("Pycca logo")),
        "bebemundo" to BrandResolution("Bebemundo", "bebemundo.ec", listOf("Bebemundo logo")),
        "jugueton" to BrandResolution("Juguetón", "jugueton.com.ec", listOf("Jugueton Ecuador logo")),

        // Restaurantes y Cafeterías
        "sweet & coffee" to BrandResolution("Sweet & Coffee", "sweetandcoffee.com.ec", listOf("Sweet and Coffee logo")),
        "sweet and coffee" to BrandResolution("Sweet & Coffee", "sweetandcoffee.com.ec", listOf("Sweet and Coffee logo")),
        "juan valdez" to BrandResolution("Juan Valdez", "juanvaldezcafe.com", listOf("Juan Valdez Cafe logo")),
        "kfc" to BrandResolution("KFC", "kfc.com.ec", listOf("KFC logo", "KFC Ecuador logo")),
        "mcdonald" to BrandResolution("McDonald's", "mcdonalds.com.ec", listOf("McDonalds logo")),
        "mcdonalds" to BrandResolution("McDonald's", "mcdonalds.com.ec", listOf("McDonalds logo")),
        "burger king" to BrandResolution("Burger King", "burgerking.com.ec", listOf("Burger King logo")),
        "pizza hut" to BrandResolution("Pizza Hut", "pizzahut.com.ec", listOf("Pizza Hut logo")),
        "dominos" to BrandResolution("Domino's Pizza", "dominos.com.ec", listOf("Dominos Pizza logo")),
        "papa johns" to BrandResolution("Papa John's", "papajohns.com.ec", listOf("Papa Johns logo")),

        // Telefonía y Combustibles
        "claro" to BrandResolution("Claro", "claro.com.ec", listOf("Claro Ecuador logo", "Claro logo")),
        "conecel" to BrandResolution("Claro", "claro.com.ec", listOf("Claro Ecuador logo")),
        "movistar" to BrandResolution("Movistar", "movistar.com.ec", listOf("Movistar Ecuador logo")),
        "otecel" to BrandResolution("Movistar", "movistar.com.ec", listOf("Movistar Ecuador logo")),
        "tuenti" to BrandResolution("Tuenti", "tuenti.ec", listOf("Tuenti Ecuador logo")),
        "primax" to BrandResolution("Primax", "primax.com.ec", listOf("Primax Ecuador logo")),
        "petroecuador" to BrandResolution("Petroecuador", "eppetroecuador.ec", listOf("Petroecuador logo")),
        "terpel" to BrandResolution("Terpel", "terpel.com", listOf("Terpel logo")),

        // Movilidad, Apps y Servicios Globales
        "uber" to BrandResolution("Uber", "uber.com", listOf("Uber logo", "Uber Eats logo")),
        "didi" to BrandResolution("Didi", "didi-global.com", listOf("Didi logo")),
        "cabify" to BrandResolution("Cabify", "cabify.com", listOf("Cabify logo")),
        "pedidosya" to BrandResolution("PedidosYa", "pedidosya.com.ec", listOf("PedidosYa logo")),
        "pedidos ya" to BrandResolution("PedidosYa", "pedidosya.com.ec", listOf("PedidosYa logo")),
        "rappi" to BrandResolution("Rappi", "rappi.com.ec", listOf("Rappi logo")),
        "netflix" to BrandResolution("Netflix", "netflix.com", listOf("Netflix logo")),
        "spotify" to BrandResolution("Spotify", "spotify.com", listOf("Spotify logo")),
        "apple" to BrandResolution("Apple", "apple.com", listOf("Apple logo")),
        "google" to BrandResolution("Google", "google.com", listOf("Google logo")),
        "amazon" to BrandResolution("Amazon", "amazon.com", listOf("Amazon logo")),
        "shein" to BrandResolution("Shein", "shein.com", listOf("Shein logo")),
        "temu" to BrandResolution("Temu", "temu.com", listOf("Temu logo")),
        "openai" to BrandResolution("OpenAI", "openai.com", listOf("OpenAI logo", "ChatGPT logo")),
        "chatgpt" to BrandResolution("ChatGPT", "openai.com", listOf("ChatGPT logo")),
        "playstation" to BrandResolution("PlayStation", "playstation.com", listOf("PlayStation logo")),
        "steam" to BrandResolution("Steam", "steampowered.com", listOf("Steam logo")),
        "disney" to BrandResolution("Disney+", "disneyplus.com", listOf("Disney Plus logo")),
        "max" to BrandResolution("Max", "max.com", listOf("Max logo HBO")),
        "hbo" to BrandResolution("HBO Max", "max.com", listOf("HBO Max logo")),
        "youtube" to BrandResolution("YouTube", "youtube.com", listOf("YouTube logo"))
    )

    fun sanitizeEstablishmentQuery(query: String): String {
        var clean = query.trim()
        clean = clean.replace(Regex("""(?i)\b(s\.?a\.?s?|c\.?a\.?|ltda\.?|cia\.?\s*ltda\.?|corp\.?|inc\.?|llc|e\.?p\.?)\b"""), " ")
        clean = clean.replace(Regex("""(?i)\b(sucursal|agencia|matriz|local|ruc\s*\d+|pos|debito|consumo|compra|tarjeta|pago)\b"""), " ")
        clean = clean.replace(Regex("""[*#_]"""), " ")
        clean = clean.replace(Regex("""\s+"""), " ").trim()
        return if (clean.length >= 2) clean else query.trim()
    }

    private fun resolveBrandLocally(query: String): BrandResolution? {
        val lower = query.lowercase(Locale.ROOT).trim()
        KNOWN_BRANDS[lower]?.let { return it }
        for ((key, value) in KNOWN_BRANDS) {
            if (lower.contains(key) || key.contains(lower)) {
                return value
            }
        }
        return null
    }

    private suspend fun resolveBrandWithGemini(apiKey: String, modelName: String, query: String): BrandResolution? {
        if (apiKey.isBlank()) return null
        return try {
            val cleanModel = sanitizeModelName(modelName)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"
            val prompt = "Dado el comercio '$query', responde ÚNICAMENTE un JSON sin markdown ni comillas triples con este formato: {\"brand\":\"nombre de marca comercial\",\"domain\":\"dominio web oficial o null\",\"queries\":[\"busqueda 1\",\"busqueda 2\"]}"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
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
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: ""
                val jsonResponse = JSONObject(bodyStr)
                val text = jsonResponse.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                val cleanJson = text.replace("```json", "", ignoreCase = true).replace("```", "").trim()
                val parsed = JSONObject(cleanJson)
                val brand = parsed.optString("brand", "").trim()
                val domain = parsed.optString("domain", "").trim().takeIf { it.isNotBlank() && it != "null" }
                val queriesArr = parsed.optJSONArray("queries")
                val queries = mutableListOf<String>()
                if (queriesArr != null) {
                    for (i in 0 until queriesArr.length()) {
                        val q = queriesArr.optString(i, "").trim()
                        if (q.isNotBlank()) queries.add(q)
                    }
                }
                if (brand.isNotBlank()) {
                    BrandResolution(
                        displayName = brand,
                        domain = domain,
                        searchQueries = queries
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchWikipediaCandidates(httpClient: OkHttpClient, searchTerm: String): List<LogoCandidate> {
        val list = mutableListOf<LogoCandidate>()
        try {
            val enc = URLEncoder.encode(searchTerm, "UTF-8")
            val url = "https://es.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$enc&prop=pageimages&pithumbsize=500&format=json"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CreditiaApp/1.0 (info@creditia.app)")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = JSONObject(body)
                    val queryObj = root.optJSONObject("query")
                    val pagesObj = queryObj?.optJSONObject("pages")
                    if (pagesObj != null) {
                        val keys = pagesObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val page = pagesObj.optJSONObject(key) ?: continue
                            val thumbObj = page.optJSONObject("thumbnail")
                            val thumbUrl = thumbObj?.optString("source", "")?.trim() ?: ""
                            val title = page.optString("title", searchTerm).trim()
                            if (thumbUrl.isNotBlank() && !thumbUrl.endsWith(".pdf", ignoreCase = true) && !thumbUrl.endsWith(".pdf.jpg", ignoreCase = true)) {
                                list.add(
                                    LogoCandidate(
                                        url = thumbUrl,
                                        title = if (title.isNotBlank()) title else searchTerm,
                                        domain = "es.wikipedia.org",
                                        source = "Wikipedia",
                                        thumbnailUrl = thumbUrl
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try { Log.w("GeminiService", "Aviso en Wikipedia: ${e.message}") } catch (_: Throwable) {}
        }
        return list
    }

    private fun fetchWikimediaCandidates(httpClient: OkHttpClient, searchTerm: String): List<LogoCandidate> {
        val list = mutableListOf<LogoCandidate>()
        try {
            val enc = URLEncoder.encode("$searchTerm logo", "UTF-8")
            val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=$enc&gsrlimit=10&prop=imageinfo&iiprop=url|thumburl|dimensions&iiurlwidth=500&format=json"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CreditiaApp/1.0 (info@creditia.app)")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = JSONObject(body)
                    val queryObj = root.optJSONObject("query")
                    val pagesObj = queryObj?.optJSONObject("pages")
                    if (pagesObj != null) {
                        val keys = pagesObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val page = pagesObj.optJSONObject(key) ?: continue
                            val imageInfoArray = page.optJSONArray("imageinfo") ?: continue
                            val info = imageInfoArray.optJSONObject(0) ?: continue
                            val fullUrl = info.optString("url", "").trim()
                            val thumbUrl = info.optString("thumburl", "").trim()
                            val pageTitle = page.optString("title", searchTerm).removePrefix("File:").removeSuffix(".svg").removeSuffix(".png").removeSuffix(".jpg").trim()

                            val usableThumb = if (thumbUrl.isNotBlank()) thumbUrl else fullUrl
                            val usableFull = if (fullUrl.endsWith(".svg", ignoreCase = true) && thumbUrl.isNotBlank()) {
                                thumbUrl
                            } else if (fullUrl.isNotBlank()) {
                                fullUrl
                            } else {
                                thumbUrl
                            }

                            if (usableFull.isNotBlank() && !usableFull.endsWith(".pdf", ignoreCase = true) && !usableFull.endsWith(".pdf.jpg", ignoreCase = true)) {
                                list.add(
                                    LogoCandidate(
                                        url = usableFull,
                                        title = if (pageTitle.isNotBlank()) pageTitle else "$searchTerm (Wikimedia)",
                                        domain = "commons.wikimedia.org",
                                        source = "Wikimedia",
                                        thumbnailUrl = usableThumb
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try { Log.w("GeminiService", "Aviso en Wikimedia: ${e.message}") } catch (_: Throwable) {}
        }
        return list
    }

    private fun fetchBingCandidates(httpClient: OkHttpClient, query: String, sourceName: String): List<LogoCandidate> {
        val list = mutableListOf<LogoCandidate>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/images/search?q=$encoded&form=HDRSC2&first=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string() ?: ""
                    val iuscRegex = Regex("""class="iusc"[^>]*\bm="([^"]+)"""")
                    for (match in iuscRegex.findAll(html).take(15)) {
                        try {
                            val rawJson = unescapeHtml(match.groupValues[1])
                            val murl = Regex(""""murl":"([^"]+)"""").find(rawJson)?.groupValues?.get(1)?.replace("&amp;", "&")?.trim() ?: ""
                            val turl = Regex(""""turl":"([^"]+)"""").find(rawJson)?.groupValues?.get(1)?.replace("&amp;", "&")?.trim() ?: ""
                            val title = Regex(""""t":"([^"]+)"""").find(rawJson)?.groupValues?.get(1)?.trim() ?: query
                            val purl = Regex(""""purl":"([^"]+)"""").find(rawJson)?.groupValues?.get(1)?.trim() ?: ""
                            val domain = try {
                                java.net.URI(purl).host?.removePrefix("www.") ?: "Web"
                            } catch (e: Exception) {
                                "Web"
                            }

                            val fullImg = if (murl.startsWith("http")) murl else turl
                            val thumbImg = if (turl.startsWith("http")) turl else murl

                            if (fullImg.startsWith("http") && !fullImg.endsWith(".pdf", ignoreCase = true)) {
                                list.add(
                                    LogoCandidate(
                                        url = fullImg,
                                        title = if (title.isNotBlank()) title else query,
                                        domain = domain,
                                        source = sourceName,
                                        thumbnailUrl = thumbImg
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Ignorar json individual
                        }
                    }
                }
            }
        } catch (e: Exception) {
            try { Log.w("GeminiService", "Aviso en Bing ($query): ${e.message}") } catch (_: Throwable) {}
        }
        return list
    }

    private fun createDomainCandidates(domain: String, brandName: String): List<LogoCandidate> {
        val cleanDomain = domain.lowercase(Locale.ROOT).trim().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        if (cleanDomain.isBlank() || cleanDomain.length < 3) return emptyList()

        return listOf(
            LogoCandidate(
                url = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$cleanDomain&size=256",
                title = "$brandName (Icono HD)",
                domain = cleanDomain,
                source = "Icono HD",
                thumbnailUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$cleanDomain&size=256"
            ),
            LogoCandidate(
                url = "https://icon.horse/icon/$cleanDomain",
                title = "$brandName (Marca Web)",
                domain = cleanDomain,
                source = "Marca Web",
                thumbnailUrl = "https://icon.horse/icon/$cleanDomain"
            ),
            LogoCandidate(
                url = "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=512",
                title = "$brandName (Favicon 512)",
                domain = cleanDomain,
                source = "Icono 512",
                thumbnailUrl = "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=256"
            )
        )
    }

    /**
     * Busca imágenes y logotipos en internet para el nombre del establecimiento comercial consultando
     * en paralelo múltiples fuentes confiables (Wikipedia Oficial, Wikimedia Commons, Motores Web Multi-Consulta,
     * y Dominios Oficiales HD), permitiendo que todas las imágenes sean seleccionadas directamente en la app.
     */
    suspend fun searchLogoCandidates(
        apiKey: String,
        modelName: String,
        query: String
    ): List<LogoCandidate> = withContext(Dispatchers.IO) {
        val rawClean = query.trim()
        if (rawClean.isBlank()) return@withContext emptyList()

        val sanitizedQuery = sanitizeEstablishmentQuery(rawClean)
        val localBrand = resolveBrandLocally(sanitizedQuery) ?: resolveBrandLocally(rawClean)

        coroutineScope {
            // Si la API key está disponible y el comercio no está en el catálogo local, consultar a Gemini en paralelo
            val geminiDeferred = if (localBrand == null && apiKey.isNotBlank()) {
                async { resolveBrandWithGemini(apiKey, modelName, rawClean) }
            } else null

            val brandName = localBrand?.displayName ?: sanitizedQuery
            val resolvedDomain = localBrand?.domain ?: if (sanitizedQuery.length > 3 && !sanitizedQuery.contains(" ")) {
                if (sanitizedQuery.contains(".")) sanitizedQuery else "$sanitizedQuery.com"
            } else null

            // Lista de consultas web especializadas
            val bingQueries = mutableListOf<String>()
            localBrand?.searchQueries?.let { bingQueries.addAll(it) }
            bingQueries.add("$brandName logo")
            if (rawClean.length <= 4 || brandName.length <= 4) {
                bingQueries.add("$brandName Ecuador logo")
            }
            bingQueries.add("$brandName logo png vector")
            bingQueries.add(rawClean)
            val distinctBingQueries = bingQueries.distinct().take(4)

            // Lanzar consultas concurrentes a todas las fuentes
            val wikiJob = async { fetchWikipediaCandidates(client, brandName) }
            val wikimediaJob = async { fetchWikimediaCandidates(client, brandName) }
            val bingJobs = distinctBingQueries.map { q ->
                async { fetchBingCandidates(client, q, sourceName = "Web") }
            }

            // Esperar resultados de las fuentes web
            val wikiCandidates = wikiJob.await()
            val wikimediaCandidates = wikimediaJob.await()
            val bingCandidates = bingJobs.awaitAll().flatten()

            // Esperar resolución de Gemini si fue lanzada
            val geminiBrand = geminiDeferred?.await()
            val finalDomain = resolvedDomain ?: geminiBrand?.domain

            val domainCandidates = if (!finalDomain.isNullOrBlank()) {
                createDomainCandidates(finalDomain, brandName)
            } else emptyList()

            // Si Gemini dio consultas adicionales no incluidas, podemos incorporar sus logos
            val extraGeminiCandidates = if (geminiBrand != null && geminiBrand.displayName != brandName) {
                fetchWikimediaCandidates(client, geminiBrand.displayName)
            } else emptyList()

            // Consolidar todos los candidatos y eliminar duplicados
            val allCandidates = mutableListOf<LogoCandidate>()
            allCandidates.addAll(domainCandidates)
            allCandidates.addAll(wikimediaCandidates)
            allCandidates.addAll(wikiCandidates)
            allCandidates.addAll(extraGeminiCandidates)
            allCandidates.addAll(bingCandidates)

            // Priorizar y limitar a 50 resultados distintos
            allCandidates.distinctBy { it.url }.take(50)
        }
    }

    private fun fetchImageBytes(context: Context, input: String): ByteArray? {
        return try {
            when {
                input.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(input))?.use { it.readBytes() }
                }
                input.startsWith("file://") -> {
                    val f = File(input.removePrefix("file://"))
                    if (f.exists()) f.readBytes() else null
                }
                input.startsWith("/") -> {
                    val f = File(input)
                    if (f.exists()) f.readBytes() else null
                }
                input.startsWith("http://") || input.startsWith("https://") -> {
                    val req = Request.Builder()
                        .url(input)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.bytes() else null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Descarga y procesa un logotipo o imagen comercial (desde URL web o URI local), gestionando
     * y convirtiendo su tamaño a un formato estándar optimizado para logotipos (máx 512px) y guardándolo
     * en almacenamiento interno como PNG con transparencia preservada, garantizando rendimiento y uso offline.
     */
    suspend fun downloadAndSaveLogo(
        context: Context,
        logoUrlOrUri: String,
        establishmentName: String,
        fallbackUrl: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanInput = logoUrlOrUri.trim()
        if (cleanInput.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La URL o imagen no puede estar vacía."))
        }

        try {
            val isSvg = cleanInput.endsWith(".svg", ignoreCase = true)
            val hasPngFallback = !fallbackUrl.isNullOrBlank() && !fallbackUrl.endsWith(".svg", ignoreCase = true)
            val primaryUrl = if (isSvg && hasPngFallback) fallbackUrl!!.trim() else cleanInput
            val secondaryUrl = if (isSvg && hasPngFallback) cleanInput else fallbackUrl?.trim()

            var bytes = fetchImageBytes(context, primaryUrl)
            if (bytes == null && !secondaryUrl.isNullOrBlank() && secondaryUrl != primaryUrl) {
                bytes = fetchImageBytes(context, secondaryUrl)
            }

            if (bytes == null || bytes.size < 50) {
                return@withContext Result.failure(Exception("No se pudo descargar u obtener la imagen seleccionada."))
            }

            // 1. Medir dimensiones reales de la imagen sin decodificarla por completo en memoria
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return@withContext Result.failure(Exception("El archivo o enlace seleccionado no contiene una imagen válida."))
            }

            // 2. Calcular factor de submuestreo (inSampleSize) para evitar OutOfMemory en imágenes de gran tamaño
            val targetMaxDimension = 512
            var inSampleSize = 1
            val maxSide = max(rawWidth, rawHeight)
            while ((maxSide / (inSampleSize * 2)) >= targetMaxDimension) {
                inSampleSize *= 2
            }

            // 3. Decodificar la imagen muestreada de forma segura
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return@withContext Result.failure(Exception("No se pudo decodificar el formato de la imagen."))

            // 4. Redimensionar exactamente preservando la relación de aspecto si supera el tamaño objetivo (máx 512px)
            val currentMaxSide = max(sampledBitmap.width, sampledBitmap.height)
            val finalBitmap = if (currentMaxSide > targetMaxDimension) {
                val scale = targetMaxDimension.toFloat() / currentMaxSide.toFloat()
                val targetWidth = (sampledBitmap.width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (sampledBitmap.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
                if (scaled != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                scaled
            } else {
                sampledBitmap
            }

            // 5. Guardar en directorio interno de logotipos en formato PNG
            val logosDir = LogoStorageUtil.getLogosDir(context)

            val safeBaseName = establishmentName.lowercase(Locale.ROOT)
                .replace(Regex("""[^a-z0-9]"""), "_")
                .take(25)
            val destFile = File(logosDir, "logo_${safeBaseName}_${System.currentTimeMillis()}.png")

            FileOutputStream(destFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            finalBitmap.recycle()

            Result.success(destFile.absolutePath)
        } catch (e: OutOfMemoryError) {
            Result.failure(Exception("La imagen seleccionada es demasiado pesada para la memoria del dispositivo."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca en internet el logotipo oficial del establecimiento comercial usando Gemini IA y servicios HD,
     * y lo descarga al almacenamiento interno de la app para que funcione offline.
     */
    suspend fun fetchAndSaveEstablishmentLogo(
        context: Context,
        apiKey: String,
        modelName: String,
        establishmentName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val candidates = searchLogoCandidates(apiKey, modelName, establishmentName)
        if (candidates.isNotEmpty()) {
            val candidate = candidates.first()
            downloadAndSaveLogo(
                context = context,
                logoUrlOrUri = candidate.url,
                establishmentName = establishmentName,
                fallbackUrl = candidate.thumbnailUrl
            )
        } else {
            Result.failure(Exception("No se encontró ningún logotipo válido para \"$establishmentName\""))
        }
    }
}
