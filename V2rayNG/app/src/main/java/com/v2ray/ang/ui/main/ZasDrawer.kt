package com.v2ray.ang.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// À REMPLACER par tes vrais liens avant de valider
private const val WHATSAPP_GROUP_URL = "https://chat.whatsapp.com/JfQ69FEeuHe4knUQ2Jcu6U?s=ms&p=a&iam=2"
private const val SUPPORT_URL = "https://wa.me/22677200109"
private const val SHARE_TEXT = "Télécharge ZAS VPN : LIEN_DE_TELECHARGEMENT"

@Composable
fun ZasDrawerContent(onClose: () -> Unit) {
    val context = LocalContext.current

    fun openLink(url: String) {
        onClose()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
        }
    }

    fun shareApp() {
        onClose()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
        }
        try {
            context.startActivity(Intent.createChooser(send, "Partager ZAS VPN"))
        } catch (e: Exception) {
        }
    }

    ModalDrawerSheet(drawerContainerColor = Color(0xFF0E1A33)) {
        Text(
            "ZAS VPN",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(24.dp)
        )
        DrawerRow("💬", "Groupe WhatsApp") { openLink(WHATSAPP_GROUP_URL) }
        DrawerRow("🛟", "Assistance") { openLink(SUPPORT_URL) }
        DrawerRow("📤", "Partager l'application") { shareApp() }
    }
}

@Composable
private fun DrawerRow(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 18.sp)
    }
}
