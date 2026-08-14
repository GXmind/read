package com.codex.yuedu;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
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

    public static List<Note> notesForBook(Context c, Book book) {
        List<Note> out = new ArrayList<>();
        if (book == null) return out;
        try {
            JSONObject root = new JSONObject(notes(c, book.uri));
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String pageKey = keys.next();
                int page;
                try { page = Integer.parseInt(pageKey); } catch (Exception e) { continue; }
                Note note = Note.fromJson(book.uri, book, page, root.opt(pageKey));
                if (!note.content.trim().isEmpty()) out.add(note);
            }
        } catch (Exception ignored) { }
        out.sort((a,b) -> Integer.compare(a.page,b.page));
        return out;
    }

    public static List<Note> allNotes(Context c) {
        List<Note> out = new ArrayList<>();
        for (Book book : books(c)) out.addAll(notesForBook(c, book));
        out.sort(Comparator.comparingLong((Note n) -> n.updatedAt).reversed());
        return out;
    }

    public static Note note(Context c, Book book, int page) {
        for (Note note : notesForBook(c, book)) if (note.page == page) return note;
        return null;
    }

    public static void saveNote(Context c, Note note) {
        try {
            JSONObject root = new JSONObject(notes(c, note.bookUri));
            root.put(String.valueOf(note.page), note.toJson());
            notes(c, note.bookUri, root.toString());
        } catch (Exception ignored) { }
    }

    public static void deleteNote(Context c, String uri, int page) {
        try {
            JSONObject root = new JSONObject(notes(c, uri));
            root.remove(String.valueOf(page));
            notes(c, uri, root.toString());
        } catch (Exception ignored) { }
    }

    public static List<Bookmark> bookmarksForBook(Context c, Book book) {
        List<Bookmark> out = new ArrayList<>();
        if (book == null) return out;
        try {
            JSONArray values = new JSONArray(bookmarks(c, book.uri));
            for (int i = 0; i < values.length(); i++) {
                Object value = values.opt(i);
                if (value instanceof JSONObject) out.add(Bookmark.fromJson(book, (JSONObject)value));
                else {
                    int page = values.optInt(i, -1);
                    if (page >= 0) out.add(new Bookmark(book.uri, book.title, book.type, page, -1, "", "", 0));
                }
            }
        } catch (Exception ignored) { }
        out.sort((a,b) -> Integer.compare(a.page,b.page));
        return out;
    }

    public static List<Bookmark> allBookmarks(Context c) {
        List<Bookmark> out = new ArrayList<>();
        for (Book book : books(c)) out.addAll(bookmarksForBook(c, book));
        out.sort((a,b) -> {
            int recent = Long.compare(b.createdAt, a.createdAt);
            return recent != 0 ? recent : Integer.compare(a.page, b.page);
        });
        return out;
    }

    public static Bookmark bookmark(Context c, Book book, int page) {
        for (Bookmark bookmark : bookmarksForBook(c, book)) if (bookmark.page == page) return bookmark;
        return null;
    }

    public static void saveBookmark(Context c, Bookmark bookmark) {
        Book book = new Book(bookmark.bookUri, bookmark.bookTitle, bookmark.bookType);
        JSONArray out = new JSONArray();
        try {
            for (Bookmark old : bookmarksForBook(c, book)) if (old.page != bookmark.page) out.put(old.toJson());
            out.put(bookmark.toJson());
            bookmarks(c, bookmark.bookUri, out.toString());
        } catch (Exception ignored) { }
    }

    public static void deleteBookmark(Context c, String uri, int page) {
        Book book = null;
        for (Book candidate : books(c)) if (candidate.uri.equals(uri)) { book = candidate; break; }
        if (book == null) return;
        JSONArray out = new JSONArray();
        try {
            for (Bookmark old : bookmarksForBook(c, book)) if (old.page != page) out.put(old.toJson());
            bookmarks(c, uri, out.toString());
        } catch (Exception ignored) { }
    }
}
