package com.v2ray.ang.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// À REMPLACER par tes vrais liens avant de valider
private const val ZAS_SUPPORT_URL = "https://wa.me/22677200109"
private const val ZAS_RESELLER_URL = "https://wa.me/22677200109?text=Je%20veux%20devenir%20revendeur%20ZAS%20VPN"
private const val ZAS_SUGGEST_URL = "https://wa.me/22677200109?text=Suggestion%20pour%20ZAS%20VPN%20%3A"

@Composable
fun ZasMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    val close: () -> Unit = { dialog = "" }

    fun show(name: String) { advanced = false; onDismiss(); dialog = name }
    fun link(url: String) { advanced = false; onDismiss(); zasOpen(context, url) }
    fun adv(a: MainMoreMenuAction) { advanced = false; onDismiss(); onMoreMenuAction(a) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { advanced = false; onDismiss() },
        modifier = Modifier.background(Color(0xFF1C1C21))
    ) {
        if (!advanced) {
            ZasMoreItem("📖", "Guide d'utilisation") { show("guide") }
            ZasMoreItem("💳", "Acheté") { show("subscription") }
            ZasMoreItem("🔑", "Code d'activation") { show("activate") }
            ZasMoreItem("📍", "IP Finder") { show("ip") }
            ZasMoreItem("⚙️", "Paramètres") { advanced = false; onDismiss(); onNavigate(MainDestination.Settings) }
            ZasMoreItem("🤝", "Devenir revendeur") { link(ZAS_RESELLER_URL) }
            ZasMoreItem("🎧", "Contacter le support") { link(ZAS_SUPPORT_URL) }
            ZasMoreItem("💡", "Suggestions") { link(ZAS_SUGGEST_URL) }
            ZasMoreItem("🛠️", "Outils avancés") { advanced = true }
        } else {
            ZasMoreItem("⬅️", "Retour") { advanced = false }
            ZasMoreItem("🔄", "Redémarrer le service") { adv(MainMoreMenuAction.RestartService) }
            ZasMoreItem("⏱️", "Tester tous les serveurs") { adv(MainMoreMenuAction.TestAllRealPing) }
            ZasMoreItem("↕️", "Trier par résultats") { adv(MainMoreMenuAction.SortByTestResults) }
            ZasMoreItem("🧹", "Supprimer les doublons") { adv(MainMoreMenuAction.DeleteDuplicate) }
            ZasMoreItem("🚫", "Supprimer les invalides") { adv(MainMoreMenuAction.DeleteInvalid) }
            ZasMoreItem("🗑️", "Tout supprimer") { adv(MainMoreMenuAction.DeleteAll) }
        }
    }

    when (dialog) {
        "guide" -> ZasInfoDialog("Guide d'utilisation", close) {
            Text(
                "1. Touchez + puis Importer pour ajouter votre code ou fichier ZAS.\n\n" +
                    "2. Touchez un serveur dans la liste pour le choisir.\n\n" +
                    "3. Touchez le bouton rond pour vous connecter.\n\n" +
                    "4. Touchez la ligne d'état pour tester la connexion.",
                color = Color.White, fontSize = 15.sp
            )
        }

        "subscription" -> ZasInfoDialog(
            "Abonnement", close, confirmLabel = "Nous contacter",
            onConfirm = { close(); zasOpen(context, ZAS_SUPPORT_URL) }
        ) {
            Text(
                "Pour souscrire ou renouveler votre abonnement, contactez-nous sur WhatsApp. " +
                    "Vous recevrez un code d'activation à coller dans « Code d'activation ».",
                color = Color.White, fontSize = 15.sp
            )
        }

        "activate" -> {
            var code by remember { mutableStateOf("") }
            ZasInfoDialog(
                "Code d'activation", close, confirmLabel = "Activer",
                onConfirm = {
                    if (code.isNotBlank()) onAction(MainAction.ImportBatchConfig(code.trim()))
                    close()
                }
            ) {
                Text("Collez ici votre code ZAS (il commence par ZAS1:).", color = Color(0xFF8A96B3), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth(), maxLines = 4
                )
                TextButton(onClick = {
                    code = try { Utils.getClipboard(context) } catch (e: Exception) { "" }
                }) { Text("Coller") }
            }
        }

        "ip" -> {
            var info by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
            LaunchedEffect(Unit) {
                info = withContext(Dispatchers.IO) { zasFetchIpDetails() } ?: emptyList()
            }
            ZasInfoDialog("IP Finder", close) {
                val data = info
                if (data == null) {
                    Text("Recherche en cours…", color = Color.White)
                } else if (data.isEmpty()) {
                    Text("Impossible de récupérer l'IP. Vérifiez votre connexion.", color = Color.White)
                } else {
                    data.forEach { (k, v) -> Text("$k : $v", color = Color.White, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun ZasMoreItem(emoji: String, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = Color.White, fontSize = 16.sp) },
        leadingIcon = { Text(emoji, fontSize = 18.sp) },
        onClick = onClick
    )
}

@Composable
private fun ZasInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C21),
        title = { Text(title, color = Color.White) },
        text = { Column { content() } },
        confirmButton = {
            if (confirmLabel != null) TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

private fun zasOpen(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
    }
}

private fun zasFetchIpDetails(): List<Pair<String, String>>? {
    for (u in listOf("https://ipwho.is/", "https://ipapi.co/json/")) {
        try {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 6000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val ip = o.optString("ip", "")
            if (ip.isBlank()) continue
            val country = o.optString("country_name", "").ifBlank { o.optString("country", "—") }
            val city = o.optString("city", "—")
            val isp = o.optJSONObject("connection")?.optString("isp", "") ?: o.optString("org", "")
            return listOf(
                "IP" to ip,
                "Pays" to country.ifBlank { "—" },
                "Ville" to city.ifBlank { "—" },
                "Opérateur" to isp.ifBlank { "—" }
            )
        } catch (e: Exception) {
        }
    }
    return null
}
