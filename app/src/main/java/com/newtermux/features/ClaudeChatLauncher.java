package com.newtermux.features;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.termux.app.TermuxActivity;

public class ClaudeChatLauncher {

    private static final String CLAUDE_PACKAGE = "claude.chat";
    private static final String CLAUDE_MAIN = "claude.chat.MainActivity";

    private final TermuxActivity mActivity;

    public ClaudeChatLauncher(TermuxActivity activity) {
        this.mActivity = activity;
    }

    public boolean isInstalled() {
        try {
            mActivity.getPackageManager().getPackageInfo(CLAUDE_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void launch() {
        Intent intent = new Intent();
        intent.setClassName(CLAUDE_PACKAGE, CLAUDE_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("launch_terminal", true);
        try {
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mActivity, "Claude Chat app is not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
