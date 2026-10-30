package com.newtermux.features;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.termux.R;

/**
 * Shared Bannerlator-style pop-out menu for the legacy (View-based) UI: a rounded, outlined
 * card ({@code bg_popup_menu}) with a thin gray divider between each option. Mirrors the
 * Compose {@code outlinedMenuCard()} / {@code MenuItemDivider()} look so every menu in the app
 * reads identically.
 */
public final class NtPopupMenu {

    private NtPopupMenu() {}

    public interface OnSelect { void onSelect(int index); }

    /** Anchored dropdown (e.g. a session tab chip). */
    public static void showAsDropDown(Context ctx, View anchor, String title, String[] items, OnSelect cb) {
        build(ctx, title, items, cb).showAsDropDown(anchor);
    }

    /** Contextual popup shown at an absolute screen location (e.g. a long-press point). */
    public static void showAtLocation(Context ctx, View parent, int x, int y, String title, String[] items, OnSelect cb) {
        build(ctx, title, items, cb).showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
    }

    private static PopupWindow build(Context ctx, String title, String[] items, OnSelect cb) {
        final float d = ctx.getResources().getDisplayMetrics().density;
        final int padH = (int) (16 * d);
        final int padV = (int) (12 * d);

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_popup_menu);
        container.setPadding(0, (int) (4 * d), 0, (int) (4 * d));

        final PopupWindow popup = new PopupWindow(container,
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(8 * d);

        boolean hasTitle = title != null && !title.isEmpty();
        if (hasTitle) {
            TextView t = new TextView(ctx);
            t.setText(title);
            t.setTextColor(ContextCompat.getColor(ctx, R.color.nt_on_surface));
            t.setTextSize(13f);
            t.setPadding(padH, (int) (8 * d), padH, (int) (8 * d));
            t.setMaxWidth((int) (280 * d));
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            container.addView(t);
            container.addView(divider(ctx, d));
        }

        for (int i = 0; i < items.length; i++) {
            if (i > 0) container.addView(divider(ctx, d));
            TextView row = new TextView(ctx);
            row.setText(items[i]);
            row.setTextColor(ContextCompat.getColor(ctx, R.color.nt_on_surface));
            row.setTextSize(16f);
            row.setPadding(padH, padV, padH, padV);
            final int idx = i;
            row.setOnClickListener(v -> {
                popup.dismiss();
                cb.onSelect(idx);
            });
            container.addView(row);
        }

        // Size the popup to its content (short labels shouldn't produce a wide menu).
        // Measure with an UNSPECIFIED width so the MATCH_PARENT dividers don't force full width.
        container.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int width = container.getMeasuredWidth();
        int min = (int) (140 * d);
        int max = (int) (320 * d);
        int finalWidth = Math.max(min, Math.min(max, width));
        popup.setWidth(finalWidth);
        // Give the popup its real height: with WRAP_CONTENT showAsDropDown() can't tell that the
        // menu doesn't fit below the anchor (e.g. the bottom status line), so it never flips it
        // above and the menu gets clipped by the navigation bar.
        container.measure(
            View.MeasureSpec.makeMeasureSpec(finalWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setHeight(container.getMeasuredHeight());
        return popup;
    }

    private static View divider(Context ctx, float d) {
        View divider = new View(ctx);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) d)));
        divider.setBackgroundColor(ContextCompat.getColor(ctx, R.color.nt_menu_divider));
        return divider;
    }
}
