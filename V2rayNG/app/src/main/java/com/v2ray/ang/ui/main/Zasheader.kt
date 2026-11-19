package com.v2ray.ang.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.telephony.TelephonyManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val CardBg = Color(0xCC0E1A33)
private val Blue = Color(0xFF2F80FF)
private val Green = Color(0xFF22D3A0)
private val Gold = Color(0xFFFFC107)
private val Muted = Color(0xFF8A96B3)
private val Line = Color(0xFF1B3A7A)

@Composable
fun ZasHeader(
    isRunning: Boolean,
    serverGuid: String?,
    statusText: String,
    onToggle: () -> Unit,
    testCountry: String? = null,
    testIp: String? = null,
    onPing: () -> Unit = {},
) {
    val context = LocalContext.current

    val serverName = remember(serverGuid) {
        serverGuid?.let { MmkvManager.decodeServerConfig(it)?.remarks } ?: "Aucun serveur"
    }

    var seconds by remember { mutableStateOf(0L) }
    LaunchedEffect(isRunning) {
        seconds = 0
        while (isRunning) {
            delay(1000)
            seconds++
        }
    }

    var up by remember { mutableStateOf(0L) }
    var down by remember { mutableStateOf(0L) }
    LaunchedEffect(isRunning) {
        up = 0; down = 0
        if (!isRunning) return@LaunchedEffect
        val uid = android.os.Process.myUid()
        val tx0 = TrafficStats.getUidTxBytes(uid)
        val rx0 = TrafficStats.getUidRxBytes(uid)
        if (tx0 < 0 || rx0 < 0) return@LaunchedEffect
        while (true) {
            delay(1000)
            up = (TrafficStats.getUidTxBytes(uid) - tx0).coerceAtLeast(0)
            down = (TrafficStats.getUidRxBytes(uid) - rx0).coerceAtLeast(0)
        }
    }

    var ip by remember { mutableStateOf("—") }
    var country by remember { mutableStateOf("—") }
    LaunchedEffect(isRunning) {
        ip = "…"; country = "…"
        delay(2000)
        var r: Pair<String, String>? = null
        var tries = 0
        while (r == null && tries < 3) {
            r = withContext(Dispatchers.IO) { fetchIpInfo() }
            if (r == null) delay(2000)
            tries++
        }
        ip = r?.first ?: "—"
        country = r?.second ?: "—"
    }

    val operator = remember(isRunning) {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .networkOperatorName.ifBlank { "—" }
    }
    val netType = remember(isRunning) { networkType(context) }
    var showNet by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerButton(isRunning, onToggle)
        Spacer(Modifier.height(10.dp))
        Text(
            if (isRunning) "CONNECTÉ" else "DÉCONNECTÉ",
            color = if (isRunning) Green else Muted,
            fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp
        )
        Text("Serveur actif : $serverName", color = Color.White, fontSize = 15.sp)
        if (statusText.isNotBlank()) Text(statusText, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBg)
                .border(1.dp, Line, RoundedCornerShape(24.dp)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TEMPS DE CONNEXION", color = Muted, letterSpacing = 3.sp, fontSize = 12.sp)
            Text(formatTime(seconds), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("▲ ENVOYÉ", formatBytes(up), Blue, Modifier.weight(1f))
                StatBox("▼ REÇU", formatBytes(down), Green, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBg)
                .border(1.dp, Blue, RoundedCornerShape(24.dp))
                .clickable { showNet = !showNet }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("INFORMATIONS RÉSEAU", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (showNet) {
                InfoLine("Réseau", operator)
                InfoLine("Type", netType)
                InfoLine("Pays", testCountry ?: country)
                InfoLine("IP", testIp ?: ip)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "MES SERVEURS", color = Color(0xFF29C6E8), fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, fontSize = 13.sp, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PowerButton(connected: Boolean, onClick: () -> Unit) {
    val c by animateColorAsState(if (connected) Green else Blue, label = "power")
    Box(
        Modifier.size(120.dp)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent)),
                    radius = size.minDimension / 1.5f
                )
            }
            .clip(CircleShape).border(5.dp, c, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(60.dp)) {
            val sw = 8.dp.toPx()
            val d = size.width - sw
            val col = Color(0xFFE6ECFF)
            drawArc(
                col, startAngle = -60f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(sw / 2, sw / 2 + size.height * 0.08f), size = Size(d, d),
                style = Stroke(sw, cap = StrokeCap.Round)
            )
            drawLine(col, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * 0.5f), sw, StrokeCap.Round)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, m: Modifier) {
    Column(m.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A1530)).padding(10.dp)) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoLine(k: String, v: String) = Row {
    Text("$k : ", color = Muted, fontSize = 14.sp)
    Text(v, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

private fun formatTime(t: Long) = "%02d : %02d : %02d".format(t / 3600, (t % 3600) / 60, t % 60)

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.1f Ko".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f Mo".format(b / 1048576.0)
    else -> "%.2f Go".format(b / 1073741824.0)
}

@Suppress("DEPRECATION")
private fun networkType(ctx: Context): String {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    for (n in cm.allNetworks) {
        val caps = cm.getNetworkCapabilities(n) ?: continue
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi"
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Données mobiles"
    }
    return "—"
}

private fun fetchIpInfo(): Pair<String, String>? {
    val urls = listOf("https://ipwho.is/", "https://ipapi.co/json/")
    for (u in urls) {
        try {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 6000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val ip = o.optString("ip", "")
            val country = o.optString("country_name", o.optString("country", ""))
            if (ip.isNotBlank()) return Pair(ip, country.ifBlank { "—" })
        } catch (e: Exception) {
        }
    }
    return null
}
