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
import com.isaakhanimann.journal.data.substances.classes.EndocrineInfo
import com.isaakhanimann.journal.data.substances.classes.HrtModelInfo
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
            if (substance.endocrineInfo != null) {
                EndocrineInformationSection(endocrineInfo = substance.endocrineInfo)
            }
            if (substance.timeCourse.isNotEmpty()) {
                TimeCourseSection(timeCourses = substance.timeCourse)
            }
            if (substance.doseUseReferences.isNotEmpty()) {
                DoseUseReferenceSection(
                    references = substance.doseUseReferences,
                    isCardiovascular = "cardiovascular" in substance.categories,
                    isEndocrine =
                        "endocrine" in substance.categories || "hrt-related" in substance.categories
                )
            }
            if (substance.hrtModelInfo != null) {
                HrtModelReadinessSection(hrtModelInfo = substance.hrtModelInfo)
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
    SectionWithTitle(title = stringResource(R.string.clinical_information_title)) {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            ClinicalInfoListRow(stringResource(R.string.clinical_atc_code), clinicalInfo.atcCodes)
            ClinicalInfoListRow(stringResource(R.string.clinical_drug_class), clinicalInfo.drugClass)
            ClinicalInfoListRow(stringResource(R.string.clinical_main_indications), clinicalInfo.indications)
            ClinicalInfoListRow(stringResource(R.string.clinical_contraindications), clinicalInfo.contraindications)
            ClinicalInfoListRow(stringResource(R.string.clinical_major_warnings), clinicalInfo.majorWarnings)
            ClinicalInfoListRow(stringResource(R.string.clinical_major_interactions), clinicalInfo.majorInteractions)
            ClinicalInfoListRow(stringResource(R.string.clinical_monitoring), clinicalInfo.monitoring)
            SourceRefs(sourceRefs = clinicalInfo.sourceRefs)
            VerticalSpace()
        }
    }
}

@Composable
fun EndocrineInformationSection(endocrineInfo: EndocrineInfo) {
    SectionWithTitle(title = stringResource(R.string.endocrine_information_title)) {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_hormone_class),
                endocrineInfo.hormoneClass
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_mechanisms),
                endocrineInfo.mechanisms
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_affected_hormones),
                endocrineInfo.affectedHormones
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_monitoring_labs),
                endocrineInfo.monitoringLabs
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_assay_caveats),
                endocrineInfo.assayCaveats
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_safety_signals),
                endocrineInfo.safetySignals
            )
            ClinicalInfoListRow(
                stringResource(R.string.endocrine_model_roles),
                endocrineInfo.modelRoles
            )
            SourceRefs(sourceRefs = endocrineInfo.sourceRefs)
            VerticalSpace()
        }
    }
}

@Composable
fun TimeCourseSection(timeCourses: List<TimeCourse>) {
    SectionWithTitle(title = stringResource(R.string.time_course_title)) {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = stringResource(R.string.time_course_disclaimer),
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
                    TimeValueRow(stringResource(R.string.time_course_onset), timeCourse.onset)
                    TimeValueRow(stringResource(R.string.time_course_tmax), timeCourse.tmax)
                    TimeValueRow(stringResource(R.string.time_course_peak_effect), timeCourse.peakEffect)
                    TimeValueRow(stringResource(R.string.time_course_duration_of_action), timeCourse.durationOfAction)
                    TimeValueRow(stringResource(R.string.time_course_elimination_half_life), timeCourse.eliminationHalfLife)
                    TimeValueRow(stringResource(R.string.time_course_time_to_steady_state), timeCourse.timeToSteadyState)
                    TimeValueRow(stringResource(R.string.time_course_washout), timeCourse.washout)
                    TimeValueRow(stringResource(R.string.time_course_peak_window), timeCourse.peakWindow)
                    TimeValueRow(stringResource(R.string.time_course_trough_window), timeCourse.troughWindow)
                    if (timeCourse.depotRelease) {
                        BooleanInfoRow(stringResource(R.string.time_course_depot_release))
                    }
                    if (timeCourse.injectionIntervalSensitive) {
                        BooleanInfoRow(
                            stringResource(R.string.time_course_injection_interval_sensitive)
                        )
                    }
                    if (timeCourse.assayTimingSensitive) {
                        BooleanInfoRow(stringResource(R.string.time_course_assay_timing_sensitive))
                    }
                    ClinicalInfoListRow(stringResource(R.string.time_course_notes), timeCourse.notes)
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
fun HrtModelReadinessSection(hrtModelInfo: HrtModelInfo) {
    SectionWithTitle(title = stringResource(R.string.hrt_model_readiness_title)) {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = stringResource(R.string.hrt_model_readiness_disclaimer),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoLabel(stringResource(R.string.hrt_model_compatible))
            Text(
                text = stringResource(
                    if (hrtModelInfo.modelCompatible) R.string.label_yes else R.string.label_no
                )
            )
            VerticalSpace()
            ClinicalInfoListRow(
                stringResource(R.string.hrt_model_roles),
                hrtModelInfo.modelRoles
            )
            ClinicalInfoListRow(
                stringResource(R.string.hrt_primary_modeled_analytes),
                hrtModelInfo.primaryModeledAnalytes
            )
            ClinicalInfoListRow(
                stringResource(R.string.hrt_required_event_fields),
                hrtModelInfo.requiredEventFields
            )
            ClinicalInfoListRow(
                stringResource(R.string.hrt_required_lab_fields),
                hrtModelInfo.requiredLabFields
            )
            ClinicalInfoListRow(
                stringResource(R.string.hrt_model_caveats),
                hrtModelInfo.caveats
            )
            SourceRefs(sourceRefs = hrtModelInfo.sourceRefs)
            VerticalSpace()
        }
    }
}

@Composable
fun TherapeuticDrugMonitoringSection(tdm: TherapeuticDrugMonitoring) {
    SectionWithTitle(title = stringResource(R.string.tdm_title)) {
        Column(Modifier.padding(horizontal = horizontalPadding)) {
            Text(
                text = stringResource(R.string.medical_information_disclaimer),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoLabel(stringResource(R.string.tdm_routine_monitoring))
            Text(text = stringResource(if (tdm.isRoutinelyMonitored) R.string.label_yes else R.string.label_no))
            VerticalSpace()
            ClinicalInfoTextRow(stringResource(R.string.tdm_monitoring_type), tdm.monitoringType)
            ClinicalInfoTextRow(stringResource(R.string.tdm_reason), tdm.reason)
            ClinicalInfoListRow(stringResource(R.string.tdm_analytes), tdm.analytes)
            ClinicalInfoTextRow(stringResource(R.string.tdm_specimen), tdm.specimen)
            ClinicalInfoTextRow(stringResource(R.string.tdm_sampling_time), tdm.samplingTime)
            TherapeuticRangeRows(stringResource(R.string.tdm_therapeutic_ranges), tdm.therapeuticRanges)
            ToxicityThresholdRows(stringResource(R.string.tdm_toxicity_thresholds), tdm.toxicityThresholds)
            ToxicityThresholdRows(stringResource(R.string.tdm_critical_values), tdm.criticalValues)
            ClinicalInfoTextRow(stringResource(R.string.tdm_assay_method), tdm.assayMethod)
            ClinicalInfoListRow(stringResource(R.string.tdm_interpretation_caveats), tdm.interpretationCaveats)
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
private fun BooleanInfoRow(label: String) {
    InfoLabel(label)
    Text(text = stringResource(R.string.label_yes))
    VerticalSpace()
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
        sourceRef.evidenceLevel?.let {
            Text(
                text = stringResource(R.string.source_evidence_level, it),
                style = MaterialTheme.typography.bodySmall
            )
        }
        sourceRef.labelSection?.let {
            Text(
                text = stringResource(R.string.source_label_section, it),
                style = MaterialTheme.typography.bodySmall
            )
        }
        sourceRef.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
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
