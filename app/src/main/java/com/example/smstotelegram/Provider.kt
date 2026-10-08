package com.example.smstotelegram

import java.util.UUID
import com.google.gson.annotations.SerializedName

data class Provider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    // رقم الحساب/المستلم الذي يظهر داخل رسالة SMS
    @SerializedName(value = "recipientNumber", alternate = ["senderPattern"])
    val recipientNumber: String,
    // اسم/رقم المرسل كما يظهر في الرسائل (مثال: VF-Cash) - اختياري
    val senderId: String? = null,
    // طريقة الدفع (نوع الخدمة): محفظة إلكترونية / إنستا باي / تيلدا / بطاقة بنكية / تحويل بنكي
    val method: String = ""
)

/** أنواع طريقة الدفع المتاحة عند إضافة مزود خدمة (نفس مجموعات المنصة). */
val PAYMENT_METHODS = listOf("محفظة إلكترونية", "إنستا باي", "تيلدا", "بطاقة بنكية", "تحويل بنكي")
