package com.example.smstotelegram.db

data class ProviderCount(
    val providerId: String, val providerName: String, val accountCount: Int,
    val totalAmount: Double, val transactionCount: Int
)

data class AccountStat(
    val providerId: String, val providerName: String, val recipientNumber: String,
    val totalAmount: Double, val transactionCount: Int
)

@androidx.room.Dao
interface MessageDao {
    @androidx.room.Insert
    suspend fun insert(message: MessageEntity)

    @androidx.room.Query("SELECT * FROM messages WHERE providerId = :providerId ORDER BY timestamp DESC")
    suspend fun getMessagesForProvider(providerId: String): List<MessageEntity>

    @androidx.room.Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessagesCount(): Int

    @androidx.room.Query("SELECT COUNT(DISTINCT recipientNumber) FROM messages WHERE recipientNumber != ''")
    suspend fun getDistinctAccountsCount(): Int

    @androidx.room.Query("SELECT providerId, providerName, COUNT(DISTINCT recipientNumber) as accountCount, COALESCE(SUM(amount),0) as totalAmount, COUNT(*) as transactionCount FROM messages WHERE recipientNumber != '' GROUP BY providerId, providerName ORDER BY accountCount DESC")
    suspend fun getProviderStats(): List<ProviderCount>

    @androidx.room.Query("SELECT providerId, providerName, recipientNumber, COALESCE(SUM(amount),0) as totalAmount, COUNT(*) as transactionCount FROM messages WHERE recipientNumber != '' GROUP BY providerId, providerName, recipientNumber ORDER BY providerName, recipientNumber")
    suspend fun getAccountStats(): List<AccountStat>

    @androidx.room.Query("SELECT providerId, providerName, recipientNumber, COALESCE(SUM(amount),0) as totalAmount, COUNT(*) as transactionCount FROM messages WHERE providerId = :providerId AND recipientNumber != '' GROUP BY providerId, providerName, recipientNumber ORDER BY recipientNumber")
    suspend fun getAccountStatsForProvider(providerId: String): List<AccountStat>
}
