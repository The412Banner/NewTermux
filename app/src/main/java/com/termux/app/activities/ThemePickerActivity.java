package com.termux.app.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.newtermux.features.AccentSwatchView;
import com.newtermux.features.ColorPickerDialog;
import com.newtermux.features.NewTermuxColorTheme;
import com.newtermux.features.NewTermuxTheme;
import com.newtermux.features.ThemePreviewView;
import com.termux.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ThemePickerActivity extends AppCompatActivity {

    private static final int COLS = 3;
    private static final int MENU_CUSTOM_THEME = 1001;

    // Sentinel objects for list item types
    private static final Object HEADER_TERMINAL    = new Object();
    private static final Object HEADER_ACCENT      = new Object();
    private static final Object ITEM_CUSTOM_ACCENT = new Object();

    private static final int TYPE_HEADER        = 0;
    private static final int TYPE_TERMINAL      = 1;
    private static final int TYPE_ACCENT        = 2;
    private static final int TYPE_CUSTOM_ACCENT = 3;

    private PickerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_picker);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Themes & Colors");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAdapter = new PickerAdapter(this);

        GridLayoutManager lm = new GridLayoutManager(this, COLS);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int pos) {
                return mAdapter.getItemViewType(pos) == TYPE_HEADER ? COLS : 1;
            }
        });

        RecyclerView rv = findViewById(R.id.theme_picker_rv);
        rv.setLayoutManager(lm);
        rv.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CUSTOM_THEME, 0, "Custom Theme");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == MENU_CUSTOM_THEME) {
            showCustomThemeScope();
            return true;
        }
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ---- Custom theme editor ---------------------------------------------------

    private void showCustomThemeScope() {
        new AlertDialog.Builder(this)
            .setTitle("Custom Theme Scope")
            .setItems(new String[]{
                "Core 3  (Background, Foreground, Cursor)",
                "All 18 terminal colors"
            }, (d, which) -> showThemeEditor(which == 1))
            .show();
    }

    private void showThemeEditor(boolean allColors) {
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

        String baseContent = NewTermuxColorTheme.getCustomThemeContent(this);
        LinkedHashMap<String, Integer> colorMap = parseThemeContent(baseContent, keys);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_theme_editor, null);
        LinearLayout colorList = dialogView.findViewById(R.id.theme_color_list);

        View[] swatches = new View[keys.length];
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final int idx = i;
            View row = LayoutInflater.from(this).inflate(R.layout.item_theme_color, colorList, false);
            TextView rowLabel = row.findViewById(R.id.slot_label);
            View swatch = row.findViewById(R.id.color_swatch);

            rowLabel.setText(labels[i]);
            int rowColor = colorMap.containsKey(key) ? colorMap.get(key) : 0xFF808080;
            setSwatchColor(swatch, rowColor);
            swatches[idx] = swatch;

            swatch.setOnClickListener(v -> {
                int cur = colorMap.containsKey(key) ? colorMap.get(key) : 0xFF808080;
                new ColorPickerDialog(this)
                    .setInitialColor(cur)
                    .setOnColorSelectedListener(newColor -> {
                        colorMap.put(key, newColor);
                        setSwatchColor(swatches[idx], newColor);
                    })
                    .show();
            });

            colorList.addView(row);
        }

        new AlertDialog.Builder(this)
            .setTitle("Custom Theme")
            .setView(dialogView)
            .setPositiveButton("Apply", (d, w) -> {
                String newContent = buildThemeContent(baseContent, colorMap);
                NewTermuxColorTheme.applyCustomTheme(this, newContent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setSwatchColor(View swatch, int color) {
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

    // ---- Adapter ----------------------------------------------------------------

    static class PickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final Context ctx;
        final List<Object> items = new ArrayList<>();
        String activeTheme;
        int activeAccent;

        PickerAdapter(Context ctx) {
            this.ctx = ctx;
            rebuild();
        }

        void rebuild() {
            activeTheme  = NewTermuxColorTheme.getCurrentTheme(ctx);
            activeAccent = NewTermuxTheme.getAccentColor(ctx);
            items.clear();
            items.add(HEADER_TERMINAL);
            for (String key : NewTermuxColorTheme.THEME_KEYS) {
                if (!NewTermuxColorTheme.THEME_KEY_CUSTOM.equals(key))
                    items.add(key);
            }
            items.add(HEADER_ACCENT);
            for (int color : NewTermuxTheme.COLORS) {
                items.add(color);
            }
            items.add(ITEM_CUSTOM_ACCENT);
        }

        @Override
        public int getItemViewType(int pos) {
            Object o = items.get(pos);
            if (o == HEADER_TERMINAL || o == HEADER_ACCENT) return TYPE_HEADER;
            if (o == ITEM_CUSTOM_ACCENT) return TYPE_CUSTOM_ACCENT;
            if (o instanceof String)     return TYPE_TERMINAL;
            return TYPE_ACCENT;
        }

        @Override public int getItemCount() { return items.size(); }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            LayoutInflater inf = LayoutInflater.from(ctx);
            switch (vt) {
                case TYPE_HEADER:
                    return new HeaderVH(inf.inflate(R.layout.item_theme_section_header, parent, false));
                case TYPE_TERMINAL:
                    return new TerminalVH(inf.inflate(R.layout.item_terminal_theme_card, parent, false));
                default: // TYPE_ACCENT + TYPE_CUSTOM_ACCENT
                    return new AccentVH(inf.inflate(R.layout.item_accent_color_card, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            Object item = items.get(pos);

            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText(
                        item == HEADER_TERMINAL ? "Terminal Theme" : "Accent Color");

            } else if (holder instanceof TerminalVH) {
                String key = (String) item;
                TerminalVH tvh = (TerminalVH) holder;
                tvh.preview.setTheme(NewTermuxColorTheme.getPreviewColors(key),
                        key.equals(activeTheme), activeAccent);
                tvh.name.setText(NewTermuxColorTheme.getThemeName(key));
                tvh.itemView.setOnClickListener(v -> {
                    NewTermuxColorTheme.applyTheme(ctx, key);
                    activeTheme = key;
                    notifyDataSetChanged();
                });

            } else {
                // AccentVH — handles both preset and custom
                AccentVH avh = (AccentVH) holder;
                boolean isCustomSlot = (item == ITEM_CUSTOM_ACCENT);

                if (isCustomSlot) {
                    boolean customActive = NewTermuxTheme.isCustomAccentActive(ctx);
                    avh.swatch.setColor(customActive ? activeAccent : 0xFF666666,
                            customActive, true);
                    avh.name.setText("Custom…");
                    avh.itemView.setOnClickListener(v ->
                        new ColorPickerDialog(ctx)
                            .setInitialColor(NewTermuxTheme.getAccentColor(ctx))
                            .setOnColorSelectedListener(color -> {
                                NewTermuxTheme.setAccentColor(ctx, color);
                                activeAccent = color;
                                notifyDataSetChanged();
                            })
                            .show()
                    );
                } else {
                    int color = (Integer) item;
                    avh.swatch.setColor(color, color == activeAccent, false);
                    avh.name.setText(NewTermuxTheme.getColorName(color));
                    avh.itemView.setOnClickListener(v -> {
                        NewTermuxTheme.setAccentColor(ctx, color);
                        activeAccent = color;
                        notifyDataSetChanged();
                    });
                }
            }
        }

        // ---- ViewHolders -------------------------------------------------------

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView title;
            HeaderVH(View v) { super(v); title = (TextView) v; }
        }

        static class TerminalVH extends RecyclerView.ViewHolder {
            ThemePreviewView preview;
            TextView name;
            TerminalVH(View v) {
                super(v);
                preview = v.findViewById(R.id.theme_preview);
                name    = v.findViewById(R.id.theme_name);
            }
        }

        static class AccentVH extends RecyclerView.ViewHolder {
            AccentSwatchView swatch;
            TextView name;
            AccentVH(View v) {
                super(v);
                swatch = v.findViewById(R.id.accent_swatch);
                name   = v.findViewById(R.id.accent_name);
            }
        }
    }
}
