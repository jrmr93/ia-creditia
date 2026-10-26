package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.UUID

object ReceiptStorageUtil {
    private const val TAG = "ReceiptStorageUtil"
    private const val RECEIPTS_DIR_NAME = "receipts"

    /**
     * Retorna el directorio interno de la aplicación donde se almacenan las fotos de comprobantes.
     */
    fun getReceiptsDir(context: Context): File {
        val dir = File(context.filesDir, RECEIPTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copia la imagen desde el Uri origen (ej. escáner ML Kit o galería)
     * hacia el almacenamiento interno privado de la aplicación.
     * Retorna la ruta absoluta del archivo guardado.
     */
    fun saveReceiptImage(context: Context, sourceUri: Uri): String? {
        return try {
            val receiptsDir = getReceiptsDir(context)
            val fileName = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(receiptsDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Comprobante guardado exitosamente en: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar comprobante en almacenamiento interno: ${e.message}", e)
            null
        }
    }

    /**
     * Elimina el archivo de foto de un comprobante dado su path o URI.
     */
    fun deleteReceiptFile(filePathOrUri: String?) {
        if (filePathOrUri.isNullOrBlank()) return
        val paths = filePathOrUri.split("|").map { it.trim() }.filter { it.isNotBlank() }
        paths.forEach { singlePath ->
            try {
                val cleanPath = singlePath.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists() && file.isFile) {
                    val deleted = file.delete()
                    try {
                        Log.d(TAG, "Archivo de comprobante eliminado ($deleted): ${file.absolutePath}")
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Elimina todos los archivos de fotos de comprobantes almacenados en la app.
     */
    fun deleteAllReceiptFiles(context: Context) {
        try {
            val dir = getReceiptsDir(context)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                files?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                Log.d(TAG, "Se eliminaron todos los comprobantes locales (${files?.size ?: 0} archivos)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todos los comprobantes: ${e.message}", e)
        }
    }

    /**
     * Elimina las fotos asociadas a una lista de consumos.
     */
    fun deleteReceiptsForExpenses(expenses: List<ExpenseEntity>) {
        expenses.forEach { expense ->
            deleteReceiptFile(expense.receiptUri)
        }
    }

    /**
     * Guarda la imagen del comprobante en el almacenamiento público del dispositivo
     * (Directorio Pictures/Creditia en la galería o Downloads).
     */
    fun downloadReceiptImage(
        context: Context,
        imageUriStr: String,
        suggestedTitle: String? = null
    ): Boolean {
        return try {
            val cleanTitle = suggestedTitle?.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                ?.take(30)
                ?.ifBlank { "Comprobante" } ?: "Comprobante"
            val fileName = "Creditia_${cleanTitle}_${System.currentTimeMillis()}.jpg"

            fun getInputStream(): InputStream? {
                return when {
                    imageUriStr.startsWith("/") || imageUriStr.startsWith("file://") -> {
                        val file = File(imageUriStr.removePrefix("file://"))
                        if (file.exists()) file.inputStream() else null
                    }
                    imageUriStr.startsWith("content://") -> {
                        context.contentResolver.openInputStream(Uri.parse(imageUriStr))
                    }
                    imageUriStr.startsWith("http://") || imageUriStr.startsWith("https://") -> {
                        URL(imageUriStr).openStream()
                    }
                    else -> {
                        val file = File(imageUriStr)
                        if (file.exists()) file.inputStream() else null
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Creditia")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val imageUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (imageUri != null) {
                        val input = getInputStream() ?: return false
                        input.use { inStream ->
                            context.contentResolver.openOutputStream(imageUri)?.use { outStream ->
                                inStream.copyTo(outStream)
                            } ?: return false
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(imageUri, contentValues, null, null)
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore insert falló, intentando fallback de almacenamiento: ${e.message}")
                }
            }

            // Fallback para API < 29 o entornos donde MediaStore inserción directa no está disponible
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Creditia"
            )
            if (!picturesDir.exists()) {
                picturesDir.mkdirs()
            }
            val destFile = File(picturesDir, fileName)
            val input = getInputStream() ?: return false
            input.use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar comprobante a la galería: ${e.message}", e)
            false
        }
    }
}
