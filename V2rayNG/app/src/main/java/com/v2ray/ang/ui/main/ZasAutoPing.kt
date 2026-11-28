package com.v2ray.ang.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ZasAutoPingCard(isRunning: Boolean, onPing: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zas_prefs", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("auto_ping", false)) }
    val ping by rememberUpdatedState(onPing)

    LaunchedEffect(isRunning, enabled) {
        while (isRunning && enabled) {
            delay(25_000)
            ping()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xCC0E1A33))
                .border(1.dp, Color(0xFF2F3B54), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📡", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Auto Ping", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Maintient le tunnel actif avec un contrôle réseau périodique",
                    color = Color(0xFFA9B4CC), fontSize = 12.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    prefs.edit().putBoolean("auto_ping", it).apply()
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF00D4C0),
                    checkedThumbColor = Color.White
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        ZasIpCard()
    }
}
