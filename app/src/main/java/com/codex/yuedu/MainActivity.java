package com.codex.yuedu;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK=100;
    private static final int ACCENT=Color.rgb(250,76,60);
    private LinearLayout content;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if(!SecurityGuard.verifySelf(this)){new AlertDialog.Builder(this).setTitle("安全校验失败").setMessage("应用签名不是悦读官方生产证书。为保护书籍与笔记，程序已停止运行。请仅安装 GXmind/read Releases 中发布的版本。").setCancelable(false).setPositiveButton("退出",(d,w)->finishAndRemoveTask()).show();return;}
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        render();
        if(Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData()!=null) importUri(getIntent().getData());
    }
    @Override protected void onResume(){super.onResume();if(content!=null)render();}

    private TextView label(String value,float size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private GradientDrawable rounded(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}

    private void render() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xfffaf9f7);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(20),dp(24),dp(32));scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("书架",30,Color.rgb(25,25,27));title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView search=label("⌕",31,Color.rgb(45,45,48));search.setGravity(Gravity.CENTER);search.setContentDescription("搜索");search.setOnClickListener(v->startActivity(new Intent(this,SearchActivity.class)));header.addView(search,new LinearLayout.LayoutParams(dp(46),dp(46)));
        TextView more=label("⋯",29,Color.rgb(45,45,48));more.setGravity(Gravity.CENTER);more.setOnClickListener(v->pick());more.setContentDescription("导入文档");header.addView(more,new LinearLayout.LayoutParams(dp(42),dp(46)));content.addView(header);

        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER_VERTICAL);tabs.addView(tab("书架",true));tabs.addView(tab("阅读历史",false));TextView notesTab=tab("我的笔记",false);notesTab.setOnClickListener(v->openNotes());tabs.addView(notesTab);content.addView(tabs,new LinearLayout.LayoutParams(-1,dp(48)));

        List<Book> books=LibraryStore.books(this);
        LinearLayout continueCard=new LinearLayout(this);continueCard.setPadding(dp(19),dp(15),dp(13),dp(15));continueCard.setGravity(Gravity.CENTER_VERTICAL);continueCard.setBackground(rounded(Color.rgb(255,247,244),24));continueCard.setElevation(dp(2));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        TextView small=label("本地阅读",13,Color.rgb(156,93,78));info.addView(small);
        TextView headline=label(books.isEmpty()?"导入第一本小说":"继续阅读 · "+books.get(0).title,16,Color.rgb(50,39,36));headline.setTypeface(Typeface.DEFAULT,Typeface.BOLD);headline.setSingleLine();headline.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(headline);continueCard.addView(info,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView go=label("继续  ›",14,Color.WHITE);go.setTypeface(Typeface.DEFAULT,Typeface.BOLD);go.setGravity(Gravity.CENTER);go.setBackground(rounded(ACCENT,18));continueCard.addView(go,new LinearLayout.LayoutParams(dp(74),dp(36)));
        continueCard.setOnClickListener(v->{if(books.isEmpty())pick();else open(books.get(0));});
        LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,dp(80));cardLp.setMargins(0,dp(7),0,dp(20));content.addView(continueCard,cardLp);

        LinearLayout filters=new LinearLayout(this);filters.setGravity(Gravity.CENTER_VERTICAL);filters.addView(chip("全部",true));filters.addView(chip("TXT",false));filters.addView(chip("EPUB",false));filters.addView(chip("Word",false));filters.addView(chip("PDF",false));content.addView(filters,new LinearLayout.LayoutParams(-1,dp(42)));

        GridLayout grid=new GridLayout(this);grid.setColumnCount(3);grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);grid.setUseDefaultMargins(false);
        addImportTile(grid);
        for(Book book:books)addBookTile(grid,book);
        content.addView(grid);
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(-1,dp(70));navLp.setMargins(dp(12),0,dp(12),dp(12));root.addView(bottomNav(),navLp);
        setContentView(root);
    }

    private TextView tab(String text,boolean selected){TextView t=label(text,selected?17:15,selected?Color.rgb(25,25,27):Color.rgb(145,145,150));t.setGravity(Gravity.CENTER);if(selected)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,0,dp(26),0);return t;}
    private TextView chip(String text,boolean selected){TextView t=label(text,13,selected?ACCENT:Color.rgb(105,105,110));t.setGravity(Gravity.CENTER);t.setBackground(rounded(selected?Color.rgb(255,238,235):Color.rgb(244,243,242),18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(34),1);p.setMargins(0,0,dp(8),0);t.setLayoutParams(p);return t;}

    private void addImportTile(GridLayout grid){
        LinearLayout tile=tileContainer();TextView cover=label("＋\n\n导入本地书",17,Color.rgb(125,125,130));cover.setGravity(Gravity.CENTER);cover.setLineSpacing(0,.85f);GradientDrawable bg=rounded(Color.rgb(247,247,248),18);bg.setStroke(dp(1),Color.rgb(225,225,228),dp(5),dp(4));cover.setBackground(bg);tile.addView(cover,new LinearLayout.LayoutParams(-1,dp(142)));TextView name=label("扫描手机文档",14,Color.rgb(60,60,64));name.setGravity(Gravity.CENTER);tile.addView(name,new LinearLayout.LayoutParams(-1,dp(39)));tile.setOnClickListener(v->pick());grid.addView(tile,tileParams());
    }
    private void addBookTile(GridLayout grid,Book book){
        LinearLayout tile=tileContainer();
        FrameLayout cover=new FrameLayout(this);int[] palette={0xff596b8d,0xff8b6253,0xff537a70,0xff7f668e,0xffb06e5b,0xff586d78};int color=palette[Math.abs(book.title.hashCode())%palette.length];cover.setBackground(rounded(color,18));cover.setElevation(dp(2));
        TextView type=label(book.type.toUpperCase(Locale.ROOT),10,0xddffffff);type.setGravity(Gravity.CENTER);type.setBackground(rounded(0x33000000,10));FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(48),dp(22),Gravity.TOP|Gravity.LEFT);tp.setMargins(dp(10),dp(10),0,0);cover.addView(type,tp);
        TextView initial=label(shortTitle(book.title),18,Color.WHITE);initial.setGravity(Gravity.CENTER);initial.setPadding(dp(12),dp(25),dp(12),dp(14));initial.setTypeface(Typeface.SERIF,Typeface.BOLD);cover.addView(initial,new FrameLayout.LayoutParams(-1,-1));
        tile.addView(cover,new LinearLayout.LayoutParams(-1,dp(142)));
        TextView name=label(stripExt(book.title),14,Color.rgb(34,34,37));name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(name,new LinearLayout.LayoutParams(-1,dp(25)));
        int saved=LibraryStore.progress(this,book.uri);TextView progress=label(saved==0?"尚未开始":"读至第 "+(saved+1)+" 页",12,Color.rgb(155,155,160));tile.addView(progress,new LinearLayout.LayoutParams(-1,dp(22)));tile.setOnClickListener(v->open(book));grid.addView(tile,tileParams());
    }
    private LinearLayout tileContainer(){LinearLayout t=new LinearLayout(this);t.setOrientation(LinearLayout.VERTICAL);t.setPadding(dp(2),dp(7),dp(14),dp(9));return t;}
    private GridLayout.LayoutParams tileParams(){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(205);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);return p;}
    private String shortTitle(String s){String n=stripExt(s);return n.length()>8?n.substring(0,8)+"\n…":n;}
    private String stripExt(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}

    private LinearLayout bottomNav(){LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(10),dp(4),dp(10),dp(4));nav.setBackground(rounded(Color.WHITE,24));nav.setElevation(dp(12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);nav.addView(navItem("▣","书架",true),p);nav.addView(navItem("▤","本地",false),p);LinearLayout notes=navItem("✎","笔记",false);notes.setOnClickListener(v->openNotes());nav.addView(notes,p);LinearLayout mine=navItem("○","更新",false);mine.setOnClickListener(v->startActivity(new Intent(this,UpdateActivity.class)));nav.addView(mine,p);return nav;}
    private LinearLayout navItem(String icon,String text,boolean on){LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);TextView i=label(icon,21,on?ACCENT:Color.rgb(130,130,135));i.setGravity(Gravity.CENTER);item.addView(i,new LinearLayout.LayoutParams(-1,dp(29)));TextView t=label(text,11,on?ACCENT:Color.rgb(120,120,125));t.setGravity(Gravity.CENTER);item.addView(t,new LinearLayout.LayoutParams(-1,dp(25)));if(!on)item.setOnClickListener(v->Toast.makeText(this,"该页面将在后续版本开放",Toast.LENGTH_SHORT).show());return item;}

    private void pick() {Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","application/epub+zip","application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});startActivityForResult(i,PICK);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==PICK&&c==RESULT_OK&&data!=null)importUri(data.getData());}
    private void importUri(Uri uri){if(uri==null)return;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String name=name(uri),type=extension(name);if(type==null){Toast.makeText(this,"暂不支持该文件，请选择 TXT、EPUB、DOCX 或 PDF",Toast.LENGTH_LONG).show();return;}Book b=new Book(uri.toString(),name,type);LibraryStore.add(this,b);open(b);}
    private void open(Book b){startActivity(new Intent(this,ReaderActivity.class).putExtra("uri",b.uri).putExtra("title",b.title).putExtra("type",b.type));}
    private void openNotes(){startActivity(new Intent(this,NotesActivity.class));}
    private String name(Uri uri){String n=null;try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}return n==null?"未命名文档":n;}
    private String extension(String n){String s=n.toLowerCase(Locale.ROOT);if(s.endsWith(".txt"))return"txt";if(s.endsWith(".epub"))return"epub";if(s.endsWith(".docx"))return"docx";if(s.endsWith(".pdf"))return"pdf";return null;}
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
