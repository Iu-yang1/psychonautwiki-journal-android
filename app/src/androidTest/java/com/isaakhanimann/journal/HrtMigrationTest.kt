package com.isaakhanimann.journal

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.isaakhanimann.journal.data.room.AppDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HrtMigrationTest {
    @Test fun everyReleasedSchemaPreservesIngestion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (version in 1..18) {
            val name = "hrt-migration-test-$version"
            context.deleteDatabase(name)
            val schema = JSONObject(instrumentation.context.assets.open("com.isaakhanimann.journal.data.room.AppDatabase/$version.json").bufferedReader().use { it.readText() }).getJSONObject("database")
            val file = context.getDatabasePath(name)
            file.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    entity.optJSONArray("indices")?.let { indices ->
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val values = ContentValues()
                db.rawQuery("PRAGMA table_info(Ingestion)", null).use { c ->
                    while (c.moveToNext()) {
                        val column = c.getString(1)
                        when {
                            column == "id" -> values.put(column, 42)
                            column == "substanceName" -> values.put(column, "Oral Estradiol")
                            column == "administrationRoute" -> values.put(column, "ORAL")
                            column == "dose" -> values.put(column, 2.5)
                            column == "time" -> values.put(column, 123456789L)
                            c.getInt(3) == 1 -> if (c.getString(2).contains("TEXT")) values.put(column, "") else values.put(column, 0)
                        }
                    }
                }
                assertTrue(db.insertOrThrow("Ingestion", null, values) > 0)
                db.version = version
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(
                AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21
            ).build()
            try {
                room.openHelper.writableDatabase.query("SELECT id, substanceName, dose, time FROM Ingestion").use { c ->
                    assertTrue("v$version", c.moveToFirst())
                    assertEquals(42, c.getInt(0)); assertEquals("Oral Estradiol", c.getString(1))
                    assertEquals(2.5, c.getDouble(2), 0.0); assertEquals(123456789L, c.getLong(3))
                    assertFalse(c.moveToNext())
                }
            } finally { room.close(); context.deleteDatabase(name) }
        }
    }
}
