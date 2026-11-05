package com.isaakhanimann.journal.data.hrt

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "medication_links", foreignKeys = [ForeignKey(entity = com.isaakhanimann.journal.data.room.experiences.entities.Ingestion::class, parentColumns = ["id"], childColumns = ["ingestionId"], onDelete = ForeignKey.CASCADE)])
data class MedicationLink(@PrimaryKey val ingestionId: Int, val medicationId: String?, val status: String, val originalName: String, val origin: String = "automatic")

@Serializable
@Entity(tableName = "hrt_plans")
data class HrtPlan(@PrimaryKey val id: String, val medicationId: String, val name: String, val consumer: String?, val route: String, val dose: Double, val unit: String, val start: Long, val intervalHours: Int, val paused: Boolean = false)

@Serializable
@Entity(tableName = "hrt_stock")
data class HrtStock(@PrimaryKey val id: String, val medicationId: String, val name: String, val unit: String, val expires: Long? = null)

@Serializable
@Entity(tableName = "hrt_stock_movements", indices = [Index(value = ["eventKey"], unique = true)])
data class StockMovement(@PrimaryKey val id: String, val stockId: String, val amount: Double, val eventKey: String, val reason: String, val time: Long)

@Serializable
@Entity(tableName = "hrt_labs")
data class HrtLab(@PrimaryKey val id: String, val consumer: String?, val analyte: String, val time: Long, val value: Double, val unit: String, val comparison: String = "=", val method: String = "", val referenceRange: String = "")

@Serializable
@Entity(tableName = "hrt_event_details", foreignKeys = [ForeignKey(entity = com.isaakhanimann.journal.data.room.experiences.entities.Ingestion::class, parentColumns = ["id"], childColumns = ["ingestionId"], onDelete = ForeignKey.CASCADE)])
data class HrtEventDetail(@PrimaryKey val ingestionId: Int, val formulation: String? = null, val strengthMg: Double? = null, val site: String? = null, val patchRateMcgDay: Double? = null, val patchRemovedAt: Long? = null, val doseBasis: String? = null)

@Serializable
@Entity(tableName = "hrt_model_settings")
data class HrtModelSetting(@PrimaryKey val id: String, val payload: String)

@Serializable
data class HrtBackup(val plans: List<HrtPlan> = emptyList(), val stock: List<HrtStock> = emptyList(), val movements: List<StockMovement> = emptyList(), val labs: List<HrtLab> = emptyList(), val settings: List<HrtModelSetting> = emptyList())

@Dao
interface HrtDao {
    @Query("SELECT * FROM Ingestion WHERE id = :id") suspend fun event(id: Int): com.isaakhanimann.journal.data.room.experiences.entities.Ingestion?
    @Query("SELECT * FROM hrt_plans WHERE id = :id") suspend fun plan(id: String): HrtPlan?
    @Query("SELECT * FROM hrt_model_settings WHERE id = :id") suspend fun setting(id: String): HrtModelSetting?
    @Query("DELETE FROM hrt_model_settings WHERE id = :id") suspend fun deleteSetting(id: String)
    @Query("SELECT * FROM medication_links") fun links(): Flow<List<MedicationLink>>
    @Query("SELECT * FROM hrt_plans") fun plans(): Flow<List<HrtPlan>>
    @Query("SELECT * FROM hrt_stock") fun stock(): Flow<List<HrtStock>>
    @Query("SELECT * FROM hrt_stock_movements") fun movements(): Flow<List<StockMovement>>
    @Query("SELECT * FROM hrt_labs ORDER BY time DESC") fun labs(): Flow<List<HrtLab>>
    @Query("SELECT * FROM hrt_event_details") fun details(): Flow<List<HrtEventDetail>>
    @Query("SELECT * FROM hrt_model_settings") fun settings(): Flow<List<HrtModelSetting>>
    @Upsert suspend fun save(link: MedicationLink)
    @Upsert suspend fun save(plan: HrtPlan)
    @Upsert suspend fun save(stock: HrtStock)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun save(movement: StockMovement)
    @Upsert suspend fun save(lab: HrtLab)
    @Delete suspend fun delete(lab: HrtLab)
    @Upsert suspend fun save(detail: HrtEventDetail)
    @Upsert suspend fun save(setting: HrtModelSetting)
    @Query("SELECT * FROM medication_links WHERE ingestionId = :id") suspend fun link(id: Int): MedicationLink?
    @Query("SELECT * FROM hrt_event_details WHERE ingestionId = :id") suspend fun detail(id: Int): HrtEventDetail?
    @Query("DELETE FROM hrt_event_details WHERE ingestionId = :id") suspend fun deleteDetail(id: Int)
    @Query("DELETE FROM medication_links WHERE ingestionId = :id") suspend fun unlink(id: Int)
    @Query("SELECT * FROM hrt_plans") suspend fun exportPlans(): List<HrtPlan>
    @Query("SELECT * FROM hrt_stock") suspend fun exportStock(): List<HrtStock>
    @Query("SELECT * FROM hrt_stock_movements") suspend fun exportMovements(): List<StockMovement>
    @Query("SELECT * FROM hrt_labs") suspend fun exportLabs(): List<HrtLab>
    @Query("SELECT * FROM hrt_model_settings") suspend fun exportSettings(): List<HrtModelSetting>
    @Query("DELETE FROM hrt_plans") suspend fun clearPlans()
    @Query("DELETE FROM hrt_stock") suspend fun clearStock()
    @Query("DELETE FROM hrt_stock_movements") suspend fun clearMovements()
    @Query("DELETE FROM hrt_labs") suspend fun clearLabs()
    @Query("DELETE FROM hrt_model_settings") suspend fun clearSettings()
}
