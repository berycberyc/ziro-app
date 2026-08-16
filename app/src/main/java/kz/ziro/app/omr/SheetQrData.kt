package kz.ziro.app.omr

/**
 * The data encoded into every pass/answer-sheet QR code: just the
 * registration ID — the same number already used for the entry pass on
 * the website. Everything else (student, classroom, variant, test type)
 * is looked up from the database by this ID, so there's nothing to keep
 * in sync between the QR content and the actual data.
 * Format: "ziro-pass|<registrationId>"
 */
object SheetQrData {
    fun encode(registrationId: String): String = "ziro-pass|$registrationId"

    fun decode(raw: String): String? {
        val parts = raw.split("|")
        if (parts.size != 2 || parts[0] != "ziro-pass") return null
        return parts[1]
    }
}
