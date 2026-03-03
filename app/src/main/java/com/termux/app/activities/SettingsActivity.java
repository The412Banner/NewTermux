package com.termux.app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.termux.app.TermuxActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.termux.app.TermuxInstaller;

import com.newtermux.features.NewTermuxColorTheme;
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

    public static class BackupPreferencesFragment extends PreferenceFragmentCompat {

        private ActivityResultLauncher<String[]> mRestoreFilePicker;
        private ActivityResultLauncher<String> mBasicBackupSaver;
        private ActivityResultLauncher<String> mFullBackupSaver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mBasicBackupSaver = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/gzip"),
                uri -> {
                    if (uri == null) return;
                    Context ctx = getContext();
                    if (ctx == null) return;
                    runBackupToUri(ctx, uri, false);
                }
            );

            mFullBackupSaver = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/gzip"),
                uri -> {
                    if (uri == null) return;
                    Context ctx = getContext();
                    if (ctx == null) return;
                    runBackupToUri(ctx, uri, true);
                }
            );

            mRestoreFilePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    Context ctx = getContext();
                    if (ctx == null) return;
                    new AlertDialog.Builder(ctx)
                        .setTitle("What type of backup is this?")
                        .setMessage("Choose the correct type so the right restore method is used.")
                        .setPositiveButton("Full (home + usr)", (d, w) -> confirmRestore(ctx, uri, true))
                        .setNeutralButton("Basic (home only)", (d, w) -> confirmRestore(ctx, uri, false))
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            );
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.newtermux_backup_preferences, rootKey);
            Context context = getContext();
            if (context == null) return;

            Preference basicBackupPref = findPreference("basic_backup_now");
            if (basicBackupPref != null) {
                basicBackupPref.setOnPreferenceClickListener(pref -> {
                    mBasicBackupSaver.launch("termux-home-backup.tar.gz");
                    return true;
                });
            }

            Preference fullBackupPref = findPreference("full_backup_now");
            if (fullBackupPref != null) {
                fullBackupPref.setOnPreferenceClickListener(pref -> {
                    mFullBackupSaver.launch("termux-full-backup.tar.gz");
                    return true;
                });
            }

            Preference restorePref = findPreference("restore_now");
            if (restorePref != null) {
                restorePref.setOnPreferenceClickListener(pref -> {
                    mRestoreFilePicker.launch(new String[]{"*/*"});
                    return true;
                });
            }
        }

        private void runBackupToUri(Context context, Uri uri, boolean full) {
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setMessage(full ? "Backing up home + usr\u2026" : "Backing up home\u2026");
            dialog.setCancelable(false);
            dialog.show();
            new Thread(() -> {
                String error = null;
                try {
                    String[] cmd = full
                        ? new String[]{
                            "/data/data/com.termux/files/usr/bin/tar", "-zcf", "-",
                            "-C", "/data/data/com.termux/files", "./home", "./usr"
                          }
                        : new String[]{
                            "/data/data/com.termux/files/usr/bin/tar", "-zcf", "-",
                            "-C", "/data/data/com.termux/files/home", "."
                          };
                    Process p = Runtime.getRuntime().exec(cmd);
                    try (java.io.InputStream in = p.getInputStream();
                         java.io.OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    String errText = readStream(p.getErrorStream());
                    int exit = p.waitFor();
                    if (exit != 0) error = errText.isEmpty() ? "tar exited with code " + exit : errText;
                } catch (Exception e) {
                    error = e.getMessage();
                }
                final String finalError = error;
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(context,
                        finalError == null ? "Backup complete" : "Backup failed: " + finalError,
                        finalError == null ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG
                    ).show();
                });
            }).start();
        }

        private void confirmRestore(Context context, Uri fileUri, boolean full) {
            String msg = full
                ? "This will overwrite your home and usr directories. Continue?"
                : "This will overwrite your home directory. Continue?";
            new AlertDialog.Builder(context)
                .setTitle("Restore Termux")
                .setMessage(msg)
                .setPositiveButton("Restore", (d, w) -> runRestore(context, fileUri, full))
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void runRestore(Context context, Uri fileUri, boolean full) {
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setMessage("Restoring\u2026");
            dialog.setCancelable(false);
            dialog.show();
            new Thread(() -> {
                String error = null;
                try {
                    String filePath;
                    if ("content".equals(fileUri.getScheme())) {
                        java.io.File tmp = new java.io.File(context.getCacheDir(), "restore_tmp.tar.gz");
                        try (java.io.InputStream in = context.getContentResolver().openInputStream(fileUri);
                             java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                        }
                        filePath = tmp.getAbsolutePath();
                    } else {
                        filePath = fileUri.getPath();
                    }
                    Process p;
                    if (full) {
                        p = Runtime.getRuntime().exec(new String[]{
                            "/data/data/com.termux/files/usr/bin/tar", "-zxvf", filePath,
                            "-C", "/data/data/com.termux/files",
                            "--recursive-unlink", "--preserve-permissions"
                        });
                    } else {
                        p = Runtime.getRuntime().exec(new String[]{
                            "/data/data/com.termux/files/usr/bin/tar", "-zxvf", filePath,
                            "-C", "/data/data/com.termux/files/home"
                        });
                    }
                    String errText = readStream(p.getErrorStream());
                    int exit = p.waitFor();
                    if (exit != 0) error = errText;
                } catch (Exception e) {
                    error = e.getMessage();
                }
                final String finalError = error;
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(context,
                        finalError == null ? "Restore complete" : "Restore failed: " + finalError,
                        finalError == null ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG
                    ).show();
                });
            }).start();
        }

        private static int runCommand(String[] cmd) throws Exception {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            return p.exitValue();
        }

        private static String readStream(java.io.InputStream is) throws java.io.IOException {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            return baos.toString();
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

                return true;
            };

            for (String key : boolKeys) {
                Preference pref = findPreference(key);
                if (pref != null) pref.setOnPreferenceChangeListener(listener);
            }

            // --- Shell section ---
            boolean zshInstalled = new java.io.File(
                com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH, "bin/zsh").exists();

            SwitchPreferenceCompat autosuggPref   = findPreference(NewTermuxSettings.KEY_ZSH_AUTOSUGGESTIONS);
            SwitchPreferenceCompat syntaxHlPref   = findPreference(NewTermuxSettings.KEY_ZSH_SYNTAX_HIGHLIGHTING);

            if (autosuggPref != null) {
                autosuggPref.setEnabled(zshInstalled);
                autosuggPref.setChecked(NewTermuxSettings.isZshAutosuggestionsEnabled(context));
                if (!zshInstalled) autosuggPref.setSummary("Install Zsh first to enable this");
                autosuggPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean val = (Boolean) newValue;
                    NewTermuxSettings.set(context, NewTermuxSettings.KEY_ZSH_AUTOSUGGESTIONS, val);
                    new Thread(() -> TermuxInstaller.setZshAutosuggestions(context, val)).start();
                    showRestartWarning();
                    return true;
                });
            }

            if (syntaxHlPref != null) {
                syntaxHlPref.setEnabled(zshInstalled);
                syntaxHlPref.setChecked(NewTermuxSettings.isZshSyntaxHighlightingEnabled(context));
                if (!zshInstalled) syntaxHlPref.setSummary("Install Zsh first to enable this");
                syntaxHlPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean val = (Boolean) newValue;
                    NewTermuxSettings.set(context, NewTermuxSettings.KEY_ZSH_SYNTAX_HIGHLIGHTING, val);
                    new Thread(() -> TermuxInstaller.setZshSyntaxHighlighting(context, val)).start();
                    showRestartWarning();
                    return true;
                });
            }

            Preference installZshPref = findPreference("install_zsh");
            if (installZshPref != null) {
                if (zshInstalled) {
                    installZshPref.setTitle("Zsh");
                    installZshPref.setSummary("✓ Installed");
                    installZshPref.setEnabled(false);
                } else {
                    installZshPref.setOnPreferenceClickListener(pref -> {
                        NewTermuxSettings.setPendingCommand(context, "pkg install zsh\n");
                        Activity activity = getActivity();
                        if (activity != null) activity.finish();
                        return true;
                    });
                }
            }

            // --- Storage permission ---
            Preference storagePermPref = findPreference("grant_storage_permission");
            if (storagePermPref != null) {
                storagePermPref.setOnPreferenceClickListener(pref -> {
                    Activity activity = getActivity();
                    if (activity instanceof TermuxActivity) {
                        ((TermuxActivity) activity).requestStoragePermission(false);
                    }
                    return true;
                });
            }
        }

        private void showRestartWarning() {
            Activity activity = getActivity();
            if (activity == null) return;
            new AlertDialog.Builder(activity)
                .setTitle("Restart Required")
                .setMessage("Start a new terminal session for this change to take effect.")
                .setPositiveButton("OK", null)
                .show();
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

            // --- Terminal Theme ---
            String currentTheme = NewTermuxColorTheme.getCurrentTheme(context);
            for (String themeKey : NewTermuxColorTheme.THEME_KEYS) {
                final String key = themeKey;
                Preference pref = findPreference("theme_" + key);
                if (pref == null) continue;
                if (key.equals(currentTheme)) pref.setSummary("✓ Active");
                pref.setOnPreferenceClickListener(preference -> {
                    NewTermuxColorTheme.applyTheme(context, key);
                    for (String k : NewTermuxColorTheme.THEME_KEYS) {
                        Preference p = findPreference("theme_" + k);
                        if (p != null) p.setSummary(k.equals(key) ? "✓ Active" : null);
                    }
                    return true;
                });
            }

            // --- Accent Color ---
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
