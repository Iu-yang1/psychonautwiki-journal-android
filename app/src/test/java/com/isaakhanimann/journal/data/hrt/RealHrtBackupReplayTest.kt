package com.isaakhanimann.journal.data.hrt

import android.app.Application
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.di.AppModule
import com.isaakhanimann.journal.ui.tabs.settings.JournalExport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.File
import java.security.MessageDigest

/** Optional local replay. The user's backup stays outside the repository and is never rewritten. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class RealHrtBackupReplayTest {
    @Test fun completeBackupKeepsRowsRecognizesActualRoutesAndProducesGelScenarioCurves() = runBlocking {
        val path = System.getenv("JOURNAL_REPLAY_BACKUP")
        assumeTrue("Private replay fixture not provided", !path.isNullOrBlank())
        val file = File(requireNotNull(path))
        val originalBytes = file.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(originalBytes)
        val backup = Json { ignoreUnknownKeys = true }.decodeFromString<JournalExport>(originalBytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("experiences_db")
        val database = AppModule.provideAppDatabase(context)
        try {
            JournalImportService(context, database).replace(backup) { _, _ -> }
            val substances = SubstanceRepository(context, SubstanceParser())
            val repository = HrtRepository(database, substances)
            val before = database.experienceDao().getIngestionsSortedDescendingFlow().first()
            val beforeDetails = database.hrtDao().details().first()
            val beforeSettings = database.hrtDao().exportSettings()
            repository.identify(before, emptySet())
            val after = database.experienceDao().getIngestionsSortedDescendingFlow().first()
            val links = database.hrtDao().links().first()
            val actualValerate = before.filter { it.substanceName == "Oral Estradiol Valerate" && it.administrationRoute.name == "SUBLINGUAL" }
            assertTrue(actualValerate.isNotEmpty())
            actualValerate.forEach { record -> assertEquals("identified", links.single { it.ingestionId == record.id }.status) }
            assertEquals(before, after)
            assertEquals(beforeDetails, database.hrtDao().details().first())
            assertEquals(beforeSettings, database.hrtDao().exportSettings())
            val now = before.maxOf { it.time.toEpochMilli() } + 3600000
            val service = HormoneChartService(HormoneReferenceRepository(context).catalog)
            val data = HormoneJournalData(after, links, beforeDetails, beforeSettings, emptyList(), substances = substances.getAllSubstances())
            val chart = service.calculate(data, HormoneChartRequest(historyDays = 180, forecastDays = 0), now)
            val gelIds = before.filter { it.substanceName == "Estradiol Gel" }.map { it.id.toString() }.toSet()
            assertTrue(gelIds.isNotEmpty())
            assertEquals(2, chart.gelScenarios.size)
            chart.gelScenarios.forEach { scenario ->
                assertEquals(gelIds, scenario.recordIds.toSet())
                assertTrue(scenario.history.any { it.pgMl > 0 })
                assertTrue(scenario.history.all { it.pgMl.isFinite() && it.pgMl >= 0 })
            }
            assertTrue(chart.excluded.none { it.reason == "needs_identity" })
            assertEquals(actualValerate.map { it.id.toString() }.toSet(), chart.extrapolatedRecordIds)
            assertTrue(chart.history.any { it.pgMl > 0 })
            assertEquals(actualValerate.size, chart.recognizedHrt.count { it.name == "Oral Estradiol Valerate" })
            val week = service.calculate(data, HormoneChartRequest(historyDays = 7, forecastDays = 0), now)
            assertEquals(chart.recognizedHrt.size, week.recognizedHrt.size)
            val unbridged = service.calculate(data, HormoneChartRequest(historyDays = 180, forecastDays = 0, bridgeSublingualValerate = false), now)
            assertTrue(unbridged.extrapolatedRecordIds.isEmpty())
            assertTrue(actualValerate.none { it.id.toString() in unbridged.includedActual })
            assertEquals(before, database.experienceDao().getIngestionsSortedDescendingFlow().first())
            System.getenv("JOURNAL_REPLAY_OUTPUT")?.let { path ->
                val output = File(path)
                require(output.canonicalFile != file.canonicalFile)
                val result = buildJsonObject {
                    put("fromHours", week.fromHours); put("nowHours", week.nowHours)
                    put("recognized", chart.recognizedHrt.size); put("bridgeRecords", chart.extrapolatedRecordIds.size); put("gelRecords", gelIds.size)
                    put("scenarios", buildJsonArray { week.gelScenarios.forEach { scenario -> add(buildJsonObject {
                        put("label", scenario.label)
                        put("samples", buildJsonArray { scenario.history.forEach { point -> add(buildJsonArray { add(point.timeHours); add(point.pgMl) }) } })
                    }) } })
                    put("doses", buildJsonArray { week.doseRecords.forEach { record -> add(buildJsonObject {
                        put("name", record.name); put("time", record.time); put("dose", record.dose ?: 0.0); put("unit", record.unit.orEmpty()); put("route", record.route)
                    }) } })
                }
                output.parentFile.mkdirs(); output.writeText(result.toString(), Charsets.UTF_8)
            }
            println("PRIVATE_REPLAY: ${after.size} original events preserved, ${chart.recognizedHrt.size} HRT events recognized, ${actualValerate.size} sublingual EV identified, ${gelIds.size} active-mg gel records yield two research scenarios")
        } finally { database.close(); context.deleteDatabase("experiences_db") }
        assertArrayEquals(digest, MessageDigest.getInstance("SHA-256").digest(file.readBytes()))
    }
}
