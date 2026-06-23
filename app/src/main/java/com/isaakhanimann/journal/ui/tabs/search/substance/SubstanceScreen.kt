/*
 * Copyright (c) 2022-2023. Isaak Hanimann.
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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.room.experiences.entities.CustomUnit
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.Category
import com.isaakhanimann.journal.data.substances.classes.ClinicalInfo
import com.isaakhanimann.journal.data.substances.classes.SourceRef
import com.isaakhanimann.journal.data.substances.classes.SubstanceWithCategories
import com.isaakhanimann.journal.data.substances.classes.TherapeuticDrugMonitoring
import com.isaakhanimann.journal.data.substances.classes.TherapeuticRange
import com.isaakhanimann.journal.data.substances.classes.TimeCourse
import com.isaakhanimann.journal.data.substances.classes.TimeValue
import com.isaakhanimann.journal.data.substances.classes.ToxicityThreshold
import com.isaakhanimann.journal.ui.DOSE_DISCLAIMER
import com.isaakhanimann.journal.ui.FULL_STOMACH_DISCLAIMER
import com.isaakhanimann.journal.ui.tabs.journal.addingestion.dose.ChasingTheDragonText
import com.isaakhanimann.journal.ui.tabs.journal.addingestion.dose.OptionalDosageUnitDisclaimer
import com.isaakhanimann.journal.ui.tabs.journal.addingestion.dose.customunit.CustomUnitRoaDoseView
import com.isaakhanimann.journal.ui.tabs.journal.addingestion.time.TimePickerButton
import com.isaakhanimann.journal.ui.tabs.journal.experience.TimelineDisplayOption
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.TimeDisplayOption
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.AllTimelines
import com.isaakhanimann.journal.ui.tabs.search.localizedCategoryDisplayName
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.ToleranceSection
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.dose.RoaDoseView
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.duration.RoaDurationView
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.toReadableString
import com.isaakhanimann.journal.ui.theme.JournalTheme
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import com.isaakhanimann.journal.ui.theme.verticalPaddingCards
import com.isaakhanimann.journal.ui.utils.getShortTimeText
import com.isaakhanimann.journal.ui.utils.localizedClinicalRouteText
import com.isaakhanimann.journal.ui.utils.localizedDisplayText
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Composable
fun SubstanceScreen(
    navigateToDosageExplanationScreen: () -> Unit,
    navigateToSaferHallucinogensScreen: () -> Unit,
    navigateToSaferStimulantsScreen: () -> Unit,
    navigateToVolumetricDosingScreen: () -> Unit,
    navigateToExplainTimeline: () -> Unit,
    navigateToCategoryScreen: (categoryName: String) -> Unit,
    viewModel: SubstanceViewModel = hiltViewModel()
) {
    SubstanceScreen(
        timelineDisplayOption = viewModel.timelineDisplayOptionFlow.collectAsState().value,
        ingestionTime = viewModel.ingestionTimeFlow.collectAsState().value,
        onChangeIngestionTime = viewModel::changeIngestionTime,
        navigateToDosageExplanationScreen = navigateToDosageExplanationScreen,
        navigateToSaferHallucinogensScreen = navigateToSaferHallucinogensScreen,
        navigateToSaferStimulantsScreen = navigateToSaferStimulantsScreen,
        navigateToVolumetricDosingScreen = navigateToVolumetricDosingScreen,
        navigateToCategoryScreen = navigateToCategoryScreen,
        navigateToExplainTimeline = navigateToExplainTimeline,
        substanceWithCategories = viewModel.substanceWithCategories,
        customUnits = viewModel.customUnitsFlow.collectAsState().value,
    )
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SubstanceScreenPreview(
    @PreviewParameter(SubstanceWithCategoriesPreviewProvider::class) substanceWithCategories: SubstanceWithCategories
) {
    JournalTheme {
        SubstanceScreen(
            timelineDisplayOption = TimelineDisplayOption.Loading,
            ingestionTime = LocalDateTime.now(),
            onChangeIngestionTime = {},
            navigateToDosageExplanationScreen = {},
            navigateToSaferHallucinogensScreen = {},
            navigateToSaferStimulantsScreen = {},
            navigateToVolumetricDosingScreen = {},
            navigateToExplainTimeline = {},
            navigateToCategoryScreen = {},
            substanceWithCategories = substanceWithCategories,
            customUnits = listOf(
                CustomUnit.mdmaSample
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubstanceScreen(
    timelineDisplayOption: TimelineDisplayOption,
    ingestionTime: LocalDateTime,
    onChangeIngestionTime: (LocalDateTime) -> Unit,
    navigateToDosageExplanationScreen: () -> Unit,
    navigateToSaferHallucinogensScreen: () -> Unit,
    navigateToSaferStimulantsScreen: () -> Unit,
    navigateToVolumetricDosingScreen: () -> Unit,
    navigateToExplainTimeline: () -> Unit,
    navigateToCategoryScreen: (categoryName: String) -> Unit,
    substanceWithCategories: SubstanceWithCategories,
    customUnits: List<CustomUnit>
) {
    val substance = substanceWithCategories.substance
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(substance.name) },
                actions = {
                    TextButton(
                        onClick = { uriHandler.openUri(substance.url) },
                    ) {
                        Text(stringResource(R.string.article))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            if (!substance.isApproved) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = verticalPaddingCards
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(
                            vertical = 5.dp,
                            horizontal = horizontalPadding
                        )
                    ) {
                        Icon(imageVector = Icons.Default.GppBad, contentDescription = "Verified")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.info_is_not_approved))
                    }
                }
            }
            val categories = substanceWithCategories.categories
            if (substance.summary != null || categories.isNotEmpty()) {
                VerticalSpace()
                ElevatedCard(
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPaddingCards
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                horizontal = horizontalPadding,
                                vertical = 10.dp
                            )
                            .fillMaxWidth()
                    ) {
                        if (substance.summary != null) {
                            Text(text = substance.summary)
                            VerticalSpace()
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            categories.forEach { category ->
                                CategoryChipFromSubstanceScreen(category, navigateToCategoryScreen)
                            }
                        }
                    }
                }
            }
            if (substance.clinicalInfo != null) {
                ClinicalInformationSection(clinicalInfo = substance.clinicalInfo)
            }
            if (substance.timeCourse.isNotEmpty()) {
                TimeCourseSection(timeCourses = substance.timeCourse)
            }
            if (substance.tdm != null) {
                TherapeuticDrugMonitoringSection(tdm = substance.tdm)
            }
            val roasWithDosesDefined = substance.roas.filter { roa ->
                val roaDose = roa.roaDose
                val isEveryDoseNull =
                    roaDose?.lightMin == null && roaDose?.commonMin == null && roaDose?.strongMin == null && roaDose?.heavyMin == null
                return@filter !isEveryDoseNull
            }
            if (substance.dosageRemark != null || roasWithDosesDefined.isNotEmpty()) {
                SectionWithTitle(title = stringResource(R.string.dosage)) {
                    Column(Modifier.padding(horizontal = horizontalPadding)) {
                        if (substance.dosageRemark != null) {
                            Text(text = substance.dosageRemark)
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                        }
                        roasWithDosesDefined.forEach { roa ->
                            Column(
                                modifier = Modifier.padding(vertical = 5.dp)
                            ) {
                                Text(
                                    text = roa.route.localizedDisplayText(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (roa.roaDose == null) {
                                    Text(text = stringResource(R.string.no_dosage_info))
                                } else {
                                    RoaDoseView(roaDose = roa.roaDose)
                                }
                                roa.roaDose?.let { roaDose ->
                                    val customUnitsForRoute =
                                        customUnits.filter { it.administrationRoute == roa.route && it.dose != null }
                                    customUnitsForRoute.forEach { customUnit ->
                                        Text(
                                            text = customUnit.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        CustomUnitRoaDoseView(
                                            roaDose = roaDose,
                                            customUnit = customUnit
                                        )
                                    }
                                }
                                val bio = roa.bioavailability
                                if (bio != null) {
                                    Text(text = stringResource(
                                        R.string.bioavailability,
                                        bio.min?.toReadableString() ?: "..",
                                        bio.max?.toReadableString() ?: ".."
                                    ))
                                }
                                if (roa.route == AdministrationRoute.SMOKED && substance.name != "Cannabis") {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    ChasingTheDragonText(
                                        titleStyle = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                        VerticalSpace()
                        OptionalDosageUnitDisclaimer(substance.name)
                        Text(text = DOSE_DISCLAIMER)
                        VerticalSpace()
                        if (substance.roas.any { it.roaDose?.shouldUseVolumetricDosing == true }) {
                            HorizontalDivider()
                            TextButton(onClick = navigateToVolumetricDosingScreen) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.volumetric_dosing))
                            }
                        }
                        HorizontalDivider()
                        TextButton(onClick = navigateToDosageExplanationScreen) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Info",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.dosage_classification))
                        }

                    }
                }
            }
            if (substance.tolerance != null || substance.crossTolerances.isNotEmpty()) {
                SectionWithTitle(title = stringResource(R.string.tolerance)) {
                    Column {
                        VerticalSpace()
                        ToleranceSection(
                            tolerance = substance.tolerance,
                            crossTolerances = substance.crossTolerances,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        VerticalSpace()
                    }
                }
            }
            if (substance.toxicities.isNotEmpty()) {
                SectionWithTitle(title = stringResource(R.string.toxicity)) {
                    Column {
                        VerticalSpace()
                        if (substance.toxicities.size == 1) {
                            Text(
                                substance.toxicities.firstOrNull() ?: "",
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        } else {
                            BulletPoints(
                                points = substance.toxicities,
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        }
                        VerticalSpace()
                    }
                }
            }
            val roasWithDurationsDefined = substance.roas.filter { roa ->
                val roaDuration = roa.roaDuration
                val isEveryDurationNull =
                    roaDuration?.onset == null && roaDuration?.comeup == null && roaDuration?.peak == null && roaDuration?.offset == null && roaDuration?.total == null
                return@filter !isEveryDurationNull
            }
            if (roasWithDurationsDefined.isNotEmpty()) {
                SectionWithTitle(title = stringResource(R.string.duration)) {
                    Column(Modifier.padding(horizontal = horizontalPadding)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.start_time))
                            Spacer(modifier = Modifier.width(5.dp))
                            TimePickerButton(
                                localDateTime = ingestionTime,
                                onChange = onChangeIngestionTime,
                                timeString = ingestionTime.getShortTimeText(),
                                hasOutline = false,
                            )
                            val isTimeALotDifferentToNow = ChronoUnit.MINUTES.between(
                                ingestionTime,
                                LocalDateTime.now()
                            ).absoluteValue > 5
                            Spacer(modifier = Modifier.width(5.dp))
                            AnimatedVisibility(visible = isTimeALotDifferentToNow) {
                                IconButton(onClick = { onChangeIngestionTime(LocalDateTime.now()) }) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = "Reset to now"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = navigateToExplainTimeline) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Timeline disclaimer"
                                )
                            }
                        }
                        VerticalSpace()
                        when (timelineDisplayOption) {
                            TimelineDisplayOption.Hidden -> {}
                            TimelineDisplayOption.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            TimelineDisplayOption.NotWorthDrawing -> {}
                            is TimelineDisplayOption.Shown -> {
                                val timelineModel = timelineDisplayOption.allTimelinesModel
                                AllTimelines(
                                    model = timelineModel,
                                    isShowingCurrentTime = false,
                                    timeDisplayOption = TimeDisplayOption.RELATIVE_TO_NOW,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        roasWithDurationsDefined.forEachIndexed { index, roa ->
                            Column(
                                modifier = Modifier.padding(
                                    vertical = 5.dp,
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RouteColorCircle(roa.route)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = roa.route.localizedDisplayText(),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                val roaDuration = roa.roaDuration
                                if (roaDuration == null) {
                                    Text(text = stringResource(R.string.no_duration_info))
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    RoaDurationView(roaDuration = roaDuration)
                                    if (roa.route == AdministrationRoute.ORAL) {
                                        Text(
                                            text = FULL_STOMACH_DISCLAIMER,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            if (index < roasWithDurationsDefined.size - 1) {
                                HorizontalDivider()
                            }
                        }
                        VerticalSpace()
                    }
                }
            }
            val interactions = substance.interactions
            if (interactions != null) {
                if (interactions.dangerous.isNotEmpty() || interactions.unsafe.isNotEmpty() || interactions.uncertain.isNotEmpty()) {
                    SectionWithTitle(stringResource(R.string.interactions)) {
                        InteractionsView(
                            interactions = substance.interactions,
                            substanceURL = substance.url,
                        )
                    }
                }
            }
            if (substance.effectsSummary != null) {
                SectionWithTitle(title = stringResource(R.string.effects)) {
                    Column {
                        Text(
                            text = substance.effectsSummary,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (substance.generalRisks != null && substance.longtermRisks != null) {
                SectionWithTitle(title = stringResource(R.string.risks)) {
                    Column {
                        Text(
                            text = substance.generalRisks,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                SectionWithTitle(title = stringResource(R.string.long_term)) {
                    Column {
                        Text(
                            text = substance.longtermRisks,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (substance.saferUse.isNotEmpty()) {
                SectionWithTitle(title = stringResource(R.string.safer_use)) {
                    Column {
                        BulletPoints(
                            points = substance.saferUse,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (substance.addictionPotential != null) {
                SectionWithTitle(title = stringResource(R.string.addiction_potential)) {
                    Column {
                        Text(
                            substance.addictionPotential,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            val firstRoa = substance.roas.firstOrNull()
            val useVolumetric = firstRoa?.roaDose?.shouldUseVolumetricDosing == true
            if (substance.isHallucinogen || substance.isStimulant || useVolumetric) {
                SectionWithTitle(title = stringResource(R.string.see_also)) {
                    Column {
                        if (substance.isHallucinogen) {
                            TextButton(onClick = navigateToSaferHallucinogensScreen) {
                                Text(
                                    text = stringResource(R.string.safer_hallucinogen_use),
                                    modifier = Modifier.padding(horizontal = horizontalPadding)
                                )
                            }
                            HorizontalDivider()
                        }
                        if (substance.isStimulant) {
                            TextButton(onClick = navigateToSaferStimulantsScreen) {
                                Text(
                                    text = stringResource(R.string.safer_stimulant_use),
                                    modifier = Modifier.padding(horizontal = horizontalPadding)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun ClinicalInformationSection(clinicalInfo: ClinicalInfo) {
    SectionWithTitle(title = "临床资料 / Clinical information") {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            ClinicalInfoListRow("ATC 编码 / ATC code", clinicalInfo.atcCodes)
            ClinicalInfoListRow("药物类别 / Drug class", clinicalInfo.drugClass)
            ClinicalInfoListRow("主要适应证 / Main indications", clinicalInfo.indications)
            ClinicalInfoListRow("禁忌证 / Contraindications", clinicalInfo.contraindications)
            ClinicalInfoListRow("重要警示 / Major warnings", clinicalInfo.majorWarnings)
            ClinicalInfoListRow("重要相互作用 / Major interactions", clinicalInfo.majorInteractions)
            ClinicalInfoListRow("监测项目 / Monitoring", clinicalInfo.monitoring)
            SourceRefs(sourceRefs = clinicalInfo.sourceRefs)
            VerticalSpace()
        }
    }
}

@Composable
fun TimeCourseSection(timeCourses: List<TimeCourse>) {
    SectionWithTitle(title = "药代 / 药效时间进程") {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = "Tmax 表示血浆浓度达峰时间，不一定等于最大临床效果时间。以上信息仅供学习和资料索引，不用于诊断、处方或自行调整药物。",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            timeCourses.forEachIndexed { index, timeCourse ->
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(
                        text = listOfNotNull(
                            localizedClinicalRouteText(timeCourse.route),
                            timeCourse.formulation
                        ).joinToString(" / "),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TimeCourseChart(timeCourse = timeCourse)
                    Spacer(modifier = Modifier.height(6.dp))
                    TimeValueRow("起效时间 / Onset", timeCourse.onset)
                    TimeValueRow("Tmax", timeCourse.tmax)
                    TimeValueRow("药效峰值 / Peak effect", timeCourse.peakEffect)
                    TimeValueRow("作用持续时间 / Duration of action", timeCourse.durationOfAction)
                    TimeValueRow("消除半衰期 / Elimination half-life", timeCourse.eliminationHalfLife)
                    TimeValueRow("稳态时间 / Time to steady state", timeCourse.timeToSteadyState)
                    TimeValueRow("洗脱时间 / Washout", timeCourse.washout)
                    ClinicalInfoListRow("备注 / Notes", timeCourse.notes)
                    SourceRefs(sourceRefs = timeCourse.sourceRefs)
                }
                if (index < timeCourses.size - 1) {
                    HorizontalDivider()
                }
            }
            VerticalSpace()
        }
    }
}

@Composable
fun TherapeuticDrugMonitoringSection(tdm: TherapeuticDrugMonitoring) {
    SectionWithTitle(title = "治疗药物监测 / TDM") {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = "This information is for educational reference and data indexing only. It is not medical advice and must not be used for diagnosis, prescribing, self-medication, or dose adjustment. 本资料仅用于学习和资料索引，不构成医疗建议，不用于诊断、处方、自行用药或调整剂量。",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoLabel("常规监测 / Routine monitoring")
            Text(text = stringResource(if (tdm.isRoutinelyMonitored) R.string.label_yes else R.string.label_no))
            VerticalSpace()
            ClinicalInfoTextRow("监测类型 / Monitoring type", tdm.monitoringType)
            ClinicalInfoTextRow("原因 / Reason", tdm.reason)
            ClinicalInfoListRow("分析物 / Analytes", tdm.analytes)
            ClinicalInfoTextRow("标本 / Specimen", tdm.specimen)
            ClinicalInfoTextRow("采样时间 / Sampling time", tdm.samplingTime)
            TherapeuticRangeRows("治疗范围 / Therapeutic ranges", tdm.therapeuticRanges)
            ToxicityThresholdRows("中毒阈值 / Toxicity thresholds", tdm.toxicityThresholds)
            ToxicityThresholdRows("危急值 / Critical values", tdm.criticalValues)
            ClinicalInfoTextRow("检测方法 / Assay method", tdm.assayMethod)
            ClinicalInfoListRow("解释注意事项 / Interpretation caveats", tdm.interpretationCaveats)
            SourceRefs(sourceRefs = tdm.sourceRefs)
            VerticalSpace()
        }
    }
}

@Composable
private fun ClinicalInfoListRow(label: String, values: List<String>) {
    if (values.isEmpty()) return
    InfoLabel(label)
    if (values.size == 1) {
        Text(text = values.first())
    } else {
        BulletPoints(points = values)
    }
    VerticalSpace()
}

@Composable
private fun ClinicalInfoTextRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    InfoLabel(label)
    Text(text = value)
    VerticalSpace()
}

@Composable
private fun TherapeuticRangeRows(label: String, ranges: List<TherapeuticRange>) {
    if (ranges.isEmpty()) return
    InfoLabel(label)
    ranges.forEach { range ->
        Text(text = listOfNotNull(range.indication, "${range.range} ${range.unit}").joinToString(": "))
        range.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
    VerticalSpace()
}

@Composable
private fun ToxicityThresholdRows(label: String, thresholds: List<ToxicityThreshold>) {
    if (thresholds.isEmpty()) return
    InfoLabel(label)
    thresholds.forEach { threshold ->
        Text(text = "${threshold.threshold} ${threshold.unit}")
        threshold.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
    VerticalSpace()
}

@Composable
private fun TimeValueRow(label: String, timeValue: TimeValue?) {
    if (timeValue == null) return
    InfoLabel(label)
    Text(text = timeValue.toReadableText(stringResource(R.string.not_specified)))
    timeValue.basis?.let {
        Text(text = stringResource(R.string.basis_label, it), style = MaterialTheme.typography.bodySmall)
    }
    timeValue.note?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall)
    }
    VerticalSpace()
}

@Composable
private fun InfoLabel(label: String) {
    Text(
        text = label,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SourceRefs(sourceRefs: List<SourceRef>) {
    if (sourceRefs.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    InfoLabel(stringResource(R.string.sources))
    sourceRefs.forEach { sourceRef ->
        TextButton(onClick = { uriHandler.openUri(sourceRef.url) }) {
            Text(stringResource(R.string.source_ref_format, sourceRef.title, sourceRef.sourceType, sourceRef.accessedDate))
        }
    }
    VerticalSpace()
}

private fun TimeValue.toReadableText(notSpecifiedText: String): String {
    val minText = min?.toReadableString()
    val maxText = max?.toReadableString()
    val valueText = when {
        minText != null && maxText != null && minText != maxText -> "$minText-$maxText"
        minText != null -> minText
        maxText != null -> maxText
        else -> notSpecifiedText
    }
    return "$valueText $unit"
}

@Composable
fun RouteColorCircle(administrationRoute: AdministrationRoute) {
    val isDarkTheme = isSystemInDarkTheme()
    Surface(
        shape = CircleShape,
        color = administrationRoute.color.getComposeColor(isDarkTheme),
        modifier = Modifier
            .size(20.dp)
    ) {}
}

@Composable
fun BulletPoints(points: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        points.forEach {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(7.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = it)
            }
        }
    }
}

@Composable
fun VerticalSpace() {
    Spacer(modifier = Modifier.height(5.dp))
}


@Composable
fun CategoryChipFromSubstanceScreen(
    category: Category,
    navigateToCategoryScreen: (categoryName: String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape = CircleShape)
            .clickable {
                navigateToCategoryScreen(category.name)
            }
            .background(color = category.color.copy(alpha = 0.2f))
            .height(48.dp)
            .padding(horizontal = 12.dp)

    ) {
        Text(text = localizedCategoryDisplayName(category.name))
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go to",
            modifier = Modifier.size(20.dp)
        )
    }
}
