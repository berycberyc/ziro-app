package kz.ziro.app.omr

/**
 * The data encoded into every answer sheet's QR code.
 * Format: "ziro|<testTypeId>|<studentName>|<classroom>|<variant>"
 * Kept as a simple pipe-delimited string (not JSON) so it stays compact
 * and easy to decode quickly on-device.
 */
data class SheetQrData(
    val testTypeId: String,
    val studentName: String,
    val classroom: String,
    val variant: String
) {
    fun encode(): String = "ziro|$testTypeId|$studentName|$classroom|$variant"

    companion object {
        fun decode(raw: String): SheetQrData? {
            val parts = raw.split("|")
            if (parts.size != 5 || parts[0] != "ziro") return null
            return SheetQrData(
                testTypeId = parts[1],
                studentName = parts[2],
                classroom = parts[3],
                variant = parts[4]
            )
        }
    }
}
