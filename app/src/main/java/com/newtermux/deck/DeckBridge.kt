package com.newtermux.deck

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import androidx.compose.runtime.mutableIntStateOf
import com.termux.app.TermuxActivity
import com.termux.app.terminal.MiniTerminalPipView
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.terminal.TerminalSession
import java.lang.ref.WeakReference

/**
 * Links the terminal activity (top screen) with [ControlDeckActivity] (second screen) for the
 * "Dual screen" landscape layout. Everything runs on the main thread.
 */
object DeckBridge {
    private const val TAG = "NT_DECK"
    private const val LAUNCH_TIMEOUT_MS = 3000L

    private var activityRef: WeakReference<TermuxActivity>? = null
    private var deckRef: WeakReference<ControlDeckActivity>? = null
    private var launchAt = 0L
    private var closingByApp = false
    private val main = Handler(Looper.getMainLooper())
    private val pips = ArrayList<MiniTerminalPipView>()

    /** Set when the user closed the deck; cleared when NewTermux comes back to the foreground. */
    @JvmStatic var userHidden = false

    /** Bumped when sessions or drawer commands change, so the deck recomposes. */
    val version = mutableIntStateOf(0)

    /** The deck's extra keys while it is up; the terminal reads CTRL/ALT state from it. */
    @JvmStatic var deckKeys: ExtraKeysView? = null
        internal set

    @JvmStatic
    fun setActivity(activity: TermuxActivity?) {
        activityRef = activity?.let { WeakReference(it) }
    }

    @JvmStatic
    fun clearActivity(activity: TermuxActivity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    fun activity(): TermuxActivity? = activityRef?.get()

    /** A presentation display other than the built-in default one (the Duo's bottom screen). */
    @JvmStatic
    fun secondaryDisplayId(ctx: Context): Int {
        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return -1
        return dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0 }
            ?.displayId ?: -1
    }

    @JvmStatic
    fun hasSecondaryDisplay(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && secondaryDisplayId(ctx) >= 0

    @JvmStatic
    fun isDeckShowing(): Boolean = deckRef?.get()?.let { !it.isFinishing && !it.isDestroyed } == true

    /**
     * Shows the deck on the second display. Returns true when it is up or launching; false when
     * there is no second display or the launch was refused.
     */
    @JvmStatic
    fun showDeck(activity: Activity): Boolean {
        if (isDeckShowing()) return true
        if (launchAt != 0L && System.currentTimeMillis() - launchAt < LAUNCH_TIMEOUT_MS) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val id = secondaryDisplayId(activity)
        if (id < 0) return false
        return try {
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(id)
            val intent = Intent(activity, ControlDeckActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent, options.toBundle())
            launchAt = System.currentTimeMillis()
            Log.i(TAG, "deck launched on display $id")
            // If the deck never shows up (launch silently refused), fall back to the status line.
            main.postDelayed({
                if (deckRef?.get() == null && launchAt != 0L) {
                    Log.w(TAG, "deck did not start on display $id")
                    launchAt = 0L
                    userHidden = true
                    activity()?.onDeckClosed()
                }
            }, LAUNCH_TIMEOUT_MS)
            true
        } catch (e: Exception) {
            Log.w(TAG, "deck launch on display $id failed: $e")
            launchAt = 0L
            false
        }
    }

    /** Closes the deck because the layout no longer wants it (not a user action). */
    @JvmStatic
    fun hideDeck() {
        launchAt = 0L
        val deck = deckRef?.get() ?: return
        closingByApp = true
        deck.finish()
    }

    /** The deck's close button. */
    fun hideByUser() {
        userHidden = true
        deckRef?.get()?.finish()
    }

    internal fun register(deck: ControlDeckActivity) {
        deckRef = WeakReference(deck)
        launchAt = 0L
        closingByApp = false
    }

    internal fun unregister(deck: ControlDeckActivity) {
        if (deckRef?.get() !== deck) return
        deckRef = null
        deckKeys = null
        pips.clear()
        val byApp = closingByApp
        closingByApp = false
        if (!byApp) {
            // Closed with Back, the close button or by the system: keep it closed until NewTermux
            // is resumed, and give the top screen its controls back.
            userHidden = true
            activity()?.onDeckClosed()
        }
    }

    @JvmStatic
    fun notifyChanged() {
        version.intValue++
    }

    @JvmStatic
    fun notifyPipUpdate(session: TerminalSession) {
        for (p in pips) if (p.session === session) p.notifyUpdate()
    }

    internal fun addPip(p: MiniTerminalPipView) { pips.add(p) }
    internal fun removePip(p: MiniTerminalPipView) { pips.remove(p) }
}
