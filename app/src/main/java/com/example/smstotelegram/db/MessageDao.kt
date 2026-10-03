package com.example.smstotelegram.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class ProviderCount(val providerId: String, val providerName: String, val count: Int)

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE providerId = :providerId ORDER BY timestamp DESC")
    suspend fun getMessagesForProvider(providerId: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessagesCount(): Int

    @Query("SELECT COUNT(DISTINCT sender) FROM messages")
    suspend fun getDistinctSendersCount(): Int

    @Query(
        "SELECT providerId, providerName, COUNT(*) as count FROM messages " +
        "GROUP BY providerId ORDER BY count DESC"
    )
    suspend fun getCountsPerProvider(): List<ProviderCount>
}
