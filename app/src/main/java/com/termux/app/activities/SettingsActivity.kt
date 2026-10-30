package com.termux.app.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.newtermux.compose.MenuItemDivider
import com.newtermux.compose.NewTermuxComposeTheme
import com.newtermux.compose.outlinedMenuCard
import com.newtermux.features.NewTermuxSettings
import com.newtermux.features.TextExpansionStore
import com.newtermux.update.UpdateHighlightsDialog
import com.newtermux.update.UpdateHost
import com.newtermux.update.UpdateManager
import com.termux.app.TermuxActivity
import com.termux.shared.android.PermissionUtils
import com.termux.app.TermuxInstaller
import com.termux.app.models.UserAction
import com.termux.shared.android.AndroidUtils
import com.termux.shared.android.PackageUtils
import com.termux.shared.file.FileUtils
import com.termux.shared.interact.ShareUtils
import com.termux.shared.logger.Logger
import com.termux.shared.models.ReportInfo
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences
import com.termux.shared.activities.ReportActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * NewTermux settings, fully rewritten in Jetpack Compose (Phase 5), replacing
 * the androidx.preference framework. Uses an internal back-stack to navigate
 * the root list, the NewTermux sections (Backup/Features/Text Expansion), and
 * the upstream Termux + plugin preference sub-screens, binding directly to the
 * existing SharedPreferences / data-store APIs.
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewTermuxComposeTheme(this) {
                SettingsRoot(activity = this)
            }
        }
    }
}

private enum class Route {
    ROOT, BACKUP, FEATURES, TEXT_EXPANSION, UPDATES,
    TERMUX, TERMINAL_IO, TERMINAL_VIEW, DEBUGGING,
    PLUGIN_API, PLUGIN_FLOAT, PLUGIN_TASKER, PLUGIN_WIDGET,
}

@Composable
private fun SettingsRoot(activity: Activity) {
    val stack = remember { mutableStateListOf(Route.ROOT) }
    fun push(r: Route) = stack.add(r)
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else activity.finish() }

    BackHandler { pop() }

    when (stack.last()) {
        Route.ROOT -> RootScreen(activity, onBack = { pop() }, onNav = { push(it) })
        Route.BACKUP -> BackupScreen(onBack = { pop() })
        Route.FEATURES -> FeaturesScreen(activity, onBack = { pop() })
        Route.TEXT_EXPANSION -> TextExpansionScreen(onBack = { pop() })
        Route.UPDATES -> UpdatesScreen(activity, onBack = { pop() })
        Route.TERMUX -> TermuxScreen(onBack = { pop() }, onNav = { push(it) })
        Route.TERMINAL_IO -> TerminalIOScreen(onBack = { pop() })
        Route.TERMINAL_VIEW -> TerminalViewScreen(onBack = { pop() })
        Route.DEBUGGING -> DebuggingScreen(onBack = { pop() })
        Route.PLUGIN_API -> PluginScreen("Termux:API", Plugin.API, onBack = { pop() })
        Route.PLUGIN_FLOAT -> PluginScreen("Termux:Float", Plugin.FLOAT, onBack = { pop() })
        Route.PLUGIN_TASKER -> PluginScreen("Termux:Tasker", Plugin.TASKER, onBack = { pop() })
        Route.PLUGIN_WIDGET -> PluginScreen("Termux:Widget", Plugin.WIDGET, onBack = { pop() })
    }
}

// ---------------------------------------------------------------- shared UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        content(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()))
    }
}

@Composable
private fun CategoryHeader(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun NavRow(title: String, summary: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** Bool switch backed by NewTermuxSettings.get/set. */
@Composable
private fun NtSwitch(context: Context, key: String, title: String, summary: String?) {
    var checked by remember { mutableStateOf(NewTermuxSettings.get(context, key)) }
    SwitchRow(title, summary, checked) {
        checked = it
        NewTermuxSettings.set(context, key, it)
    }
}

/**
 * "Keep alive in background" toggle. Backed by NewTermuxSettings, and when enabled it also asks
 * the user to allowlist the app from battery optimization (if not already exempt) since the wake
 * lock alone can still lose to Doze.
 */
@Composable
private fun KeepAliveSwitch(activity: Activity) {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(NewTermuxSettings.isKeepAliveInBackground(context)) }
    SwitchRow(
        title = "Keep alive in background",
        summary = "Hold a foreground wake lock while sessions run so the app survives when you launch a game. Turn off to save battery.",
        checked = checked,
    ) {
        checked = it
        NewTermuxSettings.set(context, NewTermuxSettings.KEY_KEEP_ALIVE_BACKGROUND, it)
        if (it && !PermissionUtils.checkIfBatteryOptimizationsDisabled(context)) {
            // Mark prompted so the first-run nudge won't also fire, then request the exemption.
            NewTermuxSettings.setBatteryOptPrompted(context, true)
            runCatching { PermissionUtils.requestDisableBatteryOptimizations(activity) }
        }
    }
}

@Composable
private fun LogLevelRow(context: Context, current: Int, onSelect: (Int) -> Unit) {
    val values = remember { Logger.getLogLevelsArray().map { it.toString() } }
    val labels = remember { Logger.getLogLevelLabelsArray(context, Logger.getLogLevelsArray(), true).map { it.toString() } }
    var expanded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(current) }
    val idx = values.indexOf(value.toString()).coerceAtLeast(0)
    Box {
        NavRow(title = "Log level", summary = labels.getOrNull(idx)) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.outlinedMenuCard()) {
            values.forEachIndexed { i, v ->
                if (i > 0) MenuItemDivider()
                DropdownMenuItem(text = { Text(labels.getOrElse(i) { v }) }, onClick = {
                    expanded = false
                    v.toIntOrNull()?.let { lvl ->
                        value = lvl
                        onSelect(lvl)
                    }
                })
            }
        }
    }
}

// ---------------------------------------------------------------- root

@Composable
private fun RootScreen(activity: Activity, onBack: () -> Unit, onNav: (Route) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val apiInstalled = remember { TermuxAPIAppSharedPreferences.build(context, false) != null }
    val floatInstalled = remember { TermuxFloatAppSharedPreferences.build(context, false) != null }
    val taskerInstalled = remember { TermuxTaskerAppSharedPreferences.build(context, false) != null }
    val widgetInstalled = remember { TermuxWidgetAppSharedPreferences.build(context, false) != null }
    val donateVisible = remember { isDonateVisible(context) }

    fun launch(cls: Class<*>) = context.startActivity(Intent(context, cls))

    SettingsScaffold("Settings", onBack) { mod ->
        Column(modifier = mod) {
            NavRow("Updates", "Version ${UpdateManager.installedVersionName()} · check for new releases") { onNav(Route.UPDATES) }
            NavRow("Appearance", "Accent color, UI theme and layout") { launch(ThemePickerActivity::class.java) }
            NavRow("Package Manager", "Browse, search and install packages") { launch(PackageManagerActivity::class.java) }
            NavRow("SSH Manager", "Save and connect to multiple SSH servers") { launch(SshManagerActivity::class.java) }
            NavRow("File Manager", "Browse, edit, and manage files in your Termux home") { launch(FileManagerActivity::class.java) }
            HorizontalDivider()
            NavRow("Backup & Restore", "Back up or restore your Termux environment") { onNav(Route.BACKUP) }
            NavRow("Features", "Toggle NewTermux features on or off") { onNav(Route.FEATURES) }
            NavRow("Text Expansion", "Auto-expand short triggers to full commands") { onNav(Route.TEXT_EXPANSION) }
            HorizontalDivider()
            NavRow("Termux", "Terminal, keyboard and debugging options") { onNav(Route.TERMUX) }
            if (apiInstalled) NavRow("Termux:API", "API plugin settings") { onNav(Route.PLUGIN_API) }
            if (floatInstalled) NavRow("Termux:Float", "Floating window plugin settings") { onNav(Route.PLUGIN_FLOAT) }
            if (taskerInstalled) NavRow("Termux:Tasker", "Tasker plugin settings") { onNav(Route.PLUGIN_TASKER) }
            if (widgetInstalled) NavRow("Termux:Widget", "Widget plugin settings") { onNav(Route.PLUGIN_WIDGET) }
            HorizontalDivider()
            NavRow("About", "App, device and plugin info") {
                scope.launch { openAbout(context) }
            }
            if (donateVisible) NavRow("Donate", "Support development") { ShareUtils.openUrl(context, TermuxConstants.TERMUX_DONATE_URL) }
        }
    }
}

// ---------------------------------------------------------------- Updates

@Composable
private fun UpdatesScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(UpdateManager.isNotifyEnabled(context)) }
    var pre by remember { mutableStateOf(UpdateManager.isIncludePrereleases(context)) }
    var pretend by remember { mutableStateOf(UpdateManager.isPretendOld(context)) }
    var skipped by remember { mutableStateOf(UpdateManager.skippedVersion(context)) }

    fun checkNow() {
        checking = true
        status = null
        UpdateManager.check(context, force = true) { info ->
            checking = false
            found = info?.takeIf { it.isNewer }
            status = when {
                info == null -> "Couldn't reach GitHub. Check the connection and try again."
                info.isNewer -> "Update available: v${info.versionName}" + if (info.prerelease) " (prerelease)" else ""
                else -> "You're up to date (newest release: ${info.tag})."
            }
            if (found != null) showDialog = true
        }
    }

    SettingsScaffold("Updates", onBack) { mod ->
        Column(modifier = mod) {
            NavRow("Installed version", UpdateManager.installedVersionName(), enabled = false) {}
            NavRow("Check now", status ?: "Look for a newer release on GitHub", enabled = !checking) { checkNow() }
            if (checking) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(10.dp))
                    Text("Checking…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            found?.let { info -> NavRow("View update v${info.versionName}", "What's new, then download and install") { showDialog = true } }
            HorizontalDivider()
            SwitchRow("Notify me about updates", "Show a notice on the terminal screen when a new release is out (checked at most every 6 hours)", notify) {
                notify = it
                UpdateManager.setNotifyEnabled(context, it)
            }
            SwitchRow("Include prereleases", "Also offer test releases (-pre builds)", pre) {
                pre = it
                UpdateManager.setIncludePrereleases(context, it)
            }
            if (skipped.isNotEmpty()) {
                NavRow("Skipped version: v$skipped", "Tap to be notified about it again") {
                    UpdateManager.clearSkippedVersion(context)
                    skipped = ""
                }
            }
            NavRow("Release page", "Open the NewTermux releases on GitHub") { UpdateManager.openReleasePage(activity) }
            if (UpdateManager.isTestBuild()) {
                HorizontalDivider()
                CategoryHeader("Test build")
                SwitchRow("Pretend this is version 0.0.0", "Makes the current release show up as an update, to test the notice, download and installer", pretend) {
                    pretend = it
                    UpdateManager.setPretendOld(context, it)
                }
            }
        }
    }

    val info = found
    if (showDialog && info != null) {
        UpdateHighlightsDialog(
            info = info,
            onLater = { showDialog = false },
            onSkip = {
                UpdateManager.skipVersion(context, info.versionName)
                skipped = info.versionName
                showDialog = false
            },
            onUpdate = {
                showDialog = false
                UpdateManager.downloadAndInstall(activity, info)
            },
        )
    }
    UpdateHost(activity)
}

// ---------------------------------------------------------------- Features

@Composable
private fun FeaturesScreen(activity: Activity, onBack: () -> Unit) {
    val context = LocalContext.current
    var showScriptEditor by remember { mutableStateOf(false) }
    var showRestartWarning by remember { mutableStateOf(false) }

    val zshInstalled = remember { File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "bin/zsh").exists() }

    SettingsScaffold("Features", onBack) { mod ->
        Column(modifier = mod) {
            CategoryHeader("Keyboard")
            NtSwitch(context, NewTermuxSettings.KEY_KEYBOARD_SUGGESTIONS, "Keyboard Suggestions", "Show autocorrect and word suggestions bar")
            NtSwitch(context, NewTermuxSettings.KEY_AUTOCORRECT, "Command Autocorrect", "Suggest corrections for mistyped commands (spacebar)")
            NtSwitch(context, NewTermuxSettings.KEY_URL_DETECTION_ENABLED, "URL Detection", "Long-press a URL in the terminal to open or copy it")
            NtSwitch(context, NewTermuxSettings.KEY_EXTRA_KEYS_VISIBLE, "Show Extra Keys Toolbar", "Show the ESC, TAB, arrow key row above the keyboard")
            NtSwitch(context, NewTermuxSettings.KEY_EXTRA_KEYS_IN_DRAWER, "Extra Keys in Right Drawer", "Move extra keys to a swipeable right-side drawer — takes effect on restart")

            CategoryHeader("Toolbar Buttons")
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_AC_BUTTON, "AC Toggle Button", "Show autocorrect on/off button in toolbar")
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_ROOT_BUTTON, "Root Toggle Button", null)
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_STT_BUTTON, "Speech-to-Text Button", null)
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_PACKAGES_BUTTON, "Package Manager Button", null)
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_CLEAR_BUTTON, "Clear Terminal Button", null)

            CategoryHeader("Session Tabs")
            NtSwitch(context, NewTermuxSettings.KEY_SESSION_TABS, "Show Session Tabs", "Show session tab chips at the top")
            NtSwitch(context, NewTermuxSettings.KEY_SESSION_RENAME_ENABLED, "Session Renaming", "Long-press a session tab to rename it")

            CategoryHeader("Startup")
            NtSwitch(context, NewTermuxSettings.KEY_STARTUP_SCRIPT_ENABLED, "Startup Script", "Run ~/.termux/startup-script.sh in each new session")
            NavRow("Edit Startup Script", "Edit ~/.termux/startup-script.sh") { showScriptEditor = true }

            CategoryHeader("Shell")
            if (zshInstalled) {
                NavRow("Zsh", "✓ Installed", enabled = false) {}
            } else {
                NavRow("Install Zsh", "Required for syntax highlighting and autosuggestions") {
                    NewTermuxSettings.setPendingCommand(context, "pkg install zsh\n")
                    activity.finish()
                }
            }
            ZshPluginsSwitch(context, zshInstalled, onChanged = { showRestartWarning = true })

            CategoryHeader("Drawer")
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_DRAWER_EXPORT_SCRIPT, "Export Screen & Make Script", "Show Export Screen and Make Script buttons in the drawer")
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_DRAWER_PKG_UPDATE, "Pkg Update Button", "Show a button that runs pkg update && pkg upgrade -y")
            NtSwitch(context, NewTermuxSettings.KEY_SHOW_DRAWER_CMD_BUTTONS, "Drawer Command Buttons", "Show customisable command shortcut buttons")

            CategoryHeader("Background")
            KeepAliveSwitch(activity)

            CategoryHeader("Permissions")
            NavRow("Grant Storage Permission", "Allow access to /sdcard and set up ~/storage symlinks") {
                (activity as? TermuxActivity)?.requestStoragePermission(false)
            }
            Spacer(Modifier.size(16.dp))
        }
    }

    if (showScriptEditor) {
        StartupScriptEditorDialog(context, onDismiss = { showScriptEditor = false })
    }
    if (showRestartWarning) {
        AlertDialog(
            onDismissRequest = { showRestartWarning = false },
            title = { Text("Restart Required") },
            text = { Text("Start a new terminal session for this change to take effect.") },
            confirmButton = { TextButton(onClick = { showRestartWarning = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun ZshPluginsSwitch(context: Context, zshInstalled: Boolean, onChanged: () -> Unit) {
    var checked by remember { mutableStateOf(NewTermuxSettings.isZshPluginsEnabled(context)) }
    SwitchRow(
        title = "Shell Enhancements",
        summary = if (zshInstalled) "Autosuggestions + syntax highlighting (requires Zsh)" else "Install Zsh first to enable this",
        checked = checked,
        enabled = zshInstalled,
    ) {
        checked = it
        NewTermuxSettings.set(context, NewTermuxSettings.KEY_ZSH_PLUGINS, it)
        Thread { TermuxInstaller.setZshPlugins(context, it) }.start()
        onChanged()
    }
}

@Composable
private fun StartupScriptEditorDialog(context: Context, onDismiss: () -> Unit) {
    val scriptFile = remember { File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".termux/startup-script.sh") }
    var text by remember { mutableStateOf(if (scriptFile.exists()) runCatching { scriptFile.readText() }.getOrDefault("") else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Startup Script") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    scriptFile.parentFile?.mkdirs()
                    scriptFile.writeText(text)
                    Toast.makeText(context, "Startup script saved", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- Text Expansion

@Composable
private fun TextExpansionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NewTermuxSettings.isTextExpansionEnabled(context)) }
    val items = remember { mutableStateListOf<TextExpansionStore.TextExpansion>().apply { addAll(TextExpansionStore.load(context)) } }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun persist() = TextExpansionStore.save(context, items.toMutableList())

    SettingsScaffold("Text Expansion", onBack) { mod ->
        Column(modifier = mod) {
            SwitchRow("Enable Text Expansion", "Auto-expand short triggers to full commands", enabled) {
                enabled = it
                NewTermuxSettings.set(context, NewTermuxSettings.KEY_TEXT_EXPANSION_ENABLED, it)
            }
            HorizontalDivider()
            NavRow("Add New", "Create a new expansion") { editIndex = null; showEditor = true }
            if (items.isEmpty()) {
                Text(
                    "No expansions yet. Tap 'Add New' to create one.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEachIndexed { i, exp ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editIndex = i; showEditor = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${exp.trigger}  →  ${exp.expansion}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { items.removeAt(i); persist() }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showEditor) {
        val editing = editIndex?.let { items.getOrNull(it) }
        ExpansionEditorDialog(
            initTrigger = editing?.trigger ?: "",
            initExpansion = editing?.expansion ?: "",
            onDismiss = { showEditor = false },
            onSave = { trigger, expansion ->
                val idx = editIndex
                if (idx != null && idx < items.size) {
                    items[idx].trigger = trigger
                    items[idx].expansion = expansion
                    items[idx] = items[idx] // trigger recomposition
                } else {
                    val e = TextExpansionStore.TextExpansion()
                    e.trigger = trigger; e.expansion = expansion
                    items.add(e)
                }
                persist()
                showEditor = false
            },
        )
    }
}

@Composable
private fun ExpansionEditorDialog(
    initTrigger: String,
    initExpansion: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var trigger by remember { mutableStateOf(initTrigger) }
    var expansion by remember { mutableStateOf(initExpansion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initTrigger.isNotEmpty() || initExpansion.isNotEmpty()) "Edit Expansion" else "Add Expansion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(trigger, { trigger = it }, label = { Text("Trigger (e.g. ;ll)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(expansion, { expansion = it }, label = { Text("Expansion (e.g. ls -la)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (trigger.trim().isEmpty()) {
                    Toast.makeText(context, "Trigger cannot be empty", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onSave(trigger.trim(), expansion)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- Backup

@Composable
private fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreFull by remember { mutableStateOf<Boolean?>(null) }

    val basicSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
        if (uri != null) scope.launch {
            busy = "Backing up home…"
            val err = withContext(Dispatchers.IO) { runBackup(context, uri, false) }
            busy = null
            toast(context, if (err == null) "Backup complete" else "Backup failed: $err")
        }
    }
    val fullSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
        if (uri != null) scope.launch {
            busy = "Backing up home + usr…"
            val err = withContext(Dispatchers.IO) { runBackup(context, uri, true) }
            busy = null
            toast(context, if (err == null) "Backup complete" else "Backup failed: $err")
        }
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { restoreUri = uri; restoreFull = null }
    }

    SettingsScaffold("Backup & Restore", onBack) { mod ->
        Column(modifier = mod) {
            NavRow("Basic backup (home only)", "Save a .tar.gz of your home directory") { basicSaver.launch("termux-home-backup.tar.gz") }
            NavRow("Full backup (home + usr)", "Save a .tar.gz of home and usr") { fullSaver.launch("termux-full-backup.tar.gz") }
            HorizontalDivider()
            NavRow("Restore from backup", "Pick a .tar.gz to restore") { restorePicker.launch(arrayOf("*/*")) }
        }
    }

    busy?.let { msg ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.size(16.dp))
                    Text(msg)
                }
            },
        )
    }

    // Restore: choose type
    if (restoreUri != null && restoreFull == null) {
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("What type of backup is this?") },
            text = { Text("Choose the correct type so the right restore method is used.") },
            confirmButton = { TextButton(onClick = { restoreFull = true }) { Text("Full (home + usr)") } },
            dismissButton = { TextButton(onClick = { restoreFull = false }) { Text("Basic (home only)") } },
        )
    }
    // Restore: confirm
    if (restoreUri != null && restoreFull != null) {
        val uri = restoreUri!!
        val full = restoreFull!!
        AlertDialog(
            onDismissRequest = { restoreUri = null; restoreFull = null },
            title = { Text("Restore Termux") },
            text = { Text(if (full) "This will overwrite your home and usr directories. Continue?" else "This will overwrite your home directory. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    restoreUri = null; restoreFull = null
                    scope.launch {
                        busy = "Restoring…"
                        val err = withContext(Dispatchers.IO) { runRestore(context, uri, full) }
                        busy = null
                        toast(context, if (err == null) "Restore complete" else "Restore failed: $err")
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null; restoreFull = null }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- Termux (upstream)

@Composable
private fun TermuxScreen(onBack: () -> Unit, onNav: (Route) -> Unit) {
    SettingsScaffold("Termux", onBack) { mod ->
        Column(modifier = mod) {
            NavRow("Debugging", "Log level and debug options") { onNav(Route.DEBUGGING) }
            NavRow("Terminal I/O", "Soft keyboard behavior") { onNav(Route.TERMINAL_IO) }
            NavRow("Terminal View", "Terminal margin adjustment") { onNav(Route.TERMINAL_VIEW) }
        }
    }
}

@Composable
private fun TerminalIOScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { TermuxAppSharedPreferences.build(context, true) }
    SettingsScaffold("Terminal I/O", onBack) { mod ->
        Column(modifier = mod) {
            if (prefs == null) { Text("Unavailable", modifier = Modifier.padding(16.dp)); return@Column }
            CategoryHeader("Keyboard")
            var soft by remember { mutableStateOf(prefs.isSoftKeyboardEnabled) }
            SwitchRow("Soft keyboard enabled", "Show the on-screen keyboard", soft) { soft = it; prefs.setSoftKeyboardEnabled(it) }
            var softNoHw by remember { mutableStateOf(prefs.isSoftKeyboardEnabledOnlyIfNoHardware) }
            SwitchRow("Only if no hardware keyboard", "Hide soft keyboard when a hardware keyboard is connected", softNoHw) { softNoHw = it; prefs.setSoftKeyboardEnabledOnlyIfNoHardware(it) }
        }
    }
}

@Composable
private fun TerminalViewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { TermuxAppSharedPreferences.build(context, true) }
    SettingsScaffold("Terminal View", onBack) { mod ->
        Column(modifier = mod) {
            if (prefs == null) { Text("Unavailable", modifier = Modifier.padding(16.dp)); return@Column }
            CategoryHeader("View")
            var margin by remember { mutableStateOf(prefs.isTerminalMarginAdjustmentEnabled) }
            SwitchRow("Terminal margin adjustment", "Auto-adjust margins to avoid rounded corners/cutouts", margin) { margin = it; prefs.setTerminalMarginAdjustment(it) }
        }
    }
}

@Composable
private fun DebuggingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { TermuxAppSharedPreferences.build(context, true) }
    SettingsScaffold("Debugging", onBack) { mod ->
        Column(modifier = mod) {
            if (prefs == null) { Text("Unavailable", modifier = Modifier.padding(16.dp)); return@Column }
            CategoryHeader("Logging")
            LogLevelRow(context, prefs.logLevel) { prefs.setLogLevel(context, it) }
            var keyLog by remember { mutableStateOf(prefs.isTerminalViewKeyLoggingEnabled) }
            SwitchRow("Terminal view key logging", "Log key events (verbose)", keyLog) { keyLog = it; prefs.setTerminalViewKeyLoggingEnabled(it) }
            var pluginErr by remember { mutableStateOf(prefs.arePluginErrorNotificationsEnabled(false)) }
            SwitchRow("Plugin error notifications", null, pluginErr) { pluginErr = it; prefs.setPluginErrorNotificationsEnabled(it) }
            var crash by remember { mutableStateOf(prefs.areCrashReportNotificationsEnabled(false)) }
            SwitchRow("Crash report notifications", null, crash) { crash = it; prefs.setCrashReportNotificationsEnabled(it) }
        }
    }
}

// ---------------------------------------------------------------- Plugins

private enum class Plugin { API, FLOAT, TASKER, WIDGET }

@Composable
private fun PluginScreen(title: String, plugin: Plugin, onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsScaffold(title, onBack) { mod ->
        Column(modifier = mod) {
            CategoryHeader("Logging")
            when (plugin) {
                Plugin.API -> {
                    val p = remember { TermuxAPIAppSharedPreferences.build(context, true) }
                    if (p != null) LogLevelRow(context, p.getLogLevel(true)) { p.setLogLevel(context, it, true) }
                }
                Plugin.FLOAT -> {
                    val p = remember { TermuxFloatAppSharedPreferences.build(context, true) }
                    if (p != null) {
                        LogLevelRow(context, p.getLogLevel(true)) { p.setLogLevel(context, it, true) }
                        var keyLog by remember { mutableStateOf(p.isTerminalViewKeyLoggingEnabled(true)) }
                        SwitchRow("Terminal view key logging", null, keyLog) { keyLog = it; p.setTerminalViewKeyLoggingEnabled(it, true) }
                    }
                }
                Plugin.TASKER -> {
                    val p = remember { TermuxTaskerAppSharedPreferences.build(context, true) }
                    if (p != null) LogLevelRow(context, p.getLogLevel(true)) { p.setLogLevel(context, it, true) }
                }
                Plugin.WIDGET -> {
                    val p = remember { TermuxWidgetAppSharedPreferences.build(context, true) }
                    if (p != null) LogLevelRow(context, p.getLogLevel(true)) { p.setLogLevel(context, it, true) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- helpers

private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

private fun isDonateVisible(context: Context): Boolean {
    val digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context) ?: return true
    val apkRelease = TermuxUtils.getAPKRelease(digest)
    return !(apkRelease == null || apkRelease == TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)
}

private fun openAbout(context: Context) {
    val about = StringBuilder()
    about.append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES))
    about.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true))
    about.append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context))

    val userActionName = UserAction.ABOUT.getName()
    val reportInfo = ReportInfo(userActionName, TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, "About")
    reportInfo.setReportString(about.toString())
    reportInfo.setReportSaveFileLabelAndPath(
        userActionName,
        Environment.getExternalStorageDirectory().toString() + "/" +
            FileUtils.sanitizeFileName(TermuxConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true),
    )
    ReportActivity.startReportActivity(context, reportInfo)
}

private fun runBackup(context: Context, uri: Uri, full: Boolean): String? {
    return try {
        val cmd = if (full) arrayOf(
            "/data/data/com.termux/files/usr/bin/tar", "-zcf", "-",
            "-C", "/data/data/com.termux/files", "./home", "./usr",
        ) else arrayOf(
            "/data/data/com.termux/files/usr/bin/tar", "-zcf", "-",
            "-C", "/data/data/com.termux/files/home", ".",
        )
        val p = Runtime.getRuntime().exec(cmd)
        p.inputStream.use { input ->
            context.contentResolver.openOutputStream(uri).use { out ->
                if (out == null) return "Could not open output"
                input.copyTo(out)
            }
        }
        val errText = readStream(p.errorStream)
        val exit = p.waitFor()
        if (exit != 0) (errText.ifEmpty { "tar exited with code $exit" }) else null
    } catch (e: Exception) {
        e.message ?: "error"
    }
}

private fun runRestore(context: Context, fileUri: Uri, full: Boolean): String? {
    return try {
        val filePath: String = if (fileUri.scheme == "content") {
            val tmp = File(context.cacheDir, "restore_tmp.tar.gz")
            context.contentResolver.openInputStream(fileUri).use { input ->
                if (input == null) return "Could not open input"
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            }
            tmp.absolutePath
        } else {
            fileUri.path ?: return "Invalid path"
        }
        val p = if (full) Runtime.getRuntime().exec(arrayOf(
            "/data/data/com.termux/files/usr/bin/tar", "-zxvf", filePath,
            "-C", "/data/data/com.termux/files", "--recursive-unlink", "--preserve-permissions",
        )) else Runtime.getRuntime().exec(arrayOf(
            "/data/data/com.termux/files/usr/bin/tar", "-zxvf", filePath,
            "-C", "/data/data/com.termux/files/home",
        ))
        val errText = readStream(p.errorStream)
        val exit = p.waitFor()
        if (exit != 0) errText else null
    } catch (e: Exception) {
        e.message ?: "error"
    }
}

private fun readStream(input: InputStream): String {
    val baos = ByteArrayOutputStream()
    val buf = ByteArray(4096)
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        baos.write(buf, 0, n)
    }
    return baos.toString()
}
