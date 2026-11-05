package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.classes.Substance

fun Substance.hrtCategoryLabel(): String = when {
    "hrt-antiandrogens" in categories -> "抗雄激素"
    "hrt-gnrh" in categories -> "GnRH 类"
    "hrt-estrogens" in categories -> "雌激素"
    "hrt-progestogens" in categories -> "孕激素"
    else -> "其他 HRT"
}

/** Explicitly selected product definitions, never inferred from a generic gel name. */
object HrtReferenceProducts {
    const val ESTROGEL_LABEL = "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=762ac371-bb8b-47e7-b48f-e80d452c9dd4"
    fun forMedication(medicationId: String, catalog: HormoneReferenceCatalog): List<MedicationProduct> {
        fun profile(id: String) = catalog.models.singleOrNull { it.id == id && it.acceptsMedication(medicationId) }
        val products = mutableListOf<MedicationProduct>()
        fun add(profileId: String, suffix: String, name: String, unit: String, strength: Double?, release: Double? = null) {
            val reference = profile(profileId) ?: return
            products += MedicationProduct("reference:$profileId:$suffix:$medicationId", medicationId, name, reference.formulationId, unit, strength,
                patchReleaseMcgDay = release, modelProfileId = reference.id,
                sourceUrl = if (profileId == "e2-oral-ev-reference-v1") "https://www.medicines.org.uk/emc/product/1417/smpc" else catalog.sources[reference.sources.first()]?.url)
        }
        add("e2-gel006-reference-v1", "g", "EstroGel 0.06% · 每克含 0.6 mg", "g", 0.6)
        add("e2-gel006-reference-v1", "pump", "EstroGel 0.06% · 每泵含 0.75 mg", "pump", 0.75)
        add("e2-divigel-reference-v1", "g", "Divigel 0.1% · 每克含 1 mg", "g", 1.0)
        add("e2-evamist-1-reference-v1", "spray", "Evamist · 每喷含 1.53 mg", "spray", 1.53)
        add("e2-oral-reference-v1", "mg", "微粒化口服E2 · 按成分mg记录", "mg", 1.0)
        add("e2-sl-reference-v1", "mg", "微粒化舌下E2 · 按成分mg记录", "mg", 1.0)
        add("e2-oral-ev-reference-v1", "tablet", "口服EV · 每片2 mg酯", "tablet", 2.0)
        add("e2-patch-tw-reference-v1", "patch", "每周两次贴片参考 · 每贴50 μg/天", "patch", null, 50.0)
        add("ee-oral-reference-v1", "tablet", "Microgynon研究 · 每片EE成分30 μg", "tablet", 0.03)
        add("e4-oral-reference-v1", "tablet", "NEXTSTELLIS研究 · 每片无水E4成分14.2 mg", "tablet", 14.2)
        add("e4-oral-reference-v1", "hydrate", "E4单水合物 · 每15 mg相当于无水14.2 mg", "mg", 14.2/15.0)
        add("cee-e1-reference-v1", "tablet", "Premarin · 每片CEE混合物0.625 mg", "tablet", 0.625)
        return products
    }
}

fun hormoneAnalyteLabel(id: String): String = when (id) {
    "E2" -> "雌二醇 E2"
    "EE" -> "炔雌醇 EE"
    "E4" -> "雌四醇 E4"
    "E1" -> "雌酮 E1（CEE）"
    "EQUILIN" -> "Equilin（CEE）"
    else -> id
}
