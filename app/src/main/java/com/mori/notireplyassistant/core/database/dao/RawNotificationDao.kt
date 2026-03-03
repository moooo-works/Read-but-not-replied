package com.mori.notireplyassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mori.notireplyassistant.core.database.entity.RawNotificationEntity

@Dao
interface RawNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawEvent(event: RawNotificationEntity): Long

    @Query("SELECT * FROM raw_notifications WHERE package_name = :packageName ORDER BY post_time DESC")
    suspend fun getEventsByPackage(packageName: String): List<RawNotificationEntity>

    @Query("DELETE FROM raw_notifications")
    suspend fun deleteAllRawNotifications()
}
