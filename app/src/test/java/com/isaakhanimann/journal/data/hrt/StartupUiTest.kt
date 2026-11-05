package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.JournalApp
import com.isaakhanimann.journal.MainActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.isaakhanimann.journal.ui.main.ARE_CONDITIONS_ACCEPTED
import kotlinx.coroutines.runBlocking
import org.junit.Test
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
    @Test fun acceptedProductionApplicationReachesTheHrtChart() {
        val app = compose.activity.application as JournalApp
        val field = app.userPreferences.javaClass.getDeclaredField("dataStore").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val store = field.get(app.userPreferences) as DataStore<Preferences>
        runBlocking { store.edit { it[ARE_CONDITIONS_ACCEPTED] = true } }
        compose.waitUntil(15000) { compose.onAllNodesWithText("激素曲线 · 估算").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("激素曲线 · 估算").assertExists()
    }
}
