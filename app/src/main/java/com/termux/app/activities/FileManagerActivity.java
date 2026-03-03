package com.termux.app.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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
import androidx.core.content.FileProvider;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.theme.NightMode;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileManagerActivity extends AppCompatActivity {

    private static final int MENU_NEW_FILE   = 1;
    private static final int MENU_NEW_FOLDER = 2;
    private static final long MAX_TEXT_SIZE  = 512 * 1024; // 512 KB

    private static final String[] TEXT_EXTENSIONS = {
        "txt", "sh", "bash", "zsh", "py", "js", "ts", "java", "kt", "c", "cpp", "h",
        "md", "json", "xml", "yaml", "yml", "toml", "conf", "cfg", "ini", "properties",
        "env", "rc", "profile", "log", "csv", "html", "css", "rb", "go", "rs", "php"
    };

    private File mCurrentDir;
    private FileAdapter mAdapter;
    private List<File> mFiles = new ArrayList<>();
    private TextView mPathBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
        setContentView(R.layout.activity_file_manager);
        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setToolbarTitle(this, com.termux.shared.R.id.toolbar, "File Manager", 0);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        mPathBar = findViewById(R.id.fm_path_bar);
        ListView listView = findViewById(R.id.fm_list);
        TextView emptyView = findViewById(R.id.fm_empty);

        mAdapter = new FileAdapter(this, mFiles);
        listView.setEmptyView(emptyView);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener((p, v, pos, id) -> onFileClick(mFiles.get(pos)));
        listView.setOnItemLongClickListener((p, v, pos, id) -> { onFileLongClick(pos); return true; });

        navigateTo(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_NEW_FILE,   0, "New File").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_NEW_FOLDER, 0, "New Folder").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == MENU_NEW_FILE)   { promptNewFile();   return true; }
        if (id == MENU_NEW_FOLDER) { promptNewFolder(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        File parent = mCurrentDir.getParentFile();
        // Don't navigate above the Termux files root
        if (parent != null && mCurrentDir.getAbsolutePath()
                .startsWith(TermuxConstants.TERMUX_FILES_DIR_PATH)
                && !mCurrentDir.getAbsolutePath().equals(TermuxConstants.TERMUX_FILES_DIR_PATH)) {
            navigateTo(parent);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void navigateTo(File dir) {
        mCurrentDir = dir;
        mPathBar.setText(friendlyPath(dir.getAbsolutePath()));
        refreshList();
    }

    private void refreshList() {
        mFiles.clear();
        File[] entries = mCurrentDir.listFiles();
        if (entries != null) {
            Arrays.sort(entries, Comparator
                .<File, Boolean>comparing(f -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));
            for (File f : entries) {
                if (!f.getName().equals(".") && !f.getName().equals("..")) {
                    mFiles.add(f);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void onFileClick(File file) {
        if (file.isDirectory()) {
            navigateTo(file);
        } else if (isTextFile(file)) {
            openTextEditor(file);
        } else {
            showFileOptions(file);
        }
    }

    private void onFileLongClick(int position) {
        File file = mFiles.get(position);
        List<String> options = new ArrayList<>();
        if (isTextFile(file)) options.add("Edit");
        options.add("Share");
        options.add("Copy path");
        options.add("Delete");

        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(options.toArray(new String[0]), (d, which) -> {
                String choice = options.get(which);
                switch (choice) {
                    case "Edit":      openTextEditor(file); break;
                    case "Share":     shareFile(file);      break;
                    case "Copy path": copyPath(file);       break;
                    case "Delete":    confirmDelete(file);  break;
                }
            })
            .show();
    }

    private void showFileOptions(File file) {
        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(new String[]{"Share", "Copy path", "Delete"}, (d, which) -> {
                if (which == 0) shareFile(file);
                else if (which == 1) copyPath(file);
                else confirmDelete(file);
            })
            .show();
    }

    private void openTextEditor(File file) {
        if (file.length() > MAX_TEXT_SIZE) {
            Toast.makeText(this, "File too large to edit in-app", Toast.LENGTH_SHORT).show();
            return;
        }
        String content = readFile(file);

        EditText editor = new EditText(this);
        editor.setText(content);
        editor.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        editor.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setTypeface(Typeface.MONOSPACE);
        int pad = dp(12);
        editor.setPadding(pad, pad, pad, pad);
        editor.setMinLines(12);

        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setView(editor)
            .setPositiveButton("Save", (d, w) -> {
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(editor.getText().toString());
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share " + file.getName()));
        } catch (Exception e) {
            // Fallback: copy path
            copyPath(file);
            Toast.makeText(this, "Share unavailable, path copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPath(File file) {
        android.content.ClipboardManager cm =
            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("path", file.getAbsolutePath()));
            Toast.makeText(this, "Path copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete \"" + file.getName() + "\"?")
            .setPositiveButton("Delete", (d, w) -> {
                if (deleteRecursive(file)) {
                    refreshList();
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptNewFile() {
        EditText et = new EditText(this);
        et.setHint("filename.txt");
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
            .setTitle("New File")
            .setView(et)
            .setPositiveButton("Create", (d, w) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) return;
                File f = new File(mCurrentDir, name);
                try {
                    if (f.createNewFile()) { refreshList(); }
                    else Toast.makeText(this, "File already exists", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptNewFolder() {
        EditText et = new EditText(this);
        et.setHint("folder-name");
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setView(et)
            .setPositiveButton("Create", (d, w) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) return;
                File f = new File(mCurrentDir, name);
                if (f.mkdir()) { refreshList(); }
                else Toast.makeText(this, "Could not create folder", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private boolean isTextFile(File file) {
        if (file.isDirectory()) return false;
        String name = file.getName().toLowerCase();
        // No extension but common config file names
        if (!name.contains(".")) {
            return name.startsWith(".") || name.equals("makefile") || name.equals("dockerfile");
        }
        String ext = name.substring(name.lastIndexOf('.') + 1);
        for (String te : TEXT_EXTENSIONS) if (te.equals(ext)) return true;
        return false;
    }

    private String readFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) if (!deleteRecursive(c)) return false;
        }
        return f.delete();
    }

    private String friendlyPath(String path) {
        String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
        if (path.startsWith(home)) return "~" + path.substring(home.length());
        return path;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static class FileAdapter extends ArrayAdapter<File> {

        FileAdapter(Context ctx, List<File> files) {
            super(ctx, 0, files);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                int pad = (int) (12 * getContext().getResources().getDisplayMetrics().density);
                row.setPadding(pad, pad, pad, pad);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView icon = new TextView(getContext());
                icon.setTextSize(20f);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                iconLp.rightMargin = pad;
                icon.setLayoutParams(iconLp);
                row.addView(icon);

                LinearLayout textCol = new LinearLayout(getContext());
                textCol.setOrientation(LinearLayout.VERTICAL);

                TextView name = new TextView(getContext());
                name.setTextSize(15f);
                textCol.addView(name);

                TextView detail = new TextView(getContext());
                detail.setTextSize(12f);
                textCol.addView(detail);

                row.addView(textCol);
                holder = new ViewHolder(icon, name, detail);
                row.setTag(holder);
                convertView = row;
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            File file = getItem(position);
            if (file != null) {
                if (file.isDirectory()) {
                    holder.icon.setText("\uD83D\uDCC1"); // 📁
                    holder.name.setText(file.getName());
                    holder.detail.setText("Folder");
                } else {
                    holder.icon.setText("\uD83D\uDCC4"); // 📄
                    holder.name.setText(file.getName());
                    holder.detail.setText(formatSize(file.length()));
                }
            }
            return convertView;
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }

        static class ViewHolder {
            final TextView icon, name, detail;
            ViewHolder(TextView i, TextView n, TextView d) { icon = i; name = n; detail = d; }
        }
    }
}
