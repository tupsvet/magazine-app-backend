package com.magazines.service

import com.magazines.domain.exception.BadRequestException
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.io.DEFAULT_BUFFER_SIZE

class FileStorageService(
    private val storagePath: String,
) {
    fun saveCover(magazineId: UUID, partData: PartData.FileItem): String {
        try {
            val contentType = partData.contentType?.withoutParameters()?.toString()
                ?: throw BadRequestException("Unsupported cover format: null")

            val ext = when (contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw BadRequestException("Unsupported cover format: $contentType")
            }

            val relative = "$magazineId.$ext"
            val dir = File(storagePath, "covers")
            dir.mkdirs()
            val file = File(dir, relative)

            copyPartToFileWithLimit(partData, file, MAX_COVER_BYTES, "Cover file too large (max 5MB)")
            return relative
        } finally {
            partData.dispose()
        }
    }

    fun savePdf(issueId: UUID, partData: PartData.FileItem): String {
        try {
            val contentType = partData.contentType?.withoutParameters()?.toString()
                ?: throw BadRequestException("Unsupported PDF format: null")
            if (contentType != "application/pdf") {
                throw BadRequestException("Unsupported PDF format: $contentType")
            }

            val relative = "$issueId.pdf"
            val dir = File(storagePath, "pdfs")
            dir.mkdirs()
            val file = File(dir, relative)

            copyPartToFileWithLimit(partData, file, MAX_PDF_BYTES, "PDF file too large (max 50MB)")
            return relative
        } finally {
            partData.dispose()
        }
    }

    private fun copyPartToFileWithLimit(
        partData: PartData.FileItem,
        target: File,
        maxBytes: Long,
        tooLargeMessage: String,
    ) {
        try {
            var total = 0L
            partData.provider().use { input ->
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (!input.endOfInput) {
                        val read = input.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        total += read
                        if (total > maxBytes) {
                            throw BadRequestException(tooLargeMessage)
                        }
                        out.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: BadRequestException) {
            target.delete()
            throw e
        }
    }

    fun deleteFile(relativePath: String, type: String): Boolean {
        if (type != "covers" && type != "pdfs") return false
        if (relativePath.isEmpty() || relativePath.contains("..")) return false
        if (relativePath.any { it == '/' || it == '\\' }) return false

        val root = File(storagePath, type).canonicalFile
        val target = File(root, relativePath).canonicalFile
        val rootPath = root.path + File.separator
        if (!target.path.startsWith(rootPath) && target != root) return false

        return target.isFile && target.delete()
    }

    private companion object {
        private val MAX_COVER_BYTES = 5L * 1024 * 1024
        private val MAX_PDF_BYTES = 50L * 1024 * 1024
    }
}
