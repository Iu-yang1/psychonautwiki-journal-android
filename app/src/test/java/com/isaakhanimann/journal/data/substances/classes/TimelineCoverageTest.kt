/*
 * Copyright (c) 2026. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.data.substances.classes

import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCoverageTest {

    private fun allSubstances(): List<Substance> {
        val text = requireNotNull(
            javaClass.classLoader!!.getResourceAsStream("substances.json")
        ).bufferedReader().use { it.readText() }
        return SubstanceParser().parseSubstanceFile(text).substances
    }

    @Test
    fun everyAvailableIngestionRouteHasADrawableTimelineUnlessNotRecommended() {
        val missing = mutableListOf<String>()

        allSubstances().forEach { substance ->
            val routes = substance.availableRoutesForIngestion()
            if (routes.isEmpty()) {
                missing.add("${substance.name}: no ingestion routes")
            }
            routes.forEach { route ->
                val intentionallyUndrawn = substance.timeCourse.any { timeCourse ->
                    route in timeCourse.matchingAdministrationRoutes() &&
                        timeCourse.effectTimelineStatus == EFFECT_TIMELINE_NOT_RECOMMENDED
                }
                val duration = substance.getRoa(route)?.roaDuration
                    ?: substance.derivedRoaDurationForRoute(route)
                if (duration == null && !intentionallyUndrawn) {
                    missing.add("${substance.name}/${route.name}")
                }
            }
        }

        assertTrue(
            "Missing drawable timelines: ${missing.joinToString()}",
            missing.isEmpty()
        )
    }

    @Test
    fun effectiveHalfLifeProducesADrawablePkEstimate() {
        val lisinopril = allSubstances()
            .single { it.name == "Lisinopril" }

        val duration = lisinopril.derivedRoaDurationForRoute(
            AdministrationRoute.ORAL
        )

        assertNotNull(duration)
        assertNotNull(duration?.total)
    }

    @Test
    fun estradiolGelAndOralTabletUseASingleDailyWindow() {
        val substances = allSubstances()
        val gel = substances.single { it.name == "Estradiol Gel" }
        val gelTimeCourse = gel.timeCourse.single()
        assertEquals(8.0, gelTimeCourse.tmax?.min)
        assertEquals(16.0, gelTimeCourse.tmax?.max)
        assertEquals(12.0, gelTimeCourse.timeToSteadyState?.min)
        assertEquals(24.0, gelTimeCourse.effectTimelineWindow?.max)
        assertNull(gelTimeCourse.durationOfAction)
        assertEquals(
            24f,
            requireNotNull(gel.derivedRoaDurationForRoute(AdministrationRoute.TRANSDERMAL)?.total?.max),
            0.01f
        )

        val oral = substances.single { it.name == "Oral Estradiol" }
        val oralTimeCourse = oral.timeCourse.single()
        assertEquals(5.0, oralTimeCourse.tmax?.min)
        assertEquals(8.0, oralTimeCourse.tmax?.max)
        assertEquals(12.0, oralTimeCourse.eliminationHalfLife?.min)
        assertEquals(14.0, oralTimeCourse.eliminationHalfLife?.max)
        assertEquals(24.0, oralTimeCourse.effectTimelineWindow?.max)
        assertNull(oralTimeCourse.durationOfAction)
        assertEquals(
            24f,
            requireNotNull(oral.derivedRoaDurationForRoute(AdministrationRoute.ORAL)?.total?.max),
            0.01f
        )

        listOf("Estradiol", "Oral Estradiol Valerate").forEach { name ->
            val timeCourse = substances.single { it.name == name }.timeCourse.single()
            assertEquals(24.0, timeCourse.effectTimelineWindow?.max)
            assertNull(timeCourse.durationOfAction)
        }
    }

    @Test
    fun sublingualAndBuccalEstradiolDoNotHaveDerivedEffectTimelines() {
        val estradiol = allSubstances().single { it.name == "Sublingual/Buccal Estradiol" }

        assertTrue(
            estradiol.timeCourse.all {
                it.effectTimelineStatus == EFFECT_TIMELINE_NOT_RECOMMENDED
            }
        )
        assertNull(estradiol.derivedRoaDurationForRoute(AdministrationRoute.SUBLINGUAL))
        assertNull(estradiol.derivedRoaDurationForRoute(AdministrationRoute.BUCCAL))
    }

    @Test
    fun peakEffectNeverRepresentsAPlasmaPeak() {
        val forbidden = listOf("pharmacokinetic", "plasma", "concentration", "cmax", "tmax")

        allSubstances().flatMap { it.timeCourse }.forEach { timeCourse ->
            val peakEffect = timeCourse.peakEffect ?: return@forEach
            val text = listOfNotNull(peakEffect.basis, peakEffect.note)
                .joinToString(" ")
                .lowercase()
            assertFalse(
                "Peak effect describes a plasma peak: ${timeCourse.route} $text",
                forbidden.any { it in text }
            )
        }
    }

    @Test
    fun routesAreNotClonedFromAnotherAdministrationRoute() {
        val inferred = allSubstances()
            .flatMap { substance -> substance.timeCourse.map { substance to it } }
            .filter { (_, timeCourse) ->
                timeCourse.effectTimelineNote?.contains(
                    "inferred from the same substance",
                    ignoreCase = true
                ) == true
            }

        assertTrue("Route timelines were cloned: $inferred", inferred.isEmpty())
    }

    @Test
    fun unabsorbedResinsDoNotHaveDerivedEffectTimelines() {
        listOf("Cholestyramine", "Colesevelam").forEach { name ->
            val substance = allSubstances().single { it.name == name }
            assertTrue(
                substance.timeCourse.all {
                    it.effectTimelineStatus == EFFECT_TIMELINE_NOT_RECOMMENDED
                }
            )
            assertNull(substance.derivedRoaDurationForRoute(AdministrationRoute.ORAL))
        }
    }

    @Test
    fun dosingIntervalsUseTimelineWindowsInsteadOfDurations() {
        val substances = allSubstances()
        val expectedWindows = mapOf(
            "Lisinopril" to 24.0,
            "Carvedilol" to 12.0,
            "Estradiol Valerate Injection" to 28.0,
            "Leuprolide Acetate" to 28.0,
            "Penfluridol" to 7.0,
        )

        expectedWindows.forEach { (name, expectedWindow) ->
            val timeCourse = substances.single { it.name == name }.timeCourse.single()
            assertEquals(expectedWindow, timeCourse.effectTimelineWindow?.max)
            assertNull("$name stores a dosing interval as duration", timeCourse.durationOfAction)
        }

        listOf("Ticlopidine", "Vorapaxar", "Amiodarone", "Goserelin", "Histrelin Implant")
            .forEach { name ->
                val timeCourse = substances.single { it.name == name }.timeCourse.first()
                assertEquals(
                    EFFECT_TIMELINE_NOT_RECOMMENDED,
                    timeCourse.effectTimelineStatus
                )
            }
    }

    @Test
    fun continuousInfusionsAreNotDrawnAsSinglePeakCurves() {
        val substances = allSubstances()
        listOf("Sodium Nitroprusside", "Tirofiban").forEach { name ->
            val substance = substances.single { it.name == name }
            val timeCourse = substance.timeCourse.single()
            assertEquals(
                EFFECT_TIMELINE_NOT_RECOMMENDED,
                timeCourse.effectTimelineStatus
            )
            assertNull("$name stores a duration for a continuous infusion", timeCourse.durationOfAction)
            assertNull(
                "$name still produces an intravenous chart",
                substance.derivedRoaDurationForRoute(AdministrationRoute.INTRAVENOUS)
            )
        }
    }

    @Test
    fun formulationAndWearWindowsStayPlotWindows() {
        val substances = allSubstances()

        val telmisartan = substances.single { it.name == "Telmisartan" }
            .timeCourse.single()
        assertEquals(24.0, telmisartan.effectTimelineWindow?.max)
        assertNull("Telmisartan stores the dosing interval as duration", telmisartan.durationOfAction)

        val verapamilEr = substances.single { it.name == "Verapamil" }
            .timeCourse.single {
                it.formulation?.contains("extended-release", ignoreCase = true) == true
            }
        assertEquals(24.0, verapamilEr.effectTimelineWindow?.max)
        assertNull(
            "Verapamil ER stores the formulation coverage as duration",
            verapamilEr.durationOfAction
        )

        val nitroglycerin = substances.single { it.name == "Nitroglycerin" }
        val patch = nitroglycerin.timeCourse.single {
            it.route.equals("TRANSDERMAL", ignoreCase = true)
        }
        assertEquals(14.0, patch.effectTimelineWindow?.max)
        assertNull("Nitroglycerin patch stores wear time as duration", patch.durationOfAction)
        assertEquals(
            14f,
            requireNotNull(
                nitroglycerin.derivedRoaDurationForRoute(AdministrationRoute.TRANSDERMAL)
                    ?.total?.max
            ),
            0.01f
        )
    }
}
