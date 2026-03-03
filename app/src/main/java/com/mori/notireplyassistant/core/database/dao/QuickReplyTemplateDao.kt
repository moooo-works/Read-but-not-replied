package com.mori.notireplyassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mori.notireplyassistant.core.database.entity.QuickReplyTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickReplyTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuickReplyTemplateEntity)

    @Query("SELECT * FROM quick_reply_templates ORDER BY sort_order ASC, usage_count DESC")
    fun getAllTemplates(): Flow<List<QuickReplyTemplateEntity>>

    @Query("DELETE FROM quick_reply_templates")
    suspend fun deleteAllTemplates()
}
