package com.newtermux.features;

import android.content.Context;
import android.util.Log;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles auto-correct and command suggestions for NewTermux.
 * Provides two layers: common shell command corrections + Android spell checker.
 */
public class AutoCorrectHandler implements SpellCheckerSession.SpellCheckerSessionListener {

    private static final String TAG = "AutoCorrectHandler";

    public interface SuggestionCallback {
        void onSuggestions(List<String> suggestions, String originalWord);
    }

    private final Context mContext;
    private SpellCheckerSession mSpellCheckerSession;
    private SuggestionCallback mCallback;
    private boolean mEnabled = true;

    // Common shell command corrections
    private static final Map<String, String> COMMAND_CORRECTIONS = new HashMap<String, String>() {{
        put("sl", "ls");
        put("lsa", "ls -a");
        put("lsl", "ls -l");
        put("grpe", "grep");
        put("gerp", "grep");
        put("rn", "rm");
        put("mdkir", "mkdir");
        put("mkidr", "mkdir");
        put("cta", "cat");
        put("ehco", "echo");
        put("ecoh", "echo");
        put("pyhton", "python");
        put("pytohn", "python");
        put("pythno", "python");
        put("pyhton3", "python3");
        put("gti", "git");
        put("got", "git");
        put("suod", "sudo");
        put("sduo", "sudo");
        put("apt-ge", "apt-get");
        put("apt-gt", "apt-get");
        put("apg-get", "apt-get");
        put("pck", "pkg");
        put("pkc", "pkg");
        put("namo", "nano");
        put("naon", "nano");
        put("fim", "vim");
        put("vi m", "vim");
        put("sssh", "ssh");
        put("scp ", "scp");
        put("wgte", "wget");
        put("wgeet", "wget");
        put("curll", "curl");
        put("curlk", "curl");
        put("pythin", "python");
        put("exti", "exit");
        put("exitt", "exit");
        put("clrea", "clear");
        put("celar", "clear");
        put("clar", "clear");
        put("histyory", "history");
        put("histroy", "history");
        put("chnmod", "chmod");
        put("chmo", "chmod");
        put("chonw", "chown");
        put("cdown", "chown");
        put("fild", "find");
        put("finf", "find");
    }};

    public AutoCorrectHandler(Context context) {
        mContext = context;
        initSpellChecker();
    }

    private void initSpellChecker() {
        try {
            TextServicesManager tsm = (TextServicesManager)
                mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (tsm != null) {
                mSpellCheckerSession = tsm.newSpellCheckerSession(null, Locale.getDefault(), this, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Spell checker unavailable: " + e.getMessage());
        }
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setCallback(SuggestionCallback callback) {
        mCallback = callback;
    }

    /**
     * Check if a command has a known correction and return it.
     * @return corrected command or null if no correction available.
     */
    public String getCommandCorrection(String input) {
        if (!mEnabled || input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        // Check the first word (the command itself)
        String[] parts = trimmed.split("\\s+", 2);
        String cmd = parts[0];
        String correction = COMMAND_CORRECTIONS.get(cmd);
        if (correction != null) {
            return parts.length > 1 ? correction + " " + parts[1] : correction;
        }
        return null;
    }

    /**
     * Get all possible suggestions for a word using the spell checker.
     */
    public void getSuggestions(String word) {
        if (!mEnabled || mSpellCheckerSession == null) return;
        mSpellCheckerSession.getSuggestions(new TextInfo(word), 5);
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        if (results == null || mCallback == null) return;
        for (SuggestionsInfo info : results) {
            if (info != null && info.getSuggestionsCount() > 0) {
                List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < info.getSuggestionsCount(); i++) {
                    suggestions.add(info.getSuggestionAt(i));
                }
                // Merge with any command corrections
                String orig = suggestions.isEmpty() ? "" : suggestions.get(0);
                mCallback.onSuggestions(suggestions, orig);
            }
        }
    }

    @Override
    public void onGetSentenceSuggestions(android.view.textservice.SentenceSuggestionsInfo[] results) {}

    public void destroy() {
        if (mSpellCheckerSession != null) {
            mSpellCheckerSession.close();
            mSpellCheckerSession = null;
        }
    }
}
