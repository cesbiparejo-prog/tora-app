package com.dani.tshuva;

import android.content.*; import org.json.*; import java.util.*;

public class Store {
    static SharedPreferences p(Context c) { return c.getSharedPreferences("tshuva", Context.MODE_PRIVATE); }
    static String apiKey(Context c) { return p(c).getString("key", ""); }
    static String model(Context c) {
        String m = migrate(p(c).getString("model", Gemini.DEFAULT_MODEL));
        if (!m.equals(p(c).getString("model", ""))) p(c).edit().putString("model", m).apply();
        return m;
    }
    /** gemini-2.5-flash ya no está disponible para usuarios nuevos: se pasa al modelo por defecto. */
    static String migrate(String m) {
        String t = m == null ? "" : m.trim();
        return (t.isEmpty() || t.equals("gemini-2.5-flash") || t.equals("models/gemini-2.5-flash")) ? Gemini.DEFAULT_MODEL : t;
    }
    static int hour(Context c) { return p(c).getInt("hour", 8); }
    static int minute(Context c) { return p(c).getInt("min", 0); }
    static boolean notifOn(Context c) { return p(c).getBoolean("notif", true); }
    static long cleanSince(Context c) {
        long v = p(c).getLong("since", 0);
        if (v == 0) { v = System.currentTimeMillis(); p(c).edit().putLong("since", v).apply(); }
        return v;
    }

    static final String PERFIL_INICIAL = "Hombre de 35 años. Músico y adorador. Creció en la iglesia (trasfondo cristiano) y ahora se acerca a las raíces hebreas y a la Torá. Le cuesta manejar la ansiedad y la frustración, que suelen ser disparadores de sus impulsos.";
    static final String MEMORIA_INICIAL = "- Perfil: hombre de 35 años, músico y adorador; creció en la iglesia y ahora se nutre de las raíces hebreas y la Torá.\n- Disparadores conocidos: ansiedad y frustración.\n- Lo que puede ayudarle: música y alabanza (tocar su instrumento, cantar Tehilim, niggunim), respiración, reestructuración cognitiva, TIPP.";
    static String profile(Context c) { return p(c).getString("perfil", PERFIL_INICIAL); }
    static String memory(Context c) {
        if (!p(c).getBoolean("memSeed", false)) { p(c).edit().putBoolean("memSeed", true).putString("memoria", p(c).getString("memoria", "").isEmpty() ? MEMORIA_INICIAL : p(c).getString("memoria", "") + "\n" + MEMORIA_INICIAL).apply(); }
        return p(c).getString("memoria", "");
    }
    static void setMemory(Context c, String m) { p(c).edit().putString("memoria", m).apply(); }
    static int sinceMemory(Context c) { return p(c).getInt("memN", 0); }
    static void setSinceMemory(Context c, int n) { p(c).edit().putInt("memN", n).apply(); }

    static List<String[]> load(Context c, String k) {
        List<String[]> l = new ArrayList<>();
        try { JSONArray a = new JSONArray(p(c).getString(k, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONArray e = a.getJSONArray(i); l.add(new String[]{e.getString(0), e.getString(1), e.optString(2, "")}); }
        } catch (JSONException ignored) {}
        return l;
    }
    static void save(Context c, String k, List<String[]> l) {
        JSONArray a = new JSONArray();
        for (String[] e : l) { JSONArray x = new JSONArray(); for (String s : e) x.put(s); a.put(x); }
        p(c).edit().putString(k, a.toString()).apply();
    }
}
