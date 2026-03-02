package com.newtermux.features;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.termux.terminal.TerminalSession;
import com.termux.app.TermuxActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PackageManagerMenu {

    private final TermuxActivity mActivity;
    private final List<String> mPackageList = new ArrayList<>();
    private PopupWindow mPopupWindow;
    private ArrayAdapter<String> mAdapter;

    public PackageManagerMenu(TermuxActivity activity) {
        this.mActivity = activity;
        loadPackages();
    }

    private void loadPackages() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(mActivity.getAssets().open("packages.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    mPackageList.add(line.trim());
                }
            }
        } catch (Exception e) {
            Log.e("PackageManagerMenu", "Error loading packages", e);
        }
    }

    public void show(View anchor) {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            return;
        }

        Context context = mActivity;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        layout.setPadding(16, 16, 16, 16);

        SearchView searchView = new SearchView(context);
        searchView.setQueryHint("Search packages...");
        searchView.setIconifiedByDefault(false);
        layout.addView(searchView);

        ListView listView = new ListView(context);
        listView.setDivider(new ColorDrawable(Color.parseColor("#333333")));
        listView.setDividerHeight(1);
        
        mAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, new ArrayList<>(mPackageList)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(14);
                return view;
            }
        };
        listView.setAdapter(mAdapter);
        layout.addView(listView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = mAdapter.getItem(position);
            installPackage(pkg);
            mPopupWindow.dismiss();
        });

        mPopupWindow = new PopupWindow(layout, 600, 800, true);
        mPopupWindow.setElevation(20);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E1E1E")));
        mPopupWindow.showAsDropDown(anchor);
    }

    private void installPackage(String pkg) {
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) {
            String command = "pkg install " + pkg + "
";
            session.write(command);
        }
    }
}
