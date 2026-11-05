package com.isaakhanimann.journal.ui.main

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecoveryCenter(error: String?, onRetry: () -> Unit, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    var archive by remember { mutableStateOf<RecoveryArchive?>(null) }
    val scope = rememberCoroutineScope()
    var listing by remember { mutableStateOf<RecoveryListing?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<RecoveryManifest?>(null) }
    var busy by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val opened = withContext(Dispatchers.IO) { DatabaseRecovery.archive(context) }
            archive = opened
            listing = withContext(Dispatchers.IO) { opened.list() }
        }
        catch (exception: Exception) { notice = exception.message ?: "无法读取快照" }
    }
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (error == null) "本地恢复快照" else "数据暂未打开", style = MaterialTheme.typography.headlineSmall)
        if (error != null) {
            Text("原有文件会保留。可重试打开，或从已验证快照恢复。")
            TextButton({ details = !details }) { Text("错误详情") }
            if (details) SelectionContainer { Text(error) }
            Button(onRetry, enabled = !busy) { Text("重试打开") }
        }
        if (listing == null && notice == null) CircularProgressIndicator()
        if (listing?.snapshots?.isEmpty() == true) Text("暂无可用快照")
        listing?.snapshots?.forEach { snapshot ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                val reason = when (snapshot.reason) { "upgrade", "legacy-upgrade" -> "升级前"; "before-restore" -> "恢复前保护"; else -> snapshot.reason }
                Text("${format.format(Instant.ofEpochMilli(snapshot.createdAt))} · $reason")
                Text(if (snapshot.version == 0) "原始文件快照（版本未知）" else "数据库版本 ${snapshot.version}")
                TextButton({ selected = snapshot }, enabled = !busy) { Text("校验并恢复") }
            } }
        }
        listing?.invalid?.takeIf { it.isNotEmpty() }?.let { invalid ->
            Text("${invalid.size} 个快照清单损坏，文件仍保留。", color = MaterialTheme.colorScheme.error)
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (onClose != null) TextButton(onClose, enabled = !busy) { Text("返回") }
        else TextButton({
            (context as? Activity)?.finishAffinity()
            android.os.Process.killProcess(android.os.Process.myPid())
        }, enabled = !busy) { Text("关闭应用") }
    }
    selected?.let { snapshot -> AlertDialog(onDismissRequest = { if (!busy) selected = null }, title = { Text("恢复到此快照？") }, text = {
        Text("将替换记录、照片和偏好，恢复前状态会另存为保护快照。应用将关闭，重新打开后执行恢复。")
    }, confirmButton = { TextButton({
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    require(snapshot.version in 0..21) { "此快照来自更高版本，不能由当前版本恢复" }
                    requireNotNull(archive).request(snapshot.id)
                }
                (context as? Activity)?.finishAffinity()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (exception: Exception) { notice = exception.message ?: "无法安排恢复"; selected = null }
            finally { busy = false }
        }
    }, enabled = !busy) { Text("恢复并关闭应用") } }, dismissButton = { TextButton({ selected = null }, enabled = !busy) { Text("取消") } }) }
}
