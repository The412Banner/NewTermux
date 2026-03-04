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

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.newtermux.features.ColorPickerDialog;
import com.newtermux.features.NewTermuxColorTheme;
import com.newtermux.features.NewTermuxSettings;
import com.newtermux.features.NewTermuxTheme;

import java.util.LinkedHashMap;
import java.util.Map;

import com.termux.R;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.file.FileUtils;
import com.termux.shared.models.ReportInfo;
import com.termux.app.models.UserAction;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.android.PackageUtils;
import android.content.Intent;
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

            Preference sshManagerPref = findPreference("ssh_manager");
            if (sshManagerPref != null) {
                sshManagerPref.setOnPreferenceClickListener(pref -> {
                    startActivity(new Intent(context, SshManagerActivity.class));
                    return true;
                });
            }

            Preference fileManagerPref = findPreference("file_manager");
            if (fileManagerPref != null) {
                fileManagerPref.setOnPreferenceClickListener(pref -> {
                    startActivity(new Intent(context, FileManagerActivity.class));
                    return true;
                });
            }
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
                NewTermuxSettings.KEY_AUTOCORRECT,
                NewTermuxSettings.KEY_SHOW_DRAWER_EXPORT_SCRIPT,
                NewTermuxSettings.KEY_SHOW_DRAWER_PKG_UPDATE,
                NewTermuxSettings.KEY_SHOW_DRAWER_CMD_BUTTONS,
                NewTermuxSettings.KEY_STARTUP_SCRIPT_ENABLED,
                NewTermuxSettings.KEY_URL_DETECTION_ENABLED,
                NewTermuxSettings.KEY_SESSION_RENAME_ENABLED,
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

            SwitchPreferenceCompat pluginsPref = findPreference(NewTermuxSettings.KEY_ZSH_PLUGINS);

            if (pluginsPref != null) {
                pluginsPref.setEnabled(zshInstalled);
                pluginsPref.setChecked(NewTermuxSettings.isZshPluginsEnabled(context));
                if (!zshInstalled) pluginsPref.setSummary("Install Zsh first to enable this");
                pluginsPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean val = (Boolean) newValue;
                    NewTermuxSettings.set(context, NewTermuxSettings.KEY_ZSH_PLUGINS, val);
                    new Thread(() -> TermuxInstaller.setZshPlugins(context, val)).start();
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

            // --- Edit Startup Script ---
            Preference editScriptPref = findPreference("edit_startup_script");
            if (editScriptPref != null) {
                editScriptPref.setOnPreferenceClickListener(pref -> {
                    showStartupScriptEditor(context);
                    return true;
                });
            }

        }

        private void showStartupScriptEditor(Context context) {
            java.io.File scriptFile = new java.io.File(
                com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH,
                ".termux/startup-script.sh");

            String existing = "";
            if (scriptFile.exists()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(scriptFile))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append('\n');
                    existing = sb.toString();
                } catch (java.io.IOException ignored) {}
            }

            android.widget.EditText editor = new android.widget.EditText(context);
            editor.setText(existing);
            editor.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            editor.setTypeface(android.graphics.Typeface.MONOSPACE);
            int pad = (int) (12 * context.getResources().getDisplayMetrics().density);
            editor.setPadding(pad, pad, pad, pad);
            editor.setMinLines(10);

            new AlertDialog.Builder(requireContext())
                .setTitle("Edit Startup Script")
                .setView(editor)
                .setPositiveButton("Save", (d, w) -> {
                    String content = editor.getText().toString();
                    try {
                        scriptFile.getParentFile().mkdirs();
                        try (java.io.FileWriter fw = new java.io.FileWriter(scriptFile)) {
                            fw.write(content);
                        }
                        android.widget.Toast.makeText(context, "Startup script saved", android.widget.Toast.LENGTH_SHORT).show();
                    } catch (java.io.IOException e) {
                        android.widget.Toast.makeText(context, "Failed to save: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    public static class TextExpansionPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.newtermux_text_expansion_preferences, rootKey);
            Context context = getContext();
            if (context == null) return;

            SwitchPreferenceCompat togglePref = findPreference(NewTermuxSettings.KEY_TEXT_EXPANSION_ENABLED);
            if (togglePref != null) {
                togglePref.setChecked(NewTermuxSettings.isTextExpansionEnabled(context));
                togglePref.setOnPreferenceChangeListener((pref, newValue) -> {
                    NewTermuxSettings.set(context, NewTermuxSettings.KEY_TEXT_EXPANSION_ENABLED, (Boolean) newValue);
                    return true;
                });
            }

            Preference manageExpPref = findPreference("manage_text_expansions");
            if (manageExpPref != null) {
                manageExpPref.setOnPreferenceClickListener(pref -> {
                    showManageExpansionsDialog(context);
                    return true;
                });
            }
        }

        private void showManageExpansionsDialog(Context context) {
            int dp = (int) context.getResources().getDisplayMetrics().density;

            android.widget.LinearLayout listContainer = new android.widget.LinearLayout(context);
            listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            listContainer.setPadding(0, 0, 0, 0);

            android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
            scrollView.addView(listContainer);
            android.widget.LinearLayout.LayoutParams scrollParams =
                new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 300 * dp);
            scrollView.setLayoutParams(scrollParams);

            android.widget.Button addNewBtn = new android.widget.Button(context);
            addNewBtn.setText("Add New");

            android.widget.LinearLayout container = new android.widget.LinearLayout(context);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(12 * dp, 8 * dp, 12 * dp, 8 * dp);
            container.addView(scrollView);
            container.addView(addNewBtn);

            Runnable[] refreshRef = new Runnable[1];
            refreshRef[0] = () -> {
                listContainer.removeAllViews();
                java.util.List<com.newtermux.features.TextExpansionStore.TextExpansion> expansions =
                    com.newtermux.features.TextExpansionStore.load(context);

                if (expansions.isEmpty()) {
                    android.widget.TextView empty = new android.widget.TextView(context);
                    empty.setText("No expansions yet. Tap 'Add New' to create one.");
                    empty.setPadding(0, 8 * dp, 0, 8 * dp);
                    listContainer.addView(empty);
                    return;
                }

                for (int i = 0; i < expansions.size(); i++) {
                    final int idx = i;
                    com.newtermux.features.TextExpansionStore.TextExpansion exp = expansions.get(idx);

                    android.widget.LinearLayout row = new android.widget.LinearLayout(context);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setPadding(0, 4 * dp, 0, 4 * dp);

                    android.widget.TextView label = new android.widget.TextView(context);
                    label.setText(exp.trigger + "  →  " + exp.expansion);
                    label.setTypeface(android.graphics.Typeface.MONOSPACE);
                    label.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    android.widget.Button deleteBtn = new android.widget.Button(context);
                    deleteBtn.setText("Delete");

                    row.addView(label);
                    row.addView(deleteBtn);
                    listContainer.addView(row);

                    row.setOnClickListener(v ->
                        showEditExpansionDialog(context, idx, refreshRef[0]));

                    deleteBtn.setOnClickListener(v -> {
                        expansions.remove(idx);
                        com.newtermux.features.TextExpansionStore.save(context, expansions);
                        refreshRef[0].run();
                    });
                }
            };

            refreshRef[0].run();

            addNewBtn.setOnClickListener(v ->
                showEditExpansionDialog(context, -1, refreshRef[0]));

            new AlertDialog.Builder(requireContext())
                .setTitle("Text Expansions")
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
        }

        private void showEditExpansionDialog(Context context, int editIdx, Runnable onSave) {
            int dp = (int) context.getResources().getDisplayMetrics().density;

            java.util.List<com.newtermux.features.TextExpansionStore.TextExpansion> current =
                com.newtermux.features.TextExpansionStore.load(context);
            String initTrigger = (editIdx >= 0 && editIdx < current.size()) ? current.get(editIdx).trigger : "";
            String initExpansion = (editIdx >= 0 && editIdx < current.size()) ? current.get(editIdx).expansion : "";

            android.widget.EditText triggerEdit = new android.widget.EditText(context);
            triggerEdit.setHint("Trigger (e.g. ;ll)");
            triggerEdit.setText(initTrigger);
            triggerEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            android.widget.EditText expansionEdit = new android.widget.EditText(context);
            expansionEdit.setHint("Expansion (e.g. ls -la)");
            expansionEdit.setText(initExpansion);
            expansionEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(12 * dp, 8 * dp, 12 * dp, 8 * dp);
            layout.addView(triggerEdit);
            layout.addView(expansionEdit);

            new AlertDialog.Builder(requireContext())
                .setTitle(editIdx >= 0 ? "Edit Expansion" : "Add Expansion")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String trigger = triggerEdit.getText().toString().trim();
                    String expansion = expansionEdit.getText().toString();
                    if (trigger.isEmpty()) {
                        android.widget.Toast.makeText(context, "Trigger cannot be empty",
                            android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    java.util.List<com.newtermux.features.TextExpansionStore.TextExpansion> list =
                        com.newtermux.features.TextExpansionStore.load(context);
                    if (editIdx >= 0 && editIdx < list.size()) {
                        list.get(editIdx).trigger = trigger;
                        list.get(editIdx).expansion = expansion;
                    } else {
                        com.newtermux.features.TextExpansionStore.TextExpansion newExp =
                            new com.newtermux.features.TextExpansionStore.TextExpansion();
                        newExp.trigger = trigger;
                        newExp.expansion = expansion;
                        list.add(newExp);
                    }
                    com.newtermux.features.TextExpansionStore.save(context, list);
                    onSave.run();
                })
                .setNegativeButton("Cancel", null)
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

            // --- Terminal Theme (presets) ---
            String currentTheme = NewTermuxColorTheme.getCurrentTheme(context);
            for (String themeKey : NewTermuxColorTheme.THEME_KEYS) {
                if (NewTermuxColorTheme.THEME_KEY_CUSTOM.equals(themeKey)) continue;
                final String key = themeKey;
                Preference pref = findPreference("theme_" + key);
                if (pref == null) continue;
                if (key.equals(currentTheme)) pref.setSummary("✓ Active");
                pref.setOnPreferenceClickListener(preference -> {
                    NewTermuxColorTheme.applyTheme(context, key);
                    refreshThemeSummaries(key);
                    return true;
                });
            }

            // --- Terminal Theme (custom) ---
            Preference themeCustomPref = findPreference("theme_custom");
            if (themeCustomPref != null) {
                if (NewTermuxColorTheme.THEME_KEY_CUSTOM.equals(currentTheme))
                    themeCustomPref.setSummary("✓ Active");
                themeCustomPref.setOnPreferenceClickListener(pref -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Custom Theme Scope")
                        .setItems(new String[]{
                            "Core 3  (Background, Foreground, Cursor)",
                            "All 18 terminal colors"
                        }, (d, which) -> showThemeEditor(context, which == 1))
                        .show();
                    return true;
                });
            }

            // --- Accent Color (presets) ---
            int current = NewTermuxTheme.getAccentColor(context);
            for (int i = 0; i < COLOR_KEYS.length; i++) {
                final int color = COLOR_VALUES[i];
                Preference pref = findPreference(COLOR_KEYS[i]);
                if (pref == null) continue;
                if (color == current) pref.setSummary("✓ Active");
                pref.setOnPreferenceClickListener(preference -> {
                    NewTermuxTheme.setAccentColor(context, color);
                    refreshAccentSummaries(color);
                    return true;
                });
            }

            // --- Accent Color (custom) ---
            Preference colorCustomPref = findPreference("color_custom");
            if (colorCustomPref != null) {
                if (NewTermuxTheme.isCustomAccentActive(context))
                    colorCustomPref.setSummary("✓ Active (" + colorHex(current) + ")");
                colorCustomPref.setOnPreferenceClickListener(pref -> {
                    new ColorPickerDialog(context)
                        .setInitialColor(NewTermuxTheme.getAccentColor(context))
                        .setOnColorSelectedListener(color -> {
                            NewTermuxTheme.setAccentColor(context, color);
                            refreshAccentSummaries(color);
                        })
                        .show();
                    return true;
                });
            }
        }

        private void refreshThemeSummaries(String activeKey) {
            for (String k : NewTermuxColorTheme.THEME_KEYS) {
                Preference p = findPreference("theme_" + k);
                if (p != null) p.setSummary(k.equals(activeKey) ? "✓ Active" : null);
            }
        }

        private void refreshAccentSummaries(int activeColor) {
            for (int j = 0; j < COLOR_KEYS.length; j++) {
                Preference p = findPreference(COLOR_KEYS[j]);
                if (p != null) p.setSummary(COLOR_VALUES[j] == activeColor ? "✓ Active" : null);
            }
            Preference customPref = findPreference("color_custom");
            if (customPref != null) {
                Context ctx = getContext();
                if (ctx != null && NewTermuxTheme.isCustomAccentActive(ctx)) {
                    customPref.setSummary("✓ Active (" + colorHex(activeColor) + ")");
                } else {
                    customPref.setSummary(null);
                }
            }
        }

        private void showThemeEditor(Context context, boolean allColors) {
            String[] keys, labels;
            if (allColors) {
                keys = new String[]{
                    "background", "foreground", "cursor",
                    "color0", "color1", "color2", "color3", "color4",
                    "color5", "color6", "color7", "color8", "color9",
                    "color10", "color11", "color12", "color13", "color14", "color15"
                };
                labels = new String[]{
                    "Background", "Foreground", "Cursor",
                    "Color 0 (Black)", "Color 1 (Red)", "Color 2 (Green)", "Color 3 (Yellow)",
                    "Color 4 (Blue)", "Color 5 (Magenta)", "Color 6 (Cyan)", "Color 7 (White)",
                    "Color 8 (Bright Black)", "Color 9 (Bright Red)", "Color 10 (Bright Green)",
                    "Color 11 (Bright Yellow)", "Color 12 (Bright Blue)", "Color 13 (Bright Magenta)",
                    "Color 14 (Bright Cyan)", "Color 15 (Bright White)"
                };
            } else {
                keys = new String[]{"background", "foreground", "cursor"};
                labels = new String[]{"Background", "Foreground", "Cursor"};
            }

            String baseContent = NewTermuxColorTheme.getCustomThemeContent(context);
            LinkedHashMap<String, Integer> colorMap = parseThemeContent(baseContent, keys);

            android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_theme_editor, null);
            LinearLayout colorList = dialogView.findViewById(R.id.theme_color_list);

            android.view.View[] swatches = new android.view.View[keys.length];
            for (int i = 0; i < keys.length; i++) {
                final String key = keys[i];
                final int idx = i;
                android.view.View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_theme_color, colorList, false);
                TextView rowLabel = row.findViewById(R.id.slot_label);
                android.view.View swatch = row.findViewById(R.id.color_swatch);

                rowLabel.setText(labels[i]);
                int rowColor = colorMap.containsKey(key) ? colorMap.get(key) : 0xFF808080;
                setSwatchColor(swatch, rowColor);
                swatches[idx] = swatch;

                swatch.setOnClickListener(v -> {
                    int cur = colorMap.containsKey(key) ? colorMap.get(key) : 0xFF808080;
                    new ColorPickerDialog(requireContext())
                        .setInitialColor(cur)
                        .setOnColorSelectedListener(newColor -> {
                            colorMap.put(key, newColor);
                            setSwatchColor(swatches[idx], newColor);
                        })
                        .show();
                });

                colorList.addView(row);
            }

            new AlertDialog.Builder(requireContext())
                .setTitle("Custom Theme")
                .setView(dialogView)
                .setPositiveButton("Apply", (d, w) -> {
                    String newContent = buildThemeContent(baseContent, colorMap);
                    NewTermuxColorTheme.applyCustomTheme(context, newContent);
                    refreshThemeSummaries(NewTermuxColorTheme.THEME_KEY_CUSTOM);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        private void setSwatchColor(android.view.View swatch, int color) {
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(12f);
            gd.setColor(color);
            swatch.setBackground(gd);
        }

        private LinkedHashMap<String, Integer> parseThemeContent(String content, String[] keys) {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            for (String k : keys) map.put(k, 0xFF808080);
            if (content == null) return map;
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if (map.containsKey(k)) {
                    try {
                        map.put(k, Color.parseColor(v.startsWith("#") ? v : "#" + v));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return map;
        }

        private String buildThemeContent(String baseContent, LinkedHashMap<String, Integer> colorMap) {
            LinkedHashMap<String, String> allLines = new LinkedHashMap<>();
            if (baseContent != null) {
                for (String line : baseContent.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int eq = line.indexOf('=');
                    if (eq < 0) continue;
                    allLines.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
            for (Map.Entry<String, Integer> entry : colorMap.entrySet()) {
                allLines.put(entry.getKey(), String.format("#%06X", 0xFFFFFF & entry.getValue()));
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : allLines.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }
            return sb.toString();
        }

        private static String colorHex(int color) {
            return String.format("#%06X", 0xFFFFFF & color);
        }
    }

}
