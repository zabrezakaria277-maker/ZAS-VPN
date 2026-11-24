package com.v2ray.ang.ui.main

import android.net.TrafficStats
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.delay

private val CardBg = Color(0xCC0E1A33)
private val Blue = Color(0xFF2F80FF)
private val Green = Color(0xFF22D3A0)
private val Red = Color(0xFFFF5252)
private val Muted = Color(0xFF8A96B3)
private val Line = Color(0xFF1B3A7A)

@Composable
fun ZasHeader(
    isRunning: Boolean,
    serverGuid: String?,
    statusText: String,
    onToggle: () -> Unit,
    onPing: () -> Unit = {},
    onExpired: () -> Unit = {},
    testCountry: String? = null,
    testIp: String? = null,
) {
    val context = LocalContext.current

    val serverName = remember(serverGuid) {
        serverGuid?.let { MmkvManager.decodeServerConfig(it)?.remarks } ?: "Aucun serveur"
    }

    var expiresAt by remember { mutableStateOf(ZasExpiry.get(context)) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            expiresAt = ZasExpiry.get(context)
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val unlimited = expiresAt <= 0L
    val remaining = if (unlimited) 0L else (expiresAt - now).coerceAtLeast(0L)
    val expired = !unlimited && remaining == 0L

    val expiredAction by rememberUpdatedState(onExpired)
    LaunchedEffect(isRunning, expired) {
        if (isRunning && expired) expiredAction()
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

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerButton(isRunning, onToggle)
        Spacer(Modifier.height(6.dp))
        Text(
            if (isRunning) "CONNECTÉ" else "DÉCONNECTÉ",
            color = if (isRunning) Green else Muted,
            fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
        )
        Text(
            "Serveur actif : $serverName", color = Color.White, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (statusText.isNotBlank()) Text(
            statusText, color = Muted, fontSize = 11.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBg)
                .border(1.dp, Line, RoundedCornerShape(22.dp)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TEMPS RESTANT", color = Muted, letterSpacing = 3.sp, fontSize = 11.sp)
            Text(
                if (unlimited) "ILLIMITÉ" else formatRemaining(remaining),
                color = if (expired) Red else Color.White,
                fontSize = if (unlimited) 26.sp else 32.sp,
                fontWeight = FontWeight.Light
            )
            if (expired) Text(
                "Abonnement expiré. Contactez le support.", color = Red, fontSize = 12.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("▲ ENVOYÉ", formatBytes(up), Blue, Modifier.weight(1f))
                StatBox("▼ REÇU", formatBytes(down), Green, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))
        ZasAutoPingCard(isRunning = isRunning, onPing = onPing)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun PowerButton(connected: Boolean, onClick: () -> Unit) {
    val c by animateColorAsState(if (connected) Green else Blue, label = "power")
    Box(
        Modifier.size(96.dp)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent)),
                    radius = size.minDimension / 1.5f
                )
            }
            .clip(CircleShape).border(5.dp, c, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(44.dp)) {
            val sw = 6.dp.toPx()
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
    Column(m.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0A1530)).padding(8.dp)) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatRemaining(ms: Long): String {
    val s = ms / 1000
    val d = s / 86400
    val h = (s % 86400) / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (d > 0) "%dj %02d:%02d:%02d".format(d, h, m, sec)
    else "%02d:%02d:%02d".format(h, m, sec)
}

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.1f Ko".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f Mo".format(b / 1048576.0)
    else -> "%.2f Go".format(b / 1073741824.0)
}
