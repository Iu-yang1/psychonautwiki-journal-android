package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.TestosteroneReferences

@Composable
fun TestosteroneReferenceSources(onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(onDismissRequest = onClose, title = { Text("总睾酮研究参考") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("数据版本 ${TestosteroneReferences.version}。以下为原始论文报告的组均值，保留原单位；不是治疗目标或推荐剂量。")
            TestosteroneReferences.studies.forEach { study ->
                Text(study.label, style = MaterialTheme.typography.titleSmall)
                Text("${study.sourceTitle}\n${study.table} · 随机分组 n=${study.participants}")
                Text(study.regimen)
                Text(study.points.joinToString("；") { "${it.month} 月：${it.mean} ${study.unit}" })
                TextButton({ uriHandler.openUri(study.sourceUrl) }) { Text("查看原始研究 · ${study.table}") }
            }
            Text("计算方式", style = MaterialTheme.typography.titleSmall)
            Text("研究随访点之间线性连接，按日历月对齐，最长六个月。原论文的 SD／SE 未当作个人预测区间；未拟合个人 EC50、抑制下限、响应时间或两药联合抑制强度。")
            Text("历史用药与残留", style = MaterialTheme.typography.titleSmall)
            Text("全部有效 CPA／螺内酯实际记录用于历史对照和残留代理。记录缺失、停药和换药的效果不能从组均值推断。残留不用于计算总 T 下降。")
            Text("CPA：Androcur 5.2，终末半衰期 43.9 ± 12.8 小时。螺内酯：DailyMed 12.3，canrenone 16.5 小时；用作残留代理是近似。")
            TextButton({ uriHandler.openUri(TestosteroneReferences.cpaPkUrl) }) { Text("CPA 半衰期来源") }
            TextButton({ uriHandler.openUri(TestosteroneReferences.spiroPkUrl) }) { Text("螺内酯代谢物半衰期来源") }
        }
    }, confirmButton = { TextButton(onClose) { Text("关闭") } })
}
