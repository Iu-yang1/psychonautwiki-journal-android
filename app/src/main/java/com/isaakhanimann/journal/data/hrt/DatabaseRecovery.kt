package com.isaakhanimann.journal.data.hrt

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

object DatabaseRecovery {
    @Volatile var bootFailure: String? = null
        private set

    private fun targets(context: Context): Map<String, File> {
        val db = context.getDatabasePath("experiences_db")
        return linkedMapOf("db" to db, "wal" to File(db.path + "-wal"), "shm" to File(db.path + "-shm"),
            "journal" to File(db.path + "-journal"), "files" to context.filesDir,
            "prefs" to File(context.applicationInfo.dataDir, "shared_prefs"))
    }

    fun archive(context: Context) = RecoveryArchive(File(context.noBackupFilesDir, "recovery"), targets(context))

    fun beforeApplicationStarts(context: Context) {
        try { archive(context).applyPending(); bootFailure = null }
        catch (exception: Exception) {
            bootFailure = exception.message ?: "恢复未完成"
            Log.e("DatabaseRecovery", "Recovery before application startup failed", exception)
        }
    }

    fun retryExisting(context: Context) {
        val archive = archive(context)
        check(!archive.hasPendingWork()) { "恢复事务尚未完成，请关闭后重新打开应用" }
        archive.clearFailure()
        bootFailure = null
    }

    fun importLegacySnapshots(context: Context) {
        val archive = archive(context)
        context.noBackupFilesDir.listFiles().orEmpty().filter { it.isDirectory && it.name.matches(Regex("upgrade-v[0-9]+-to-21")) }.forEach { folder ->
            val marker = File(folder, "converted-to")
            if (!File(folder, "complete").exists() || marker.exists()) return@forEach
            try {
                val db = File(folder, "experiences_db")
                val version = SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
                val sources = linkedMapOf("db" to db, "wal" to File(db.path + "-wal"), "shm" to File(db.path + "-shm"),
                    "journal" to File(db.path + "-journal"), "files" to File(folder, "files"), "prefs" to File(folder, "shared_prefs"))
                val converted = archive.captureFrom(sources, version, "legacy-upgrade")
                marker.writeText(converted.id, Charsets.UTF_8)
            } catch (exception: Exception) { Log.e("DatabaseRecovery", "Cannot verify legacy snapshot ${folder.name}", exception) }
        }
    }
}
