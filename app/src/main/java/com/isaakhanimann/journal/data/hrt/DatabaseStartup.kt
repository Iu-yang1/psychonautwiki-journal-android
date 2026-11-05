package com.isaakhanimann.journal.data.hrt

import android.content.Context
import android.util.Log
import com.isaakhanimann.journal.applicationScope
import com.isaakhanimann.journal.data.room.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DatabaseStartupState {
    data object Checking : DatabaseStartupState
    data object Ready : DatabaseStartupState
    data class Failed(val message: String) : DatabaseStartupState
}

@Singleton
class DatabaseStartup @Inject constructor(@param:ApplicationContext private val context: Context, private val database: AppDatabase) {
    private val mutable = MutableStateFlow<DatabaseStartupState>(DatabaseStartupState.Checking)
    val state = mutable.asStateFlow()
    @Volatile private var running = false

    @Synchronized fun start(retry: Boolean = false) {
        if (running || mutable.value == DatabaseStartupState.Ready) return
        running = true
        mutable.value = DatabaseStartupState.Checking
        applicationScope.launch(Dispatchers.IO) {
            try {
                if (retry) DatabaseRecovery.retryExisting(context)
                DatabaseRecovery.bootFailure?.let { error(it) }
                DatabaseRecovery.archive(context).failureMessage()?.let { error(it) }
                check(!DatabaseRecovery.archive(context).hasPendingWork()) { "恢复已安排，请关闭后重新打开应用" }
                DatabaseRecovery.importLegacySnapshots(context)
                UpgradeSnapshot.prepare(context)
                database.openHelper.writableDatabase
                LegacyMigrationData.publish(database.openHelper.writableDatabase)
                JournalImportService.cleanupUnreferenced(context, database)
                mutable.value = DatabaseStartupState.Ready
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Log.e("DatabaseStartup", "Cannot open journal database", exception)
                mutable.value = DatabaseStartupState.Failed(exception.message ?: "数据库升级失败")
            } finally { running = false }
        }
    }

    suspend fun awaitReady(): Boolean = state.first { it != DatabaseStartupState.Checking } == DatabaseStartupState.Ready
}
