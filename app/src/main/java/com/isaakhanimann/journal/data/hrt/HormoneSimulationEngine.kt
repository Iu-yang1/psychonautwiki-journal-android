package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable enum class HormoneRoute { INJECTION, ORAL, SUBLINGUAL, GEL, PATCH, SPRAY }
@Serializable enum class HormoneKernel { TWO_STAGE, THREE_STAGE, PATCH_DEPOT, PATCH_ZERO_ORDER, DUAL_ABSORPTION, BIPHASIC }

data class HormoneEvent(
    val id: String, val medicationId: String, val route: HormoneRoute, val timeHours: Double,
    val doseMg: Double?, val endHours: Double? = null, val releaseMgHour: Double? = null,
    val profileId: String? = null, val planned: Boolean = false,
)

/** Dose basis is fixed by each reference profile; outputs of different analytes must remain separate. */
data class HormoneParameters(
    val medicationId: String, val route: HormoneRoute, val version: String, val source: String, val reviewed: Boolean,
    val absorptionPerHour: Double, val eliminationPerHour: Double, val responseScale: Double,
    val mucosalFraction: Double? = null, val swallowedAbsorptionPerHour: Double? = null,
    val kernel: HormoneKernel = if (route == HormoneRoute.PATCH) HormoneKernel.PATCH_ZERO_ORDER else HormoneKernel.TWO_STAGE,
    val transferPerHour: Double? = null, val id: String = medicationId,
    val lagHours: Double = 0.0, val fastEliminationPerHour: Double? = null, val fastFraction: Double? = null,
)
data class HormoneSample(val timeHours: Double, val pgMl: Double)
data class SimulationResult(val samples: List<HormoneSample>, val included: List<String>, val excluded: Map<String, String>)

class HormoneSimulationEngine {
    fun simulate(events: List<HormoneEvent>, parameters: List<HormoneParameters>, from: Double, to: Double, now: Double, points: Int = 501, includePlanned: Boolean = false, additionalTimes: List<Double> = emptyList(), checkActive: () -> Unit = {}): SimulationResult {
        require(from.isFinite() && to.isFinite() && now.isFinite() && from < to && points in 2..10000)
        require((to - from).isFinite()) { "Invalid model time span" }
        require(events.map { it.id }.distinct().size == events.size) { "Duplicate model event ID" }
        val excluded = linkedMapOf<String, String>()
        val eligible = events.mapNotNull { event ->
            val matches = parameters.filter { it.medicationId == event.medicationId && it.route == event.route && (event.profileId == null || it.id == event.profileId) && it.reviewed }
            val p = matches.singleOrNull()
            val reason = when {
                !event.timeHours.isFinite() || event.timeHours > now && !(includePlanned && event.planned) || event.planned && !includePlanned -> "future_or_invalid_time"
                matches.size > 1 -> "ambiguous_parameter_set"
                p == null -> "unsupported_parameter_set"
                !valid(p) -> "invalid_parameters"
                event.route == HormoneRoute.PATCH && (event.endHours == null || !event.endHours.isFinite() || event.endHours <= event.timeHours || event.releaseMgHour == null || !event.releaseMgHour.isFinite() || event.releaseMgHour <= 0) -> "incomplete_patch"
                event.route != HormoneRoute.PATCH && (event.doseMg == null || !event.doseMg.isFinite() || event.doseMg <= 0) -> "missing_dose"
                (if (event.route == HormoneRoute.PATCH) event.releaseMgHour else event.doseMg)?.let { !(it * p.responseScale).isFinite() } == true -> "dose_out_of_range"
                else -> null
            }
            if (reason != null) { excluded[event.id] = reason; null } else event to requireNotNull(p)
        }
        if (eligible.isEmpty()) return SimulationResult(emptyList(), emptyList(), excluded)
        val times = ((0 until points).map { from + (to - from) * it / (points - 1) } + additionalTimes.filter { it.isFinite() && it in from..to }).distinct().sorted()
        val samples = times.map { t ->
            checkActive()
            val level = eligible.sumOf { (event, p) -> concentration(t, event, p) }
            check(level.isFinite() && level >= -1e-8) { "Non-finite model output" }
            HormoneSample(t, level.coerceAtLeast(0.0))
        }
        return SimulationResult(samples, eligible.map { it.first.id }, excluded)
    }

    private fun valid(p: HormoneParameters): Boolean {
        if (p.source.isBlank() || p.version.isBlank()) return false
        if (!p.lagHours.isFinite() || p.lagHours < 0) return false
        if (listOf(p.absorptionPerHour, p.eliminationPerHour, p.responseScale).any { !it.isFinite() || it <= 0 }) return false
        if (p.kernel == HormoneKernel.THREE_STAGE && (p.transferPerHour == null || !p.transferPerHour.isFinite() || p.transferPerHour <= 0)) return false
        if (p.kernel == HormoneKernel.DUAL_ABSORPTION && (p.mucosalFraction == null || !p.mucosalFraction.isFinite() || p.mucosalFraction !in 0.0..1.0 || p.swallowedAbsorptionPerHour == null || !p.swallowedAbsorptionPerHour.isFinite() || p.swallowedAbsorptionPerHour <= 0)) return false
        if (p.kernel == HormoneKernel.BIPHASIC && (p.fastEliminationPerHour == null || !p.fastEliminationPerHour.isFinite() || p.fastEliminationPerHour <= 0 || p.fastFraction == null || !p.fastFraction.isFinite() || p.fastFraction !in 0.0..1.0)) return false
        if (p.route == HormoneRoute.PATCH && p.kernel !in setOf(HormoneKernel.PATCH_DEPOT, HormoneKernel.PATCH_ZERO_ORDER)) return false
        if (p.route != HormoneRoute.PATCH && p.kernel in setOf(HormoneKernel.PATCH_DEPOT, HormoneKernel.PATCH_ZERO_ORDER)) return false
        return true
    }

    private fun concentration(t: Double, event: HormoneEvent, p: HormoneParameters): Double {
        val dt = t - event.timeHours - p.lagHours
        if (dt <= 0) return 0.0
        val a = p.absorptionPerHour
        val c = p.eliminationPerHour
        val amount = when (p.kernel) {
            HormoneKernel.PATCH_ZERO_ORDER -> {
                val wear = requireNotNull(event.endHours) - event.timeHours
                requireNotNull(event.releaseMgHour) / c * -expm1(-c * min(dt, wear)) * exp(-c * max(dt - wear, 0.0))
            }
            HormoneKernel.PATCH_DEPOT -> {
                val wear = requireNotNull(event.endHours) - event.timeHours
                requireNotNull(event.releaseMgHour) * bateman(min(dt, wear), a, c) * exp(-c * max(dt - wear, 0.0))
            }
            HormoneKernel.THREE_STAGE -> requireNotNull(event.doseMg) * chainThree(dt, a, requireNotNull(p.transferPerHour), c)
            HormoneKernel.DUAL_ABSORPTION -> requireNotNull(event.doseMg) * (requireNotNull(p.mucosalFraction) * bateman(dt, a, c) + (1 - p.mucosalFraction) * bateman(dt, requireNotNull(p.swallowedAbsorptionPerHour), c))
            HormoneKernel.TWO_STAGE -> requireNotNull(event.doseMg) * bateman(dt, a, c)
            HormoneKernel.BIPHASIC -> requireNotNull(event.doseMg) * (requireNotNull(p.fastFraction) * bateman(dt, a, requireNotNull(p.fastEliminationPerHour)) + (1 - p.fastFraction) * bateman(dt, a, c))
        }
        return amount * p.responseScale
    }

    private fun bateman(t: Double, a: Double, c: Double): Double {
        if (abs(a - c) <= 1e-9 * max(a, c)) return a * t * exp(-a * t)
        val difference = abs(a - c)
        return a * exp(-min(a, c) * t) * -expm1(-difference * t) / difference
    }

    private fun chainThree(t: Double, a: Double, b: Double, c: Double): Double {
        val largest = maxOf(a, b, c)
        check((largest * t).isFinite()) { "Rates or time outside numerical range" }
        val separation = minOf(abs(a-b), abs(a-c), abs(b-c))
        if (separation > largest * 1e-4 && largest * t > 1e-3) {
            return a*b*(exp(-a*t)/((a-b)*(a-c)) - exp(-b*t)/((a-b)*(b-c)) + exp(-c*t)/((a-c)*(b-c)))
        }
        // Scaling/squaring handles equal rates and tiny times without a guessed rate perturbation.
        val squares = max(0.0, ceil(log2(largest * t))).toInt()
        val dt = t / 2.0.pow(squares)
        val q = doubleArrayOf(-a*dt, 0.0, 0.0, a*dt, -b*dt, 0.0, 0.0, b*dt, -c*dt)
        var term = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
        var sum = term.copyOf()
        for (order in 1..40) {
            term = multiply(term, q).map { it / order }.toDoubleArray()
            for (i in sum.indices) sum[i] += term[i]
            if (term.maxOf { abs(it) } < 1e-18) break
        }
        repeat(squares) { sum = multiply(sum, sum) }
        return sum[6]
    }

    private fun multiply(a: DoubleArray, b: DoubleArray): DoubleArray = DoubleArray(9) { index ->
        val row = index / 3; val column = index % 3
        (0..2).sumOf { k -> a[row*3+k] * b[k*3+column] }
    }
}

object LabUnits {
    fun estradiolPgMl(value: Double, unit: String): Double? = when(unit) { "pg/mL" -> value; "pmol/L" -> value / 3.671; else -> null }
    fun testosteroneNgDl(value: Double, unit: String): Double? = when(unit) { "ng/dL" -> value; "nmol/L" -> value / 0.0347; else -> null }
}
