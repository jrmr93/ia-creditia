package com.example.util

import android.content.Context
import android.util.Log
import java.io.File

object LogoStorageUtil {
    private const val TAG = "LogoStorageUtil"
    private const val LOGOS_DIR_NAME = "logos"

    /**
     * Retorna el directorio interno de la aplicación donde se almacenan las fotos de logotipos.
     */
    fun getLogosDir(context: Context): File {
        val dir = File(context.filesDir, LOGOS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Elimina todos los archivos de fotos de logotipos almacenados en la app.
     */
    fun deleteAllLogoFiles(context: Context) {
        try {
            val dir = getLogosDir(context)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                files?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                Log.d(TAG, "Se eliminaron todos los logos locales (${files?.size ?: 0} archivos)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todos los logos: ${e.message}", e)
        }
    }

    /**
     * Elimina un archivo de logo específico dado su path o URI.
     */
    fun deleteLogoFile(filePathOrUri: String?) {
        if (filePathOrUri.isNullOrBlank()) return
        val paths = filePathOrUri.split("|").map { it.trim() }.filter { it.isNotBlank() }
        paths.forEach { singlePath ->
            try {
                if (!singlePath.startsWith("http://") && !singlePath.startsWith("https://")) {
                    val cleanPath = singlePath.removePrefix("file://")
                    val file = File(cleanPath)
                    if (file.exists() && file.isFile) {
                        file.delete()
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Retorna la lista de todos los archivos de logos existentes en el almacenamiento interno.
     */
    fun getAllLogoFiles(context: Context): List<File> {
        val dir = getLogosDir(context)
        return dir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    /**
     * Reconstruye o valida la ruta de un logo en el almacenamiento local para asegurar
     * compatibilidad tras restaurar respaldos o mover la base de datos entre dispositivos.
     */
    fun resolveLogoPath(context: Context, rawPathOrName: String?): String? {
        if (rawPathOrName.isNullOrBlank()) return null
        val parts = rawPathOrName.split("|").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        val logosDir = getLogosDir(context)

        val resolvedParts = parts.map { part ->
            if (part.startsWith("http://") || part.startsWith("https://")) {
                part
            } else {
                val clean = part.removePrefix("file://")
                val fileName = File(clean).name
                val localFile = File(logosDir, fileName)
                if (localFile.exists()) {
                    localFile.absolutePath
                } else {
                    val origFile = File(clean)
                    if (origFile.exists()) origFile.absolutePath else localFile.absolutePath
                }
            }
        }
        return resolvedParts.joinToString("|").ifBlank { null }
    }
}
