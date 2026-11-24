package com.v2ray.ang.ui.main

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

@Stable
class ZasHeaderScroll {
    var offset by mutableStateOf(0f)
    var maxPx = 0f

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            val old = offset
            val new = (old + available.y).coerceIn(-maxPx, 0f)
            offset = new
            return Offset(0f, new - old)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (available.y <= 0f) return Offset.Zero
            val old = offset
            val new = (old + available.y).coerceIn(-maxPx, 0f)
            offset = new
            return Offset(0f, new - old)
        }
    }
}

@Composable
fun ZasCollapsibleHeader(
    scroll: ZasHeaderScroll,
    isRunning: Boolean,
    serverGuid: String?,
    statusText: String,
    onToggle: () -> Unit,
    onPing: () -> Unit = {},
    onExpired: () -> Unit = {},
    testCountry: String? = null,
    testIp: String? = null,
) {
    val dragState = rememberScrollableState { delta ->
        val old = scroll.offset
        val new = (old + delta).coerceIn(-scroll.maxPx, 0f)
        scroll.offset = new
        new - old
    }
    Box(
        Modifier
            .clipToBounds()
            .scrollable(dragState, Orientation.Vertical)
            .layout { measurable, constraints ->
                val p = measurable.measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val full = p.height.toFloat()
                scroll.maxPx = full
                val off = scroll.offset.coerceIn(-full, 0f)
                layout(p.width, (full + off).toInt().coerceAtLeast(0)) {
                    p.place(0, off.toInt())
                }
            }
    ) {
        ZasHeader(
            isRunning = isRunning,
            serverGuid = serverGuid,
            statusText = statusText,
            onToggle = onToggle,
            onPing = onPing,
            onExpired = onExpired,
            testCountry = testCountry,
            testIp = testIp
        )
    }
}
