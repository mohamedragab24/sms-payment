package com.example.smstotelegram

object SmsParser {
    data class Parsed(val recipientNumber: String, val amount: Double)

    private val amountPatterns = listOf(
        Regex("(?i)(?:amount|amt|received|deposit|credited|transaction of|مبلغ|استلمت|تم استلام|ايداع|إيداع|تحويل).*?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
        Regex("(?i)([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:EGP|جنيه|جنية|ج)"),
        Regex("(?i)(?:EGP|جنيه|جنية|ج)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
    )

    private val numberRegex = Regex("(?<!\\d)(?:01[0-9]{9}|20[0-9]{8,10}|[0-9]{7,15})(?!\\d)")

    fun findRecipient(body: String, configured: List<String>): String? {
        val normalized = body.replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5").replace("٦", "6")
            .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        return configured.firstOrNull { wanted ->
            val digits = wanted.filter { it.isDigit() }
            digits.isNotBlank() && normalized.filter { it.isDigit() }.contains(digits)
        } ?: numberRegex.find(normalized)?.value
    }

    fun findAmount(body: String): Double? {
        val normalized = body.replace("٫", ".").replace(",", "")
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5").replace("٦", "6")
            .replace("٧", "7").replace("٨", "8").replace("٩", "9")
        for (regex in amountPatterns) {
            val match = regex.find(normalized) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            raw.toDoubleOrNull()?.let { return it }
        }
        return null
    }
}
