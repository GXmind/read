package com.codex.yuedu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.*;

public class SearchActivity extends Activity {
    private static final int ACCENT=0xfffa4c3c;
    private EditText queryInput;
    private LinearLayout results;
    private TextView summary;
    private int searchRevision;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Color.WHITE);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);build();
    }
    private GradientDrawable rounded(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(Math.max(radius,22)));return g;}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(15),dp(20),0);root.setBackgroundColor(0xfffaf9f7);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);TextView back=text("‹",31,0xff333335);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(42),dp(52)));
        queryInput=new EditText(this);queryInput.setSingleLine();queryInput.setTextSize(16);queryInput.setHint("搜索书名或正文内容");queryInput.setPadding(dp(15),0,dp(12),0);queryInput.setBackground(rounded(0xfff4f4f5,22));queryInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);queryInput.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_SEARCH||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){search();return true;}return false;});bar.addView(queryInput,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView search=text("搜索",15,ACCENT);search.setTypeface(Typeface.DEFAULT,Typeface.BOLD);search.setGravity(Gravity.CENTER);search.setOnClickListener(v->search());bar.addView(search,new LinearLayout.LayoutParams(dp(58),dp(52)));root.addView(bar);
        summary=text("搜索书架内全部 TXT、EPUB、DOCX 内容；PDF 支持书名搜索",13,0xff99999e);summary.setPadding(dp(4),dp(14),dp(4),dp(10));root.addView(summary,new LinearLayout.LayoutParams(-1,dp(52)));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);results.setPadding(0,0,0,dp(28));scroll.addView(results);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);queryInput.requestFocus();queryInput.postDelayed(()->((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(queryInput,InputMethodManager.SHOW_IMPLICIT),180);
    }
    private void search(){
        String query=queryInput.getText().toString().trim();if(query.isEmpty()){queryInput.setError("请输入搜索内容");return;}((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(queryInput.getWindowToken(),0);int revision=++searchRevision;results.removeAllViews();summary.setText("正在搜索全部本地书籍…");ProgressBar progress=new ProgressBar(this);results.addView(progress,new LinearLayout.LayoutParams(-1,dp(56)));
        new Thread(()->{List<Hit> hits=new ArrayList<>();String needle=query.toLowerCase(Locale.ROOT);for(Book b:LibraryStore.books(this)){if(b.title.toLowerCase(Locale.ROOT).contains(needle))hits.add(new Hit(b,0,"书名匹配",stripExt(b.title)));if("pdf".equals(b.type))continue;try{DocumentParser.ParsedBook parsed=DocumentParser.parse(getContentResolver(),Uri.parse(b.uri),b.type);String lower=parsed.text.toLowerCase(Locale.ROOT);int from=0,perBook=0;while(perBook<8&&hits.size()<80){int at=lower.indexOf(needle,from);if(at<0)break;hits.add(new Hit(b,at,chapterAt(parsed,at),snippet(parsed.text,at,query.length())));perBook++;from=at+Math.max(1,needle.length());}}catch(Exception ignored){}}runOnUiThread(()->showResults(revision,query,hits));}).start();
    }
    private void showResults(int revision,String query,List<Hit> hits){if(revision!=searchRevision||isFinishing())return;results.removeAllViews();summary.setText(hits.isEmpty()?"没有找到“"+query+"”":"找到 "+hits.size()+" 条结果");if(hits.isEmpty()){TextView empty=text("换一个关键词试试\n也可以检查文档是否仍保存在手机中",16,0xff99999e);empty.setGravity(Gravity.CENTER);results.addView(empty,new LinearLayout.LayoutParams(-1,dp(210)));return;}for(Hit hit:hits)addHit(hit);}
    private void addHit(Hit hit){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(13),dp(16),dp(13));card.setBackground(rounded(0xfffafafa,13));TextView book=text(stripExt(hit.book.title),16,0xff29292c);book.setTypeface(Typeface.DEFAULT,Typeface.BOLD);book.setSingleLine();book.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(book,new LinearLayout.LayoutParams(-1,dp(28)));TextView chapter=text(hit.chapter,12,ACCENT);chapter.setSingleLine();card.addView(chapter,new LinearLayout.LayoutParams(-1,dp(23)));TextView snippet=text(hit.snippet,14,0xff66666b);snippet.setMaxLines(2);snippet.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(snippet,new LinearLayout.LayoutParams(-1,dp(48)));card.setOnClickListener(v->startActivity(new Intent(this,ReaderActivity.class).putExtra("uri",hit.book.uri).putExtra("title",hit.book.title).putExtra("type",hit.book.type).putExtra("offset",hit.offset)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(112));p.setMargins(0,0,0,dp(10));results.addView(card,p);
    }
    private String chapterAt(DocumentParser.ParsedBook parsed,int offset){String name="正文";for(DocumentParser.Chapter c:parsed.chapters){if(c.offset<=offset)name=c.title;else break;}return name;}
    private String snippet(String source,int at,int length){int start=Math.max(0,at-34),end=Math.min(source.length(),at+length+58);return (start>0?"…":"")+source.substring(start,end).replace('\n',' ').replaceAll("\\s{2,}"," ")+(end<source.length()?"…":"");}
    private String stripExt(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private static class Hit{final Book book;final int offset;final String chapter,snippet;Hit(Book b,int o,String c,String s){book=b;offset=o;chapter=c;snippet=s;}}
}
