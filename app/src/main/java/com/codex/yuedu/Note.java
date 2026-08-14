package com.codex.yuedu;

import org.json.JSONObject;

public final class Note {
    public final String bookUri;
    public final String bookTitle;
    public final String bookType;
    public final int page;
    public final int offset;
    public final String chapter;
    public final String quote;
    public final String content;
    public final long updatedAt;

    public Note(String bookUri, String bookTitle, String bookType, int page, int offset,
                String chapter, String quote, String content, long updatedAt) {
        this.bookUri = bookUri;
        this.bookTitle = bookTitle;
        this.bookType = bookType;
        this.page = page;
        this.offset = offset;
        this.chapter = chapter;
        this.quote = quote;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("bookTitle", bookTitle)
                    .put("bookType", bookType)
                    .put("page", page)
                    .put("offset", offset)
                    .put("chapter", chapter)
                    .put("quote", quote)
                    .put("content", content)
                    .put("updatedAt", updatedAt);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    public static Note fromJson(String uri, Book book, int fallbackPage, Object value) {
        if (value instanceof JSONObject) {
            JSONObject o = (JSONObject) value;
            return new Note(uri,
                    o.optString("bookTitle", book == null ? "未命名文档" : book.title),
                    o.optString("bookType", book == null ? "txt" : book.type),
                    o.optInt("page", fallbackPage), o.optInt("offset", -1),
                    o.optString("chapter", ""), o.optString("quote", ""),
                    o.optString("content", ""), o.optLong("updatedAt", 0));
        }
        return new Note(uri, book == null ? "未命名文档" : book.title,
                book == null ? "txt" : book.type, fallbackPage, -1, "", "",
                value == null ? "" : String.valueOf(value), 0);
    }
}
