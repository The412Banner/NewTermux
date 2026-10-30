package com.newtermux.deck

import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
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
import java.util.Collections
import java.util.WeakHashMap

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
    private val main = Handler(Looper.getMainLooper())

    // Our activities (Settings and what it opens) living on the second screen; the deck stays
    // hidden while there are any, so it doesn't cover them.
    private val secondScreenActivities: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
    private var secondScreenLaunchAt = 0L
    private var callbacksRegistered = false
    private const val SECOND_SCREEN_LAUNCH_TIMEOUT_MS = 3000L

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

    /** Settings (or another of our screens) is open on the second screen, or about to be. */
    @JvmStatic
    fun isSecondScreenBusy(): Boolean =
        secondScreenActivities.isNotEmpty() ||
            (secondScreenLaunchAt != 0L && System.currentTimeMillis() - secondScreenLaunchAt < SECOND_SCREEN_LAUNCH_TIMEOUT_MS)

    /**
     * Opens [intent] on the second screen in place of the deck, so the terminal stays readable on
     * the main screen; the deck comes back when the last such screen closes. Returns false when it
     * can't (no second display / launch refused) so the caller opens it normally.
     */
    @JvmStatic
    fun openOnSecondScreen(activity: TermuxActivity, intent: Intent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (isSecondScreenBusy()) return true
        val display = secondaryDisplay(activity) ?: return false
        trackSecondScreenActivities(activity.application)
        return try {
            secondScreenLaunchAt = System.currentTimeMillis()
            hideDeck()
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId)
            // A task can't span displays: start a separate one there.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            activity.startActivity(intent, options.toBundle())
            Log.i(TAG, "${intent.component?.shortClassName} opened on display ${display.displayId}")
            // Launch refused without an exception: give the deck back after the timeout.
            main.postDelayed({
                if (secondScreenActivities.isEmpty() && secondScreenLaunchAt != 0L) {
                    secondScreenLaunchAt = 0L
                    activity()?.onDeckClosed()
                }
            }, SECOND_SCREEN_LAUNCH_TIMEOUT_MS)
            true
        } catch (e: Exception) {
            Log.w(TAG, "open on display ${display.displayId} failed: $e")
            secondScreenLaunchAt = 0L
            activity()?.onDeckClosed()
            false
        }
    }

    private fun trackSecondScreenActivities(app: Application) {
        if (callbacksRegistered) return
        callbacksRegistered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
                if (a is TermuxActivity || displayIdOf(a) == Display.DEFAULT_DISPLAY) return
                secondScreenActivities.add(a)
                secondScreenLaunchAt = 0L
            }

            override fun onActivityDestroyed(a: Activity) {
                if (!secondScreenActivities.remove(a) || a.isChangingConfigurations) return
                // Last one gone (e.g. Settings closed with Back): put the deck back.
                if (secondScreenActivities.isEmpty()) main.post { activity()?.onDeckClosed() }
            }

            override fun onActivityStarted(a: Activity) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
        })
    }

    @Suppress("DEPRECATION")
    private fun displayIdOf(a: Activity): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) a.display?.displayId ?: Display.DEFAULT_DISPLAY
        else a.windowManager.defaultDisplay.displayId

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
