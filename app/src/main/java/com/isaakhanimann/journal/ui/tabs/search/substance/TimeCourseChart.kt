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

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.substances.classes.TimeCourse
import com.isaakhanimann.journal.data.substances.classes.TimeValue
import com.isaakhanimann.journal.data.substances.classes.representativeHours
import com.isaakhanimann.journal.ui.utils.localizedClinicalRouteText
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun TimeCourseChart(timeCourse: TimeCourse) {
    val chart = remember(timeCourse) { timeCourse.toChartModel() } ?: return
    val color = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val labelSize = MaterialTheme.typography.labelSmall.fontSize
    val labelPaint = remember(density, labelColor, labelSize) {
        Paint().apply {
            this.color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
            textAlign = Paint.Align.CENTER
            textSize = density.run { labelSize.toPx() }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
        ) {
            val labelHeight = labelPaint.textSize * 1.6f
            val leftPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()
            val topPadding = 8.dp.toPx()
            val bottom = size.height - labelHeight
            val graphHeight = bottom - topPadding
            val graphWidth = size.width - leftPadding - rightPadding
            val pixelsPerHour = graphWidth / chart.endHour

            fun x(hour: Float) = leftPadding + hour.coerceIn(0f, chart.endHour) * pixelsPerHour
            fun y(strength: Float) = bottom - strength.coerceIn(0f, 1f) * graphHeight

            val areaPath = Path().apply {
                moveTo(x(0f), y(0f))
                lineTo(x(chart.onsetStartHour), y(0f))
                quadraticTo(
                    x(chart.riseControlHour),
                    y(0f),
                    x(chart.peakHour),
                    y(1f)
                )
                quadraticTo(
                    x(chart.fallControlHour),
                    y(1f),
                    x(chart.endHour),
                    y(0f)
                )
                lineTo(x(0f), y(0f))
                close()
            }
            drawPath(path = areaPath, color = color.copy(alpha = 0.22f))

            val solidPath = Path().apply {
                moveTo(x(0f), y(0f))
                lineTo(x(chart.onsetStartHour), y(0f))
                quadraticTo(
                    x(chart.riseControlHour),
                    y(0f),
                    x(chart.peakHour),
                    y(1f)
                )
            }
            drawPath(
                path = solidPath,
                color = color,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            val dashedPath = Path().apply {
                moveTo(x(chart.peakHour), y(1f))
                quadraticTo(
                    x(chart.fallControlHour),
                    y(1f),
                    x(chart.endHour),
                    y(0f)
                )
            }
            drawPath(
                path = dashedPath,
                color = color,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 12.dp.toPx()))
                )
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPadding, bottom),
                end = Offset(size.width - rightPadding, bottom),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = color,
                radius = 8.dp.toPx(),
                center = Offset(x(0f), bottom)
            )

            val axisMarks = chart.axisMarks()
            drawContext.canvas.nativeCanvas.apply {
                axisMarks.forEach { hour ->
                    drawText(hour.toAxisLabel(), x(hour), size.height - 2.dp.toPx(), labelPaint)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier
                    .width(18.dp)
                    .height(18.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = listOfNotNull(
                    localizedClinicalRouteText(timeCourse.route),
                    timeCourse.formulation
                ).joinToString(" / "),
                style = MaterialTheme.typography.titleSmall
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val endPill = listOf(
                stringResource(R.string.time_course_chart_duration) to timeCourse.durationOfAction,
                stringResource(R.string.time_course_chart_clearance) to timeCourse.washout,
                stringResource(R.string.time_course_chart_half_life) to timeCourse.eliminationHalfLife
            ).firstOrNull { it.second != null }
            TimePill(stringResource(R.string.time_course_chart_onset), timeCourse.onset)
            TimePill(stringResource(R.string.time_course_tmax), timeCourse.tmax)
            TimePill(stringResource(R.string.time_course_chart_peak_effect), timeCourse.peakEffect)
            endPill?.let { TimePill(it.first, it.second) }
        }
    }
}

@Composable
private fun TimePill(label: String, value: TimeValue?) {
    if (value == null) return
    Surface(shape = RoundedCornerShape(5.dp), tonalElevation = 8.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(text = value.toShortText(), style = MaterialTheme.typography.bodyMedium)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class TimeCourseChartModel(
    val onsetStartHour: Float,
    val riseControlHour: Float,
    val peakHour: Float,
    val fallControlHour: Float,
    val endHour: Float
) {
    fun axisMarks(): List<Float> {
        val step = when {
            endHour <= 6f -> 1f
            endHour <= 24f -> 4f
            endHour <= 72f -> 12f
            endHour <= 168f -> 24f
            else -> 48f
        }
        val marks = mutableListOf(0f)
        var next = step
        while (next < endHour) {
            marks.add(next)
            next += step
        }
        marks.add(endHour)
        return marks.distinct()
    }
}

private fun TimeCourse.toChartModel(): TimeCourseChartModel? {
    val representativeOnset = onset?.representativeHours()
    val onsetStart = representativeOnset ?: 0f
    val onsetEnd = representativeOnset ?: onsetStart
    val durationEnd = durationOfAction?.representativeHours()
    val washoutEnd = washout?.representativeHours()
    val clearanceEnd = eliminationHalfLife?.representativeHours()?.times(5f)
    val chartEndCandidate = listOfNotNull(durationEnd, washoutEnd, clearanceEnd).maxOrNull()
    val peak = peakEffect?.representativeHours()
        ?: tmax?.representativeHours()
        ?: if (chartEndCandidate != null) {
            onsetEnd + ((chartEndCandidate - onsetEnd).coerceAtLeast(0.5f) * 0.25f)
        } else {
            null
        }
        ?: return null
    val rawEnd = chartEndCandidate ?: (peak + max(peak * 0.5f, 0.5f))
    val end = max(rawEnd, peak + 0.5f)
    val roundedEnd = max(0.25f, ceil(end * 2f) / 2f)
    return TimeCourseChartModel(
        onsetStartHour = onsetStart.coerceAtLeast(0f),
        riseControlHour = onsetEnd.coerceIn(0f, roundedEnd),
        peakHour = peak.coerceIn(0f, roundedEnd),
        fallControlHour = (peak + (roundedEnd - peak) * 0.45f).coerceIn(0f, roundedEnd),
        endHour = roundedEnd
    )
}

private fun Float.toAxisLabel(): String {
    val rounded = if (this % 1f == 0f) toInt().toString() else toString()
    return if (this >= 24f && this % 24f == 0f) {
        "${(this / 24f).toInt()}d"
    } else {
        "${rounded}h"
    }
}

private fun TimeValue.toShortText(): String {
    val minText = min?.toReadableString()
    val maxText = max?.toReadableString()
    val valueText = when {
        minText != null && maxText != null && minText != maxText -> "$minText-$maxText"
        minText != null -> minText
        maxText != null -> maxText
        else -> "--"
    }
    return "$valueText$unit"
}

private fun Double.toReadableString(): String {
    return if (rem(1.0) == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
