package com.isaakhanimann.journal.data.hrt

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.isaakhanimann.journal.data.room.AppDatabase
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.room.experiences.entities.*
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.di.AppModule
import com.isaakhanimann.journal.ui.tabs.settings.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowBitmapFactory
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class RoomRecoveryIntegrationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var database: AppDatabase

    @Before fun open() {
        ShadowBitmapFactory.setAllowInvalidImageData(false)
        context.deleteDatabase("experiences_db")
        database = AppModule.provideAppDatabase(context)
    }
    @After fun close() { database.close(); context.deleteDatabase("experiences_db") }

    private suspend fun seed(): Ingestion {
        val time = Instant.ofEpochSecond(123456L)
        val experience = database.experienceDao().insert(Experience(0, "old", "old text", time, time, location = null)).toInt()
        val event = Ingestion(substanceName = "Oral Estradiol", time = time, creationDate = time, administrationRoute = AdministrationRoute.ORAL,
            dose = 2.0, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = "mg", experienceId = experience,
            notes = "old note", stomachFullness = null, consumerName = "person B", customUnitId = null)
        return event.copy(id = database.experienceDao().insert(event).toInt())
    }

    private fun photoBackup(bytes: ByteArray): JournalExport {
        val time = Instant.ofEpochMilli(1000)
        val photo = TimedNotePhotoSerializable(imageBase64 = Base64.getEncoder().encodeToString(bytes), creationDate = time, originalFileName = "../../escape.png")
        val note = TimedNoteSerializable(time, time, "restored", null, isPartOfTimeline = true, photos = listOf(photo))
        return JournalExport(experiences = listOf(ExperienceSerializable("new", "new text", sortDate = time, timedNotes = listOf(note))))
    }

    private fun png(): ByteArray = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAEUlEQVR4nGP4z8DwH4QZYAwAR8oH+WdZbrcAAAAASUVORK5CYII=")

    @Test fun invalidImageDoesNotReplaceExistingDatabase() = runBlocking {
        val old = seed()
        try { JournalImportService(context, database).replace(photoBackup("invalid image".toByteArray())) { _, _ -> }; fail("Invalid image accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals(old, database.hrtDao().event(old.id))
        assertTrue(File(context.filesDir, "images").listFiles().orEmpty().none { it.name.startsWith("import-") })
    }

    @Test fun transactionFailureRestoresOldRowsAndCleansPreparedPhotos() = runBlocking {
        val old = seed()
        try {
            JournalImportService(context, database).replace(photoBackup(png())) { _, message ->
                if (message.startsWith("Importing experience")) throw IOException("Injected failure after delete")
            }
            fail("Expected injected transaction failure")
        } catch (_: IOException) { }
        assertEquals(old, database.hrtDao().event(old.id))
        assertTrue(database.experienceDao().allPhotoPaths().isEmpty())
        assertTrue(File(context.filesDir, "images").listFiles().orEmpty().none { it.name.startsWith("import-") })
    }

    @Test fun committedPhotoKeepsOriginalBytesAndIgnoresExternalFilename() = runBlocking {
        val old = seed(); val bytes = png()
        JournalImportService(context, database).replace(photoBackup(bytes)) { _, _ -> }
        assertNull(database.hrtDao().event(old.id))
        val path = database.experienceDao().allPhotoPaths().single()
        assertTrue(File(path).canonicalPath.startsWith(File(context.filesDir, "images").canonicalPath + File.separator))
        assertArrayEquals(bytes, File(path).readBytes())
        JournalImportService.cleanupUnreferenced(context, database)
        assertTrue(File(path).exists())
    }

    @Test fun cancelledImportDoesNotClearOldRecordsOrRetainPreparedPhotos() = runBlocking {
        val old = seed()
        try {
            JournalImportService(context, database).replace(photoBackup(png())) { _, message ->
                if (message.startsWith("Importing experience")) throw kotlinx.coroutines.CancellationException("cancelled by caller")
            }
            fail("Expected cancellation")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertEquals(old, database.hrtDao().event(old.id))
        assertTrue(File(context.filesDir, "images").listFiles().orEmpty().none { it.name.startsWith("import-") })
    }

    @Test fun duplicatePlanActionAndDoseEditsKeepStockConsistent() = runBlocking {
        val substances = SubstanceRepository(context, SubstanceParser())
        val repo = HrtRepository(database, substances)
        val medication = requireNotNull(substances.getSubstance("Oral Estradiol"))
        val product = MedicationProduct("p", medication.id, "tablet", "tablet", "tablet", 2.0)
        val stock = HrtStock("s", medication.id, "batch", "tablet")
        repo.saveProduct(product); repo.saveStock(stock, product, 10.0)
        val plan = HrtPlan("plan", medication.id, medication.name, null, "ORAL", 4.0, "mg", System.currentTimeMillis() - 3600000, 24)
        repo.savePlan(plan, PlanSchedule(stockId = stock.id))
        repo.take(plan, plan.start)
        try { repo.take(plan, plan.start); fail("Duplicate intake accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(8.0, InventoryLedger.balance(database.hrtDao().exportMovements()), 0.0)
        val event = database.experienceDao().getIngestionsSortedDescendingFlow().first().single()
        repo.saveProduct(product.copy(strengthMgPerUnit = 3.0))
        val experiences = ExperienceRepository(database.experienceDao(), repo)
        experiences.update(event.copy(dose = 6.0))
        assertEquals(7.0, InventoryLedger.balance(database.hrtDao().exportMovements()), 0.0)
        experiences.delete(event)
        assertEquals(10.0, InventoryLedger.balance(database.hrtDao().exportMovements()), 0.0)
    }

    @Test fun batchUndoRefusesToOverwriteLaterSupplementalEdits() = runBlocking {
        val first = seed()
        val second = first.copy(id = database.experienceDao().insert(first.copy(id = 0)).toInt())
        val repo = HrtRepository(database, SubstanceRepository(context, SubstanceParser()))
        repo.identify(listOf(first, second), emptySet())
        val before = database.hrtDao().link(first.id)
        val batch = repo.associateBatch(listOf(first, second), requireNotNull(before?.medicationId))
        database.hrtDao().save(HrtEventDetail(second.id, site = "later edit"))
        try { repo.undoAssociationBatch(batch); fail("Later edit overwritten") } catch (_: IllegalArgumentException) { }
        assertEquals("manual", database.hrtDao().link(first.id)?.origin)
        database.hrtDao().deleteDetail(second.id)
        repo.undoAssociationBatch(batch)
        assertEquals(before, database.hrtDao().link(first.id))
        assertEquals(first, database.hrtDao().event(first.id))
    }

    @Test fun staleBatchMemberRollsBackEarlierAssociationWrites() = runBlocking {
        val first = seed()
        val second = first.copy(id = database.experienceDao().insert(first.copy(id = 0)).toInt())
        val repo = HrtRepository(database, SubstanceRepository(context, SubstanceParser()))
        repo.identify(listOf(first, second), emptySet())
        val before = database.hrtDao().link(first.id)
        database.experienceDao().update(second.copy(notes = "changed after preview"))
        try { repo.associateBatch(listOf(first, second), requireNotNull(before?.medicationId)); fail("Stale batch accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(before, database.hrtDao().link(first.id))
        assertTrue(database.hrtDao().exportSettings().none { it.id.startsWith("link-batch:") })
    }

    @Test fun recipeCustomUnitQuantityAndCreationDateSurviveImport() = runBlocking {
        val time = Instant.ofEpochSecond(1234)
        val unit = CustomUnitSerializable(99, "Oral Estradiol", "tablet", administrationRoute = AdministrationRoute.ORAL, dose = 2.0,
            isArchived = false, unit = "tablet", unitPlural = null, originalUnit = "mg", note = "")
        val component = RecipeSubcomponentSerializable(88, null, customUnitId = 99, customUnitDose = 1.5, dose = null,
            estimatedDoseStandardDeviation = null, isEstimate = false, originalUnit = "mg", creationDate = time)
        val recipe = CustomRecipeSerializable(77, "recipe", administrationRoute = AdministrationRoute.ORAL, isArchived = false,
            unit = "serving", unitPlural = null, note = "", subcomponents = listOf(component))
        JournalImportService(context, database).replace(JournalExport(formatVersion = 2, customUnits = listOf(unit), customRecipes = listOf(recipe))) { _, _ -> }
        val restored = database.experienceDao().getAllCustomRecipesWithSubcomponentsSorted().single().subcomponents.single()
        assertEquals(1.5, restored.customUnitDose!!, 0.0)
        assertEquals(time, restored.creationDate)
        val repo = HrtRepository(database, SubstanceRepository(context, SubstanceParser()))
        assertEquals(3.0, restored.getActualDose(ExperienceRepository(database.experienceDao(), repo))!!, 0.0)
    }

    @Test fun realV18SnapshotCanRestoreAfterUpgradeAndMigrateAgain() {
        database.close(); context.deleteDatabase("experiences_db")
        val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream("com.isaakhanimann.journal.data.room.AppDatabase/18.json")).bufferedReader().use { it.readText() }).getJSONObject("database")
        val path = context.getDatabasePath("experiences_db").also { it.parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                entity.optJSONArray("indices")?.let { indices -> for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)) }
            }
            db.execSQL("INSERT INTO Ingestion (id,substanceName,time,administrationRoute,dose,isDoseAnEstimate,experienceId) VALUES(42,'Oral Estradiol',123,'ORAL',2.5,0,1)")
            db.version = 18
        }
        val image = File(context.filesDir, "images/old.jpg").also { it.parentFile!!.mkdirs(); it.writeText("old image") }
        UpgradeSnapshot.prepare(context)
        val archive = DatabaseRecovery.archive(context)
        val snapshot = archive.list().snapshots.single { it.version == 18 && it.reason == "upgrade" }
        database = AppModule.provideAppDatabase(context)
        database.openHelper.writableDatabase.execSQL("UPDATE Ingestion SET dose=9 WHERE id=42")
        image.writeText("new image")
        database.close()
        archive.request(snapshot.id); archive.applyPending()
        assertEquals("old image", image.readText())
        database = AppModule.provideAppDatabase(context)
        database.openHelper.writableDatabase.query("SELECT dose FROM Ingestion WHERE id=42").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(2.5, cursor.getDouble(0), 0.0) }
        assertTrue(archive.list().snapshots.any { it.reason == "before-restore" })
    }

    @Test fun allHistoricalSchemasMigrateWithoutLosingIngestion() {
        database.close()
        for (version in 1..18) {
            val before = linkedMapOf<String, Map<String, String?>>()
            context.deleteDatabase("experiences_db")
            val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream("com.isaakhanimann.journal.data.room.AppDatabase/$version.json")).bufferedReader().use { it.readText() }).getJSONObject("database")
            val path = context.getDatabasePath("experiences_db").also { it.parentFile!!.mkdirs() }
            SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index); val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    entity.optJSONArray("indices")?.let { indices ->
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val values = ContentValues()
                db.rawQuery("PRAGMA table_info(Ingestion)", null).use { cursor -> while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    when {
                        name == "id" -> values.put(name, 42)
                        name == "substanceName" -> values.put(name, "Oral Estradiol")
                        name == "administrationRoute" -> values.put(name, "ORAL")
                        name == "dose" -> values.put(name, 2.5)
                        name == "time" -> values.put(name, 123456789L)
                        cursor.getInt(3) == 1 -> if (cursor.getString(2).contains("TEXT")) values.put(name, "") else values.put(name, 0)
                    }
                } }
                db.insertOrThrow("Ingestion", null, values); db.version = version
                val ids = linkedMapOf("Experience" to 17, "CustomUnit" to 23, "CustomRecipe" to 31, "TimedNote" to 41,
                    "RecipeSubcomponent" to 53, "TimedNotePhoto" to 61, "IngestionReminder" to 71, "ingestion_reminders" to 71,
                    "CustomSubstance" to 73, "ShulginRating" to 77, "SubstanceCompanion" to 0)
                ids.forEach { (table, id) ->
                    val exists = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }
                    if (exists) {
                        val row = ContentValues()
                        db.rawQuery("PRAGMA table_info(`$table`)", null).use { columns -> while (columns.moveToNext()) {
                            val name = columns.getString(1)
                            when {
                                name == "id" -> row.put(name, id)
                                name == "experienceId" -> row.put(name, 17)
                                name == "recipeId" -> row.put(name, 31)
                                name == "timedNoteId" -> row.put(name, 41)
                                name == "customUnitId" -> row.put(name, 23)
                                name == "customUnitDose" -> row.put(name, 1.25)
                                name == "substanceName" -> row.put(name, "Oral Estradiol")
                                name == "administrationRoute" -> row.put(name, "ORAL")
                                name == "repeatMode" -> row.put(name, "DAILY")
                                name == "option" -> row.put(name, ShulginRatingOption.entries.first().name)
                                name == "color" -> row.put(name, "BLUE")
                                name == "customColor" -> row.put(name, 123456789)
                                name == "reminderTime" -> if (columns.getString(2) == "TEXT") row.put(name, "08:00") else row.put(name, 123456789L)
                                name in setOf("unit", "units", "originalUnit") -> row.put(name, "mg")
                                name == "dose" -> row.put(name, 2.5)
                                name == "note" -> row.put(name, "保留原始备注")
                                name == "filePath" -> row.put(name, "legacy-photo.jpg")
                                columns.getInt(3) == 1 -> if (columns.getString(2).contains("TEXT")) row.put(name, "legacy-$name") else row.put(name, 0)
                            }
                        } }
                        db.insertOrThrow(table, null, row)
                        val where = if (table == "SubstanceCompanion") "substanceName='Oral Estradiol'" else "id=$id"
                        before[table] = db.rawQuery("SELECT * FROM `$table` WHERE $where", null).use { cursor ->
                            check(cursor.moveToFirst()); cursor.columnNames.withIndex().associate { it.value to if (cursor.isNull(it.index)) null else cursor.getString(it.index) }
                        }
                    }
                }
            }
            database = AppModule.provideAppDatabase(context)
            database.openHelper.writableDatabase.query("SELECT id,substanceName,dose,time FROM Ingestion").use { cursor ->
                assertTrue("v$version", cursor.moveToFirst()); assertEquals(42, cursor.getInt(0)); assertEquals("Oral Estradiol", cursor.getString(1))
                assertEquals(2.5, cursor.getDouble(2), 0.0); assertEquals(123456789L, cursor.getLong(3)); assertFalse(cursor.moveToNext())
            }
            before.forEach { (table, row) ->
                val where = if (table == "SubstanceCompanion") "substanceName='Oral Estradiol'" else "id=${row.getValue("id")}"
                database.openHelper.writableDatabase.query("SELECT * FROM `$table` WHERE $where").use { cursor ->
                    assertTrue("v$version $table", cursor.moveToFirst())
                    row.filterKeys { it in cursor.columnNames }.forEach { (column, expected) ->
                        val index = cursor.getColumnIndexOrThrow(column)
                        assertEquals("v$version $table.$column", expected, if (cursor.isNull(index)) null else cursor.getString(index))
                    }
                }
            }
            if (version == 15) database.openHelper.writableDatabase.query("SELECT payload FROM hrt_model_settings WHERE id='legacy:reminder:71'").use { cursor ->
                assertTrue(cursor.moveToFirst()); assertTrue(cursor.getString(0).contains("123456789"))
            }
            if (version == 12) database.openHelper.writableDatabase.query("SELECT payload FROM hrt_model_settings WHERE id='legacy:recipe-v12:53'").use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals("mg", JSONObject(cursor.getString(0)).getJSONObject("record").getString("unit"))
            }
            database.close()
        }
    }
}
