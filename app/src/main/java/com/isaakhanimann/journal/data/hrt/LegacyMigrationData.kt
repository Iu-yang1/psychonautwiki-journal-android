package com.isaakhanimann.journal.data.hrt

import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject

/** Preserve columns and reminder formats that have no unambiguous modern equivalent. */
object LegacyMigrationData {
    private fun exists(db: SupportSQLiteDatabase, table: String): Boolean = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }

    fun rememberRows(db: SupportSQLiteDatabase, table: String, kind: String) {
        require(table in setOf("RecipeSubcomponent", "IngestionReminder"))
        if (!exists(db, table)) return
        db.execSQL("CREATE TABLE IF NOT EXISTS legacy_migration_rows (`key` TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
        db.query("SELECT * FROM `$table`").use { rows ->
            while (rows.moveToNext()) {
                val record = JSONObject()
                rows.columnNames.forEachIndexed { index, name ->
                    val value: Any = when (rows.getType(index)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                        android.database.Cursor.FIELD_TYPE_INTEGER -> rows.getLong(index)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> rows.getDouble(index)
                        else -> rows.getString(index)
                    }
                    record.put(name, value)
                }
                val key = "$kind:${record.getInt("id")}"
                val payload = JSONObject().put("sourceTable", table).put("record", record).toString()
                db.execSQL("INSERT OR IGNORE INTO legacy_migration_rows (`key`,payload) VALUES (?,?)", arrayOf(key, payload))
            }
        }
    }

    fun publish(db: SupportSQLiteDatabase) {
        rememberRows(db, "IngestionReminder", "reminder")
        if (exists(db, "legacy_migration_rows")) db.execSQL("INSERT OR IGNORE INTO hrt_model_settings (id,payload) SELECT 'legacy:' || `key`,payload FROM legacy_migration_rows")
    }
}
