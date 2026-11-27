package com.v2ray.ang.ui.main

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

sealed class ZasActivationResult {
    data class Success(
        val remainingMs: Long,
        val maxDevices: Int,
        val devicesUsed: Int,
        val links: List<String>
    ) : ZasActivationResult()

    data class Failure(val error: String, val maxDevices: Int = 0) : ZasActivationResult()
}

object ZasActivation {
    const val ACTIVATE_URL = "https://zas-vpn-server-atlas.lovable.app/api/public/activate"
    // À REMPLACER par ton numéro d'assistance WhatsApp
    const val SUPPORT_URL = "https://wa.me/22677200109"

    fun deviceId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        if (!androidId.isNullOrBlank()) return androidId
        val p = context.getSharedPreferences("zas_prefs", Context.MODE_PRIVATE)
        return p.getString("zas_device_id", null)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().also { p.edit().putString("zas_device_id", it).apply() }
    }

    suspend fun activate(context: Context, rawCode: String): ZasActivationResult =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("code", rawCode.trim())
                    .put("device_id", deviceId(context))
                    .put("device_name", Build.MODEL ?: "Android")
                    .toString()
                val c = URL(ACTIVATE_URL).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 12000
                c.readTimeout = 12000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("User-Agent", "ZAS-VPN")
                c.outputStream.use { it.write(body.toByteArray()) }
                val status = c.responseCode
                val stream = if (status in 200..299) c.inputStream else c.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json = JSONObject(text)
                if (json.optBoolean("ok", false)) {
                    val arr = json.optJSONArray("links")
                    val links = ArrayList<String>()
                    if (arr != null) for (i in 0 until arr.length()) links.add(arr.getString(i))
                    ZasActivationResult.Success(
                        remainingMs = json.optLong("remaining_seconds", 0L) * 1000L,
                        maxDevices = json.optInt("max_devices", 1),
                        devicesUsed = json.optInt("devices_used", 1),
                        links = links
                    )
                } else {
                    ZasActivationResult.Failure(
                        json.optString("error", "server_error"),
                        json.optInt("max_devices", 0)
                    )
                }
            } catch (e: Exception) {
                ZasActivationResult.Failure("network")
            }
        }

    fun message(f: ZasActivationResult.Failure): String = when (f.error) {
        "invalid_code", "invalid_request" -> "Code invalide. Vérifiez-le ou contactez le support."
        "expired" -> "Ce code a expiré."
        "device_limit" -> "Ce code est déjà utilisé sur le nombre maximum d'appareils (${f.maxDevices})."
        "too_many_attempts" -> "Trop de tentatives. Réessayez dans quelques minutes."
        "network" -> "Connexion impossible. Vérifiez votre Internet."
        else -> "Erreur du serveur. Réessayez plus tard."
    }
}
