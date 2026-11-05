package com.isaakhanimann.journal.ui.tabs.search.substance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.ui.theme.horizontalPadding

@Composable
internal fun MedicationSources(substance: Substance) {
    val sources = remember(substance) {
        (substance.sourceRefs + substance.clinicalInfo?.sourceRefs.orEmpty() +
            substance.endocrineInfo?.sourceRefs.orEmpty() + substance.tdm?.sourceRefs.orEmpty() +
            substance.timeCourse.flatMap { it.sourceRefs } + substance.doseUseReferences.flatMap { it.sourceRefs })
            .distinctBy { it.url }
    }
    if (sources.isEmpty() && substance.clinicalInfo?.atcCodes.isNullOrEmpty()) return
    var expanded by rememberSaveable(substance.id) { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(Modifier.padding(horizontal = horizontalPadding)) {
        TextButton({ expanded = !expanded }) { Text(stringResource(R.string.sources)) }
        if (expanded) {
            substance.clinicalInfo?.atcCodes?.takeIf { it.isNotEmpty() }?.let {
                Text("ATC · ${it.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            sources.forEach { source ->
                TextButton({ uri.openUri(source.url) }) { Text(source.title) }
                listOfNotNull(source.labelSection, source.accessedDate.takeIf { it.isNotBlank() })
                    .takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
