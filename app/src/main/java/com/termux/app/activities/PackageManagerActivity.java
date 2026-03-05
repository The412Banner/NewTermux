package com.termux.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.newtermux.features.NewTermuxSettings;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.theme.NightMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageManagerActivity extends AppCompatActivity {

    private static final int TAB_INSTALLED = 0;
    private static final int TAB_AVAILABLE = 1;
    private static final String DPKG_STATUS =
        com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/var/lib/dpkg/status";

    // ---- Data model ----

    static class PkgEntry {
        final String name;
        final String version;     // null for available-only entries
        final String description; // null for available-only entries

        PkgEntry(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }
    }

    // ---- State ----

    private final List<PkgEntry> mInstalled = new ArrayList<>();
    private final List<PkgEntry> mAvailable = new ArrayList<>();
    private final Set<String>    mInstalledNames = new HashSet<>();
    private final List<PkgEntry> mFiltered = new ArrayList<>();

    private PkgAdapter mAdapter;
    private TextView   mStatusText;

    private int    mCurrentTab   = TAB_INSTALLED;
    private String mCurrentQuery = "";

    private boolean mInstalledLoaded = false;
    private boolean mAvailableLoaded = false;

    private final Handler         mHandler  = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(2);

    // ---- Lifecycle ----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
        setContentView(R.layout.activity_package_manager);
        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
        AppCompatActivityUtils.setToolbarTitle(this, com.termux.shared.R.id.toolbar,
            "Package Manager", 0);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        mStatusText = findViewById(R.id.pkg_status_text);

        RecyclerView rv = findViewById(R.id.pkg_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mAdapter = new PkgAdapter();
        rv.setAdapter(mAdapter);

        TabLayout tabs = findViewById(R.id.pkg_tabs);
        tabs.addTab(tabs.newTab().setText("Installed"));
        tabs.addTab(tabs.newTab().setText("Available (…)"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                mCurrentTab = tab.getPosition();
                mCurrentQuery = "";
                SearchView sv = findViewById(R.id.pkg_search);
                if (sv != null) sv.setQuery("", false);
                rebuildFiltered();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        SearchView search = findViewById(R.id.pkg_search);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                mCurrentQuery = (q == null) ? "" : q;
                rebuildFiltered();
                return true;
            }
        });

        rebuildFiltered(); // show "Loading…" immediately
        loadInstalled();
        loadAvailable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    // ---- Background loading ----

    private void loadInstalled() {
        mExecutor.execute(() -> {
            List<PkgEntry> list = new ArrayList<>();
            File f = new File(DPKG_STATUS);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String pkg = null, ver = null, desc = null, status = null;
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("Package: ")) {
                            pkg = line.substring(9).trim();
                        } else if (line.startsWith("Version: ")) {
                            ver = line.substring(9).trim();
                        } else if (line.startsWith("Status: ")) {
                            status = line.substring(8).trim();
                        } else if (line.startsWith("Description: ")) {
                            desc = line.substring(13).trim();
                        } else if (line.isEmpty()) {
                            if (pkg != null && "install ok installed".equals(status)) {
                                list.add(new PkgEntry(pkg, ver, desc));
                            }
                            pkg = ver = desc = status = null;
                        }
                    }
                    // Last block (no trailing blank line)
                    if (pkg != null && "install ok installed".equals(status)) {
                        list.add(new PkgEntry(pkg, ver, desc));
                    }
                } catch (Exception ignored) {}
            }
            list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            mHandler.post(() -> {
                mInstalled.clear();
                mInstalled.addAll(list);
                mInstalledNames.clear();
                for (PkgEntry e : list) mInstalledNames.add(e.name);
                mInstalledLoaded = true;
                if (mCurrentTab == TAB_INSTALLED) rebuildFiltered();
            });
        });
    }

    private void loadAvailable() {
        mExecutor.execute(() -> {
            List<PkgEntry> list = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(getAssets().open("packages.txt")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // Skip empty lines, the "Listing…" artifact, and any line with spaces
                    if (!line.isEmpty() && !line.contains("...") && !line.contains(" ")) {
                        list.add(new PkgEntry(line, null, null));
                    }
                }
            } catch (Exception ignored) {}
            list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            mHandler.post(() -> {
                mAvailable.clear();
                mAvailable.addAll(list);
                mAvailableLoaded = true;
                // Update "Available (N)" tab label
                TabLayout tabs = findViewById(R.id.pkg_tabs);
                if (tabs != null && tabs.getTabAt(TAB_AVAILABLE) != null) {
                    tabs.getTabAt(TAB_AVAILABLE).setText("Available (" + list.size() + ")");
                }
                if (mCurrentTab == TAB_AVAILABLE) rebuildFiltered();
            });
        });
    }

    // ---- Filtering ----

    private void rebuildFiltered() {
        List<PkgEntry> source = (mCurrentTab == TAB_INSTALLED) ? mInstalled : mAvailable;
        boolean loaded = (mCurrentTab == TAB_INSTALLED) ? mInstalledLoaded : mAvailableLoaded;

        mFiltered.clear();
        String q = mCurrentQuery.trim().toLowerCase(Locale.ROOT);
        for (PkgEntry e : source) {
            if (q.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(q)) {
                mFiltered.add(e);
            }
        }
        mAdapter.notifyDataSetChanged();

        if (!loaded) {
            mStatusText.setText("Loading…");
        } else if (mFiltered.isEmpty()) {
            mStatusText.setText(q.isEmpty() ? "No packages found." : "No results for \"" + q + "\".");
        } else {
            int n = mFiltered.size();
            mStatusText.setText(n + " package" + (n == 1 ? "" : "s"));
        }
    }

    // ---- Detail dialog ----

    private void showPackageDetail(PkgEntry entry) {
        boolean installedEntry = (mCurrentTab == TAB_INSTALLED);
        boolean alreadyInstalled = installedEntry || mInstalledNames.contains(entry.name);

        StringBuilder msg = new StringBuilder();

        // Version (for installed entries)
        if (entry.version != null) {
            msg.append("Version: ").append(entry.version).append("\n\n");
        }

        // Description (for installed entries)
        if (entry.description != null && !entry.description.isEmpty()) {
            msg.append(entry.description).append("\n\n");
        }

        // Repository info
        msg.append("Repository: Termux Package Repository\n");
        msg.append("pkg.termux.dev\n\n");

        // Command preview
        if (!installedEntry) {
            msg.append("Command: pkg install ").append(entry.name);
        } else {
            msg.append("Command: pkg uninstall ").append(entry.name);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this)
            .setTitle(entry.name)
            .setMessage(msg.toString())
            .setNegativeButton("Close", null);

        if (!installedEntry) {
            // Available tab: Install (or Reinstall if already installed)
            String installLabel = alreadyInstalled ? "Reinstall" : "Install";
            b.setPositiveButton(installLabel,
                (d, w) -> runCommand("pkg install -y " + entry.name + "\n"));
        } else {
            // Installed tab: offer Uninstall
            b.setPositiveButton("Uninstall", (d, w) ->
                new AlertDialog.Builder(this)
                    .setTitle("Uninstall " + entry.name + "?")
                    .setMessage("This will remove the package from your Termux environment.")
                    .setPositiveButton("Uninstall",
                        (d2, w2) -> runCommand("pkg uninstall " + entry.name + "\n"))
                    .setNegativeButton("Cancel", null)
                    .show()
            );
        }

        b.show();
    }

    // ---- Command execution ----

    /** Sets the command as a pending command in TermuxActivity and brings it to the front. */
    private void runCommand(String cmd) {
        NewTermuxSettings.setPendingCommand(this, cmd);
        Intent intent = new Intent(this, TermuxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // ---- RecyclerView adapter ----

    class PkgAdapter extends RecyclerView.Adapter<PkgAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView sub;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.pkg_name);
                sub  = v.findViewById(R.id.pkg_sub);
                v.setOnClickListener(view -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < mFiltered.size()) {
                        showPackageDetail(mFiltered.get(pos));
                    }
                });
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            PkgEntry e = mFiltered.get(pos);
            h.name.setText(e.name);

            if (mCurrentTab == TAB_INSTALLED) {
                h.sub.setText(e.version != null ? "v" + e.version : "installed");
            } else {
                boolean inst = mInstalledNames.contains(e.name);
                h.sub.setText(inst ? "Installed · Termux repo" : "Termux repo");
            }
        }

        @Override
        public int getItemCount() {
            return mFiltered.size();
        }
    }
}
