package com.isaakhanimann.journal.data.hrt

object ImagePayload {
    /** Naming is separate from validation: callers must successfully decode the complete image first. */
    fun extension(bytes: ByteArray, mimeType: String?): String {
        if (bytes.size >= 8 && bytes.take(8).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 13, 10, 26, 10))) return "png"
        if (bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()) return "jpg"
        if (bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP") return "webp"
        return when (mimeType) {
            "image/gif" -> "gif"
            "image/bmp", "image/x-ms-bmp" -> "bmp"
            "image/heif" -> "heif"
            "image/heic" -> "heic"
            "image/avif" -> "avif"
            // Validated images with no reported MIME retain a neutral internal extension.
            else -> "image"
        }
    }
}
