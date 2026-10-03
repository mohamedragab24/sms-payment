package com.example.smstotelegram

import java.util.UUID

/**
 * يمثل "مزود خدمة" - اسم توضيحي + رقم أو نص المرسل اللي هنفلتر بيه الرسائل.
 * مثال: name = "فودافون كاش", senderPattern = "VodafoneCash"
 */
data class Provider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val senderPattern: String
)
