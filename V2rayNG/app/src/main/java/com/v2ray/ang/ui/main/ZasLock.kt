package com.v2ray.ang.ui.main

import android.content.Context
import android.util.Base64
import android.widget.Toast
import com.v2ray.ang.handler.MmkvManager
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder

object ZasLock {
    const val MARK = "🔒"

    fun isLocked(remarks: String?): Boolean = remarks?.trimStart()?.startsWith(MARK) == true

    fun isLockedGuid(guid: String): Boolean =
        isLocked(MmkvManager.decodeServerConfig(guid)?.remarks)

    fun toast(context: Context) {
        Toast.makeText(context, "Serveur verrouillé : action non autorisée.", Toast.LENGTH_SHORT).show()
    }

    // Ajoute 🔒 devant le nom de chaque lien (utilisé à l'export verrouillé)
    fun markLinks(links: String): String = links.lines().joinToString("\n") { raw ->
        val l = raw.trim()
        if (l.isEmpty()) {
            l
        } else if (l.startsWith("vmess://", true)) {
            try {
                val o = JSONObject(String(Base64.decode(l.substring(8), Base64.DEFAULT)))
                val ps = o.optString("ps")
                if (!isLocked(ps)) o.put("ps", "$MARK $ps")
                "vmess://" + Base64.encodeToString(o.toString().toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                l
            }
        } else {
            val i = l.indexOf('#')
            val base = if (i >= 0) l.substring(0, i) else l
            val tail = if (i >= 0) l.substring(i + 1) else ""
            val name = try {
                URLDecoder.decode(tail, "UTF-8")
            } catch (e: Exception) {
                tail
            }
            if (isLocked(name)) l
            else base + "#" + URLEncoder.encode("$MARK $name", "UTF-8").replace("+", "%20")
        }
    }
}
