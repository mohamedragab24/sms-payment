package com.example.smstotelegram

object SmsParser {

    // ---------- تنظيف النص ----------
    fun normalizeDigits(s: String): String = buildString {
        for (c in s) append(
            when (c) {
                in '٠'..'٩' -> '0' + (c - '٠')
                in '۰'..'۹' -> '0' + (c - '۰')
                '٫' -> '.'
                else -> c
            }
        )
    }

    // ---------- المبلغ ----------
    private val noise = listOf(
        // الروابط
        Regex("(?i)https?://\\S+"),
        // الرصيد الحالي (مش المبلغ المستلم): "رصيد حسابك ... الحالي 0.90 جنيه"
        Regex("(?i)(?:رصيد|balance)[^0-9]{0,40}[0-9][0-9,]*(?:\\.[0-9]+)?\\s*(?:EGP|جنيه|جنية|ج)?"),
        // العروض والكاش باك: "وكمان هيجيلك لحد 100ج كاش باك"
        Regex("(?i)(?:وزود فرصك|وكمان هيجيلك|كاش ?باك|cashback).*")
    )

    private val amountPatterns = listOf(
        Regex("(?i)(?:تم استلام|استلمت|استلام|received|deposit|credited|ايداع|إيداع|تحويل مبلغ|مبلغ|amount|amt)[^0-9]{0,40}([0-9][0-9]*(?:\\.[0-9]{1,2})?)"),
        Regex("(?i)([0-9][0-9]*(?:\\.[0-9]{1,2})?)\\s*(?:EGP|جنيه|جنية|ج\\b)"),
        Regex("(?i)(?:EGP|جنيه|جنية)\\s*([0-9][0-9]*(?:\\.[0-9]{1,2})?)")
    )

    fun findAmount(body: String): Double? {
        var text = normalizeDigits(body).replace(",", "")
        for (n in noise) text = n.replace(text, " ")
        for (regex in amountPatterns) {
            val m = regex.find(text) ?: continue
            m.groupValues[1].toDoubleOrNull()?.let { return it }
        }
        return null
    }

    // ---------- تحديد المزود / الحساب ----------
    private fun key(s: String): String = normalizeDigits(s).lowercase()
        .trim()
        .filter { it.isLetterOrDigit() }

    /**
     * قبول الرسالة فقط إذا كان المرسل نفسه مضافًا في إعدادات مزود الخدمة.
     * لا نعتمد على رقم الحساب الموجود داخل نص الرسالة، لأن أي رسالة عادية
     * قد تحتوي على الرقم وتؤدي إلى استقبالها بالخطأ.
     *
     * المطابقة تكون مع:
     * 1) اسم المرسل (senderId) المضاف للمزود، أو
     * 2) اسم مزود الخدمة نفسه إذا لم يوجد senderId مستقل.
     */
    fun findProvider(sender: String, body: String, providers: List<Provider>): Provider? {
        val senderKey = key(sender)
        if (senderKey.isBlank()) return null

        return providers.firstOrNull { provider ->
            val configuredSender = key(provider.senderId.orEmpty())
            val providerName = key(provider.name)

            (configuredSender.isNotBlank() && senderKey == configuredSender) ||
                (providerName.isNotBlank() && senderKey == providerName)
        }
    }

    // ---------- رقم العملية ----------
    fun findTransactionId(body: String): String? {
        val text = normalizeDigits(body)
        val patterns = listOf(
            Regex("(?i)(?:رقم\\s*العملية|رقم\\s*العمليه|transaction\\s*(?:id|no|number)|txn\\s*(?:id|no))\\s*[:#-]?\\s*([A-Za-z0-9_-]+)"),
            Regex("(?i)(?:عملية|عمليه)\\s*(?:رقم|#)\\s*[:#-]?\\s*([A-Za-z0-9_-]+)")
        )
        for (regex in patterns) regex.find(text)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    // ---------- رقم المرسل / من رقم ----------
    fun findFromNumber(body: String): String? {
        val text = normalizeDigits(body)
        val patterns = listOf(
            Regex("(?i)(?:من\\s*(?:رقم|الرقم)|from\\s*(?:number|no|phone))\\s*[:#-]?\\s*(\\+?[0-9][0-9 -]{6,})"),
            Regex("(?i)(?:sender|المرسل)\\s*[:#-]?\\s*(\\+?[0-9][0-9 -]{6,})")
        )
        for (regex in patterns) regex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let { return it.replace(" ", "") }
        return null
    }

}
