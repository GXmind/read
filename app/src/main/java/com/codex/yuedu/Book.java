package com.codex.yuedu;

import org.json.JSONException;
import org.json.JSONObject;

public class Book {
    public final String uri;
    public final String title;
    public final String type;

    public Book(String uri, String title, String type) {
        this.uri = uri; this.title = title; this.type = type;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("uri", uri).put("title", title).put("type", type);
    }

    public static Book fromJson(JSONObject o) throws JSONException {
        return new Book(o.getString("uri"), o.getString("title"), o.optString("type", "txt"));
    }
}
