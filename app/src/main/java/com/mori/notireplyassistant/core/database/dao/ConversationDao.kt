package com.mori.notireplyassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET pending_count = pending_count + :deltaCount, last_message_preview = :lastPreview, last_timestamp = :lastTimestamp WHERE conversation_id = :conversationId")
    suspend fun updateConversationAfterInsert(conversationId: String, deltaCount: Int, lastPreview: String, lastTimestamp: Long)

    @Query("UPDATE conversations SET pending_count = :count WHERE conversation_id = :conversationId")
    suspend fun updatePendingCount(conversationId: String, count: Int)

    @Query("UPDATE conversations SET is_archived = :isArchived WHERE conversation_id = :conversationId")
    suspend fun updateArchived(conversationId: String, isArchived: Boolean)

    @Query("UPDATE conversations SET is_pinned = :isPinned WHERE conversation_id = :conversationId")
    suspend fun updatePinned(conversationId: String, isPinned: Boolean)

    @Query("SELECT * FROM conversations WHERE is_archived = 0 ORDER BY last_timestamp DESC")
    fun getActiveConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE is_archived = 1 ORDER BY last_timestamp DESC")
    fun getArchivedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
