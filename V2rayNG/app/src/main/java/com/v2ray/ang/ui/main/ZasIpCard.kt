package com.v2ray.ang.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.net.Inet4Address

private fun zasIpOk(a: Int, b: Int): Boolean = when (a) {
    10 -> b in 41..51 || b == 61 || b == 63 || b == 67 ||
        b in 70..79 || b in 102..106 || b == 143 || b == 161 ||
        b == 163 || b == 165 || b == 166 || b in 193..199 ||
        b in 204..210 || b == 212 || b == 238
    100 -> b in 81..83
    else -> false
}

@Suppress("DEPRECATION")
private fun zasMobileIp(context: Context): String? {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (n in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            val lp = cm.getLinkProperties(n) ?: continue
            for (la in lp.linkAddresses) {
                val addr = la.address
                if (addr is Inet4Address) return addr.hostAddress
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ZasIpCard() {
    val context = LocalContext.current
    var ip by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            ip = zasMobileIp(context)
            delay(3000)
        }
    }

    val parts = ip?.split(".")
    val a = parts?.getOrNull(0)?.toIntOrNull()
    val b = parts?.getOrNull(1)?.toIntOrNull()
    val ok = a != null && b != null && zasIpOk(a, b)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (ok) Color(0xFF1E8E4E) else Color(0xFFC62828))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (ok) "📶" else "⚠️", fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            "ADRESSE IP : " + (ip ?: "réseau mobile non détecté"),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
