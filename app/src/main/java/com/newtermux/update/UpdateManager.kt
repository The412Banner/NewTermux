package com.newtermux.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.termux.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app updater. Ported from Bannerlator's UpdateManager (same author), simplified for NewTermux:
 * the GitHub releases API is the only source (no manifest branch, no small-update patches).
 *
 * Stable channel = `releases/latest` (GitHub's "Latest" release; prereleases never show up there).
 * Prerelease channel = the newest published release of any kind. Versions are compared as semver
 * on the tag (`v1.6.3`, `v1.6.3-pre.1`) against [BuildConfig.VERSION_NAME]; build metadata after
 * `+` (CI builds: `1.6.2+3541ee1`) is ignored, so dev builds are not offered their own release.
 */
object UpdateManager {
    private const val TAG = "NT_UPDATE"
    private const val REPO = "The412Banner/NewTermux"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val API_LIST = "https://api.github.com/repos/$REPO/releases?per_page=20"
    /** Automatic checks (app start/resume) reuse the last answer this long; "Check now" forces. */
    private const val CHECK_COOLDOWN_MS = 6L * 60 * 60 * 1000

    private const val PREFS = "newtermux_update"
    private const val PREF_NOTIFY = "update_notify_enabled"
    private const val PREF_SKIP = "update_skip_version"
    private const val PREF_LAST_CHECK = "update_last_check"
    private const val PREF_INCLUDE_PRE = "update_include_prereleases"
    private const val PREF_PRETEND_OLD = "update_test_pretend_old"
    private const val CACHE_NAME = "update_latest.json"
    private const val CACHE_PRE_NAME = "update_latest_pre.json"
    private const val FILE_PROVIDER_SUFFIX = ".updates"
    private const val DOWNLOAD_DIR = "updates"

    data class Highlight(val title: String, val text: String)

    data class UpdateInfo(
        val versionName: String,
        val tag: String,
        val notes: String,
        val highlights: List<Highlight>,
        val apkName: String?,
        val apkUrl: String?,
        val apkSize: Long,
        val apkSha256: String?,
        val publishedAt: String,
        val prerelease: Boolean,
        val pageUrl: String,
        /** Newer than the installed build (computed when parsed, honours the test switch). */
        val isNewer: Boolean,
    )

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val info: UpdateInfo, val bytes: Long, val total: Long, val bytesPerSec: Long) : DownloadState()
        data class Failed(val info: UpdateInfo, val reason: String) : DownloadState()
        data class Installing(val info: UpdateInfo) : DownloadState()
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download: StateFlow<DownloadState> get() = _download
    private val interrupt = AtomicBoolean()

    /** The pill was dismissed; stays quiet until the process restarts. */
    @JvmStatic var dismissedThisSession = false

    private val main = Handler(Looper.getMainLooper())

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isNotifyEnabled(ctx: Context) = prefs(ctx).getBoolean(PREF_NOTIFY, true)
    fun setNotifyEnabled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean(PREF_NOTIFY, on).apply()
    fun isIncludePrereleases(ctx: Context) = prefs(ctx).getBoolean(PREF_INCLUDE_PRE, false)
    fun setIncludePrereleases(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean(PREF_INCLUDE_PRE, on).apply()
    fun skippedVersion(ctx: Context): String = prefs(ctx).getString(PREF_SKIP, "") ?: ""
    fun skipVersion(ctx: Context, versionName: String) = prefs(ctx).edit().putString(PREF_SKIP, versionName).apply()
    fun clearSkippedVersion(ctx: Context) = prefs(ctx).edit().remove(PREF_SKIP).apply()
    fun lastCheck(ctx: Context) = prefs(ctx).getLong(PREF_LAST_CHECK, 0L)

    /** Test builds only (CI versionName contains "+"): treat the installed version as 0.0.0. */
    fun isTestBuild() = BuildConfig.VERSION_NAME.contains('+')
    fun isPretendOld(ctx: Context) = isTestBuild() && prefs(ctx).getBoolean(PREF_PRETEND_OLD, false)
    fun setPretendOld(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean(PREF_PRETEND_OLD, on).apply()

    fun installedVersionName(): String = BuildConfig.VERSION_NAME
    private fun installedVersionForCompare(ctx: Context) = if (isPretendOld(ctx)) "0.0.0" else BuildConfig.VERSION_NAME

    /** Whether the pill should appear for [info]. */
    fun shouldNotify(ctx: Context, info: UpdateInfo): Boolean =
        info.isNewer && isNotifyEnabled(ctx) && !dismissedThisSession && skippedVersion(ctx) != info.versionName

    // ── Check ────────────────────────────────────────────────────────────

    /**
     * Fetches the newest release off the main thread; [onResult] runs on the main thread with
     * the parsed release (newer or not), or null when GitHub could not be reached and nothing is
     * cached. Automatic checks ([force] = false) answer from the cache during the cooldown.
     */
    fun check(ctx: Context, force: Boolean = false, onResult: (UpdateInfo?) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            val pre = isIncludePrereleases(app)
            var result: UpdateInfo? = null
            val age = System.currentTimeMillis() - lastCheck(app)
            if (!force && age in 0 until CHECK_COOLDOWN_MS) result = loadCached(app, pre)
            if (result == null) {
                val body = if (pre) httpGet(API_LIST)?.let { pickNewest(it) } else httpGet(API_LATEST)
                val info = body?.let { parseRelease(app, it) }
                if (body != null && info != null) {
                    cache(app, body, pre)
                    prefs(app).edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
                    result = info
                } else {
                    result = loadCached(app, pre)
                }
            }
            Log.i(TAG, "check (force=$force, pre=$pre): " +
                (result?.let { "latest ${it.tag}, newer=${it.isNewer}, apk=${it.apkName}" } ?: "no answer"))
            val r = result
            main.post { onResult(r) }
        }.start()
    }

    private fun httpGet(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "NewTermux/${BuildConfig.VERSION_NAME}")
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() }
            else { Log.w(TAG, "GET $url -> HTTP ${c.responseCode}"); null }
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "GET $url failed: $e")
        null
    }

    /** Newest published, non-draft release from the list (by publish date, not by tag). */
    private fun pickNewest(listBody: String): String? = try {
        val arr = JSONArray(listBody)
        var best: JSONObject? = null
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            if (r.optBoolean("draft")) continue
            if (best == null || r.optString("published_at") > best.optString("published_at")) best = r
        }
        best?.toString()
    } catch (_: Exception) {
        null
    }

    /** Release asset name for this build: package variant (or demo) + the device's preferred ABI. */
    private fun pickAsset(assets: JSONArray?): JSONObject? {
        if (assets == null) return null
        val prefix = if (BuildConfig.IS_DEMO) "newtermux-test-coexist_demo_"
            else "termux-app_${BuildConfig.TERMUX_PACKAGE_VARIANT}-debug_"
        val wanted = Build.SUPPORTED_ABIS.map { "$prefix$it.apk" } + "${prefix}universal.apk"
        for (name in wanted) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name") == name) return a
            }
        }
        return null
    }

    private fun parseRelease(ctx: Context, body: String): UpdateInfo? = try {
        val o = JSONObject(body)
        val tag = o.getString("tag_name")
        val version = Version.clean(tag)
        val notes = o.optString("body", "")
        val asset = pickAsset(o.optJSONArray("assets"))
        val digest = asset?.optString("digest", "") ?: ""
        UpdateInfo(
            versionName = version,
            tag = tag,
            notes = plainNotes(notes),
            highlights = highlights(notes),
            apkName = asset?.optString("name"),
            apkUrl = asset?.optString("browser_download_url")?.takeIf { it.isNotBlank() },
            apkSize = asset?.optLong("size", -1L) ?: -1L,
            apkSha256 = digest.removePrefix("sha256:").lowercase().takeIf { it.length == 64 },
            publishedAt = o.optString("published_at", "").take(10),
            prerelease = o.optBoolean("prerelease"),
            pageUrl = o.optString("html_url", RELEASES_PAGE),
            isNewer = Version.compare(version, installedVersionForCompare(ctx)) > 0,
        )
    } catch (e: Exception) {
        Log.w(TAG, "release parse failed: $e")
        null
    }

    /** "- **Title.** sentence" bullets from the release notes, five at most. */
    private fun highlights(body: String): List<Highlight> {
        val re = Regex("""^\s*[-*]\s+\*\*(.+?)\*\*\s*(.*)$""")
        val out = ArrayList<Highlight>()
        for (line in body.lines()) {
            val m = re.find(line) ?: continue
            val title = m.groupValues[1].replace("`", "").trim().trimEnd('.', ':')
            val text = m.groupValues[2].trim().replace("**", "").replace("`", "")
            if (title.isNotEmpty()) out.add(Highlight(title, text))
            if (out.size == 5) break
        }
        return out
    }

    private fun plainNotes(body: String): String =
        body.lines().filterNot { it.trimStart().startsWith("#") }
            .joinToString("\n").replace("**", "").replace("`", "").trim().take(800)

    private fun cacheFile(ctx: Context, pre: Boolean) = File(ctx.cacheDir, if (pre) CACHE_PRE_NAME else CACHE_NAME)

    private fun cache(ctx: Context, body: String, pre: Boolean) {
        try { cacheFile(ctx, pre).writeText(body) } catch (_: Exception) { }
    }

    private fun loadCached(ctx: Context, pre: Boolean): UpdateInfo? = try {
        val f = cacheFile(ctx, pre)
        if (f.isFile) parseRelease(ctx, f.readText()) else null
    } catch (_: Exception) {
        null
    }

    // ── Download + install ───────────────────────────────────────────────

    fun canInstallPackages(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    private fun requestInstallPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
            } catch (_: Exception) { }
        }
    }

    fun cancelDownload() {
        interrupt.set(true)
        _download.value = DownloadState.Idle
    }

    fun clearDownloadState() {
        _download.value = DownloadState.Idle
    }

    private fun isBusy() = _download.value is DownloadState.Downloading

    /**
     * Downloads the matching APK (progress on [download]), checks its size and sha256, then opens
     * Android's package installer. Without the "install unknown apps" grant it opens that settings
     * screen first and returns.
     */
    fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        val url = info.apkUrl
        if (url == null) {
            Toast.makeText(activity, "No download for this build in that release — opening the release page", Toast.LENGTH_LONG).show()
            openReleasePage(activity, info.pageUrl)
            return
        }
        if (!canInstallPackages(activity)) {
            Toast.makeText(activity, "Allow NewTermux to install apps, then tap Update again", Toast.LENGTH_LONG).show()
            requestInstallPermission(activity)
            return
        }
        if (isBusy()) return
        interrupt.set(false)
        val dir = File(activity.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { runCatching { it.delete() } }
        val apk = File(dir, info.apkName ?: "NewTermux-update.apk")
        _download.value = DownloadState.Downloading(info, 0L, info.apkSize, 0L)
        Thread {
            val error = fetch(info, url, apk)
            main.post {
                when {
                    interrupt.get() -> { apk.delete(); _download.value = DownloadState.Idle }
                    error == null -> { _download.value = DownloadState.Installing(info); install(activity, apk) }
                    else -> { apk.delete(); _download.value = DownloadState.Failed(info, error) }
                }
            }
        }.start()
    }

    /** Returns null on success, else a short reason for the failed card. */
    private fun fetch(info: UpdateInfo, url: String, dest: File): String? {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 15000
                c.readTimeout = 30000
                c.setRequestProperty("User-Agent", "NewTermux/${BuildConfig.VERSION_NAME}")
                if (c.responseCode != 200) return "The server answered HTTP ${c.responseCode}."
                // getContentLengthLong() is API 24+; minSdk is 21
                val total = c.getHeaderField("Content-Length")?.toLongOrNull()?.takeIf { it > 0 } ?: info.apkSize
                val digest = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                var lastAt = System.currentTimeMillis()
                var lastBytes = 0L
                var rate = 0L
                c.inputStream.use { input ->
                    dest.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            if (interrupt.get()) return "Cancelled."
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            bytes += n
                            val now = System.currentTimeMillis()
                            if (now - lastAt >= 500) {
                                val inst = (bytes - lastBytes) * 1000 / (now - lastAt)
                                rate = if (rate == 0L) inst else (rate * 3 + inst) / 4
                                lastAt = now
                                lastBytes = bytes
                                if (_download.value is DownloadState.Downloading)
                                    _download.value = DownloadState.Downloading(info, bytes, total, rate)
                            }
                        }
                    }
                }
                if (info.apkSize > 0 && bytes != info.apkSize) return "The download was incomplete ($bytes of ${info.apkSize} bytes)."
                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                if (info.apkSha256 != null && sha != info.apkSha256) {
                    Log.w(TAG, "sha256 $sha, release says ${info.apkSha256}")
                    return "The downloaded file did not match the release checksum."
                }
                Log.i(TAG, "downloaded ${info.apkName} ($bytes bytes, sha256 $sha)")
                return null
            } finally {
                c.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "download failed: $e")
            return "The download did not finish. Check the connection and try again."
        }
    }

    private fun install(activity: Activity, apk: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, activity.packageName + FILE_PROVIDER_SUFFIX, apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "installer failed: $e")
            _download.value = DownloadState.Idle
            Toast.makeText(activity, "Could not open the installer", Toast.LENGTH_LONG).show()
            openReleasePage(activity, RELEASES_PAGE)
        }
    }

    fun openReleasePage(activity: Activity, url: String = RELEASES_PAGE) {
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
    }
}

/** Semver precedence (build metadata ignored); "v" prefix allowed. */
internal object Version {
    fun clean(v: String): String = v.trim().removePrefix("v").removePrefix("V").substringBefore('+')

    fun compare(a: String, b: String): Int {
        val x = clean(a)
        val y = clean(b)
        val xn = x.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val yn = y.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(xn.size, yn.size)) {
            val d = xn.getOrElse(i) { 0 }.compareTo(yn.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        val xp = if ('-' in x) x.substringAfter('-') else ""
        val yp = if ('-' in y) y.substringAfter('-') else ""
        if (xp.isEmpty() && yp.isEmpty()) return 0
        if (xp.isEmpty()) return 1
        if (yp.isEmpty()) return -1
        val xi = xp.split('.')
        val yi = yp.split('.')
        for (i in 0 until maxOf(xi.size, yi.size)) {
            if (i >= xi.size) return -1
            if (i >= yi.size) return 1
            val xa = xi[i].toIntOrNull()
            val ya = yi[i].toIntOrNull()
            val d = when {
                xa != null && ya != null -> xa.compareTo(ya)
                xa != null -> -1
                ya != null -> 1
                else -> xi[i].compareTo(yi[i])
            }
            if (d != 0) return d
        }
        return 0
    }
}
