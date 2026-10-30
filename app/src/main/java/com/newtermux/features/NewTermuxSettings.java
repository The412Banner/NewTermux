package com.newtermux.features;

import android.content.Context;
import android.content.SharedPreferences;

public class NewTermuxSettings {
    private static final String PREFS_NAME = "newtermux_settings";

    // Keys
    public static final String KEY_KEYBOARD_SUGGESTIONS = "keyboard_suggestions";
    public static final String KEY_SHOW_AC_BUTTON       = "show_ac_button";
    public static final String KEY_SHOW_ROOT_BUTTON     = "show_root_button";
    public static final String KEY_SHOW_STT_BUTTON      = "show_stt_button";
    public static final String KEY_SHOW_PACKAGES_BUTTON = "show_packages_button";
    public static final String KEY_SHOW_CLEAR_BUTTON        = "show_clear_button";
    public static final String KEY_ZSH_PLUGINS = "zsh_plugins";
    public static final String KEY_SESSION_TABS             = "session_tabs";
    public static final String KEY_AUTOCORRECT               = "autocorrect_enabled";
    public static final String KEY_SHOW_DRAWER_EXPORT_SCRIPT = "show_drawer_export_script";
    public static final String KEY_SHOW_DRAWER_PKG_UPDATE    = "show_drawer_pkg_update";
    public static final String KEY_SHOW_DRAWER_CMD_BUTTONS   = "show_drawer_cmd_buttons";
    public static final String KEY_STARTUP_SCRIPT_ENABLED    = "startup_script_enabled";
    public static final String KEY_URL_DETECTION_ENABLED     = "url_detection_enabled";
    public static final String KEY_SESSION_RENAME_ENABLED    = "session_rename_enabled";
    public static final String KEY_TEXT_EXPANSION_ENABLED    = "text_expansion_enabled";
    public static final String KEY_EXTRA_KEYS_VISIBLE        = "extra_keys_visible";
    public static final String KEY_EXTRA_KEYS_IN_DRAWER      = "extra_keys_in_drawer";
    public static final String KEY_KEEP_ALIVE_BACKGROUND     = "keep_alive_background";
    // Internal one-time gate: set once we have shown the battery-optimization nudge.
    public static final String KEY_BATTERY_OPT_PROMPTED      = "battery_opt_prompted";
    // Screen layout per orientation (String values below), set in Settings → Appearance.
    public static final String KEY_LAYOUT_PORTRAIT           = "layout_portrait";
    public static final String KEY_LAYOUT_LANDSCAPE          = "layout_landscape";
    public static final String LAYOUT_STANDARD               = "standard";     // toolbar + session previews on top
    public static final String LAYOUT_STATUS_LINE            = "status_line";  // one tmux-style line at the bottom

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isKeyboardSuggestionsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_KEYBOARD_SUGGESTIONS, false);
    }
    public static void setKeyboardSuggestions(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(KEY_KEYBOARD_SUGGESTIONS, v).apply();
    }

    public static boolean isShowAcButton(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_AC_BUTTON, true);
    }
    public static boolean isShowRootButton(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_ROOT_BUTTON, true);
    }
    public static boolean isShowSttButton(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_STT_BUTTON, true);
    }
    public static boolean isShowPackagesButton(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_PACKAGES_BUTTON, true);
    }
    public static boolean isShowClearButton(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_CLEAR_BUTTON, true);
    }
    public static boolean isZshPluginsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ZSH_PLUGINS, false);
    }
    public static boolean isSessionTabsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SESSION_TABS, true);
    }
    public static boolean isAutocorrectEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTOCORRECT, true);
    }
    public static boolean isShowDrawerExportScript(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_DRAWER_EXPORT_SCRIPT, true);
    }
    public static boolean isShowDrawerPkgUpdate(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_DRAWER_PKG_UPDATE, true);
    }
    public static boolean isShowDrawerCmdButtons(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SHOW_DRAWER_CMD_BUTTONS, true);
    }
    public static boolean isStartupScriptEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_STARTUP_SCRIPT_ENABLED, false);
    }
    public static boolean isUrlDetectionEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_URL_DETECTION_ENABLED, true);
    }
    public static boolean isSessionRenameEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SESSION_RENAME_ENABLED, true);
    }
    public static boolean isTextExpansionEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_TEXT_EXPANSION_ENABLED, false);
    }
    public static boolean isExtraKeysVisible(Context ctx) {
        return prefs(ctx).getBoolean(KEY_EXTRA_KEYS_VISIBLE, true);
    }
    public static boolean isExtraKeysInDrawer(Context ctx) {
        return prefs(ctx).getBoolean(KEY_EXTRA_KEYS_IN_DRAWER, false);
    }
    /** Whether the process should hold a foreground wake lock while sessions are alive so it
     *  survives being backgrounded by a fullscreen game. Defaults ON. */
    public static boolean isKeepAliveInBackground(Context ctx) {
        return prefs(ctx).getBoolean(KEY_KEEP_ALIVE_BACKGROUND, true);
    }

    // One-time battery-optimization nudge gate.
    public static boolean wasBatteryOptPrompted(Context ctx) {
        return prefs(ctx).getBoolean(KEY_BATTERY_OPT_PROMPTED, false);
    }
    public static void setBatteryOptPrompted(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(KEY_BATTERY_OPT_PROMPTED, v).apply();
    }

    // Screen layout for portrait or landscape. Defaults to the standard layout.
    public static String getLayout(Context ctx, boolean landscape) {
        return prefs(ctx).getString(landscape ? KEY_LAYOUT_LANDSCAPE : KEY_LAYOUT_PORTRAIT, LAYOUT_STANDARD);
    }
    public static void setLayout(Context ctx, boolean landscape, String layout) {
        prefs(ctx).edit().putString(landscape ? KEY_LAYOUT_LANDSCAPE : KEY_LAYOUT_PORTRAIT, layout).apply();
    }

    // Pending command — written by Settings, consumed and cleared by TermuxActivity.onResume()
    public static String getPendingCommand(Context ctx) {
        return prefs(ctx).getString("pending_command", null);
    }
    public static void setPendingCommand(Context ctx, String cmd) {
        prefs(ctx).edit().putString("pending_command", cmd).apply();
    }
    public static void clearPendingCommand(Context ctx) {
        prefs(ctx).edit().remove("pending_command").apply();
    }

    // Generic setter for all boolean keys (used by preference listener)
    public static void set(Context ctx, String key, boolean value) {
        prefs(ctx).edit().putBoolean(key, value).apply();
    }

    // Generic getter — uses per-key defaults
    public static boolean get(Context ctx, String key) {
        switch (key) {
            case KEY_KEYBOARD_SUGGESTIONS: return isKeyboardSuggestionsEnabled(ctx);
            case KEY_SHOW_AC_BUTTON:       return isShowAcButton(ctx);
            case KEY_SHOW_ROOT_BUTTON:     return isShowRootButton(ctx);
            case KEY_SHOW_STT_BUTTON:      return isShowSttButton(ctx);
            case KEY_SHOW_PACKAGES_BUTTON: return isShowPackagesButton(ctx);
            case KEY_SHOW_CLEAR_BUTTON:    return isShowClearButton(ctx);
            case KEY_ZSH_PLUGINS:                  return isZshPluginsEnabled(ctx);
            case KEY_SESSION_TABS:                 return isSessionTabsEnabled(ctx);
            case KEY_AUTOCORRECT:                  return isAutocorrectEnabled(ctx);
            case KEY_SHOW_DRAWER_EXPORT_SCRIPT:    return isShowDrawerExportScript(ctx);
            case KEY_SHOW_DRAWER_PKG_UPDATE:       return isShowDrawerPkgUpdate(ctx);
            case KEY_SHOW_DRAWER_CMD_BUTTONS:      return isShowDrawerCmdButtons(ctx);
            case KEY_STARTUP_SCRIPT_ENABLED:       return isStartupScriptEnabled(ctx);
            case KEY_URL_DETECTION_ENABLED:        return isUrlDetectionEnabled(ctx);
            case KEY_SESSION_RENAME_ENABLED:       return isSessionRenameEnabled(ctx);
            case KEY_TEXT_EXPANSION_ENABLED:       return isTextExpansionEnabled(ctx);
            case KEY_EXTRA_KEYS_VISIBLE:           return isExtraKeysVisible(ctx);
            case KEY_EXTRA_KEYS_IN_DRAWER:         return isExtraKeysInDrawer(ctx);
            case KEY_KEEP_ALIVE_BACKGROUND:        return isKeepAliveInBackground(ctx);
            default:                               return prefs(ctx).getBoolean(key, false);
        }
    }
}
