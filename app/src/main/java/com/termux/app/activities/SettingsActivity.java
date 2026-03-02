package com.termux.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.termux.app.TermuxInstaller;

import com.newtermux.features.NewTermuxSettings;
import com.newtermux.features.NewTermuxTheme;

import com.termux.R;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.file.FileUtils;
import com.termux.shared.models.ReportInfo;
import com.termux.app.models.UserAction;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.android.AndroidUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureTermuxAPIPreference(context);
                    configureTermuxFloatPreference(context);
                    configureTermuxTaskerPreference(context);
                    configureTermuxWidgetPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureTermuxAPIPreference(@NonNull Context context) {
            Preference termuxAPIPreference = findPreference("termux_api");
            if (termuxAPIPreference != null) {
                TermuxAPIAppSharedPreferences preferences = TermuxAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxFloatPreference(@NonNull Context context) {
            Preference termuxFloatPreference = findPreference("termux_float");
            if (termuxFloatPreference != null) {
                TermuxFloatAppSharedPreferences preferences = TermuxFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxTaskerPreference(@NonNull Context context) {
            Preference termuxTaskerPreference = findPreference("termux_tasker");
            if (termuxTaskerPreference != null) {
                TermuxTaskerAppSharedPreferences preferences = TermuxTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureTermuxWidgetPreference(@NonNull Context context) {
            Preference termuxWidgetPreference = findPreference("termux_widget");
            if (termuxWidgetPreference != null) {
                TermuxWidgetAppSharedPreferences preferences = TermuxWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(TermuxConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since Termux isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = TermuxUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, TermuxConstants.TERMUX_DONATE_URL);
                    return true;
                });
            }
        }
    }

    public static class FeaturesPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.newtermux_features_preferences, rootKey);
            Context context = getContext();
            if (context == null) return;

            // Sync initial states from our settings store into the preference UI
            String[] boolKeys = {
                NewTermuxSettings.KEY_KEYBOARD_SUGGESTIONS,
                NewTermuxSettings.KEY_SHOW_AC_BUTTON,
                NewTermuxSettings.KEY_SHOW_ROOT_BUTTON,
                NewTermuxSettings.KEY_SHOW_STT_BUTTON,
                NewTermuxSettings.KEY_SHOW_PACKAGES_BUTTON,
                NewTermuxSettings.KEY_SHOW_CLEAR_BUTTON,
                NewTermuxSettings.KEY_SESSION_TABS,
                NewTermuxSettings.KEY_OH_MY_ZSH,
                NewTermuxSettings.KEY_ZSH_AUTOSUGGESTIONS
            };
            for (String key : boolKeys) {
                SwitchPreferenceCompat pref = findPreference(key);
                if (pref == null) continue;
                pref.setChecked(NewTermuxSettings.get(context, key));
            }

            // Generic listener: save to our prefs, then handle side effects
            Preference.OnPreferenceChangeListener listener = (preference, newValue) -> {
                boolean val = (Boolean) newValue;
                String key = preference.getKey();
                NewTermuxSettings.set(context, key, val);

                if (NewTermuxSettings.KEY_ZSH_AUTOSUGGESTIONS.equals(key)) {
                    new Thread(() -> TermuxInstaller.setZshAutosuggestions(context, val)).start();
                } else if (NewTermuxSettings.KEY_OH_MY_ZSH.equals(key) && val) {
                    new Thread(() -> TermuxInstaller.installOhMyZsh(context)).start();
                }
                return true;
            };

            for (String key : boolKeys) {
                Preference pref = findPreference(key);
                if (pref != null) pref.setOnPreferenceChangeListener(listener);
            }
        }
    }

    public static class AppearancePreferencesFragment extends PreferenceFragmentCompat {

        private static final int[] COLOR_VALUES = NewTermuxTheme.COLORS;
        private static final String[] COLOR_KEYS = {
            "color_purple", "color_blue", "color_green", "color_orange",
            "color_red", "color_teal", "color_pink", "color_gold", "color_white"
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.newtermux_appearance_preferences, rootKey);
            Context context = getContext();
            if (context == null) return;

            int current = NewTermuxTheme.getAccentColor(context);

            for (int i = 0; i < COLOR_KEYS.length; i++) {
                final int color = COLOR_VALUES[i];
                Preference pref = findPreference(COLOR_KEYS[i]);
                if (pref == null) continue;
                if (color == current) pref.setSummary("✓ Active");
                pref.setOnPreferenceClickListener(preference -> {
                    NewTermuxTheme.setAccentColor(context, color);
                    for (int j = 0; j < COLOR_KEYS.length; j++) {
                        Preference p = findPreference(COLOR_KEYS[j]);
                        if (p != null) p.setSummary(COLOR_VALUES[j] == color ? "✓ Active" : null);
                    }
                    return true;
                });
            }
        }
    }

}
