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

import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCoverageTest {

    @Test
    fun everyAvailableIngestionRouteHasADrawableTimeline() {
        val text = requireNotNull(
            javaClass.classLoader!!.getResourceAsStream("substances.json")
        ).bufferedReader().use { it.readText() }
        val substances = SubstanceParser().parseSubstanceFile(text).substances
        val missing = mutableListOf<String>()

        substances.forEach { substance ->
            val routes = substance.availableRoutesForIngestion()
            if (routes.isEmpty()) {
                missing.add("${substance.name}: no ingestion routes")
            }
            routes.forEach { route ->
                val duration = substance.getRoa(route)?.roaDuration
                    ?: substance.derivedRoaDurationForRoute(route)
                if (duration == null) {
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
        val text = requireNotNull(
            javaClass.classLoader!!.getResourceAsStream("substances.json")
        ).bufferedReader().use { it.readText() }
        val lisinopril = SubstanceParser().parseSubstanceFile(text).substances
            .single { it.name == "Lisinopril" }

        val duration = lisinopril.derivedRoaDurationForRoute(
            com.isaakhanimann.journal.data.substances.AdministrationRoute.ORAL
        )

        assertNotNull(duration)
        assertNotNull(duration?.total)
    }
}
