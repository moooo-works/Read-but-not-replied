package com.mori.notireplyassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET status = :status WHERE reminder_id = :reminderId")
    suspend fun updateStatus(reminderId: Long, status: String)

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' OR status = 'SNOOZED'")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE reminder_id = :reminderId")
    suspend fun getReminderById(reminderId: Long): ReminderEntity?

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
