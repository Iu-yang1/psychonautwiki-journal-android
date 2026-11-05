package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.classes.*
import androidx.compose.ui.graphics.Color

fun SubstanceFile.withHrtTaxonomy(): SubstanceFile {
    val groups = listOf("hrt-estrogens", "hrt-antiandrogens", "hrt-progestogens", "hrt-gnrh", "hrt-other")
    return SubstanceFile(categories + groups.filter { group -> categories.none { it.name == group } }.map { Category(it, "HRT", null, Color(0xFF9575CD)) }, substances.map { sub ->
        val classes = sub.endocrineInfo?.hormoneClass.orEmpty().joinToString(" ").lowercase()
        val tags = mutableListOf<String>()
        if ("estrogen" in classes) tags += "hrt-estrogens"
        if ("antiandrogen" in classes || sub.name == "Spironolactone") tags += "hrt-antiandrogens"
        if ("progest" in classes) tags += "hrt-progestogens"
        if ("gnrh" in classes) tags += "hrt-gnrh"
        if (tags.isEmpty() && "hrt-related" in sub.categories) tags += "hrt-other"
        sub.copy(categories = (sub.categories + tags + if (tags.isNotEmpty()) listOf("hrt-related") else emptyList()).distinct())
    })
}
