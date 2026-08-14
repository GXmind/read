package com.codex.yuedu;

import org.json.JSONException;
import org.json.JSONObject;

public final class Bookmark {
    public final String bookUri;
    public final String bookTitle;
    public final String bookType;
    public final int page;
    public final int offset;
    public final String chapter;
    public final String excerpt;
    public final long createdAt;

    public Bookmark(String bookUri, String bookTitle, String bookType, int page, int offset,
                    String chapter, String excerpt, long createdAt) {
        this.bookUri = bookUri;
        this.bookTitle = bookTitle;
        this.bookType = bookType;
        this.page = page;
        this.offset = offset;
        this.chapter = chapter == null ? "" : chapter;
        this.excerpt = excerpt == null ? "" : excerpt;
        this.createdAt = createdAt;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("page", page)
                .put("offset", offset)
                .put("chapter", chapter)
                .put("excerpt", excerpt)
                .put("createdAt", createdAt);
    }

    static Bookmark fromJson(Book book, JSONObject value) {
        return new Bookmark(book.uri, book.title, book.type,
                value.optInt("page", 0), value.optInt("offset", -1),
                value.optString("chapter", ""), value.optString("excerpt", ""),
                value.optLong("createdAt", 0));
    }
}
