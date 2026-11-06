package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.JournalApp
import com.isaakhanimann.journal.MainActivity
import com.isaakhanimann.journal.R
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.isaakhanimann.journal.ui.main.ARE_CONDITIONS_ACCEPTED
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = JournalApp::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class StartupUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun acceptConditions() {
        val app = compose.activity.application as JournalApp
        val field = app.userPreferences.javaClass.getDeclaredField("dataStore").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val store = field.get(app.userPreferences) as DataStore<Preferences>
        runBlocking { store.edit { it[ARE_CONDITIONS_ACCEPTED] = true } }
    }

    @Test fun acceptedProductionApplicationReachesTheHrtChart() {
        acceptConditions()
        compose.waitUntil(15000) { compose.onAllNodesWithText("激素曲线 · 估算").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("激素曲线 · 估算").assertExists()
        val labels = listOf("HRT", R.string.stats, R.string.journal, R.string.drugs, R.string.settings)
            .map { if (it is Int) compose.activity.getString(it) else it.toString() }
        val labelBounds = labels.map { label ->
            compose.onAllNodesWithText(label).fetchSemanticsNodes().maxBy { it.boundsInRoot.top }.boundsInRoot
        }
        labelBounds.forEach { assertEquals(labelBounds.first().center.y, it.center.y, 1f) }
        val iconBounds = labels.map { label ->
            compose.onAllNodesWithContentDescription(label, useUnmergedTree = true)
                .fetchSemanticsNodes().maxBy { it.boundsInRoot.top }.boundsInRoot
        }
        iconBounds.forEach { assertEquals(iconBounds.first().center.y, it.center.y, 1f) }
    }

    @Test fun medicationDomainsFilterTheListAndSurviveTabSwitches() {
        acceptConditions()
        compose.waitUntil(15000) { compose.onAllNodesWithText("激素曲线 · 估算").fetchSemanticsNodes().isNotEmpty() }
        val drugs = compose.activity.getString(R.string.drugs)
        compose.onNodeWithText(drugs).performClick()
        val hrtDomain = hasText("HRT and endocrine") or hasText("HRT 与内分泌")
        compose.onNode(hrtDomain).performClick().assertIsSelected()
        compose.onNode(hasSetTextAction()).performTextInput("Bicalutamide")
        val medicationRow = (hasText("Bicalutamide") or hasText("比卡鲁胺")) and !hasSetTextAction()
        compose.waitUntil(15000) { compose.onAllNodes(medicationRow).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.stats)).performClick()
        compose.onNodeWithText(drugs).performClick()
        compose.onNode(hrtDomain).assertIsSelected()
        compose.onNode(hasSetTextAction()).assertTextContains("Bicalutamide")
        compose.onNodeWithText(compose.activity.getString(R.string.medication_filter_count, 0)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.medication_category)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.medication_custom_only)).assertExists()
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    @Test fun filterButtonTogglesPanelAndKeepsSelection() {
        acceptConditions()
        compose.waitUntil(15000) { compose.onAllNodesWithText("激素曲线 · 估算").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.drugs)).performClick()

        val filterButton = compose.onNodeWithContentDescription("Filter")
        val customOnly = compose.activity.getString(R.string.medication_custom_only)
        val medicationCategory = compose.activity.getString(R.string.medication_category)

        filterButton.performClick()
        compose.onNodeWithText(customOnly).assertExists()
        compose.onNodeWithText(customOnly).performClick()
        compose.onNodeWithText(medicationCategory).assertDoesNotExist()

        filterButton.performClick()
        compose.onNodeWithText(customOnly).assertDoesNotExist()

        filterButton.performClick()
        compose.onNodeWithText(customOnly).assertExists()
        compose.onNodeWithText(medicationCategory).assertDoesNotExist()
        compose.onNodeWithText(customOnly).performClick()
    }
}
