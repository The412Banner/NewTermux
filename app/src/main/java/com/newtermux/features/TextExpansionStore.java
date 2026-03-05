package com.newtermux.features;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class TextExpansionStore {

    public static class TextExpansion {
        public String trigger;
        public String expansion;
    }

    private static List<TextExpansion> sCache = null;

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("newtermux_settings", Context.MODE_PRIVATE);
    }

    public static List<TextExpansion> load(Context ctx) {
        if (sCache != null) return sCache;
        List<TextExpansion> list = new ArrayList<>();
        try {
            String json = prefs(ctx).getString("text_expansions_json", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                TextExpansion te = new TextExpansion();
                te.trigger = obj.optString("trigger", "");
                te.expansion = obj.optString("expansion", "");
                if (!te.trigger.isEmpty()) list.add(te);
            }
        } catch (Exception ignored) {}
        sCache = list;
        return list;
    }

    public static void save(Context ctx, List<TextExpansion> list) {
        try {
            JSONArray arr = new JSONArray();
            for (TextExpansion te : list) {
                JSONObject obj = new JSONObject();
                obj.put("trigger", te.trigger);
                obj.put("expansion", te.expansion);
                arr.put(obj);
            }
            prefs(ctx).edit().putString("text_expansions_json", arr.toString()).apply();
            sCache = null; // invalidate cache so next load picks up the new data
        } catch (Exception ignored) {}
    }

    public static String findExpansion(Context ctx, String trigger) {
        for (TextExpansion te : load(ctx)) {
            if (te.trigger.equals(trigger)) return te.expansion;
        }
        return null;
    }
}
