package com.codex.yuedu;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotesActivity extends Activity {
    private static final int ACCENT = 0xfffa4c3c;
    private LinearLayout list;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        render();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) render(); }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff7f7f8);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(5), dp(20), dp(4));
        top.setBackgroundColor(Color.WHITE);
        TextView back = text("‹", 34, 0xff29292c);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(54)));
        TextView title = text("我的笔记", 22, 0xff202024);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(64)));

        List<Note> notes = LibraryStore.allNotes(this);
        TextView summary = text(notes.isEmpty() ? "阅读时写下的想法会集中保存在这里" : "共 " + notes.size() + " 条 · 按最近编辑排序", 13, 0xff85858b);
        summary.setPadding(dp(24), dp(16), dp(24), dp(10));
        root.addView(summary, new LinearLayout.LayoutParams(-1, dp(50)));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), 0, dp(20), dp(30));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (notes.isEmpty()) addEmpty(); else for (Note note : notes) addCard(note);
        setContentView(root);
    }

    private void addEmpty() {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        TextView icon = text("✎", 42, 0xffc9c9ce); icon.setGravity(Gravity.CENTER);
        TextView title = text("还没有笔记", 18, 0xff505056); title.setGravity(Gravity.CENTER); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView hint = text("打开一本书，在阅读菜单中点击“笔记”", 14, 0xff99999f); hint.setGravity(Gravity.CENTER);
        empty.addView(icon, new LinearLayout.LayoutParams(-1, dp(70)));
        empty.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        empty.addView(hint, new LinearLayout.LayoutParams(-1, dp(40)));
        list.addView(empty, new LinearLayout.LayoutParams(-1, dp(300)));
    }

    private void addCard(Note note) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(13));
        card.setBackground(round(Color.WHITE, 16));
        card.setElevation(dp(1));

        LinearLayout meta = new LinearLayout(this); meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView book = text(stripExt(note.bookTitle), 15, 0xff29292d); book.setTypeface(Typeface.DEFAULT, Typeface.BOLD); book.setSingleLine(); book.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(book, new LinearLayout.LayoutParams(0, dp(28), 1));
        TextView page = text("第 " + (note.page + 1) + " 页", 12, ACCENT); page.setGravity(Gravity.CENTER); page.setBackground(round(0xffffece9, 12));
        meta.addView(page, new LinearLayout.LayoutParams(dp(64), dp(26)));
        card.addView(meta);

        if (!note.chapter.isEmpty()) {
            TextView chapter = text(note.chapter, 12, 0xff8b8179); chapter.setSingleLine(); chapter.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(chapter, new LinearLayout.LayoutParams(-1, dp(26)));
        }
        if (!note.quote.isEmpty()) {
            TextView quote = text("“" + note.quote + "”", 13, 0xff77777c); quote.setMaxLines(2); quote.setEllipsize(TextUtils.TruncateAt.END); quote.setPadding(dp(10), dp(7), dp(10), dp(7)); quote.setBackground(round(0xfff6f4f1, 9));
            card.addView(quote, new LinearLayout.LayoutParams(-1, dp(58)));
        }
        TextView body = text(note.content, 16, 0xff343438); body.setPadding(0, dp(10), 0, dp(6)); body.setMaxLines(5); body.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(body, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView date = text(formatDate(note.updatedAt), 11, 0xffaaaaaf); actions.addView(date, new LinearLayout.LayoutParams(0, dp(36), 1));
        TextView edit = action("编辑"); edit.setOnClickListener(v -> edit(note)); actions.addView(edit, new LinearLayout.LayoutParams(dp(58), dp(36)));
        TextView delete = action("删除"); delete.setTextColor(0xffa06b65); delete.setOnClickListener(v -> confirmDelete(note)); actions.addView(delete, new LinearLayout.LayoutParams(dp(58), dp(36)));
        card.addView(actions);
        card.setOnClickListener(v -> open(note));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(12)); list.addView(card, lp);
    }

    private void open(Note note) {
        Intent i = new Intent(this, ReaderActivity.class).putExtra("uri", note.bookUri).putExtra("title", note.bookTitle).putExtra("type", note.bookType).putExtra("page", note.page);
        if (note.offset >= 0) i.putExtra("offset", note.offset);
        startActivity(i);
    }

    private void edit(Note old) {
        EditText input = new EditText(this); input.setMinLines(4); input.setMaxLines(10); input.setText(old.content); input.setSelection(input.length()); input.setHint("记录你的想法…"); input.setPadding(dp(18), dp(12), dp(18), dp(12)); input.setBackground(round(0xfff5f5f6, 12));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(stripExt(old.bookTitle) + " · 第 " + (old.page + 1) + " 页").setView(input).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = input.getText().toString().trim(); if (content.isEmpty()) { input.setError("笔记内容不能为空"); return; }
            LibraryStore.saveNote(this, new Note(old.bookUri, old.bookTitle, old.bookType, old.page, old.offset, old.chapter, old.quote, content, System.currentTimeMillis())); dialog.dismiss(); render();
        })); dialog.show();
    }

    private void confirmDelete(Note note) {
        new AlertDialog.Builder(this).setTitle("删除这条笔记？").setMessage("删除后无法恢复，原书和阅读进度不会受影响。").setNegativeButton("取消", null).setPositiveButton("删除", (d,w) -> { LibraryStore.deleteNote(this, note.bookUri, note.page); render(); }).show();
    }

    private TextView action(String label) { TextView t = text(label, 13, 0xff66666c); t.setGravity(Gravity.CENTER); return t; }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private GradientDrawable round(int color, float radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private String formatDate(long time) { return time <= 0 ? "旧版笔记" : new SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(new Date(time)); }
    private String stripExt(String s) { int i = s.lastIndexOf('.'); return i > 0 ? s.substring(0, i) : s; }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
