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

package com.isaakhanimann.journal.ui.tabs.search

fun localizedCategoryDisplayName(categoryName: String): String {
    return when (categoryName) {
        "cardiovascular" -> "心血管"
        "antithrombotic" -> "抗血栓"
        "peripheral-circulation" -> "外周循环"
        "prescription-medicine" -> "处方药"
        "endocrine" -> "内分泌"
        "hrt-related" -> "HRT 相关"
        "atc-b01" -> "B01 抗血栓药"
        "atc-c01" -> "C01 心脏治疗药"
        "atc-c02" -> "C02 抗高血压药"
        "atc-c03" -> "C03 利尿剂"
        "atc-c04" -> "C04 外周血管扩张剂"
        "atc-c05" -> "C05 血管保护药"
        "atc-c07" -> "C07 β受体阻滞剂"
        "atc-c08" -> "C08 钙通道阻滞剂"
        "atc-c09" -> "C09 RAS 药物"
        "atc-c10" -> "C10 调脂药"
        else -> categoryName
    }
}
