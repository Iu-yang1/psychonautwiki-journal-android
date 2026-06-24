/*
 * Copyright (c) 2022. Isaak Hanimann.
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

package com.isaakhanimann.journal

import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestParse {

    @Test
    fun noCrash() {
        val substances = SubstanceParser().parseSubstanceFile(string = "error")
        assertTrue(substances.substances.isEmpty())
    }

    @Test
    fun noCrashExtract() {
        val result = SubstanceParser().extractSubstanceString(string = "error")
        assertTrue(result == null)
    }

    @Test
    fun testExtractSubstancesString() {
        val text = """
{
  "data": {
    "substances": [
      {
        "name": "Armodafinil",
        "roas": [
          {
            "name": "oral"
          }
        ]
      }
    ]
  }
}"""
        val result = SubstanceParser().extractSubstanceString(string = text)
        assertTrue(result == "[{\"name\":\"Armodafinil\",\"roas\":[{\"name\":\"oral\"}]}]")
    }

    @Test
    fun parseTherapeuticDrugMonitoring() {
        val text = """
{
  "categories": [],
  "substances": [
    {
      "name": "Digoxin",
      "commonNames": ["地高辛"],
      "url": "https://example.org",
      "isApproved": true,
      "categories": ["cardiovascular"],
      "tdm": {
        "isRoutinelyMonitored": true,
        "monitoringType": "serum concentration",
        "analytes": ["digoxin"],
        "specimen": "serum",
        "therapeuticRanges": [
          {
            "indication": "heart failure",
            "range": "0.5-0.9",
            "unit": "ng/mL"
          }
        ],
        "toxicityThresholds": [
          {
            "threshold": ">2.0",
            "unit": "ng/mL",
            "note": "interpret with clinical context"
          }
        ]
      },
      "roas": []
    }
  ]
}"""
        val result = SubstanceParser().parseSubstanceFile(string = text)
        val tdm = result.substances.first().tdm
        assertTrue(tdm?.isRoutinelyMonitored == true)
        assertEquals("serum concentration", tdm?.monitoringType)
        assertEquals("digoxin", tdm?.analytes?.first())
        assertEquals("0.5-0.9", tdm?.therapeuticRanges?.first()?.range)
        assertEquals(">2.0", tdm?.toxicityThresholds?.first()?.threshold)
    }

    @Test
    fun parseEndocrineHrtFields() {
        val text = """
{
  "categories": [],
  "substances": [
    {
      "name": "Estradiol Valerate Injection",
      "commonNames": ["EV", "戊酸雌二醇注射剂"],
      "url": "https://example.org",
      "categories": ["endocrine", "hrt-related"],
      "endocrineInfo": {
        "hormoneClass": ["Estrogen"],
        "mechanisms": ["Estrogen receptor agonism"],
        "modelRoles": ["e2-source", "depot-release"]
      },
      "timeCourse": [
        {
          "route": "intramuscular",
          "formulation": "oil depot",
          "depotRelease": true,
          "injectionIntervalSensitive": true,
          "assayTimingSensitive": true,
          "peakWindow": {
            "min": 1,
            "max": 3,
            "unit": "day"
          }
        }
      ],
      "doseUseReferences": [
        {
          "indication": "label context",
          "route": "intramuscular",
          "amountText": "source needed",
          "sourceType": "regulatory-label",
          "evidenceLevel": "REGULATORY_LABEL"
        }
      ],
      "hrtModelInfo": {
        "modelCompatible": true,
        "modelRoles": ["e2-source", "depot-release"],
        "primaryModeledAnalytes": ["Estradiol"]
      },
      "roas": []
    }
  ]
}"""
        val substance = SubstanceParser().parseSubstanceFile(string = text).substances.first()

        assertEquals("Estrogen", substance.endocrineInfo?.hormoneClass?.first())
        assertTrue(substance.timeCourse.first().depotRelease)
        assertTrue(substance.timeCourse.first().injectionIntervalSensitive)
        assertEquals(1.0, substance.timeCourse.first().peakWindow?.min)
        assertEquals("source needed", substance.doseUseReferences.first().amountText)
        assertTrue(substance.hrtModelInfo?.modelCompatible == true)
        assertEquals("Estradiol", substance.hrtModelInfo?.primaryModeledAnalytes?.first())
    }
}
