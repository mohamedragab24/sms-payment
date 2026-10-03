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
    private val aliasGroups = listOf(
        listOf("vfcash", "vodafonecash", "vodafone", "فودافونكاش", "فودافون"),
        listOf("etisalatcash", "etisalat", "اتصالاتكاش", "اتصالات"),
        listOf("orangecash", "orange", "اورنجكاش", "اورنج"),
        listOf("wepay", "we", "وي"),
        listOf("instapay", "انستاباي"),
        listOf("fawry", "فوري")
    )

    private fun key(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun groupOf(s: String): Int? {
        val k = key(s)
        if (k.isBlank()) return null
        return aliasGroups.indexOfFirst { g -> g.any { it == k || (it.length > 3 && k.contains(it)) } }
            .takeIf { it >= 0 }
    }

    private fun sameSender(providerName: String, sender: String): Boolean {
        val a = key(providerName); val b = key(sender)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b || (a.length > 3 && b.contains(a)) || (b.length > 3 && a.contains(b))) return true
        val ga = groupOf(providerName); val gb = groupOf(sender)
        return ga != null && ga == gb
    }

    private fun lastDigits(s: String, n: Int = 9): String {
        val d = normalizeDigits(s).filter { it.isDigit() }
        return if (d.length > n) d.takeLast(n) else d
    }

    /** يرجّع المزود المناسب: أولًا بالرقم لو موجود في نص الرسالة، وإلا باسم المرسل (VF-Cash مثلًا). */
    fun findProvider(sender: String, body: String, providers: List<Provider>): Provider? {
        // 1) اسم المرسل المحدد للمزود (كل رسائل المرسل ده تتقرأ)
        providers.firstOrNull {
            val sid = it.senderId?.let(::key).orEmpty()
            sid.isNotBlank() && sid == key(sender)
        }?.let { return it }
        val bodyDigits = normalizeDigits(body).filter { it.isDigit() }
        providers.firstOrNull {
            val d = lastDigits(it.recipientNumber)
            d.length >= 7 && bodyDigits.contains(d)
        }?.let { return it }
        return providers.firstOrNull { sameSender(it.name, sender) }
    }
}
