package com.isaakhanimann.journal.data.hrt

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.room.withTransaction
import com.isaakhanimann.journal.data.room.AppDatabase
import com.isaakhanimann.journal.ui.tabs.settings.JournalExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalImportService @Inject constructor(@param:ApplicationContext private val context: Context, private val database: AppDatabase) {
    private val mutex = Mutex()

    suspend fun replace(backup: JournalExport, progress: (Float, String) -> Unit) = mutex.withLock {
        backup.validateForReplacement()
        progress(0.1f, "校验并准备照片…")
        var prepared: PreparedAttachments? = null
        try {
            withContext(Dispatchers.IO) {
                val cancellation = currentCoroutineContext()
                AttachmentPreparer(File(context.filesDir, "images")) { bytes ->
                    cancellation.ensureActive()
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    require(options.outWidth > 0 && options.outHeight > 0) { "备份包含损坏的图片" }
                    val mime = options.outMimeType
                    var sample = 1
                    while (options.outWidth.toLong() * options.outHeight / sample / sample > 4000000L) sample *= 2
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("照片内容解码失败")
                    decoded.recycle()
                    ImagePayload.extension(bytes, mime)
                }.prepare(backup).also { prepared = it }
            }
            val attachments = requireNotNull(prepared)
            database.withTransaction {
                val dao = database.experienceDao()
                val hrt = database.hrtDao()
                dao.deleteEverything()
                hrt.clearPlans(); hrt.clearMovements(); hrt.clearStock(); hrt.clearLabs(); hrt.clearSettings()
                dao.insertEverythingWithProgress(backup, attachments.paths, progress)
                backup.hrt.plans.forEach { hrt.save(it) }
                backup.hrt.stock.forEach { hrt.save(it) }
                backup.hrt.movements.forEach { hrt.save(it) }
                backup.hrt.labs.forEach { hrt.save(it) }
                backup.hrt.settings.forEach { hrt.save(it) }
            }
        } finally {
            // Cancellation can arrive immediately after commit: consult committed rows before deleting files.
            val attachments = prepared
            if (attachments != null) withContext(NonCancellable + Dispatchers.IO) {
                try {
                    val referenced = database.experienceDao().allPhotoPaths().any { it.startsWith(attachments.directory.absolutePath + File.separator) }
                    if (!referenced) attachments.discard()
                } catch (exception: Exception) { Log.e("JournalImport", "Cannot clean prepared attachments; retained for startup reconciliation", exception) }
            }
        }
    }

    companion object {
        fun cleanupUnreferenced(context: Context, database: AppDatabase) {
            val root = File(context.filesDir, "images")
            val referenced = database.openHelper.readableDatabase.query("SELECT filePath, thumbnailPath FROM TimedNotePhoto").use { cursor ->
                buildList { while (cursor.moveToNext()) { add(cursor.getString(0)); if (!cursor.isNull(1)) add(cursor.getString(1)) } }
            }
            root.listFiles().orEmpty().filter { it.isDirectory && it.name.matches(Regex("import-[a-fA-F0-9-]+")) }.forEach { directory ->
                if (referenced.none { it.startsWith(directory.absolutePath + File.separator) }) PreparedAttachments(directory, emptyMap()).discard()
            }
        }
    }
}
