package com.isaakhanimann.journal

import android.content.Context
import com.isaakhanimann.journal.data.hrt.*
import kotlinx.coroutines.launch
import android.app.Application
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import com.isaakhanimann.journal.util.LocaleDelegate
import com.isaakhanimann.journal.util.LocaleDelegate.Companion.systemLocale
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class JournalApp : Application() {
    @Inject lateinit var legacyReminders: com.isaakhanimann.journal.notifications.LegacyReminderCoordinator
    @Inject lateinit var databaseStartup: DatabaseStartup

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        DatabaseRecovery.beforeApplicationStarts(base)
    }
    @Inject lateinit var hrtNotifications: com.isaakhanimann.journal.notifications.HrtPlanNotifications

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var substanceRepository: SubstanceRepository

    companion object {
        @JvmStatic
        lateinit var instance: JournalApp
            private set
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        updateLocale()
        substanceRepository.refreshSubstance()
        databaseStartup.start()
        applicationScope.launch { databaseStartup.state.collect { if (it == DatabaseStartupState.Ready) { hrtNotifications.start(); legacyReminders.start() } } }
    }

    fun updateLocale() {
        LocaleDelegate.defaultLocale = getLocale() ?: Locale.getDefault()
        val res = resources
        val config = res.configuration
        config.setLocale(LocaleDelegate.defaultLocale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
    }

    fun getLocale(tag: String): Locale? {
        if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
            return systemLocale
        }
        return Locale.forLanguageTag(tag)
    }

    val languageStateFlow by lazy {
        userPreferences.languageFlow.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    }

    fun getLocale(): Locale? {
        val tag: String = runBlocking { kotlinx.coroutines.withTimeoutOrNull(2000) { languageStateFlow.first { it != null } } }
            ?: "SYSTEM".also { android.util.Log.w("JournalApp", "Locale preferences unavailable before startup recovery") }
        return getLocale(tag)
    }

}

val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
