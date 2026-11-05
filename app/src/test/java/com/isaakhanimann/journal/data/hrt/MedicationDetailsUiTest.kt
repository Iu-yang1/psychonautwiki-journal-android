package com.isaakhanimann.journal.data.hrt

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.ui.tabs.search.substance.ClinicalInformationSection
import com.isaakhanimann.journal.ui.tabs.search.substance.MedicationSources
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "zh-rCN-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MedicationDetailsUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun clinicalDetailsHaveShortHeadingsAndSourcesStayCollapsedUntilOpened() {
        val text = requireNotNull(javaClass.classLoader!!.getResourceAsStream("substances.json")).bufferedReader().use { it.readText() }
        val substance = SubstanceParser().parseSubstanceFile(text).substances.single { it.name == "Bicalutamide" }
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ClinicalInformationSection(requireNotNull(substance.clinicalInfo))
            MedicationSources(substance)
        } } }
        compose.onNodeWithText("用途").assertExists()
        compose.onNodeWithText("注意事项").assertExists()
        compose.onNodeWithText("相互作用").assertExists()
        compose.onNodeWithText("监测").assertExists()
        compose.onNodeWithText("HRT 模型").assertDoesNotExist()
        compose.onNodeWithText("REGULATORY_LABEL", substring = true).assertDoesNotExist()
        val sourceTitle = substance.clinicalInfo!!.sourceRefs.first().title
        compose.onNodeWithText(sourceTitle).assertDoesNotExist()
        compose.onNodeWithText("来源").performScrollTo().performClick()
        compose.onNodeWithText(sourceTitle).assertExists()
    }
}
