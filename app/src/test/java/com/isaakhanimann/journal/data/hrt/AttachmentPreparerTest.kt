package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.ui.tabs.settings.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.Base64

class AttachmentPreparerTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun backup(payloads: List<String>): JournalExport {
        val time = Instant.ofEpochSecond(123)
        val photos = payloads.map { TimedNotePhotoSerializable(imageBase64 = it, creationDate = time, originalFileName = "../../outside.jpg") }
        val note = TimedNoteSerializable(time, time, "note", null, isPartOfTimeline = true, photos = photos)
        return JournalExport(experiences = listOf(ExperienceSerializable("title", "text", sortDate = time, timedNotes = listOf(note))))
    }
    private fun payload(text: String) = Base64.getEncoder().encodeToString(text.toByteArray())

    @Test fun duplicatePhotoIdsReceiveIndependentSafeFiles() {
        val images = temporary.newFolder("images")
        val prepared = AttachmentPreparer(images) { "png" }.prepare(backup(listOf(payload("one"), payload("two"))))
        assertEquals(setOf("0:0:0", "0:0:1"), prepared.paths.keys)
        assertEquals(2, prepared.paths.values.toSet().size)
        assertEquals(listOf("one", "two"), prepared.paths.values.map { File(it).readText() })
        prepared.paths.values.forEach { assertTrue(File(it).canonicalPath.startsWith(images.canonicalPath + File.separator)) }
    }

    @Test fun corruptLaterPhotoCleansEarlierPreparedFiles() {
        val images = temporary.newFolder("images")
        try { AttachmentPreparer(images) { "png" }.prepare(backup(listOf(payload("one"), "not-base64!"))); fail("Corrupt data accepted") }
        catch (_: IllegalArgumentException) { }
        assertTrue(images.listFiles().orEmpty().isEmpty())
    }

    @Test fun writePreparationErrorDoesNotTouchExistingAttachments() {
        val images = temporary.newFolder("images")
        val old = File(images, "old.jpg").also { it.writeText("old") }
        try { AttachmentPreparer(images) { throw IOException("injected decoder failure") }.prepare(backup(listOf(payload("one")))); fail("Expected failure") }
        catch (_: IOException) { }
        assertEquals("old", old.readText())
        assertEquals(listOf(old), images.listFiles().orEmpty().toList())
    }

    @Test fun base64LineBreaksAreAcceptedWithoutReencodingImageBytes() {
        val images = temporary.newFolder("images")
        val original = byteArrayOf(0, 1, 2, 3, 4, 5)
        val encoded = Base64.getEncoder().encodeToString(original).chunked(4).joinToString("\n")
        val prepared = AttachmentPreparer(images) { "png" }.prepare(backup(listOf(encoded)))
        assertArrayEquals(original, File(prepared.paths.values.single()).readBytes())
    }
}
