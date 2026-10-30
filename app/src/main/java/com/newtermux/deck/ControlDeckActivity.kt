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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
        // NOT_FOCUSABLE keeps key/controller input on the terminal. On its own it also puts the
        // window above the soft keyboard, which this device shows on the second screen
        // (Retroid setting ime_show_on_second=1); ALT_FOCUSABLE_IM puts the keyboard back on top.
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        DeckBridge.register(this)
        setContent {
            NewTermuxComposeTheme(this) { DeckScreen() }
        }
    }

    override fun onStop() {
        super.onStop()
        // Covered on the second screen (e.g. Home there): close, so the top screen gets the
        // status line and its "show controls" button back instead of staying without controls.
        if (!isFinishing && !isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        DeckBridge.unregister(this)
        super.onDestroy()
    }
}

private val DeckBg = Color(0xFF121016)
private val TileBg = Color(0xFF1E1A26)
private val TileBorder = Color(0xFF444444)

// One grid for the whole deck: session tiles, the new-session tile and command buttons all take
// one column, with the same gap everywhere, so they line up with each other.
private const val GRID_COLS = 4
private val Gap = 8.dp
private val LabelHeight = 18.dp
private val PreviewHeight = 72.dp
private val CommandHeight = 36.dp
private val HeaderButton = 36.dp
private val ScrollPadWidth = 104.dp

@Composable
private fun DeckScreen() {
    // Reading the version makes the deck recompose when sessions or drawer commands change.
    DeckBridge.version.intValue
    val act = DeckBridge.activity()
    Column(
        modifier = Modifier.fillMaxSize().background(DeckBg).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        Header(act)
        if (act == null) {
            Text("NewTermux is not running on the main screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        // Sessions + commands on the left (scrolls if there are many), scroll pad on the right,
        // keys across the full width below.
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(Gap)) {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val colW = (maxWidth - Gap * (GRID_COLS - 1)) / GRID_COLS
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionLabel("SESSIONS")
                    Sessions(act, colW)
                    Spacer(Modifier.height(2.dp))
                    SectionLabel("COMMANDS")
                    Commands(act, colW)
                }
            }
            ScrollPad(act, Modifier.width(ScrollPadWidth).fillMaxHeight())
        }
        Keys(act)
    }
}

@Composable
private fun Header(act: TermuxActivity?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap),
        modifier = Modifier.fillMaxWidth().height(40.dp),
    ) {
        Text("NewTermux", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        Spacer(Modifier.weight(1f))
        if (act != null) {
            if (act.isToolbarButtonShown(R.id.btn_autocorrect_toggle)) {
                HeaderAction({ act.performToolbarAction(R.id.btn_autocorrect_toggle) }) {
                    Text("AC", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            if (act.isToolbarButtonShown(R.id.btn_stt)) HeaderIcon(Icons.Filled.Mic) { act.performToolbarAction(R.id.btn_stt) }
            if (act.isToolbarButtonShown(R.id.btn_root_toggle)) HeaderIcon(Icons.Filled.Shield) { act.performToolbarAction(R.id.btn_root_toggle) }
            HeaderIcon(Icons.Filled.Settings) { act.performToolbarAction(R.id.btn_settings) }
            HeaderIcon(Icons.Filled.Add) { act.performToolbarAction(R.id.btn_new_session) }
        }
        HeaderIcon(Icons.Filled.Close) { DeckBridge.hideByUser() }
    }
}

@Composable
private fun HeaderAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(HeaderButton).clip(CircleShape).background(TileBg).combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    HeaderAction(onClick) { Icon(icon, null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(20.dp)) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}

/** Name above a fixed-size box: shared by the session tiles and the new-session tile. */
@Composable
private fun Tile(width: Dp, label: String, labelColor: Color, bold: Boolean, onClick: () -> Unit, box: @Composable () -> Unit) {
    Column(modifier = Modifier.width(width).combinedClickable(onClick = onClick)) {
        Text(
            label, modifier = Modifier.height(LabelHeight), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = labelColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Box(modifier = Modifier.fillMaxWidth().height(PreviewHeight)) { box() }
    }
}

@Composable
private fun Sessions(act: TermuxActivity, colW: Dp) {
    val sessions = act.termuxService?.termuxSessions ?: emptyList()
    val current = act.currentSession
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Gap),
    ) {
        sessions.forEachIndexed { i, ts ->
            val s = ts.terminalSession
            val active = s === current
            val name = s.mSessionName?.takeIf { it.isNotEmpty() } ?: "#${i + 1}"
            Tile(
                width = colW, label = "${i + 1}:$name",
                labelColor = if (active) MaterialTheme.colorScheme.primary else Color(0xFFBBBBBB), bold = active,
                onClick = { act.switchToSession(s) },
            ) {
                AndroidView(
                    factory = { ctx -> MiniTerminalPipView(ctx).also { DeckBridge.addPip(it) } },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { DeckBridge.removePip(it) },
                    update = { v ->
                        v.setSession(s)
                        v.setActive(active)
                    },
                )
            }
        }
        // Same size as a session preview
        Tile(width = colW, label = "New", labelColor = Color(0xFF8E8799), bold = false,
            onClick = { act.performToolbarAction(R.id.btn_new_session) }) {
            Box(
                modifier = Modifier.fillMaxSize().border(1.5.dp, TileBorder),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun Commands(act: TermuxActivity, colW: Dp) {
    val labels = act.drawerCommandLabels
    if (labels.isEmpty()) {
        Text("No drawer commands.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        labels.withIndex().chunked(GRID_COLS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                row.forEach { (i, label) ->
                    Box(
                        modifier = Modifier.width(colW).height(CommandHeight).clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = { act.clickDrawerCommand(i) },
                                onLongClick = { act.longClickDrawerCommand(i) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Keys(act: TermuxActivity) {
    val keys = act.termuxTerminalExtraKeys ?: return
    val info = keys.extraKeysInfo ?: return
    val rows = info.matrix.size.coerceAtLeast(1)
    val rowHeight = 44.dp
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
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(TileBg)
            .border(1.dp, Color(0xFF2D2838), RoundedCornerShape(8.dp))
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
        Text("▲\n\nScroll\npad\n\n▼", color = Color(0xFF8E8799), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
