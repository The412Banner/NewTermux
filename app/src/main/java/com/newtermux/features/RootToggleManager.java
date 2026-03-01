package com.newtermux.features;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Manages root access toggle for NewTermux sessions.
 * When enabled, commands run via 'su' giving full device root access.
 */
public class RootToggleManager {

    private static final String TAG = "RootToggleManager";

    public interface RootCallback {
        void onRootGranted();
        void onRootDenied(String reason);
        void onRootStateChanged(boolean isRoot);
    }

    private boolean mIsRootEnabled = false;
    private boolean mIsRootAvailable = false;
    private static RootToggleManager sInstance;

    private RootToggleManager() {}

    public static synchronized RootToggleManager getInstance() {
        if (sInstance == null) {
            sInstance = new RootToggleManager();
        }
        return sInstance;
    }

    /**
     * Check if the device is rooted by attempting to execute 'su'.
     */
    public static boolean isDeviceRooted() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            return line != null && line.contains("uid=0");
        } catch (Exception e) {
            Log.d(TAG, "Device not rooted: " + e.getMessage());
            return false;
        }
    }

    /**
     * Request root access. Triggers the su permission dialog if not already granted.
     */
    public void requestRoot(RootCallback callback) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("id\n");
                os.writeBytes("exit\n");
                os.flush();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                String output = reader.readLine();
                int exitCode = process.waitFor();

                if (exitCode == 0 && output != null && output.contains("uid=0")) {
                    mIsRootAvailable = true;
                    mIsRootEnabled = true;
                    Log.i(TAG, "Root access granted");
                    if (callback != null) callback.onRootGranted();
                } else {
                    mIsRootAvailable = false;
                    mIsRootEnabled = false;
                    if (callback != null) callback.onRootDenied("Root access denied or not available.");
                }
            } catch (IOException | InterruptedException e) {
                mIsRootAvailable = false;
                mIsRootEnabled = false;
                if (callback != null) callback.onRootDenied("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Execute a shell command with root privileges.
     */
    public CommandResult executeRootCommand(String command) {
        if (!mIsRootEnabled) {
            return new CommandResult(-1, "", "Root mode not enabled");
        }
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            String line;
            while ((line = stdout.readLine()) != null) out.append(line).append("\n");
            while ((line = stderr.readLine()) != null) err.append(line).append("\n");

            int exitCode = process.waitFor();
            return new CommandResult(exitCode, out.toString(), err.toString());
        } catch (IOException | InterruptedException e) {
            return new CommandResult(-1, "", e.getMessage());
        }
    }

    public void toggleRoot(boolean enable, RootCallback callback) {
        if (enable) {
            requestRoot(callback);
        } else {
            mIsRootEnabled = false;
            Log.i(TAG, "Root mode disabled");
            if (callback != null) callback.onRootStateChanged(false);
        }
    }

    public boolean isRootEnabled() { return mIsRootEnabled; }
    public boolean isRootAvailable() { return mIsRootAvailable; }

    /** Get the shell command prefix for root-aware execution */
    public String getShellPrefix() {
        return mIsRootEnabled ? "su -c " : "";
    }

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
        public boolean success() { return exitCode == 0; }
    }
}
