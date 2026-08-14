package com.codex.yuedu;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.Html;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DocumentParser {
    public static class Chapter {
        public final String title;
        public final int offset;
        Chapter(String title, int offset) { this.title=title; this.offset=offset; }
    }
    public static class ParsedBook {
        public final String text;
        public final List<Chapter> chapters;
        ParsedBook(String text, List<Chapter> chapters) { this.text=text; this.chapters=chapters; }
    }

    public static ParsedBook parse(ContentResolver resolver, Uri uri, String type) throws Exception {
        if ("epub".equals(type)) return epub(readAll(resolver.openInputStream(uri)));
        if ("docx".equals(type)) return docx(readAll(resolver.openInputStream(uri)));
        byte[] bytes = readAll(resolver.openInputStream(uri));
        String text = decodeText(bytes);
        return new ParsedBook(clean(text), chapters(clean(text)));
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null) throw new FileNotFoundException();
        try (InputStream src=in; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[16384]; int n;
            while ((n=src.read(buf))!=-1) out.write(buf,0,n);
            return out.toByteArray();
        }
    }

    private static String decodeText(byte[] b) {
        if (b.length>=3 && (b[0]&255)==239 && (b[1]&255)==187 && (b[2]&255)==191)
            return new String(b,3,b.length-3,StandardCharsets.UTF_8);
        String utf = new String(b, StandardCharsets.UTF_8);
        long bad = utf.chars().filter(c -> c==0xfffd).count();
        if (bad > Math.max(2, utf.length()/200)) {
            try { return new String(b, Charset.forName("GB18030")); } catch (Exception ignored) { }
        }
        return utf;
    }

    private static ParsedBook epub(byte[] zip) throws Exception {
        Map<String,byte[]> files=unzip(zip);
        String container=text(files.get("META-INF/container.xml"));
        String opfPath=match(container,"full-path\\s*=\\s*[\"']([^\"']+)");
        if (opfPath==null) throw new IOException("EPUB 缺少内容索引");
        String opf=text(files.get(opfPath));
        String base=opfPath.contains("/") ? opfPath.substring(0,opfPath.lastIndexOf('/')+1) : "";
        Map<String,String> manifest=new HashMap<>();
        Matcher item=Pattern.compile("<item\\b[^>]*id=[\"']([^\"']+)[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*/?>",Pattern.CASE_INSENSITIVE).matcher(opf);
        while(item.find()) manifest.put(item.group(1), normalize(base+item.group(2)));
        StringBuilder all=new StringBuilder(); List<Chapter> cs=new ArrayList<>();
        Matcher spine=Pattern.compile("<itemref\\b[^>]*idref=[\"']([^\"']+)[\"'][^>]*/?>",Pattern.CASE_INSENSITIVE).matcher(opf);
        while(spine.find()) {
            byte[] raw=files.get(manifest.get(spine.group(1))); if(raw==null) continue;
            String html=text(raw);
            String title=firstHeading(html);
            String plain=Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString().trim();
            if(plain.isEmpty()) continue;
            cs.add(new Chapter(title==null?"第 "+(cs.size()+1)+" 章":title,all.length()));
            all.append(plain).append("\n\n");
        }
        String result=all.toString().trim();
        if(cs.isEmpty()) cs=chapters(result);
        return new ParsedBook(result,cs);
    }

    private static ParsedBook docx(byte[] zip) throws Exception {
        Map<String,byte[]> files=unzip(zip); byte[] raw=files.get("word/document.xml");
        if(raw==null) throw new IOException("无效的 Word 文档");
        XmlPullParser x=Xml.newPullParser(); x.setInput(new ByteArrayInputStream(raw),"UTF-8");
        StringBuilder text=new StringBuilder(); int event;
        while((event=x.next())!=XmlPullParser.END_DOCUMENT) {
            if(event==XmlPullParser.START_TAG && "t".equals(x.getName())) text.append(x.nextText());
            else if(event==XmlPullParser.END_TAG && "p".equals(x.getName())) text.append('\n');
            else if(event==XmlPullParser.START_TAG && ("tab".equals(x.getName())||"br".equals(x.getName()))) text.append(' ');
        }
        String result=clean(text.toString());
        return new ParsedBook(result,chapters(result));
    }

    private static Map<String,byte[]> unzip(byte[] bytes) throws IOException {
        Map<String,byte[]> out=new HashMap<>();
        try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e; byte[] buf=new byte[8192];
            while((e=z.getNextEntry())!=null) {
                if(e.isDirectory()) continue;
                ByteArrayOutputStream b=new ByteArrayOutputStream(); int n;
                while((n=z.read(buf))!=-1) b.write(buf,0,n);
                out.put(normalize(e.getName()),b.toByteArray());
            }
        }
        return out;
    }

    private static List<Chapter> chapters(String text) {
        List<Chapter> out=new ArrayList<>();
        Pattern p=Pattern.compile("(?m)^[ \\t　]*(第[0-9零一二三四五六七八九十百千万两]+[章节卷回部篇].{0,40}|Chapter\\s+\\d+.{0,40}|序章|前言|楔子|尾声|后记)[ \\t　]*$");
        Matcher m=p.matcher(text); while(m.find()) out.add(new Chapter(m.group(1).trim(),m.start()));
        if(out.isEmpty()) out.add(new Chapter("全文",0));
        return out;
    }

    private static String clean(String s) { return s.replace("\r\n","\n").replace('\r','\n').replaceAll("\\n{4,}","\n\n\n").trim(); }
    private static String text(byte[] b) { return b==null?"":new String(b,StandardCharsets.UTF_8); }
    private static String normalize(String p) {
        Deque<String> q=new ArrayDeque<>();
        for(String s:p.replace('\\','/').split("/")) { if("..".equals(s)){if(!q.isEmpty())q.removeLast();} else if(!s.isEmpty()&&!".".equals(s))q.addLast(s); }
        return String.join("/",q);
    }
    private static String match(String s,String regex){Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(s);return m.find()?m.group(1):null;}
    private static String firstHeading(String html){String h=match(html,"<h[1-3][^>]*>(.*?)</h[1-3]>");return h==null?null:Html.fromHtml(h,Html.FROM_HTML_MODE_LEGACY).toString().trim();}
}
