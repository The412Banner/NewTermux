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
    public static final String KEY_SHOW_CLEAR_BUTTON    = "show_clear_button";
    public static final String KEY_ZSH_AUTOSUGGESTIONS       = "zsh_autosuggestions";
    public static final String KEY_ZSH_SYNTAX_HIGHLIGHTING   = "zsh_syntax_highlighting";
    public static final String KEY_SESSION_TABS              = "session_tabs";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isKeyboardSuggestionsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_KEYBOARD_SUGGESTIONS, false); // OFF by default
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
    public static boolean isZshAutosuggestionsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ZSH_AUTOSUGGESTIONS, true);
    }
    public static boolean isZshSyntaxHighlightingEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ZSH_SYNTAX_HIGHLIGHTING, true);
    }
    public static boolean isSessionTabsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SESSION_TABS, true);
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
            case KEY_ZSH_AUTOSUGGESTIONS:      return isZshAutosuggestionsEnabled(ctx);
            case KEY_ZSH_SYNTAX_HIGHLIGHTING:  return isZshSyntaxHighlightingEnabled(ctx);
            case KEY_SESSION_TABS:             return isSessionTabsEnabled(ctx);
            default:                       return prefs(ctx).getBoolean(key, false);
        }
    }
}
