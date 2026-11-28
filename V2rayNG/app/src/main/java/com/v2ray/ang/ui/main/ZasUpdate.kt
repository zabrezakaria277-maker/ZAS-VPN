package com.v2ray.ang.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ZAS_RELEASE_API =
    "https://api.github.com/repos/zabrezakaria277-maker/ZAS-VPN/releases/latest"

private class ZasRelease(val build: Long, val url: String)

// versionCode installé = 5 000 000 + 100 * build + code ABI
private fun zasInstalledBuild(context: Context): Long {
    return try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        val vc = PackageInfoCompat.getLongVersionCode(pi)
        if (vc < 5_000_000L) -1L else (vc - 5_000_000L) / 100L
    } catch (e: Exception) {
        -1L
    }
}

private fun zasFetchLatest(): ZasRelease? {
    return try {
        val conn = URL(ZAS_RELEASE_API).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        if (conn.responseCode != 200) return null
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val build = json.getString("tag_name").removePrefix("build-").toLongOrNull()
            ?: return null
        val assets = json.getJSONArray("assets")
        val names = ArrayList<String>()
        val urls = ArrayList<String>()
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.getString("name")
            if (name.endsWith(".apk")) {
                names.add(name)
                urls.add(a.getString("browser_download_url"))
            }
        }
        if (urls.isEmpty()) return null
        var idx = -1
        for (abi in Build.SUPPORTED_ABIS) {
            idx = names.indexOfFirst { it.contains("_$abi") }
            if (idx >= 0) break
        }
        if (idx < 0) idx = names.indexOfFirst { it.contains("universal") }
        if (idx < 0) idx = 0
        ZasRelease(build, urls[idx])
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ZasUpdateCheck() {
    val context = LocalContext.current
    var release by remember { mutableStateOf<ZasRelease?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val installed = zasInstalledBuild(context)
        if (installed <= 0L) return@LaunchedEffect
        val latest = withContext(Dispatchers.IO) { zasFetchLatest() }
        if (latest != null && latest.build > installed) release = latest
    }

    val r = release
    if (r != null && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            containerColor = Color(0xFF1C1C21),
            title = { Text("Mise à jour disponible", color = Color.White) },
            text = {
                Text(
                    "Une nouvelle version de ZAS VPN est disponible. " +
                        "Touchez METTRE À JOUR, téléchargez, puis installez par-dessus. " +
                        "Vos données sont conservées.",
                    color = Color.White,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissed = true
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(r.url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                    }
                }) { Text("METTRE À JOUR") }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("Plus tard") }
            }
        )
    }
}
