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

package com.isaakhanimann.journal.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.substances.AdministrationRoute

private fun AdministrationRoute.routeNameResId(): Int {
    return when (this) {
        AdministrationRoute.ORAL -> R.string.route_oral
        AdministrationRoute.SUBLINGUAL -> R.string.route_sublingual
        AdministrationRoute.BUCCAL -> R.string.route_buccal
        AdministrationRoute.INSUFFLATED -> R.string.route_insufflated
        AdministrationRoute.RECTAL -> R.string.route_rectal
        AdministrationRoute.TRANSDERMAL -> R.string.route_transdermal
        AdministrationRoute.SUBCUTANEOUS -> R.string.route_subcutaneous
        AdministrationRoute.INTRAMUSCULAR -> R.string.route_intramuscular
        AdministrationRoute.INTRAVENOUS -> R.string.route_intravenous
        AdministrationRoute.SMOKED -> R.string.route_smoked
        AdministrationRoute.INHALED -> R.string.route_inhaled
    }
}

@Composable
fun AdministrationRoute.localizedDisplayText(): String {
    return stringResource(routeNameResId())
}

fun AdministrationRoute.localizedDisplayText(context: Context): String {
    return context.getString(routeNameResId())
}

@Composable
fun AdministrationRoute.localizedDescriptionText(): String {
    return stringResource(
        when (this) {
            AdministrationRoute.ORAL -> R.string.route_desc_oral
            AdministrationRoute.SUBLINGUAL -> R.string.route_desc_sublingual
            AdministrationRoute.BUCCAL -> R.string.route_desc_buccal
            AdministrationRoute.INSUFFLATED -> R.string.route_desc_insufflated
            AdministrationRoute.RECTAL -> R.string.route_desc_rectal
            AdministrationRoute.TRANSDERMAL -> R.string.route_desc_transdermal
            AdministrationRoute.SUBCUTANEOUS -> R.string.route_desc_subcutaneous
            AdministrationRoute.INTRAMUSCULAR -> R.string.route_desc_intramuscular
            AdministrationRoute.INTRAVENOUS -> R.string.route_desc_intravenous
            AdministrationRoute.SMOKED -> R.string.route_desc_smoked
            AdministrationRoute.INHALED -> R.string.route_desc_inhaled
        }
    )
}

@Composable
fun localizedClinicalRouteText(route: String): String {
    return when (route.trim().lowercase()) {
        "oral", "po", "by mouth" -> stringResource(R.string.route_oral)
        "sublingual", "sl" -> stringResource(R.string.route_sublingual)
        "buccal" -> stringResource(R.string.route_buccal)
        "intranasal", "insufflated", "nasal" -> stringResource(R.string.route_insufflated)
        "rectal" -> stringResource(R.string.route_rectal)
        "transdermal", "topical patch" -> stringResource(R.string.route_transdermal)
        "subcutaneous", "sc", "subcut" -> stringResource(R.string.route_subcutaneous)
        "intramuscular", "im" -> stringResource(R.string.route_intramuscular)
        "intravenous", "iv", "iv infusion", "infusion" -> stringResource(R.string.route_intravenous)
        "smoked", "vaporized" -> stringResource(R.string.route_smoked)
        "inhaled" -> stringResource(R.string.route_inhaled)
        else -> route
    }
}
