package com.newtermux.update

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.newtermux.compose.NewTermuxComposeTheme
import com.newtermux.update.UpdateManager.DownloadState
import com.newtermux.update.UpdateManager.UpdateInfo
import java.util.Locale

/** FileProvider for downloaded update APKs (own class so it can't clash with a library's). */
class UpdateFileProvider : FileProvider()

private val UpdateGreen = Color(0xFF4CAF50)
private val NeutralButton = Color(0xFF333333)
private const val SLIDE_MS = 380

/**
 * Update notice on the terminal screen: a pill that slides in at the top-right of the terminal,
 * the highlights dialog behind its View button, and the download card. Hosted in a ComposeView
 * laid over the activity that never takes focus, so keyboard input stays with the terminal.
 */
object UpdateUi {
    private val pending = mutableStateOf<UpdateInfo?>(null)
    private val pillVisible = mutableStateOf(false)
    private val highlightsFor = mutableStateOf<UpdateInfo?>(null)
    private val topOffsetPx = mutableIntStateOf(0)

    @JvmStatic
    fun attach(activity: AppCompatActivity) {
        val view = ComposeView(activity).apply {
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            setContent { NewTermuxComposeTheme(activity) { Overlay(activity) } }
        }
        activity.addContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    /** Pixels from the top of the window to the top of the terminal (header height, or 0). */
    @JvmStatic
    fun setTopOffset(px: Int) {
        topOffsetPx.intValue = px
    }

    /** Automatic check on resume (cooldown applies); shows the pill when a newer release exists. */
    @JvmStatic
    fun onResume(activity: Activity) {
        UpdateManager.check(activity) { info ->
            if (info != null && UpdateManager.shouldNotify(activity, info)) {
                pending.value = info
                pillVisible.value = true
            }
        }
    }

    @Composable
    private fun Overlay(activity: Activity) {
        val density = LocalDensity.current
        val info = pending.value
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            if (info != null) {
                val top = with(density) { topOffsetPx.intValue.toDp() }
                UpdatePill(
                    info = info,
                    visible = pillVisible.value,
                    onDismiss = {
                        pillVisible.value = false
                        UpdateManager.dismissedThisSession = true
                    },
                    onView = {
                        pillVisible.value = false
                        highlightsFor.value = info
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = top + 8.dp, end = 8.dp),
                )
            }
            highlightsFor.value?.let { h ->
                UpdateHighlightsDialog(
                    info = h,
                    onLater = { highlightsFor.value = null },
                    onSkip = {
                        UpdateManager.skipVersion(activity, h.versionName)
                        highlightsFor.value = null
                    },
                    onUpdate = {
                        highlightsFor.value = null
                        UpdateManager.downloadAndInstall(activity, h)
                    },
                )
            }
            UpdateHost(activity)
        }
    }
}

fun fmtSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes >= 1L shl 30 -> String.format(Locale.ENGLISH, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1L shl 20 -> String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.ENGLISH, "%.0f KB", bytes / 1024.0)
}

// ── Pill ────────────────────────────────────────────────────────────────────────────────

@Composable
fun UpdatePill(info: UpdateInfo, visible: Boolean, onDismiss: () -> Unit, onView: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(tween(SLIDE_MS)) { it + 48 } + fadeIn(tween(SLIDE_MS / 2)),
        exit = slideOutHorizontally(tween(SLIDE_MS)) { it + 48 } + fadeOut(tween(SLIDE_MS / 2)),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(26.dp)),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.height(52.dp).padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Update available", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    val size = fmtSize(info.apkSize)
                    Text(
                        "v${info.versionName}" + (if (size.isNotEmpty()) " · $size" else ""),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1,
                    )
                }
                PillButton("Dismiss", NeutralButton, MaterialTheme.colorScheme.onSurface, onDismiss)
                PillButton("View", UpdateGreen, Color.White, onView)
            }
        }
    }
}

@Composable
private fun PillButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(15.dp),
    ) { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
}

// ── Highlights ──────────────────────────────────────────────────────────────────────────

@Composable
fun UpdateHighlightsDialog(info: UpdateInfo, onLater: () -> Unit, onSkip: () -> Unit, onUpdate: () -> Unit) {
    Dialog(onDismissRequest = onLater) {
        UpdateCardSurface {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CardHeader(
                    title = "What's new in v${info.versionName}" + if (info.prerelease) " (prerelease)" else "",
                    subtitle = listOf(
                        "v${UpdateManager.installedVersionName()} → v${info.versionName}",
                        fmtSize(info.apkSize),
                        info.publishedAt,
                    ).filter { it.isNotEmpty() }.joinToString(" · "),
                )
                Spacer(Modifier.height(14.dp))
                Text("HIGHLIGHTS", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(8.dp))
                if (info.highlights.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        info.highlights.forEach { h ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.padding(top = 5.dp).size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                Column {
                                    Text(h.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (h.text.isNotEmpty()) {
                                        Text(h.text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, lineHeight = 17.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(info.notes.ifBlank { "Bug fixes and improvements." }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Installing closes NewTermux: running commands stop. Your files, settings and tab layout are kept.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                        Text("Skip this version", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    TextButton(
                        onClick = { (ctx as? Activity)?.let { UpdateManager.openReleasePage(it, info.pageUrl) } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                    ) { Text("Full notes ↗", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardButton("Later", NeutralButton, MaterialTheme.colorScheme.onSurface, onLater, Modifier.weight(1f))
                    CardButton("Update now", UpdateGreen, Color.White, onUpdate, Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Download ────────────────────────────────────────────────────────────────────────────

/** Renders the download / failed card whenever an update download is running or has failed. */
@Composable
fun UpdateHost(activity: Activity) {
    val state by UpdateManager.download.collectAsState()
    when (val s = state) {
        is DownloadState.Downloading -> UpdateDownloadCard(s)
        is DownloadState.Failed -> UpdateFailedCard(
            s,
            onRetry = {
                UpdateManager.clearDownloadState()
                UpdateManager.downloadAndInstall(activity, s.info)
            },
            onReleasePage = {
                UpdateManager.clearDownloadState()
                UpdateManager.openReleasePage(activity, s.info.pageUrl)
            },
            onClose = { UpdateManager.clearDownloadState() },
        )
        else -> {}
    }
}

@Composable
private fun UpdateDownloadCard(s: DownloadState.Downloading) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        UpdateCardSurface {
            Column {
                CardHeader(title = "Updating to v${s.info.versionName}", subtitle = "Downloading the update…")
                Spacer(Modifier.height(14.dp))
                if (s.total > 0) {
                    LinearProgressIndicator(
                        progress = { (s.bytes.toFloat() / s.total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = UpdateGreen,
                        trackColor = NeutralButton,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp), color = UpdateGreen, trackColor = NeutralButton)
                }
                Spacer(Modifier.height(6.dp))
                val done = fmtSize(s.bytes).ifEmpty { "0 MB" }
                val of = if (s.total > 0) " of ${fmtSize(s.total)}" else ""
                val speed = if (s.bytesPerSec > 0) " · ${fmtSize(s.bytesPerSec)}/s" else ""
                Text(done + of + speed, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    CardButton("Cancel", NeutralButton, MaterialTheme.colorScheme.onSurface, { UpdateManager.cancelDownload() })
                }
            }
        }
    }
}

@Composable
private fun UpdateFailedCard(s: DownloadState.Failed, onRetry: () -> Unit, onReleasePage: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        UpdateCardSurface {
            Column {
                CardHeader(title = "Update to v${s.info.versionName} failed", subtitle = s.reason, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardButton("Release page", NeutralButton, MaterialTheme.colorScheme.onSurface, onReleasePage, Modifier.weight(1f))
                    CardButton("Retry", UpdateGreen, Color.White, onRetry, Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Shared pieces ───────────────────────────────────────────────────────────────────────

@Composable
private fun UpdateCardSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun CardHeader(title: String, subtitle: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SystemUpdate, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CardButton(label: String, bg: Color, fg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = modifier.heightIn(min = 36.dp).height(36.dp),
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
