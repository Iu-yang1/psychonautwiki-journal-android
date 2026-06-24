/*
 * Copyright (c) 2026. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.ui.tabs.search.substance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.substances.classes.DoseComponent
import com.isaakhanimann.journal.data.substances.classes.DoseRange
import com.isaakhanimann.journal.data.substances.classes.DoseUseReference
import com.isaakhanimann.journal.data.substances.classes.SourceRef
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import com.isaakhanimann.journal.ui.utils.localizedClinicalRouteText
import kotlin.math.max

@Composable
fun DoseUseReferenceSection(
    references: List<DoseUseReference>,
    isCardiovascular: Boolean,
    isEndocrine: Boolean
) {
    SectionWithTitle(title = stringResource(R.string.dose_use_references_title)) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.dose_use_references_disclaimer),
                style = MaterialTheme.typography.bodySmall
            )
            if (isCardiovascular) {
                Text(
                    text = stringResource(R.string.dose_reference_cardiovascular_caveat),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isEndocrine) {
                Text(
                    text = stringResource(R.string.dose_reference_endocrine_caveat),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            references.forEach { reference ->
                ReferenceRegimenCard(reference = reference)
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
fun ReferenceRegimenCard(reference: DoseUseReference) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = reference.indication,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            reference.population?.let { ReferenceLine(stringResource(R.string.dose_reference_population), it) }
            ReferenceLine(
                stringResource(R.string.dose_reference_route),
                localizedClinicalRouteText(reference.route)
            )
            reference.formulation?.let {
                ReferenceLine(stringResource(R.string.dose_reference_formulation), it)
            }
            ReferenceLine(
                stringResource(R.string.dose_reference_source_type),
                reference.sourceType
            )
            ReferenceLine(
                stringResource(R.string.dose_reference_evidence_level),
                reference.evidenceLevel
            )
            val groupedRanges = reference.ranges.groupBy { it.basis to it.unit }
            val hasDrawableRange = reference.ranges.any { range ->
                range.components.isEmpty() && (range.min != null || range.max != null)
            }
            if (hasDrawableRange) {
                groupedRanges.forEach { (group, ranges) ->
                    ReferenceRegimenBar(
                        ranges = ranges,
                        basis = group.first,
                        amountText = reference.amountText,
                        scheduleText = reference.scheduleText,
                        evidenceLevel = reference.evidenceLevel,
                        note = reference.note
                    )
                }
            } else {
                ReferenceRegimenTextOnly(
                    amountText = reference.amountText,
                    scheduleText = reference.scheduleText,
                    note = reference.note,
                    sourceRefs = reference.sourceRefs
                )
            }
            reference.ranges.filter { it.components.isNotEmpty() }.forEach { range ->
                ComponentDoseRange(range = range)
            }
            if (hasDrawableRange) {
                ReferenceSources(sourceRefs = reference.sourceRefs)
            }
        }
    }
}

@Composable
fun ReferenceRegimenBar(
    ranges: List<DoseRange>,
    basis: String,
    amountText: String,
    scheduleText: String?,
    evidenceLevel: String,
    note: String?
) {
    val drawable = ranges.filter { it.components.isEmpty() && (it.min != null || it.max != null) }
    if (drawable.isEmpty()) {
        Text(text = stringResource(R.string.dose_reference_no_numeric_range))
        return
    }
    val unit = drawable.first().unit
    val axisMax = drawable.maxOf { it.max ?: it.min ?: 0.0 }.coerceAtLeast(1.0)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Text(
        text = stringResource(R.string.dose_reference_basis_format, doseBasisText(basis)),
        style = MaterialTheme.typography.labelMedium
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(max(42, drawable.size * 22).dp)
    ) {
        val startX = 6.dp.toPx()
        val endX = size.width - 6.dp.toPx()
        val width = endX - startX
        drawable.forEachIndexed { index, range ->
            val y = 11.dp.toPx() + index * 22.dp.toPx()
            val low = (range.min ?: 0.0).coerceAtLeast(0.0)
            val high = (range.max ?: range.min ?: 0.0).coerceAtLeast(low)
            val color = colors[index % colors.size]
            drawLine(
                color = trackColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(startX + (low / axisMax).toFloat() * width, y),
                end = Offset(startX + (high / axisMax).toFloat() * width, y),
                strokeWidth = 9.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    drawable.forEach { range ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = rangeKindText(range.rangeKind),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = range.toDisplayText(), style = MaterialTheme.typography.labelLarge)
        }
        range.frequency?.let {
            Text(
                text = stringResource(R.string.dose_reference_frequency_format, it),
                style = MaterialTheme.typography.bodySmall
            )
        }
        range.note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
    }
    HorizontalDivider()
    ReferenceLine(stringResource(R.string.dose_reference_amount), amountText)
    scheduleText?.let { ReferenceLine(stringResource(R.string.dose_reference_schedule), it) }
    ReferenceLine(stringResource(R.string.dose_reference_evidence_level), evidenceLevel)
    note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun ReferenceRegimenTextOnly(
    amountText: String,
    scheduleText: String?,
    note: String?,
    sourceRefs: List<SourceRef>
) {
    ReferenceLine(stringResource(R.string.dose_reference_amount), amountText)
    scheduleText?.let { ReferenceLine(stringResource(R.string.dose_reference_schedule), it) }
    note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
    ReferenceSources(sourceRefs = sourceRefs)
}

@Composable
private fun ComponentDoseRange(range: DoseRange) {
    HorizontalDivider()
    Text(
        text = range.label ?: stringResource(R.string.dose_reference_component_dose),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
    range.components.forEach { component ->
        Text(
            text = stringResource(
                R.string.dose_reference_component_format,
                component.substance,
                component.toDisplayText()
            )
        )
    }
    range.frequency?.let {
        Text(
            text = stringResource(R.string.dose_reference_frequency_format, it),
            style = MaterialTheme.typography.bodySmall
        )
    }
    range.note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ReferenceSources(sourceRefs: List<SourceRef>) {
    if (sourceRefs.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(R.string.sources),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
    sourceRefs.forEach { source ->
        TextButton(onClick = { uriHandler.openUri(source.url) }) {
            Text(
                text = stringResource(
                    R.string.source_ref_format,
                    source.title,
                    source.sourceType,
                    source.accessedDate
                )
            )
        }
    }
}

@Composable
private fun ReferenceLine(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}

@Composable
private fun rangeKindText(rangeKind: String?): String {
    return stringResource(
        when (rangeKind) {
            "initial" -> R.string.dose_reference_initial
            "maintenance" -> R.string.dose_reference_maintenance
            "maximum-labeled" -> R.string.dose_reference_upper_bound
            "study-regimen", "literature-regimen" -> R.string.dose_reference_study_regimen
            "label-regimen" -> R.string.dose_reference_label_regimen
            "guideline-regimen" -> R.string.dose_reference_guideline_regimen
            "protocol-range" -> R.string.dose_reference_protocol_range
            else -> R.string.dose_reference_source_needed
        }
    )
}

@Composable
private fun doseBasisText(basis: String): String {
    return stringResource(
        when (basis) {
            "per-dose" -> R.string.dose_basis_per_dose
            "daily-total" -> R.string.dose_basis_daily_total
            "weekly-total" -> R.string.dose_basis_weekly_total
            "patch-delivery-rate" -> R.string.dose_basis_patch_delivery_rate
            "infusion-rate" -> R.string.dose_basis_infusion_rate
            "weight-based" -> R.string.dose_basis_weight_based
            "body-surface-area-based" -> R.string.dose_basis_body_surface_area
            "component-dose" -> R.string.dose_basis_component_dose
            else -> R.string.dose_basis_unknown
        }
    )
}

@Composable
private fun DoseRange.toDisplayText(): String {
    return when {
        min != null && max != null && min != max ->
            stringResource(R.string.dose_reference_range_format, min.readable(), max.readable(), unit)
        min != null && max != null ->
            stringResource(R.string.dose_reference_single_format, min.readable(), unit)
        max != null -> stringResource(R.string.dose_reference_max_format, max.readable(), unit)
        min != null -> stringResource(R.string.dose_reference_min_format, min.readable(), unit)
        else -> stringResource(R.string.dose_reference_source_needed)
    }
}

@Composable
private fun DoseComponent.toDisplayText(): String {
    return when {
        min != null && max != null && min != max ->
            stringResource(R.string.dose_reference_range_format, min.readable(), max.readable(), unit)
        min != null && max != null ->
            stringResource(R.string.dose_reference_single_format, min.readable(), unit)
        max != null -> stringResource(R.string.dose_reference_max_format, max.readable(), unit)
        min != null -> stringResource(R.string.dose_reference_min_format, min.readable(), unit)
        else -> stringResource(R.string.dose_reference_source_needed)
    }
}

private fun Double.readable(): String {
    return if (rem(1.0) == 0.0) toInt().toString() else toString()
}
