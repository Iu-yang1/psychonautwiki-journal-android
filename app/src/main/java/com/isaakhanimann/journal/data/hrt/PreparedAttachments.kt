package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.ui.tabs.settings.JournalExport
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID
import java.nio.file.Files

fun attachmentKey(experience: Int, note: Int, photo: Int) = "$experience:$note:$photo"

class PreparedAttachments(val directory: File, val paths: Map<String, String>) {
    fun discard() {
        require(directory.name.matches(Regex("import-[a-fA-F0-9-]+")))
        require(!Files.isSymbolicLink(directory.toPath()) && directory.canonicalFile.parentFile == requireNotNull(directory.parentFile).canonicalFile)
        check(directory.deleteRecursively()) { "无法清理未提交的导入附件" }
    }
}

class AttachmentPreparer(private val images: File, private val validateImage: (ByteArray) -> String) {
    fun prepare(backup: JournalExport): PreparedAttachments {
        check(images.isDirectory || images.mkdirs()) { "无法创建图片目录" }
        val directory = File(images, "import-${UUID.randomUUID()}")
        check(directory.mkdir())
        val paths = linkedMapOf<String, String>()
        try {
            backup.experiences.forEachIndexed { experienceIndex, experience ->
                experience.timedNotes.forEachIndexed { noteIndex, note ->
                    note.photos.forEachIndexed { photoIndex, photo ->
                        val bytes = Base64.getDecoder().decode(photo.imageBase64.filterNot(Char::isWhitespace))
                        val extension = validateImage(bytes)
                        require(extension.matches(Regex("[a-z0-9]{1,8}"))) { "图片格式无效" }
                        val file = File(directory, "${UUID.randomUUID()}.$extension")
                        FileOutputStream(file).use { output -> output.write(bytes); output.fd.sync() }
                        paths[attachmentKey(experienceIndex, noteIndex, photoIndex)] = file.absolutePath
                    }
                }
            }
            return PreparedAttachments(directory, paths)
        } catch (exception: Throwable) {
            try { PreparedAttachments(directory, paths).discard() }
            catch (cleanup: Exception) { exception.addSuppressed(cleanup) }
            throw exception
        }
    }
}
