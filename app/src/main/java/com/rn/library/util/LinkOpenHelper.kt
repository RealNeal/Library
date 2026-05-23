package com.rn.library.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Открывает ссылку во внешнем приложении: http(s)/content как есть,
 * локальный путь к файлу — через [FileProvider] и [Intent.ACTION_VIEW] с MIME.
 */
object LinkOpenHelper {

    fun openInExternalViewer(context: Context, rawLink: String): Boolean {
        val cleaned = rawLink.trim().replace("\\:", ":").replace("\\/", "/")
        if (cleaned.isBlank()) return false

        if (cleaned.startsWith("http://", ignoreCase = true) ||
            cleaned.startsWith("https://", ignoreCase = true)
        ) {
            return startView(context, Intent(Intent.ACTION_VIEW, Uri.parse(cleaned)))
        }

        if (cleaned.startsWith("content://", ignoreCase = true)) {
            return startView(context, Intent(Intent.ACTION_VIEW, Uri.parse(cleaned)))
        }

        val file = localFileOrNull(cleaned) ?: return false
        if (!file.isFile || !file.canRead()) return false

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = mimeForBookFile(file.name)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startView(context, intent)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun localFileOrNull(cleaned: String): File? {
        return when {
            cleaned.startsWith("file://") -> {
                val path = Uri.parse(cleaned).path ?: return null
                File(path)
            }
            cleaned.startsWith("/") -> File(cleaned)
            else -> null
        }
    }

    private fun mimeForBookFile(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "epub" -> "application/epub+zip"
            "pdf" -> "application/pdf"
            "fb2" -> "application/x-fictionbook+xml"
            else -> "application/octet-stream"
        }
    }

    private fun startView(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
