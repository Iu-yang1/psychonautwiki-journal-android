package com.isaakhanimann.journal.data.hrt

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** Runs before Room opens the database; never modifies the source schema. */
object UpgradeSnapshot {
    @Synchronized fun prepare(context: Context) {
        val source = context.getDatabasePath("experiences_db")
        if (!source.exists()) return
        val version = SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        if (version >= 21) return
        val archive = DatabaseRecovery.archive(context)
        SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "Database is busy; retry upgrade" }
            }
        }
        val existing = archive.list().snapshots.filter { it.version == version && it.reason == "upgrade" }
        for (snapshot in existing) {
            try { if (archive.matchesCurrent(snapshot.id)) return }
            catch (exception: Exception) { android.util.Log.w("UpgradeSnapshot", "Existing snapshot failed verification; preserving it and creating a new snapshot", exception) }
        }
        archive.capture(version, "upgrade")
    }
}
