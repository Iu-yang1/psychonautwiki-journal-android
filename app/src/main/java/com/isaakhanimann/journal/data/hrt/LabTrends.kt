package com.isaakhanimann.journal.data.hrt

import java.util.Locale

data class NormalizedLab(val original: HrtLab, val analyte: String, val value: Double, val unit: String)

object LabTrends {
    // Laboratory mass/molar conversions: https://backend.mayocliniclabs.com/order-tests/si-unit-conversion.html
    val analytes = listOf("E2", "总睾酮", "孕酮", "泌乳素", "LH", "FSH", "SHBG")

    fun analyteKey(name: String): String = when (name.trim().lowercase(Locale.ROOT)) {
        "e2", "estradiol", "雌二醇" -> "E2"
        "ee", "ethinylestradiol", "炔雌醇" -> "EE"
        "e4", "estetrol", "雌四醇" -> "E4"
        "e1", "estrone", "雌酮" -> "E1"
        "equilin", "马烯雌酮" -> "EQUILIN"
        "t", "testosterone", "total testosterone", "睾酮", "总睾酮" -> "总睾酮"
        "p4", "progesterone", "孕酮" -> "孕酮"
        "prl", "prolactin", "泌乳素" -> "泌乳素"
        "lh" -> "LH"
        "fsh" -> "FSH"
        "shbg" -> "SHBG"
        else -> name.trim()
    }

    fun validate(lab: HrtLab) {
        require(lab.id.isNotBlank() && lab.analyte.isNotBlank() && lab.unit.isNotBlank()) { "指标和单位不能为空" }
        require(lab.value.isFinite() && lab.value >= 0) { "化验结果无效" }
        require(lab.comparison in setOf("=", "<", ">", "<=", ">=")) { "比较符无效" }
    }

    fun normalize(lab: HrtLab): NormalizedLab {
        validate(lab)
        val analyte = analyteKey(lab.analyte)
        val key = lab.unit.trim().lowercase(Locale.ROOT).replace("μ", "u").replace("µ", "u")
        val normalized = when (analyte) {
            "EE", "E4", "E1", "EQUILIN" -> when (key) { "pg/ml", "ng/l" -> lab.value to "pg/mL"; "ng/ml", "ug/l" -> lab.value * 1000 to "pg/mL"; else -> null }
            "E2" -> when (key) { "pg/ml", "ng/l" -> lab.value to "pg/mL"; "pmol/l" -> lab.value / 3.671 to "pg/mL"; else -> null }
            "总睾酮" -> when (key) { "ng/dl" -> lab.value to "ng/dL"; "nmol/l" -> lab.value / 0.0347 to "ng/dL"; "ng/ml" -> lab.value * 100 to "ng/dL"; else -> null }
            "孕酮" -> when (key) { "ng/ml", "ug/l" -> lab.value to "ng/mL"; "nmol/l" -> lab.value / 3.18 to "ng/mL"; else -> null }
            "泌乳素" -> when (key) { "ng/ml", "ug/l" -> lab.value to "ng/mL"; else -> null }
            "LH", "FSH" -> when (key) { "miu/ml", "iu/l" -> lab.value to "IU/L"; else -> null }
            "SHBG" -> if (key == "nmol/l") lab.value to "nmol/L" else null
            else -> null
        }
        // Assay-dependent and unknown units remain separate series.
        val converted = normalized?.first ?: lab.value
        require(converted.isFinite()) { "化验换算结果超出范围" }
        return NormalizedLab(lab, analyte, converted, normalized?.second ?: lab.unit.trim())
    }

    fun series(labs: List<HrtLab>, consumer: String?): Map<Pair<String, String>, List<NormalizedLab>> =
        labs.filter { it.consumer == consumer }.map(::normalize).groupBy { it.analyte to it.unit }
            .mapValues { (_, points) -> points.sortedBy { it.original.time } }
}
