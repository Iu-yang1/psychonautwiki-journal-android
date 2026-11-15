package com.isaakhanimann.journal.data.hrt

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.isaakhanimann.journal.ui.tabs.hrt.HormoneSimulationSummary
import com.isaakhanimann.journal.ui.tabs.hrt.HormoneModelEditor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class HormoneChartUiTest {
    @get:Rule val compose = createComposeRule()
    private val catalog get() = HormoneReferenceCatalog.parse(requireNotNull(javaClass.classLoader!!.getResourceAsStream("hrt_reference_models.json")).bufferedReader().use { it.readText() })
    private fun data(excluded: List<HormoneExclusion> = emptyList()) = HormoneChartData(HormoneChartRequest(), 0.0, 24.0, 48.0,
        listOf(HormoneSample(0.0, 0.0), HormoneSample(24.0, 50.0)), listOf(HormoneSample(24.0, 50.0), HormoneSample(48.0, 100.0)),
        listOf(NormalizedLab(HrtLab("lab", null, "E2", 12*3600000L, 367.1, "pmol/L", "<"), "E2", 100.0, "pg/mL")),
        listOf("1"), listOf("plan:p:1"), excluded, setOf(catalog.models.first().id), emptySet())

    @Test fun chartDistinguishesHistoryForecastAndMeasurementAndOpensValues() {
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HormoneSimulationSummary(data(), "pg/mL", catalog, {}, {}) } } }
        compose.onNodeWithText("E2 用药", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("历史模型实线、未来计划虚线、独立化验点；可点按查看时点").assertExists()
        compose.onNodeWithContentDescription("曲线详情").performScrollTo().performClick()
        compose.onNodeWithText("数值表").performClick()
        compose.onNodeWithText("数值表 · pg/mL").assertIsDisplayed()
    }

    @Test fun exclusionCorrectionCallbackAndOriginalRecordNavigationAreAvailable() {
        var edited: Int? = null; var recordsOpened = false
        val chart = data(listOf(HormoneExclusion("12", "Estradiol Gel", 0, "missing_application_site")))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HormoneSimulationSummary(chart, "pg/mL", catalog, { recordsOpened = true }, { edited = it }) } } }
        compose.onNodeWithContentDescription("曲线详情").performScrollTo().performClick()
        compose.onNodeWithText("查看未纳入项（1）").performClick()
        compose.onNodeWithText("需要补充给药部位").assertIsDisplayed()
        compose.onNodeWithText("补全此记录").performClick()
        compose.runOnIdle { assertEquals(12, edited) }
        compose.onNodeWithContentDescription("曲线详情").performScrollTo().performClick()
        compose.onNodeWithText("历史数据适配").performClick()
        compose.runOnIdle { assertTrue(recordsOpened) }
    }

    @Test fun noAdmittedHistoryDoesNotInventASolidZeroCurve() {
        val chart = data().copy(history = emptyList(), forecast = emptyList(), includedActual = emptyList(), includedPlanned = emptyList())
        compose.setContent { MaterialTheme { HormoneSimulationSummary(chart, "pmol/L", catalog, {}, {}) } }
        compose.onNodeWithText("暂无可纳入 E2 模型的历史用药记录").assertIsDisplayed()
        compose.onNodeWithText("E2 用药", substring = true).assertDoesNotExist()
        compose.onNodeWithText("pmol/L", substring = true).assertExists()
    }

    @Test fun antiandrogenIsShownAsRecognizedRatherThanAModelCompletionFailure() {
        val chart = data().copy(recognizedHrt = listOf(HormoneContextRecord(7, "Cyproterone Acetate", "抗雄激素", 12.5, "mg", 0, "non_e2_record")))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HormoneSimulationSummary(chart, "pg/mL", catalog, {}, {}) } } }
        compose.onNodeWithText("已识别 1 条").performScrollTo().performClick()
        compose.onNodeWithText("Cyproterone Acetate").assertIsDisplayed()
        compose.onNodeWithText("已识别 · 抗雄激素").assertIsDisplayed()
        compose.onNodeWithText("补全此记录").assertDoesNotExist()
    }

    @Test fun gelActiveMgHasAVisibleScenarioChartWithoutAProductOrSite() {
        val scenario = GelStudyScenario("reference", "凝胶资料情景", listOf(HormoneSample(0.0, 0.0), HormoneSample(24.0, 80.0)), emptyList(), listOf("1"), emptySet())
        val chart = data().copy(history = emptyList(), forecast = emptyList(), includedActual = emptyList(), gelScenarios = listOf(scenario))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HormoneSimulationSummary(chart, "pg/mL", catalog, {}, {}) } } }
        compose.onNodeWithContentDescription("历史模型实线、未来计划虚线、独立化验点；可点按查看时点").assertExists()
        compose.onNodeWithText("凝胶参考").assertDoesNotExist()
        compose.onNodeWithText("暂无可纳入 E2 模型的历史用药记录").assertDoesNotExist()
    }

    @Test fun testosteroneScenarioIsShownAlongsideE2WithSeparateUnits() {
        val chart = data().copy(testosteroneScenarios = listOf(TestosteroneScenario("e2", "E2",
            listOf(TestosteroneSample(0.0, 500.0), TestosteroneSample(24.0, 400.0)),
            listOf(TestosteroneSample(24.0, 400.0), TestosteroneSample(48.0, 300.0)))))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            HormoneSimulationSummary(chart, "pg/mL", catalog, {}, {})
        } } }
        compose.onNodeWithContentDescription("历史模型实线、未来计划虚线、独立化验点；可点按查看时点").assertExists()
        compose.onNodeWithText("总睾酮 · 参数情景").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ng/dL", substring = false).performClick()
        compose.onNodeWithText("nmol/L", substring = false).assertExists()
        compose.onNodeWithContentDescription("总睾酮情景曲线和独立化验点，可点按查看数值").assertExists()
    }

    @Test fun modelEditorPreservesExactAnchorTimesWhenSavingExistingParameters() {
        var saved: HormoneModelOptions? = null
        val options = HormoneModelOptions(12.0, 12345L, TestosteroneOptions(12345L, 500.0,
            500.0, 20.0, 100.0, 24.0, 168.0))
        compose.setContent { MaterialTheme { HormoneModelEditor(options, emptyList(), { saved = it }, {}) } }
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle { assertEquals(options, saved) }
    }

    @Test fun modelEditorReportsInvalidBaselineWithoutSaving() {
        var saved = false
        compose.setContent { MaterialTheme { HormoneModelEditor(HormoneModelOptions(), emptyList(), { saved = true }, {}) } }
        compose.onNodeWithText("个人 E2 基线（pg/mL）").performTextReplacement("-1")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("E2 基线应为非负数").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertFalse(saved) }
    }

    @Test fun modelEditorPersistsDrugSpecificParametersAfterEditingOtherFields() {
        var saved: HormoneModelOptions? = null
        val config = TestosteroneOptions(12345L, 500.0, 500.0, 20.0, 100.0, 24.0, 168.0,
            AntiandrogenOptions(43.9, 10.0, .9), AntiandrogenOptions(16.5, 100.0, 0.0))
        compose.setContent { MaterialTheme {
            HormoneModelEditor(HormoneModelOptions(testosterone = config), emptyList(), { saved = it }, {})
        } }
        compose.onNodeWithText("CPA 总 T 最大抑制比例（0–1）").performScrollTo().performTextReplacement("0.8")
        compose.onNodeWithText("个人 E2 基线（pg/mL）").performScrollTo().performTextReplacement("12")
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle {
            assertEquals(config.copy(cyproterone = config.cyproterone!!.copy(maximumSuppression = .8)), saved?.testosterone)
            assertEquals(12.0, saved!!.baselineE2PgMl, 0.0)
        }
    }

    @Test fun antiandrogenDetailsShowIncludedAndUnresolvedHistoricalRecords() {
        val chart = data().copy(antiandrogenRecords = listOf(
            AntiandrogenRecord("7", "CPA", -1000, 12.5, null),
            AntiandrogenRecord("8", "螺内酯", 0, null, "需要补充药品规格")))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            HormoneSimulationSummary(chart, "pg/mL", catalog, {}, {})
        } } }
        compose.onNodeWithContentDescription("曲线详情").performScrollTo().performClick()
        compose.onNodeWithText("抗雄记录纳入详情（1/2）").performClick()
        compose.onNodeWithText("已纳入总 T 情景；包含历史残留").assertIsDisplayed()
        compose.onNodeWithText("需要补充药品规格").assertIsDisplayed()
    }
}
