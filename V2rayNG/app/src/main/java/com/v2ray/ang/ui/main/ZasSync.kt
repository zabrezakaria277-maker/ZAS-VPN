package com.v2ray.ang.ui.main

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.dto.entities.ServersCache
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.URLDecoder
import android.util.Base64

object ZasSync {
    private const val PREFS = "zas_prefs"
    private const val KEY_NAMES = "zas_synced_names"

    var trigger by mutableStateOf(0)
    fun request() {
        trigger++
    }

    private fun linkName(link: String): String {
        val l = link.trim()
        if (l.startsWith("vmess://", true)) {
            try {
                val b64 = l.substring(8).substringBefore('#')
                val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
                return json.optString("ps").trim()
            } catch (e: Exception) {
            }
        }
        val i = l.indexOf('#')
        if (i < 0) return ""
        val raw = l.substring(i + 1)
        val decoded = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }
        return decoded.trim()
    }

    suspend fun run(
        context: Context,
        currentServers: () -> List<ServersCache>,
        onAction: (MainAction) -> Unit
    ) {
        val saved = ZasExpiry.code(context)
        if (saved.isBlank()) return
        when (val r = ZasActivation.activate(context, saved)) {
            is ZasActivationResult.Failure -> {
                if (r.error == "invalid_code" || r.error == "expired" || r.error == "device_limit") {
                    ZasExpiry.expireNow(context)
                }
            }
            is ZasActivationResult.Success -> {
                ZasExpiry.saveFromServer(context, saved, r.remainingMs)
                applyLinks(context, r.links, currentServers, onAction)
            }
        }
    }

    private suspend fun applyLinks(
        context: Context,
        links: List<String>,
        currentServers: () -> List<ServersCache>,
        onAction: (MainAction) -> Unit
    ) {
        if (links.isEmpty()) return
        var waited = 0
        while (currentServers().isEmpty() && waited < 8000) {
            delay(500)
            waited += 500
        }
        val current = currentServers()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val newNames = links.map { linkName(it) }
        val oldNames = prefs.getStringSet(KEY_NAMES, emptySet()) ?: emptySet()
        val have = current.map { it.profile.remarks.trim() }.toSet()

        val toImport = links.filterIndexed { i, _ -> newNames[i] !in have }
        val gone = oldNames - newNames.toSet()

        current.filter { it.profile.remarks.trim() in gone }
            .forEach { onAction(MainAction.RemoveServer(it.guid)) }
        if (toImport.isNotEmpty()) {
            onAction(MainAction.ImportBatchConfig(toImport.joinToString("\n")))
        }
        prefs.edit().putStringSet(KEY_NAMES, newNames.toSet()).apply()
    }
}

@Composable
fun ZasAutoSync(
    mainViewModel: MainViewModel,
    groupId: String,
    onAction: (MainAction) -> Unit
) {
    val context = LocalContext.current
    val servers by mainViewModel.serversForGroup(groupId).collectAsStateWithLifecycle()
    val serversNow by rememberUpdatedState(servers)
    val actionNow by rememberUpdatedState(onAction)

    LaunchedEffect(ZasSync.trigger) {
        if (ZasSync.trigger == 0) delay(2500)
        while (true) {
            ZasSync.run(context, { serversNow }, { actionNow(it) })
            delay(30 * 60 * 1000L)
        }
    }
}
