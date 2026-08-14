package com.codex.yuedu;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class LibraryStore {
    private static SharedPreferences p(Context c) { return c.getSharedPreferences("reader_data", Context.MODE_PRIVATE); }

    public static List<Book> books(Context c) {
        List<Book> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("library", "[]"));
            for (int i=0;i<a.length();i++) out.add(Book.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) { }
        return out;
    }

    public static void add(Context c, Book b) {
        List<Book> all = books(c);
        all.removeIf(x -> x.uri.equals(b.uri));
        all.add(0, b);
        JSONArray a = new JSONArray();
        try { for (Book x: all) a.put(x.toJson()); } catch (Exception ignored) { }
        p(c).edit().putString("library", a.toString()).apply();
    }

    private static String key(String uri) { return Integer.toHexString(uri.hashCode()); }
    public static int progress(Context c, String uri) { return p(c).getInt("progress_"+key(uri), 0); }
    public static void progress(Context c, String uri, int page) { p(c).edit().putInt("progress_"+key(uri), page).apply(); }
    public static int offset(Context c, String uri) { return p(c).getInt("offset_"+key(uri), -1); }
    public static void offset(Context c, String uri, int offset) { p(c).edit().putInt("offset_"+key(uri), offset).apply(); }

    public static String bookmarks(Context c, String uri) { return p(c).getString("marks_"+key(uri), "[]"); }
    public static void bookmarks(Context c, String uri, String json) { p(c).edit().putString("marks_"+key(uri), json).apply(); }
    public static String notes(Context c, String uri) { return p(c).getString("notes_"+key(uri), "{}"); }
    public static void notes(Context c, String uri, String json) { p(c).edit().putString("notes_"+key(uri), json).apply(); }
}
