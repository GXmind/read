package com.codex.yuedu;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class BookmarksActivity extends Activity {
    private static final int ACCENT = 0xfffa4c3c;
    private LinearLayout list;

    @Override public void onCreate(Bundle state) { super.onCreate(state); render(); }
    @Override protected void onResume() { super.onResume(); if (list != null) render(); }

    private void render() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xfff7f7f8);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(12),dp(5),dp(20),dp(4)); top.setBackgroundColor(Color.WHITE);
        TextView back=text("‹",34,0xff29292c);back.setGravity(Gravity.CENTER);back.setContentDescription("返回");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(54)));
        TextView title=text("我的书签",22,0xff202024);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));root.addView(top,new LinearLayout.LayoutParams(-1,dp(64)));
        List<Bookmark> bookmarks=LibraryStore.allBookmarks(this);TextView summary=text(bookmarks.isEmpty()?"阅读时添加的书签会集中保存在这里":"共 "+bookmarks.size()+" 个 · 点击可返回原文",13,0xff85858b);summary.setPadding(dp(24),dp(16),dp(24),dp(10));root.addView(summary,new LinearLayout.LayoutParams(-1,dp(50)));
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(20),0,dp(20),dp(30));scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(bookmarks.isEmpty())addEmpty();else for(Bookmark bookmark:bookmarks)addCard(bookmark);
        setContentView(root);UiInsets.standard(this,root,Color.WHITE);
    }

    private void addEmpty(){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);TextView icon=text("☆",48,0xffc9c9ce);icon.setGravity(Gravity.CENTER);TextView title=text("还没有书签",18,0xff505056);title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);TextView hint=text("打开一本书，在阅读菜单中点击“书签”",14,0xff99999f);hint.setGravity(Gravity.CENTER);empty.addView(icon,new LinearLayout.LayoutParams(-1,dp(74)));empty.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));empty.addView(hint,new LinearLayout.LayoutParams(-1,dp(40)));list.addView(empty,new LinearLayout.LayoutParams(-1,dp(300)));}

    private void addCard(Bookmark bookmark){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(17),dp(20),dp(14));card.setBackground(round(Color.WHITE,24));card.setElevation(dp(2));
        LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);TextView book=text(stripExt(bookmark.bookTitle),15,0xff29292d);book.setTypeface(Typeface.DEFAULT,Typeface.BOLD);book.setSingleLine();book.setEllipsize(TextUtils.TruncateAt.END);meta.addView(book,new LinearLayout.LayoutParams(0,dp(28),1));TextView page=text("第 "+(bookmark.page+1)+" 页",12,ACCENT);page.setGravity(Gravity.CENTER);page.setBackground(round(0xffffece9,12));meta.addView(page,new LinearLayout.LayoutParams(dp(68),dp(26)));card.addView(meta);
        if(!bookmark.chapter.isEmpty()){TextView chapter=text(bookmark.chapter,12,0xff8b8179);chapter.setSingleLine();chapter.setEllipsize(TextUtils.TruncateAt.END);card.addView(chapter,new LinearLayout.LayoutParams(-1,dp(28)));}
        if(!bookmark.excerpt.isEmpty()){TextView excerpt=text(bookmark.excerpt,14,0xff555156);excerpt.setMaxLines(3);excerpt.setEllipsize(TextUtils.TruncateAt.END);excerpt.setPadding(dp(13),dp(9),dp(13),dp(9));excerpt.setBackground(round(0xfff6f4f1,16));card.addView(excerpt,new LinearLayout.LayoutParams(-1,dp(72)));}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);TextView date=text(formatDate(bookmark.createdAt),11,0xffaaaaaf);actions.addView(date,new LinearLayout.LayoutParams(0,dp(38),1));TextView delete=text("删除",13,0xffa06b65);delete.setGravity(Gravity.CENTER);delete.setOnClickListener(v->confirmDelete(bookmark));actions.addView(delete,new LinearLayout.LayoutParams(dp(58),dp(38)));card.addView(actions);card.setOnClickListener(v->open(bookmark));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));list.addView(card,lp);}

    private void open(Bookmark bookmark){Intent intent=new Intent(this,ReaderActivity.class).putExtra("uri",bookmark.bookUri).putExtra("title",bookmark.bookTitle).putExtra("type",bookmark.bookType).putExtra("page",bookmark.page);if(bookmark.offset>=0)intent.putExtra("offset",bookmark.offset);startActivity(intent);}
    private void confirmDelete(Bookmark bookmark){new AlertDialog.Builder(this).setTitle("删除这个书签？").setMessage("只会删除书签，不影响原书和阅读进度。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{LibraryStore.deleteBookmark(this,bookmark.bookUri,bookmark.page);render();}).show();}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable round(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private String formatDate(long time){return time<=0?"旧版书签":new SimpleDateFormat("MM月dd日 HH:mm",Locale.CHINA).format(new Date(time));}
    private String stripExt(String value){int i=value.lastIndexOf('.');return i>0?value.substring(0,i):value;}
    private int dp(float value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
