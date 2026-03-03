package com.termux.app.activities;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.newtermux.features.NewTermuxSettings;
import com.newtermux.features.SshProfile;
import com.newtermux.features.SshProfileStore;
import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.theme.NightMode;

import java.util.List;

public class SshManagerActivity extends AppCompatActivity {

    private static final int MENU_ADD = 1;

    private List<SshProfile> mProfiles;
    private ProfileAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
        setContentView(R.layout.activity_ssh_manager);
        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setToolbarTitle(this, com.termux.shared.R.id.toolbar, "SSH Manager", 0);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        mProfiles = SshProfileStore.load();
        mAdapter = new ProfileAdapter(this, mProfiles);

        ListView listView = findViewById(R.id.ssh_list);
        TextView emptyView = findViewById(R.id.ssh_empty);
        listView.setEmptyView(emptyView);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener((parent, view, position, id) ->
            confirmConnect(mProfiles.get(position)));

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showEditDeleteDialog(position);
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD, 0, "Add")
            .setIcon(android.R.drawable.ic_menu_add)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == MENU_ADD) {
            showAddEditDialog(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void confirmConnect(SshProfile profile) {
        new AlertDialog.Builder(this)
            .setTitle("Connect")
            .setMessage("Connect to " + profile.nickname + "?\n" + profile.displayLabel())
            .setPositiveButton("Connect", (d, w) -> connect(profile))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void connect(SshProfile profile) {
        String cmd = profile.buildCommand() + "\n";
        NewTermuxSettings.setPendingCommand(this, cmd);
        finishAffinity();
    }

    private void showEditDeleteDialog(int position) {
        SshProfile profile = mProfiles.get(position);
        new AlertDialog.Builder(this)
            .setTitle(profile.nickname)
            .setItems(new String[]{"Edit", "Delete"}, (d, which) -> {
                if (which == 0) showAddEditDialog(profile);
                else confirmDelete(position);
            })
            .show();
    }

    private void confirmDelete(int position) {
        SshProfile profile = mProfiles.get(position);
        new AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Delete \"" + profile.nickname + "\"?")
            .setPositiveButton("Delete", (d, w) -> {
                mProfiles.remove(position);
                SshProfileStore.save(mProfiles);
                mAdapter.notifyDataSetChanged();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAddEditDialog(SshProfile existing) {
        boolean isEdit = existing != null;
        Context ctx = this;
        int dp8 = dp(8);
        int dp16 = dp(16);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp8, dp16, dp8);

        EditText etNickname = makeField(ctx, "Nickname (e.g. My Server)", InputType.TYPE_CLASS_TEXT);
        EditText etHost = makeField(ctx, "Host (e.g. 192.168.1.1)", InputType.TYPE_CLASS_TEXT);
        EditText etPort = makeField(ctx, "Port", InputType.TYPE_CLASS_NUMBER);
        EditText etUser = makeField(ctx, "Username", InputType.TYPE_CLASS_TEXT);
        EditText etKey = makeField(ctx, "Key path (optional, e.g. ~/.ssh/id_rsa)",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        etKey.setTypeface(Typeface.MONOSPACE);

        etPort.setText("22");

        if (isEdit) {
            etNickname.setText(existing.nickname);
            etHost.setText(existing.host);
            etPort.setText(String.valueOf(existing.port));
            etUser.setText(existing.username);
            etKey.setText(existing.keyPath);
        }

        layout.addView(label(ctx, "Nickname"));
        layout.addView(etNickname);
        layout.addView(label(ctx, "Host"));
        layout.addView(etHost);
        layout.addView(label(ctx, "Port"));
        layout.addView(etPort);
        layout.addView(label(ctx, "Username"));
        layout.addView(etUser);
        layout.addView(label(ctx, "Private Key Path (leave blank for password auth)"));
        layout.addView(etKey);

        new AlertDialog.Builder(ctx)
            .setTitle(isEdit ? "Edit Profile" : "Add SSH Profile")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                String nickname = etNickname.getText().toString().trim();
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                String user = etUser.getText().toString().trim();
                String key = etKey.getText().toString().trim();

                if (nickname.isEmpty() || host.isEmpty() || user.isEmpty()) {
                    Toast.makeText(ctx, "Nickname, host, and username are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                int port = 22;
                try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}

                SshProfile profile = isEdit ? existing : new SshProfile();
                profile.nickname = nickname;
                profile.host = host;
                profile.port = port;
                profile.username = user;
                profile.keyPath = key;

                if (!isEdit) mProfiles.add(profile);
                SshProfileStore.save(mProfiles);
                mAdapter.notifyDataSetChanged();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private EditText makeField(Context ctx, String hint, int inputType) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setInputType(inputType);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        et.setLayoutParams(lp);
        return et;
    }

    private TextView label(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static class ProfileAdapter extends ArrayAdapter<SshProfile> {

        ProfileAdapter(Context ctx, List<SshProfile> profiles) {
            super(ctx, 0, profiles);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.VERTICAL);
                int pad = (int) (12 * getContext().getResources().getDisplayMetrics().density);
                row.setPadding(pad, pad, pad, pad);

                TextView title = new TextView(getContext());
                title.setTextSize(16f);
                title.setTypeface(null, Typeface.BOLD);
                row.addView(title);

                TextView sub = new TextView(getContext());
                sub.setTextSize(13f);
                row.addView(sub);

                holder = new ViewHolder(title, sub);
                row.setTag(holder);
                convertView = row;
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            SshProfile profile = getItem(position);
            if (profile != null) {
                holder.title.setText(profile.nickname);
                holder.sub.setText(profile.displayLabel());
            }
            return convertView;
        }

        static class ViewHolder {
            final TextView title, sub;
            ViewHolder(TextView t, TextView s) { title = t; sub = s; }
        }
    }
}
