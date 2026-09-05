package com.saavn.music.data.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object DesCrypto {
    private const val DES_KEY = "38346591"

    fun decryptMediaUrl(encryptedUrl: String?): String? {
        if (encryptedUrl.isNullOrBlank()) return null
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getStreamUrl(decryptedUrl: String?, bitrate: String = "320"): String? {
        if (decryptedUrl.isNullOrBlank()) return null
        val targetBitrate = when (bitrate) {
            "320", "320kbps" -> "_320"
            "160", "160kbps" -> "_160"
            "96", "96kbps" -> "_96"
            "48", "48kbps" -> "_48"
            else -> "_320"
        }
        return decryptedUrl.replace(Regex("_(?:96|160|320|48|12)"), targetBitrate)
    }
}
