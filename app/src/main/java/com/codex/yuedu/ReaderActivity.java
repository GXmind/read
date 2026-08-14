package com.codex.yuedu;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class ReaderActivity extends Activity {
    private String uri,title,type; private int page; private int pendingOffset=-1,pendingPage=-1;
    private LinearLayout root,toolbar,bottomPanel; private TextView pageText,status,titleView,fontValue,lineValue,chapterLabel; private SeekBar pageSeek;
    private DocumentParser.ParsedBook book; private android.graphics.pdf.PdfRenderer renderer; private ParcelFileDescriptor pdfFd; private ImageView pdfImage;
    private float downX; private int paperColor,textColor,fontSize,lineSpacing,paginationRevision; private final List<Integer> pageStarts=new ArrayList<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);uri=getIntent().getStringExtra("uri");title=getIntent().getStringExtra("title");type=getIntent().getStringExtra("type");pendingPage=getIntent().hasExtra("page")?getIntent().getIntExtra("page",0):-1;page=pendingPage>=0?pendingPage:LibraryStore.progress(this,uri);pendingOffset=getIntent().hasExtra("offset")?getIntent().getIntExtra("offset",0):(pendingPage>=0?-1:LibraryStore.offset(this,uri));
        android.content.SharedPreferences p=getSharedPreferences("appearance",MODE_PRIVATE);paperColor=p.getInt("paper",0xfff5eedf);textColor=p.getInt("ink",0xff2f2c28);fontSize=p.getInt("font",20);lineSpacing=p.getInt("line_spacing",8);
        build();load();
    }
    private GradientDrawable round(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}

    private void build(){
        getWindow().setStatusBarColor(paperColor);getWindow().setNavigationBarColor(paperColor);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(paperColor);
        toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(8),0,dp(8),0);toolbar.setBackgroundColor(paperColor);toolbar.setElevation(dp(2));
        addTop("‹",v->finish(),48,30);titleView=text(stripExt(title),15,0xff48443f);titleView.setSingleLine();titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);titleView.setGravity(Gravity.CENTER);toolbar.addView(titleView,new LinearLayout.LayoutParams(0,dp(54),1));addTop("⋯",v->showSettings(),48,28);toolbar.setVisibility(View.GONE);

        chapterLabel=text("正在读取章节…",12,0xff8f887f);chapterLabel.setSingleLine();chapterLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);chapterLabel.setPadding(dp(34),dp(7),dp(34),0);root.addView(chapterLabel,new LinearLayout.LayoutParams(-1,dp(36)));
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(paperColor);
        pageText=text("",fontSize,textColor);pageText.setGravity(Gravity.TOP);pageText.setIncludeFontPadding(false);pageText.setLineSpacing(dp(lineSpacing),1.05f);pageText.setPadding(dp(34),dp(16),dp(34),dp(24));frame.addView(pageText,new FrameLayout.LayoutParams(-1,-1));
        pdfImage=new ImageView(this);pdfImage.setScaleType(ImageView.ScaleType.FIT_CENTER);pdfImage.setBackgroundColor(0xff555555);pdfImage.setVisibility(View.GONE);frame.addView(pdfImage,new FrameLayout.LayoutParams(-1,-1));
        View touch=new View(this);touch.setContentDescription("阅读区域");touch.setOnClickListener(v->toggleBars());touch.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX;if(Math.abs(dx)>dp(42)){if(dx<0)next();else prev();}else if(e.getX()<v.getWidth()*.32)prev();else if(e.getX()>v.getWidth()*.68)next();else v.performClick();return true;}return true;});frame.addView(touch,new FrameLayout.LayoutParams(-1,-1));root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));

        status=text("",12,0xff8f887f);status.setGravity(Gravity.CENTER);status.setPadding(4,2,4,dp(7));root.addView(status,new LinearLayout.LayoutParams(-1,dp(28)));
        bottomPanel=makeBottomPanel();bottomPanel.setVisibility(View.GONE);
        FrameLayout screen=new FrameLayout(this);screen.setBackgroundColor(paperColor);screen.addView(root,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams topParams=new FrameLayout.LayoutParams(-1,dp(54),Gravity.TOP);screen.addView(toolbar,topParams);FrameLayout.LayoutParams bottomParams=new FrameLayout.LayoutParams(-1,dp(142),Gravity.BOTTOM);screen.addView(bottomPanel,bottomParams);setContentView(screen);
    }
    private LinearLayout makeBottomPanel(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(9),dp(24),dp(8));panel.setBackgroundColor(paperColor);panel.setElevation(dp(10));
        LinearLayout progress=new LinearLayout(this);progress.setGravity(Gravity.CENTER_VERTICAL);TextView prev=text("上一页",12,0xff6f6962);prev.setGravity(Gravity.CENTER);prev.setOnClickListener(v->prev());progress.addView(prev,new LinearLayout.LayoutParams(dp(52),dp(39)));
        pageSeek=new SeekBar(this);pageSeek.setMax(1);pageSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(0xfffa4c3c));pageSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(0xfffa4c3c));pageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user){page=value;if("pdf".equals(type))showPdf();else showPage();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});progress.addView(pageSeek,new LinearLayout.LayoutParams(0,dp(39),1));TextView next=text("下一页",12,0xff6f6962);next.setGravity(Gravity.CENTER);next.setOnClickListener(v->next());progress.addView(next,new LinearLayout.LayoutParams(dp(52),dp(39)));panel.addView(progress);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(0,-1,1);actions.addView(action("☰","目录",v->showContents()),item);actions.addView(action("☆","书签",v->showBookmarks()),item);actions.addView(action("✎","笔记",v->editNote()),item);actions.addView(action("Aa","设置",v->showSettings()),item);panel.addView(actions,new LinearLayout.LayoutParams(-1,dp(82)));return panel;
    }
    private LinearLayout action(String icon,String name,View.OnClickListener l){LinearLayout a=new LinearLayout(this);a.setOrientation(LinearLayout.VERTICAL);a.setGravity(Gravity.CENTER);TextView i=text(icon,icon.equals("Aa")?17:21,0xff3d3935);i.setGravity(Gravity.CENTER);a.addView(i,new LinearLayout.LayoutParams(-1,dp(36)));TextView n=text(name,12,0xff57514b);n.setGravity(Gravity.CENTER);a.addView(n,new LinearLayout.LayoutParams(-1,dp(30)));a.setOnClickListener(l);return a;}
    private void addTop(String s,View.OnClickListener l,int width,int size){TextView t=text(s,size,0xff393632);t.setGravity(Gravity.CENTER);t.setOnClickListener(l);toolbar.addView(t,new LinearLayout.LayoutParams(dp(width),dp(54)));}

    private void load(){if("pdf".equals(type)){loadPdf();return;}pageText.setText("正在整理章节与排版…");new Thread(()->{try{DocumentParser.ParsedBook p=DocumentParser.parse(getContentResolver(),Uri.parse(uri),type);runOnUiThread(()->{book=p;pageText.post(this::paginate);});}catch(Exception e){runOnUiThread(()->error(e));}}).start();}
    private void loadPdf(){try{pdfFd=getContentResolver().openFileDescriptor(Uri.parse(uri),"r");renderer=new android.graphics.pdf.PdfRenderer(pdfFd);page=Math.max(0,Math.min(page,renderer.getPageCount()-1));pageText.setVisibility(View.GONE);pdfImage.setVisibility(View.VISIBLE);showPdf();}catch(Exception e){error(e);}}
    private int count(){if("pdf".equals(type))return renderer==null?0:renderer.getPageCount();return book==null?0:Math.max(1,pageStarts.size());}
    private void paginate(){
        if(book==null)return;int width=pageText.getWidth()-pageText.getPaddingLeft()-pageText.getPaddingRight();int height=pageText.getHeight()-pageText.getPaddingTop()-pageText.getPaddingBottom();
        if(width<=0||height<=0){pageText.postDelayed(this::paginate,50);return;}
        final int keepOffset=pendingOffset>=0?pendingOffset:currentOffset(),keepPage=pendingPage,revision=++paginationRevision;pendingOffset=-1;pendingPage=-1;pageText.setText("正在按章节重新分页…");TextPaint paint=new TextPaint(pageText.getPaint());String source=book.text;
        new Thread(()->{List<Integer> starts=buildPageStarts(source,paint,width,height);runOnUiThread(()->{if(revision!=paginationRevision)return;pageStarts.clear();pageStarts.addAll(starts);page=keepPage>=0?Math.max(0,Math.min(keepPage,pageStarts.size()-1)):findPageForOffset(Math.max(0,keepOffset));showPage();});}).start();
    }
    private List<Integer> buildPageStarts(String source,TextPaint paint,int width,int height){
        List<Integer> starts=new ArrayList<>();if(source.isEmpty()){starts.add(0);return starts;}List<Integer> boundaries=new ArrayList<>();boundaries.add(0);
        for(DocumentParser.Chapter c:book.chapters)if(c.offset>0&&c.offset<source.length()&&!boundaries.contains(c.offset))boundaries.add(c.offset);Collections.sort(boundaries);boundaries.add(source.length());
        for(int b=0;b<boundaries.size()-1;b++){int segmentStart=boundaries.get(b),segmentEnd=boundaries.get(b+1);if(segmentStart>=segmentEnd)continue;String segment=source.substring(segmentStart,segmentEnd);StaticLayout layout=StaticLayout.Builder.obtain(segment,0,segment.length(),paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(dp(lineSpacing),1.05f).build();int line=0,lines=layout.getLineCount();while(line<lines){int absolute=segmentStart+layout.getLineStart(line);if(starts.isEmpty()||starts.get(starts.size()-1)!=absolute)starts.add(absolute);int top=layout.getLineTop(line),next=line+1;while(next<lines&&layout.getLineBottom(next)-top<=height)next++;line=Math.max(line+1,next);}}
        if(starts.isEmpty())starts.add(0);return starts;
    }
    private int currentOffset(){if(book==null)return 0;if(page>=0&&page<pageStarts.size())return pageStarts.get(page);return Math.min(book.text.length(),Math.max(0,page)*1000);}
    private int findPageForOffset(int offset){if(pageStarts.isEmpty())return 0;int found=Collections.binarySearch(pageStarts,offset);if(found>=0)return found;return Math.max(0,Math.min(pageStarts.size()-1,-found-2));}
    private void showPage(){if(book==null||pageStarts.isEmpty())return;page=Math.max(0,Math.min(page,count()-1));int from=pageStarts.get(page),to=page+1<pageStarts.size()?pageStarts.get(page+1):book.text.length();chapterLabel.setText(currentChapterName(from));pageText.setText(book.text.substring(from,to));update();}
    private String currentChapterName(int offset){String name="正文";for(DocumentParser.Chapter c:book.chapters){if(c.offset<=offset)name=c.title;else break;}return name;}
    private void showPdf(){if(renderer==null)return;page=Math.max(0,Math.min(page,count()-1));chapterLabel.setText("PDF · 第 "+(page+1)+" 页");android.graphics.pdf.PdfRenderer.Page p=renderer.openPage(page);int w=Math.max(1,getResources().getDisplayMetrics().widthPixels);int h=w*p.getHeight()/p.getWidth();Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bm.eraseColor(Color.WHITE);p.render(bm,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();pdfImage.setImageBitmap(bm);update();}
    private void update(){LibraryStore.progress(this,uri,page);if(!"pdf".equals(type))LibraryStore.offset(this,uri,currentOffset());String flags=(isMarked()?"★ ":"")+(hasCurrentNote()?"✎":"");status.setText(String.format(Locale.getDefault(),"%d / %d   %s",page+1,count(),flags));if(pageSeek!=null){pageSeek.setMax(Math.max(1,count()-1));pageSeek.setProgress(page);}}
    private void next(){if(page<count()-1){page++;if("pdf".equals(type))showPdf();else showPage();}}
    private void prev(){if(page>0){page--;if("pdf".equals(type))showPdf();else showPage();}}
    private void toggleBars(){boolean show=toolbar.getVisibility()!=View.VISIBLE;toolbar.setVisibility(show?View.VISIBLE:View.GONE);bottomPanel.setVisibility(show?View.VISIBLE:View.GONE);}

    private void showSettings(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(18),dp(22),dp(24));box.setBackground(round(0xfffdfcf9,22));
        TextView heading=text("阅读设置",20,0xff292725);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(heading,new LinearLayout.LayoutParams(-1,dp(42)));
        LinearLayout bright=new LinearLayout(this);bright.setGravity(Gravity.CENTER_VERTICAL);TextView sun=text("☀",20,0xff6b6762);sun.setGravity(Gravity.CENTER);bright.addView(sun,new LinearLayout.LayoutParams(dp(36),dp(48)));SeekBar brightness=new SeekBar(this);brightness.setMax(100);brightness.setProgress(65);brightness.setProgressTintList(android.content.res.ColorStateList.valueOf(0xfffa4c3c));brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean u){if(u){WindowManager.LayoutParams a=getWindow().getAttributes();a.screenBrightness=Math.max(.05f,v/100f);getWindow().setAttributes(a);}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});bright.addView(brightness,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(bright);
        LinearLayout sizeRow=new LinearLayout(this);sizeRow.setGravity(Gravity.CENTER_VERTICAL);TextView sizeName=text("字号",15,0xff4d4945);sizeRow.addView(sizeName,new LinearLayout.LayoutParams(0,dp(58),1));TextView minus=settingButton("A−");minus.setOnClickListener(v->changeFont(-1));sizeRow.addView(minus,new LinearLayout.LayoutParams(dp(70),dp(40)));fontValue=text(String.valueOf(fontSize),14,0xff817b75);fontValue.setGravity(Gravity.CENTER);sizeRow.addView(fontValue,new LinearLayout.LayoutParams(dp(48),dp(40)));TextView plus=settingButton("A＋");plus.setOnClickListener(v->changeFont(1));sizeRow.addView(plus,new LinearLayout.LayoutParams(dp(70),dp(40)));box.addView(sizeRow);
        LinearLayout lineRow=new LinearLayout(this);lineRow.setGravity(Gravity.CENTER_VERTICAL);TextView lineName=text("行距",15,0xff4d4945);lineRow.addView(lineName,new LinearLayout.LayoutParams(0,dp(58),1));TextView lineMinus=settingButton("−");lineMinus.setOnClickListener(v->changeLineSpacing(-2));lineRow.addView(lineMinus,new LinearLayout.LayoutParams(dp(70),dp(40)));lineValue=text(lineSpacingName(),14,0xff817b75);lineValue.setGravity(Gravity.CENTER);lineRow.addView(lineValue,new LinearLayout.LayoutParams(dp(58),dp(40)));TextView linePlus=settingButton("＋");linePlus.setOnClickListener(v->changeLineSpacing(2));lineRow.addView(linePlus,new LinearLayout.LayoutParams(dp(70),dp(40)));box.addView(lineRow);
        TextView bgTitle=text("阅读背景",15,0xff4d4945);box.addView(bgTitle,new LinearLayout.LayoutParams(-1,dp(38)));LinearLayout themes=new LinearLayout(this);themes.addView(theme("米黄",0xfff5eedf,0xff2f2c28));themes.addView(theme("护眼",0xffdfe9d3,0xff283126));themes.addView(theme("白色",0xffffffff,0xff252525));themes.addView(theme("夜间",0xff25272a,0xffc3c4c5));box.addView(themes,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView modeTitle=text("翻页方式",15,0xff4d4945);box.addView(modeTitle,new LinearLayout.LayoutParams(-1,dp(42)));LinearLayout modes=new LinearLayout(this);modes.addView(modeChip("平移",true));modes.addView(modeChip("覆盖",false));modes.addView(modeChip("上下",false));box.addView(modes,new LinearLayout.LayoutParams(-1,dp(43)));
        d.setContentView(box);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-2);w.setGravity(Gravity.BOTTOM);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams a=w.getAttributes();a.width=WindowManager.LayoutParams.MATCH_PARENT;a.dimAmount=.28f;w.setAttributes(a);}d.setOnShowListener(x->{Window win=d.getWindow();if(win!=null)win.setLayout(-1,-2);});d.show();
    }
    private TextView settingButton(String s){TextView t=text(s,15,0xff3f3b37);t.setGravity(Gravity.CENTER);t.setBackground(round(0xfff1f0ed,20));return t;}
    private TextView theme(String s,int paper,int ink){TextView t=text(s,13,ink);t.setGravity(Gravity.CENTER);t.setBackground(round(paper,12));t.setOnClickListener(v->applyTheme(paper,ink));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(0,0,dp(9),0);t.setLayoutParams(p);return t;}
    private TextView modeChip(String s,boolean on){TextView t=text(s,13,on?0xfffa4c3c:0xff66615c);t.setGravity(Gravity.CENTER);t.setBackground(round(on?0xffffece9:0xfff2f1ef,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(39),1);p.setMargins(0,0,dp(10),0);t.setLayoutParams(p);return t;}
    private void changeFont(int d){int keep=currentOffset();fontSize=Math.max(15,Math.min(30,fontSize+d));pageText.setTextSize(fontSize);if(fontValue!=null)fontValue.setText(String.valueOf(fontSize));getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("font",fontSize).apply();pendingOffset=keep;paginate();}
    private void changeLineSpacing(int delta){int keep=currentOffset();lineSpacing=Math.max(2,Math.min(18,lineSpacing+delta));pageText.setLineSpacing(dp(lineSpacing),1.05f);if(lineValue!=null)lineValue.setText(lineSpacingName());getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("line_spacing",lineSpacing).apply();pendingOffset=keep;paginate();}
    private String lineSpacingName(){if(lineSpacing<=4)return"紧凑";if(lineSpacing<=8)return"适中";if(lineSpacing<=12)return"宽松";return"超宽";}
    private void applyTheme(int paper,int ink){paperColor=paper;textColor=ink;root.setBackgroundColor(paper);toolbar.setBackgroundColor(paper);bottomPanel.setBackgroundColor(paper);pageText.setTextColor(ink);pageText.setBackgroundColor(paper);chapterLabel.setTextColor((ink&0x00ffffff)|0x99000000);status.setTextColor((ink&0x00ffffff)|0x99000000);getWindow().setStatusBarColor(paper);getWindow().setNavigationBarColor(paper);getWindow().getDecorView().setSystemUiVisibility(paper==0xff25272a?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("paper",paper).putInt("ink",ink).apply();}

    private void showContents(){if("pdf".equals(type)){String[] pages=new String[count()];for(int i=0;i<count();i++)pages[i]="第 "+(i+1)+" 页";new AlertDialog.Builder(this).setTitle("选择页码").setItems(pages,(d,w)->{page=w;showPdf();}).show();return;}if(book==null)return;String[] names=book.chapters.stream().map(c->c.title).toArray(String[]::new);new AlertDialog.Builder(this).setTitle("目录").setItems(names,(d,w)->{page=findPageForOffset(book.chapters.get(w).offset);showPage();}).show();}
    private JSONArray marks(){try{return new JSONArray(LibraryStore.bookmarks(this,uri));}catch(Exception e){return new JSONArray();}}
    private boolean isMarked(){JSONArray a=marks();for(int i=0;i<a.length();i++)if(a.optInt(i)==page)return true;return false;}
    private void showBookmarks(){JSONArray a=marks();List<Integer> saved=new ArrayList<>();for(int i=0;i<a.length();i++)saved.add(a.optInt(i));Collections.sort(saved);String[] items=new String[saved.size()+1];items[0]=isMarked()?"★ 取消当前页书签":"☆ 为当前页添加书签";for(int i=0;i<saved.size();i++)items[i+1]="第 "+(saved.get(i)+1)+" 页";new AlertDialog.Builder(this).setTitle("书签").setItems(items,(d,w)->{if(w==0){toggleMark();return;}page=saved.get(w-1);if("pdf".equals(type))showPdf();else showPage();}).setNegativeButton("关闭",null).show();}
    private void toggleMark(){JSONArray a=marks(),n=new JSONArray();boolean removed=false;for(int i=0;i<a.length();i++){int x=a.optInt(i);if(x==page)removed=true;else n.put(x);}if(!removed)n.put(page);LibraryStore.bookmarks(this,uri,n.toString());update();Toast.makeText(this,removed?"已取消书签":"已添加书签",Toast.LENGTH_SHORT).show();}
    private Book currentBook(){return new Book(uri,title,type);}
    private Note currentNote(){return LibraryStore.note(this,currentBook(),page);}
    private boolean hasCurrentNote(){return currentNote()!=null;}
    private String currentQuote(){if("pdf".equals(type)||pageText==null)return"PDF 第 "+(page+1)+" 页";String q=pageText.getText().toString().replaceAll("\\s+"," ").trim();return q.length()>160?q.substring(0,160)+"…":q;}
    private void editNote(){
        Note old=currentNote();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(4),dp(18),0);
        TextView meta=text(("pdf".equals(type)?"PDF":chapterLabel.getText())+" · 第 "+(page+1)+" 页",12,0xff8b8179);box.addView(meta,new LinearLayout.LayoutParams(-1,dp(30)));
        String quote=currentQuote();TextView excerpt=text("“"+quote+"”",13,0xff77736e);excerpt.setMaxLines(3);excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END);excerpt.setPadding(dp(12),dp(8),dp(12),dp(8));excerpt.setBackground(round(0xfff4f2ef,10));box.addView(excerpt,new LinearLayout.LayoutParams(-1,dp(70)));
        EditText input=new EditText(this);input.setMinLines(4);input.setMaxLines(10);input.setHint("记录这一页带给你的想法…");input.setText(old==null?"":old.content);input.setSelection(input.length());input.setPadding(dp(12),dp(10),dp(12),dp(10));box.addView(input,new LinearLayout.LayoutParams(-1,dp(150)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(old==null?"新增笔记":"编辑笔记").setView(box).setNegativeButton("取消",null).setNeutralButton(old==null?"查看全部":"删除",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=input.getText().toString().trim();if(value.isEmpty()){input.setError("笔记内容不能为空");return;}LibraryStore.saveNote(this,new Note(uri,title,type,page,"pdf".equals(type)?-1:currentOffset(),chapterLabel.getText().toString(),quote,value,System.currentTimeMillis()));dialog.dismiss();update();Toast.makeText(this,"笔记已保存",Toast.LENGTH_SHORT).show();});dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{if(old==null){startActivity(new Intent(this,NotesActivity.class));dialog.dismiss();}else new AlertDialog.Builder(this).setTitle("删除这条笔记？").setMessage("删除后无法恢复。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{LibraryStore.deleteNote(this,uri,page);dialog.dismiss();update();}).show();});});dialog.show();
    }
    private void error(Exception e){new AlertDialog.Builder(this).setTitle("无法打开文档").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("返回",(d,w)->finish()).show();}
    private String stripExt(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onPause(){super.onPause();LibraryStore.progress(this,uri,page);if(!"pdf".equals(type)&&book!=null)LibraryStore.offset(this,uri,currentOffset());}
    @Override protected void onDestroy(){super.onDestroy();if(renderer!=null)renderer.close();try{if(pdfFd!=null)pdfFd.close();}catch(Exception ignored){}}
}
