@file:OptIn(ExperimentalFoundationApi::class)

package com.newtermux.deck

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.newtermux.compose.NewTermuxComposeTheme
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.app.terminal.MiniTerminalPipView
import com.termux.shared.termux.extrakeys.ExtraKeysView

/**
 * Control deck for the "Dual screen" layout, shown on the second display (Retroid Pocket Duo's
 * bottom screen) while the top screen shows only the terminal. Its window is not focusable, so
 * hardware keyboard / controller input keeps going to the terminal.
 */
class ControlDeckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        DeckBridge.register(this)
        setContent {
            NewTermuxComposeTheme(this) { DeckScreen() }
        }
    }

    override fun onDestroy() {
        DeckBridge.unregister(this)
        super.onDestroy()
    }
}

private val DeckBg = Color(0xFF121016)
private val TileBg = Color(0xFF1E1A26)

@Composable
private fun DeckScreen() {
    // Reading the version makes the deck recompose when sessions or drawer commands change.
    DeckBridge.version.intValue
    val act = DeckBridge.activity()
    Column(
        modifier = Modifier.fillMaxSize().background(DeckBg).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Header(act)
        if (act == null) {
            Text("NewTermux is not running on the main screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        SectionLabel("SESSIONS")
        Sessions(act)
        SectionLabel("COMMANDS")
        Commands(act)
        SectionLabel("KEYS")
        Keys(act)
        ScrollPad(act, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun Header(act: TermuxActivity?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(44.dp)) {
        Text("NewTermux", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        Spacer(Modifier.weight(1f))
        if (act != null) {
            if (act.isToolbarButtonShown(R.id.btn_autocorrect_toggle)) {
                TextAction("AC") { act.performToolbarAction(R.id.btn_autocorrect_toggle) }
            }
            if (act.isToolbarButtonShown(R.id.btn_stt)) IconAction(Icons.Filled.Mic) { act.performToolbarAction(R.id.btn_stt) }
            if (act.isToolbarButtonShown(R.id.btn_root_toggle)) IconAction(Icons.Filled.Shield) { act.performToolbarAction(R.id.btn_root_toggle) }
            IconAction(Icons.Filled.Settings) { act.performToolbarAction(R.id.btn_settings) }
            IconAction(Icons.Filled.Add) { act.performToolbarAction(R.id.btn_new_session) }
        }
        IconAction(Icons.Filled.Close) { DeckBridge.hideByUser() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.padding(horizontal = 3.dp).size(44.dp).clip(RoundedCornerShape(22.dp)).background(TileBg)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

@Composable
private fun IconAction(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.padding(horizontal = 3.dp).size(44.dp).clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(24.dp)) }
}

@Composable
private fun Sessions(act: TermuxActivity) {
    val sessions = act.termuxService?.termuxSessions ?: emptyList()
    val current = act.currentSession
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        sessions.forEachIndexed { i, ts ->
            val s = ts.terminalSession
            val active = s === current
            val name = s.mSessionName?.takeIf { it.isNotEmpty() } ?: "#${i + 1}"
            Column(
                modifier = Modifier.width(170.dp).clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = { act.switchToSession(s) }),
            ) {
                Text(
                    "${i + 1}:$name", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (active) MaterialTheme.colorScheme.primary else Color(0xFFBBBBBB),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
                AndroidView(
                    factory = { ctx -> MiniTerminalPipView(ctx).also { DeckBridge.addPip(it) } },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    onRelease = { DeckBridge.removePip(it) },
                    update = { v ->
                        v.setSession(s)
                        v.setActive(active)
                    },
                )
            }
        }
        Box(
            modifier = Modifier.width(70.dp).height(116.dp).clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF555555), RoundedCornerShape(8.dp))
                .combinedClickable(onClick = { act.performToolbarAction(R.id.btn_new_session) }),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Add, null, tint = Color(0xFFAAAAAA)) }
    }
}

@Composable
private fun Commands(act: TermuxActivity) {
    val labels = act.drawerCommandLabels
    if (labels.isEmpty()) {
        Text("No drawer commands.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.withIndex().chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (i, label) ->
                    Box(
                        modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = { act.clickDrawerCommand(i) },
                                onLongClick = { act.longClickDrawerCommand(i) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Keys(act: TermuxActivity) {
    val keys = act.termuxTerminalExtraKeys ?: return
    val info = keys.extraKeysInfo ?: return
    val rows = info.matrix.size.coerceAtLeast(1)
    val rowHeight = 52.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    AndroidView(
        factory = { ctx ->
            ExtraKeysView(ctx, null).also { v ->
                v.setExtraKeysViewClient(keys)
                v.setButtonTextAllCaps(act.properties.shouldExtraKeysTextBeAllCaps())
                v.reload(info, rowHeightPx)
                DeckBridge.deckKeys = v
            }
        },
        modifier = Modifier.fillMaxWidth().height(rowHeight * rows),
        onRelease = { v -> if (DeckBridge.deckKeys === v) DeckBridge.deckKeys = null },
    )
}

/** Drag up/down to scroll the terminal on the main screen (one row per 24 dp). */
@Composable
private fun ScrollPad(act: TermuxActivity, modifier: Modifier) {
    val stepPx = with(LocalDensity.current) { 24.dp.toPx() }
    val acc = remember { floatArrayOf(0f) }
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(TileBg)
            .border(1.dp, Color(0xFF2D2838), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragEnd = { acc[0] = 0f }) { change, dy ->
                    change.consume()
                    acc[0] += dy
                    while (acc[0] >= stepPx) { act.terminalView?.scrollByRows(-1); acc[0] -= stepPx }
                    while (acc[0] <= -stepPx) { act.terminalView?.scrollByRows(1); acc[0] += stepPx }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Scroll pad · drag to scroll the terminal", color = Color(0xFF8E8799), fontSize = 13.sp)
    }
}
