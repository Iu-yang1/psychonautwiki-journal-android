package com.isaakhanimann.journal.data.hrt

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.isaakhanimann.journal.ui.tabs.hrt.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class LabEntryUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun switchingAnalytesDoesNotCopyResultOrUnit() {
        compose.setContent { MaterialTheme {
            var analyte by remember { mutableStateOf("E2") }
            val drafts = remember { mutableStateMapOf<String, LabInputDraft>() }
            Column { LabResultFields(analyte, { analyte = it }, drafts) }
        } }
        compose.onNodeWithText("结果").performTextInput("123")
        compose.onNodeWithText("总睾酮").performClick()
        assertEquals("", compose.onNodeWithText("结果").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        assertEquals("ng/dL", compose.onNodeWithText("报告单位").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("结果").performTextInput("20")
        compose.onNodeWithText("E2").performClick()
        assertEquals("123", compose.onNodeWithText("结果").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }
}
