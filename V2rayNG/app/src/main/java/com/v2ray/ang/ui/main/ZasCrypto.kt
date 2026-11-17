package com.v2ray.ang.ui.main

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class ZasPackage(
    val name: String,
    val expiresAt: Long,
    val locked: Boolean,
    val note: String,
    val links: String
)

object ZasCrypto {
    private const val PREFIX = "ZAS1:"
    // Choisis ta phrase secrète puis ne la change plus jamais : les anciens fichiers deviendraient illisibles.
    private const val SECRET = "zas-vpn-burkina-2026-cle-interne"

    private fun key() = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(SECRET.toByteArray(Charsets.UTF_8)), "AES"
    )

    fun isZas(text: String) = text.trim().startsWith(PREFIX)

    fun seal(name: String, expiresAt: Long, locked: Boolean, note: String, links: String): String {
        val json = JSONObject()
            .put("v", 1).put("name", name).put("exp", expiresAt)
            .put("locked", locked).put("note", note).put("links", links)
            .toString()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val enc = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + enc, Base64.NO_WRAP)
    }

    fun open(text: String): ZasPackage? = try {
        val raw = Base64.decode(text.trim().removePrefix(PREFIX), Base64.DEFAULT)
        val iv = raw.copyOfRange(0, 12)
        val data = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val o = JSONObject(String(cipher.doFinal(data), Charsets.UTF_8))
        ZasPackage(
            o.optString("name"), o.optLong("exp", 0L), o.optBoolean("locked", false),
            o.optString("note"), o.optString("links")
        )
    } catch (e: Exception) {
        null
    }
}
