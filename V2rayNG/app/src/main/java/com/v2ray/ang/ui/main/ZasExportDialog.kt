package com.v2ray.ang.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ZasExportDialog(mainViewModel: MainViewModel, groupId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val servers by mainViewModel.serversForGroup(groupId).collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("ZAS-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())) }
    var days by remember { mutableStateOf("30") }
    var noExpiry by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf(setOf<String>()) }
    var pending by remember { mutableStateOf("") }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && pending.isNotEmpty()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(pending.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Fichier enregistré", Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Échec de l'enregistrement", Toast.LENGTH_SHORT).show()
            }
        }
        pending = ""
    }

    fun build(onReady: (String, String) -> Unit) {
        val guids = servers.map { it.guid }.filter { it !in excluded }
        if (guids.isEmpty()) {
            Toast.makeText(context, "Choisis au moins un serveur", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val links = mainViewModel.zasBuildPayload(context, guids)
            if (links.isBlank()) {
                Toast.makeText(context, "Aucun serveur exportable", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val d = days.toLongOrNull() ?: 30L
            val expiresAt = if (noExpiry) 0L else System.currentTimeMillis() + d * 86_400_000L
            val fileName = name.trim().ifEmpty { "ZAS" }.replace(Regex("[^A-Za-z0-9_.-]"), "_")
            onReady(fileName, ZasCrypto.seal(fileName, expiresAt, locked, note.trim(), links))
        }
    }

    val fieldStyle = TextStyle(color = Color.White, fontSize = 16.sp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C21),
        title = { Text("Exporter des serveurs", color = Color.White) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nom du fichier") }, textStyle = fieldStyle,
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = days, onValueChange = { days = it.filter(Char::isDigit).take(4) },
                    label = { Text("Valable (jours)") }, textStyle = fieldStyle,
                    singleLine = true, enabled = !noExpiry, modifier = Modifier.fillMaxWidth()
                )
                CheckRow("Sans date d'expiration", noExpiry) { noExpiry = it }
                CheckRow("🔒 Verrouiller (le destinataire ne pourra ni voir, ni modifier, ni réexporter)", locked) { locked = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Message / note (affiché à l'importation)") },
                    textStyle = fieldStyle, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Serveurs à inclure", color = Color(0xFF8A96B3), fontSize = 14.sp)
                servers.forEach { s ->
                    CheckRow("${s.profile.remarks} (${s.profile.configType.name})", s.guid !in excluded) { on ->
                        excluded = if (on) excluded - s.guid else excluded + s.guid
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                build { fileName, text ->
                    pending = text
                    saveLauncher.launch("$fileName.zas")
                }
            }) { Text("Exporter .ZAS") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    build { _, text ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("ZAS", text))
                        Toast.makeText(context, "Copié", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }) { Text("Copier") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}
