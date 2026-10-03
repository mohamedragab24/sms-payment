package com.example.smstotelegram.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: String,
    val providerName: String,
    val sender: String,
    val recipientNumber: String,
    val amount: Double,
    val body: String,
    val timestamp: Long
)
