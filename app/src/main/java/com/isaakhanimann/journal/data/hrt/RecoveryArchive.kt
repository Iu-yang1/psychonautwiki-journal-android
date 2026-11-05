package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

@Serializable
data class RecoveryFile(val size: Long, val sha256: String)
@Serializable
data class RecoveryManifest(val id: String, val version: Int, val createdAt: Long, val reason: String, val roots: Set<String>, val files: Map<String, RecoveryFile>)
data class RecoveryListing(val snapshots: List<RecoveryManifest>, val invalid: Map<String, String>)
@Serializable
private data class RestoreRequest(val snapshot: String, val nonce: String)
@Serializable
private data class RestoreJournal(val nonce: String, val originalRoots: Set<String>)

/** Filesystem swaps run only before application data stores or Room are opened. */
class RecoveryArchive(
    private val directory: File,
    private val targets: Map<String, File>,
    private val beforeMove: (String) -> Unit = {},
) {
    private val pending get() = File(directory, "pending.json")
    private val transaction get() = File(directory, "restore-transaction")
    private val failure get() = File(directory, "failure.txt")

    init {
        require(targets.keys.all { it.matches(Regex("[a-z]+")) })
        check(directory.isDirectory || directory.mkdirs()) { "无法创建恢复目录" }
    }

    fun capture(version: Int, reason: String): RecoveryManifest = captureFrom(targets, version, reason)

    fun captureFrom(sources: Map<String, File>, version: Int, reason: String): RecoveryManifest {
        require(sources.keys == targets.keys)
        val id = "snapshot-${UUID.randomUUID()}"
        val staging = File(directory, "preparing-${UUID.randomUUID()}")
        try {
            check(staging.mkdirs())
            val payload = File(staging, "data").also { check(it.mkdirs()) }
            val present = sources.filterValues { it.exists() }.keys
            present.forEach { key -> copy(sources.getValue(key), File(payload, key)) }
            val manifest = RecoveryManifest(id, version, System.currentTimeMillis(), reason, present, hashes(payload))
            write(File(staging, "manifest.json"), Json.encodeToString(manifest))
            check(staging.renameTo(File(directory, id))) { "无法完成恢复快照" }
            return manifest
        } finally { removeOwned(staging) }
    }

    fun list(): RecoveryListing {
        val invalid = linkedMapOf<String, String>()
        val snapshots = directory.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("snapshot-") }.mapNotNull { folder ->
            try {
                Json.decodeFromString<RecoveryManifest>(File(folder, "manifest.json").readText()).also {
                    require(it.id == folder.name && targets.keys.containsAll(it.roots))
                }
            } catch (exception: Exception) { invalid[folder.name] = exception.message ?: "无效的快照清单"; null }
        }
        return RecoveryListing(snapshots.sortedByDescending { it.createdAt }, invalid)
    }

    fun verify(id: String): RecoveryManifest {
        val folder = snapshot(id)
        val manifest = Json.decodeFromString<RecoveryManifest>(File(folder, "manifest.json").readText())
        require(manifest.id == id && targets.keys.containsAll(manifest.roots)) { "快照清单无效" }
        require(hashes(File(folder, "data")) == manifest.files) { "快照文件校验失败" }
        require(File(folder, "data").listFiles().orEmpty().map { it.name }.toSet() == manifest.roots) { "快照根目录不一致" }
        return manifest
    }

    fun request(id: String) {
        check(!transaction.exists()) { "之前的恢复事务尚未完成，请关闭应用后重试" }
        verify(id)
        write(pending, Json.encodeToString(RestoreRequest(id, UUID.randomUUID().toString())))
        if (failure.exists()) check(failure.delete())
    }

    fun matchesCurrent(id: String): Boolean {
        val manifest = verify(id)
        if (targets.filterValues { it.exists() }.keys != manifest.roots) return false
        val current = linkedMapOf<String, RecoveryFile>()
        targets.forEach { (key, target) ->
            if (!target.exists()) return@forEach
            require(!Files.isSymbolicLink(target.toPath()))
            if (target.isDirectory) hashes(target).forEach { (path, digest) -> current["$key/$path"] = digest }
            else current[key] = hashFile(target)
        }
        return current == manifest.files
    }

    fun failureMessage(): String? = failure.takeIf { it.exists() }?.readText()
    fun hasPendingWork(): Boolean = pending.exists() || transaction.exists()

    fun clearFailure() { if (failure.exists()) check(failure.delete()) }

    fun applyPending() {
        if (transaction.exists()) recoverInterrupted()
        if (!pending.exists()) return
        val request = Json.decodeFromString<RestoreRequest>(pending.readText())
        try {
            val manifest = verify(request.snapshot)
            // Preserve the pre-restore state, including raw WAL files if the database is damaged.
            capture(0, "before-restore")
            check(transaction.mkdirs())
            val staged = File(transaction, "staged").also { check(it.mkdirs()) }
            manifest.roots.forEach { key -> copy(File(snapshot(request.snapshot), "data/$key"), File(staged, key)) }
            require(hashes(staged) == manifest.files) { "恢复暂存文件校验失败" }
            val journal = RestoreJournal(request.nonce, targets.filterValues { it.exists() }.keys)
            write(File(transaction, "journal.json"), Json.encodeToString(journal))
            val saved = File(transaction, "original").also { check(it.mkdirs()) }
            targets.forEach { (key, target) ->
                beforeMove(key)
                if (target.exists()) check(target.renameTo(File(saved, key))) { "无法保护原文件：$key" }
                if (key in manifest.roots) {
                    check(target.parentFile?.isDirectory == true || target.parentFile!!.mkdirs())
                    check(File(staged, key).renameTo(target)) { "无法安装恢复文件：$key" }
                }
            }
            write(File(transaction, "committed"), request.nonce)
            finishCommitted(request.nonce)
            clearFailure()
        } catch (exception: Exception) {
            try { if (transaction.exists() && !File(transaction, "committed").exists()) rollback() }
            catch (rollback: Exception) { exception.addSuppressed(rollback) }
            if (pending.exists() && !transaction.exists()) check(pending.delete())
            write(failure, "恢复未完成，现有文件已保留。${exception.message}")
            throw exception
        }
    }

    private fun recoverInterrupted() {
        val committed = File(transaction, "committed")
        if (committed.exists()) {
            finishCommitted(committed.readText())
            clearFailure()
        } else {
            rollback()
            if (pending.exists()) check(pending.delete())
            write(failure, "上次恢复被中断，已回退到恢复前状态。可重新选择快照。")
        }
    }

    private fun finishCommitted(nonce: String) {
        if (pending.exists() && Json.decodeFromString<RestoreRequest>(pending.readText()).nonce == nonce) check(pending.delete())
        // Once committed, cleanup must never be mistaken for an interrupted swap.
        val completed = File(directory, "completed-$nonce")
        check(transaction.renameTo(completed)) { "恢复已提交，但暂存目录尚未归档" }
        removeOwned(completed)
    }

    private fun rollback() {
        val file = File(transaction, "journal.json")
        if (file.exists()) {
            val journal = Json.decodeFromString<RestoreJournal>(file.readText())
            require(targets.keys.containsAll(journal.originalRoots))
            targets.entries.toList().asReversed().forEach { (key, target) ->
                val saved = File(transaction, "original/$key")
                if (saved.exists()) {
                    if (target.exists()) removeTarget(target)
                    check(saved.renameTo(target)) { "无法回退原文件：$key" }
                } else if (key !in journal.originalRoots && !File(transaction, "staged/$key").exists() && target.exists()) {
                    removeTarget(target)
                }
            }
        }
        removeOwned(transaction)
    }

    private fun snapshot(id: String): File {
        require(id.matches(Regex("snapshot-[a-fA-F0-9-]+"))) { "无效的快照名称" }
        return File(directory, id).also { require(it.canonicalFile.parentFile == directory.canonicalFile) }
    }

    private fun hashes(payload: File): Map<String, RecoveryFile> {
        check(payload.isDirectory)
        return payload.walkTopDown().mapNotNull { file ->
            require(!Files.isSymbolicLink(file.toPath())) { "快照包含符号链接" }
            require(file.canonicalPath == payload.canonicalPath || file.canonicalPath.startsWith(payload.canonicalPath + File.separator))
            if (!file.isFile) null else {
                file.relativeTo(payload).invariantSeparatorsPath to hashFile(file)
            }
        }.toMap()
    }

    private fun hashFile(file: File): RecoveryFile {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return RecoveryFile(file.length(), digest.digest().joinToString("") { "%02x".format(it) })
    }

    private fun copy(source: File, target: File) {
        require(!Files.isSymbolicLink(source.toPath())) { "不能备份符号链接" }
        if (source.isDirectory) {
            check(target.isDirectory || target.mkdirs())
            source.listFiles()?.forEach { copy(it, File(target, it.name)) } ?: error("无法读取目录：${source.name}")
        } else {
            check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
            source.inputStream().use { input -> FileOutputStream(target).use { output -> input.copyTo(output); output.fd.sync() } }
        }
    }

    private fun removeTarget(target: File) {
        require(targets.values.any { it.absoluteFile == target.absoluteFile })
        check(!Files.isSymbolicLink(target.toPath()))
        check(target.deleteRecursively()) { "无法移除恢复暂存目标" }
    }

    private fun removeOwned(file: File) {
        require(file.canonicalPath.startsWith(directory.canonicalPath + File.separator))
        if (file.exists()) check(file.deleteRecursively()) { "无法清理恢复暂存文件" }
    }

    private fun write(file: File, text: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temp).use { output -> output.write(text.toByteArray(Charsets.UTF_8)); output.fd.sync() }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
