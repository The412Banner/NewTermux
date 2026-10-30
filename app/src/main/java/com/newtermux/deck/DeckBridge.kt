package com.newtermux.deck

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.compose.runtime.mutableIntStateOf
import com.termux.app.TermuxActivity
import com.termux.app.terminal.MiniTerminalPipView
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.terminal.TerminalSession
import java.lang.ref.WeakReference

/**
 * Links the terminal activity (top screen) with [ControlDeckPresentation] (second screen) for the
 * "Dual screen" landscape layout. Everything runs on the main thread.
 */
object DeckBridge {
    private const val TAG = "NT_DECK"

    private var activityRef: WeakReference<TermuxActivity>? = null
    private var deck: ControlDeckPresentation? = null
    private var closingByApp = false
    private val pips = ArrayList<MiniTerminalPipView>()

    /** Set when the user closed the deck; cleared when NewTermux is started again. */
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
    fun secondaryDisplay(ctx: Context): Display? {
        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return null
        return dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && (it.flags and Display.FLAG_PRIVATE) == 0 }
    }

    @JvmStatic
    fun hasSecondaryDisplay(ctx: Context): Boolean = secondaryDisplay(ctx) != null

    @JvmStatic
    fun isDeckShowing(): Boolean = deck?.isShowing == true

    /**
     * Shows the deck on the second display. Returns true when it is up; false when there is no
     * second display or the window could not be added.
     */
    @JvmStatic
    fun showDeck(activity: TermuxActivity): Boolean {
        if (isDeckShowing()) return true
        val display = secondaryDisplay(activity) ?: return false
        return try {
            val p = ControlDeckPresentation(activity, display)
            p.setOnDismissListener { onDismissed(p) }
            closingByApp = false
            p.show()
            deck = p
            Log.i(TAG, "deck shown on display ${display.displayId}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "deck on display ${display.displayId} failed: $e")
            false
        }
    }

    /** Closes the deck because the layout no longer wants it (not a user action). */
    @JvmStatic
    fun hideDeck() {
        val p = deck ?: return
        closingByApp = true
        try { p.dismiss() } catch (_: Exception) { }
    }

    /** The deck's close button. */
    fun hideByUser() {
        userHidden = true
        try { deck?.dismiss() } catch (_: Exception) { }
    }

    /** Runs on the main thread after a dismiss, also when the display goes away. */
    private fun onDismissed(p: ControlDeckPresentation) {
        if (deck !== p) return
        deck = null
        deckKeys = null
        pips.clear()
        val byApp = closingByApp
        closingByApp = false
        if (!byApp) {
            // Closed with the close button or because the display went away: keep it closed until
            // NewTermux is started again, and give the top screen its controls back.
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
