package com.rn.library.data

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.util.zip.ZipFile

/**
 * Импорт превью из fb2/epub/pdf.
 * Это восстановленная реализация (не обязательно 1:1 со старой), но с теми же API,
 * которые ожидает `WorkRepository.kt`: fromFb2/fromEpub/fromPdf(File): BookImportPreview?
 */
object BookFormatImporter {

    fun fromFb2(file: File): BookImportPreview? {
        return runCatching {
            val text = file.readText(Charsets.UTF_8)
            val title = Regex("<book-title>\\s*([^<]+)\\s*</book-title>", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                .takeIf { it.isNotBlank() } ?: return null

            val annotationRaw = Regex("<annotation>([\\s\\S]*?)</annotation>", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1).orEmpty()

            val desc = stripXmlToPlain(annotationRaw).take(4000)
            val firstName = Regex("<first-name>\\s*([^<]+)\\s*</first-name>", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim()
            val lastName = Regex("<last-name>\\s*([^<]+)\\s*</last-name>", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim()
            val author = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
            BookImportPreview(title = title, description = desc, author = author)
        }.getOrNull()
    }

    fun fromEpub(file: File): BookImportPreview? {
        return runCatching {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
                val containerXml = zip.getInputStream(containerEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val opfPath = Regex("full-path\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                    .find(containerXml)?.groupValues?.getOrNull(1)?.trim()
                    ?.trimStart('/') ?: return null

                val opfEntry = zip.getEntry(opfPath) ?: return null
                val opf = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }

                val title =
                    Regex("<dc:title[^>]*>([^<]+)</dc:title>", RegexOption.IGNORE_CASE).find(opf)?.groupValues?.getOrNull(1)?.trim()
                        ?: Regex("<title>\\s*([^<]+)\\s*</title>", RegexOption.IGNORE_CASE).find(opf)?.groupValues?.getOrNull(1)?.trim()
                        ?: file.nameWithoutExtension

                val descRaw =
                    Regex("<dc:description[^>]*>([\\s\\S]*?)</dc:description>", RegexOption.IGNORE_CASE).find(opf)?.groupValues?.getOrNull(1).orEmpty()

                val author =
                    Regex("<dc:creator[^>]*>([^<]+)</dc:creator>", RegexOption.IGNORE_CASE).find(opf)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }

                BookImportPreview(title = title, description = stripXmlToPlain(descRaw).take(4000), author = author)
            }
        }.getOrNull()
    }

    fun fromPdf(file: File): BookImportPreview? {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val title = file.nameWithoutExtension.ifBlank { "PDF" }
                    BookImportPreview(title = title, description = "PDF (${renderer.pageCount})")
                }
            }
        }.getOrNull()
    }

    private fun stripXmlToPlain(raw: String): String {
        return raw
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

