package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class RecoveryArchiveTest {
    @get:Rule val folder = TemporaryFolder()
    private fun targets(): Map<String, File> {
        val live = folder.newFolder()
        return linkedMapOf("db" to File(live, "experiences_db"), "files" to File(live, "files"), "prefs" to File(live, "prefs"))
    }
    private fun state(targets: Map<String, File>, value: String) {
        targets.getValue("db").writeText(value)
        val photos = File(targets.getValue("files"), "images").also { it.mkdirs() }
        File(photos, "photo.jpg").writeText("photo-$value")
        targets.getValue("prefs").mkdirs()
        File(targets.getValue("prefs"), "settings.xml").writeText("prefs-$value")
    }
    private fun assertState(targets: Map<String, File>, value: String) {
        assertEquals(value, targets.getValue("db").readText())
        assertEquals("photo-$value", File(targets.getValue("files"), "images/photo.jpg").readText())
        assertEquals("prefs-$value", File(targets.getValue("prefs"), "settings.xml").readText())
    }

    @Test fun restoresDatabaseAttachmentsAndPreferencesTogether() {
        val targets = targets(); val archive = RecoveryArchive(folder.newFolder(), targets)
        state(targets, "old"); val snapshot = archive.capture(18, "upgrade")
        state(targets, "new"); archive.request(snapshot.id); archive.applyPending()
        assertState(targets, "old")
        assertTrue(archive.list().snapshots.any { it.reason == "before-restore" })
        assertFalse(archive.hasPendingWork())
        assertNull(archive.failureMessage())
    }

    @Test fun corruptionFailsBeforeAnyTargetChanges() {
        val targets = targets(); val root = folder.newFolder(); val archive = RecoveryArchive(root, targets)
        state(targets, "old"); val snapshot = archive.capture(18, "upgrade")
        state(targets, "new"); archive.request(snapshot.id)
        File(root, "${snapshot.id}/data/db").appendText("corruption")
        try { archive.applyPending(); fail("Corrupt snapshot accepted") } catch (_: IllegalArgumentException) { }
        assertState(targets, "new")
        assertNotNull(archive.failureMessage())
    }

    @Test fun moveFailureRollsBackAlreadyReplacedDatabase() {
        val targets = targets(); val root = folder.newFolder()
        state(targets, "old"); val snapshot = RecoveryArchive(root, targets).capture(18, "upgrade")
        state(targets, "new")
        val archive = RecoveryArchive(root, targets) { key -> if (key == "files") throw IOException("injected move failure") }
        archive.request(snapshot.id)
        try { archive.applyPending(); fail("Expected injected failure") } catch (_: IOException) { }
        assertState(targets, "new")
        assertFalse(archive.hasPendingWork())
    }

    @Test fun interruptedProcessRollsBackOnNextStartup() {
        val targets = targets(); val root = folder.newFolder()
        state(targets, "old"); val snapshot = RecoveryArchive(root, targets).capture(18, "upgrade")
        state(targets, "new")
        class ProcessDeath : Error()
        val interrupted = RecoveryArchive(root, targets) { key -> if (key == "files") throw ProcessDeath() }
        interrupted.request(snapshot.id)
        try { interrupted.applyPending(); fail("Expected interruption") } catch (_: ProcessDeath) { }
        assertEquals("old", targets.getValue("db").readText())
        val restarted = RecoveryArchive(root, targets)
        restarted.applyPending()
        assertState(targets, "new")
        assertNotNull(restarted.failureMessage())
        assertFalse(restarted.hasPendingWork())
    }

    @Test fun malformedSnapshotDoesNotHideOtherValidSnapshots() {
        val targets = targets(); val root = folder.newFolder(); val archive = RecoveryArchive(root, targets)
        state(targets, "old"); val snapshot = archive.capture(18, "upgrade")
        val broken = File(root, "snapshot-bad").also { it.mkdirs() }
        File(broken, "manifest.json").writeText("invalid")
        assertEquals(listOf(snapshot.id), archive.list().snapshots.map { it.id })
        assertEquals(setOf("snapshot-bad"), archive.list().invalid.keys)
    }

    @Test fun sameDatabaseVersionDoesNotReuseStaleSnapshotContents() {
        val targets = targets(); val archive = RecoveryArchive(folder.newFolder(), targets)
        state(targets, "old"); val snapshot = archive.capture(18, "upgrade")
        assertTrue(archive.matchesCurrent(snapshot.id))
        state(targets, "new")
        assertFalse(archive.matchesCurrent(snapshot.id))
    }

    @Test(expected = IllegalArgumentException::class) fun pathTraversalIsRejected() {
        RecoveryArchive(folder.newFolder(), targets()).request("../experiences_db")
    }
}
