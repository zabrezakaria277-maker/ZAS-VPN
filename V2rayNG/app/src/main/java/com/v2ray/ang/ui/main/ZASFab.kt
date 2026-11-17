package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.v2ray.ang.enums.EConfigType

@Composable
fun ZasFab(onAction: (MainAction) -> Unit, onExport: () -> Unit = {}) {
    var open by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }

    val close = { open = false; importOpen = false }

    Box {
        FloatingActionButton(
            onClick = { importOpen = false; open = true },
            containerColor = Color(0xFF2F80FF),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = close,
            modifier = Modifier.background(Color(0xFF1C1C21))
        ) {
            if (!importOpen) {
                ZasMenuRow("📥", "Importer") { importOpen = true }
                ZasMenuRow("📦", "Exporter") { close(); onExport() }
                ZasMenuRow("➕", "Ajouter un serveur") {
                    close(); onAction(MainAction.ImportManually(EConfigType.VMESS.value))
                }
                ZasMenuRow("🛰️", "Ajouter un serveur SS") {
                    close(); onAction(MainAction.ImportManually(EConfigType.SHADOWSOCKS.value))
                }
            } else {
                ZasMenuRow("📋", "Depuis le presse-papiers") { close(); onAction(MainAction.ImportClipboard) }
                ZasMenuRow("📷", "Scanner un QR code") { close(); onAction(MainAction.ImportQRcode) }
                ZasMenuRow("📁", "Depuis un fichier") { close(); onAction(MainAction.ImportConfigLocal) }
                ZasMenuRow("⬅️", "Retour") { importOpen = false }
            }
        }
    }
}

@Composable
private fun ZasMenuRow(emoji: String, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = Color.White, fontSize = 17.sp) },
        leadingIcon = { Text(emoji, fontSize = 20.sp) },
        onClick = onClick
    )
}
