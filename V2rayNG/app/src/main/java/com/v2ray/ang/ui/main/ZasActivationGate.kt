package com.v2ray.ang.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ZasGate {
    var openRequested by mutableStateOf(false)
    fun open() {
        openRequested = true
    }
}

private val Blue = Color(0xFF5B9BFF)

@Composable
fun ZasActivationGate(onAction: (MainAction) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(ZasExpiry.hasActive(context)) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(ZasExpiry.tick) {
        while (true) {
            active = ZasExpiry.hasActive(context)
            delay(1000)
        }
    }

    val show = (ZasExpiry.REQUIRE_CODE && !active) || ZasGate.openRequested
    if (!show) return

    fun submit() {
        if (busy || code.isBlank()) return
        busy = true
        error = ""
        scope.launch {
            when (val r = ZasActivation.activate(context, code)) {
                is ZasActivationResult.Success -> {
                    ZasExpiry.saveFromServer(context, code.trim(), r.remainingMs)
                    if (r.links.isNotEmpty()) {
                        onAction(MainAction.ImportBatchConfig(r.links.joinToString("\n")))
                        delay(2500)
                        onAction(MainAction.RemoveDuplicateServers)
                    }
                    code = ""
                    active = true
                    ZasGate.openRequested = false
                }
                is ZasActivationResult.Failure -> error = ZasActivation.message(r)
            }
            busy = false
        }
    }

    Dialog(
        onDismissRequest = { if (active) ZasGate.openRequested = false },
        properties = DialogProperties(
            dismissOnBackPress = active,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CODE D'ACTIVATION", color = Blue, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) Text(
                        "✕", color = Color.White, fontSize = 24.sp,
                        modifier = Modifier.clickable { ZasGate.openRequested = false }.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(56.dp))
                Text(
                    "Entrez votre code d'activation pour débloquer votre abonnement.",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(20); error = "" },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF16213A))
                                .border(1.dp, Color(0xFF2A3A5A), RoundedCornerShape(28.dp))
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            if (code.isEmpty()) Text(
                                "Ex : ZAS-XXX-XXX-XXX", color = Color(0xFF8B96AE), fontSize = 18.sp
                            )
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.fillMaxWidth().height(58.dp)
                        .clip(RoundedCornerShape(29.dp))
                        .background(Blue)
                        .clickable { submit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (busy) "VÉRIFICATION…" else "ACTIVER",
                        color = Color(0xFF0B1220), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                    )
                }
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(error, color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "ⓘ  Où trouver mon code ?", color = Blue, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(ZasActivation.SUPPORT_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                        }
                    }.padding(8.dp)
                )
            }
        }
    }
}
