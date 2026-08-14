package com.codex.yuedu;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class UpdateActivity extends Activity {
    private LinearLayout content;private TextView status,detail;private Button action;private AssetInfo latest;private File verifiedApk;private boolean waitingPermission;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.WHITE);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);build();}
    private GradientDrawable round(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private void build(){
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(14),dp(24),dp(28));content.setBackgroundColor(Color.WHITE);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView back=text("‹",31,0xff333335);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(44),dp(54)));TextView title=text("安全更新",23,0xff252527);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));content.addView(header);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(20),dp(20),dp(20));card.setBackground(round(0xfffff5f2,18));TextView logo=text("悦读",27,0xfffa4c3c);logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(logo,new LinearLayout.LayoutParams(-1,dp(44)));TextView current=text("当前版本  v"+currentVersionName(),14,0xff756e6a);card.addView(current,new LinearLayout.LayoutParams(-1,dp(30)));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(112));cp.setMargins(0,dp(18),0,dp(24));content.addView(card,cp);
        status=text("尚未检查更新",19,0xff2d2d30);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(status,new LinearLayout.LayoutParams(-1,dp(42)));detail=text("更新仅从 GXmind/read 的 GitHub Releases 获取。下载后将验证包名、版本、SHA-256 和 APK 签名证书。",14,0xff77777c);detail.setGravity(Gravity.TOP);detail.setLineSpacing(dp(4),1f);content.addView(detail,new LinearLayout.LayoutParams(-1,0,1));
        action=new Button(this);action.setText("检查更新");action.setTextSize(16);action.setTextColor(Color.WHITE);action.setAllCaps(false);action.setTypeface(Typeface.DEFAULT,Typeface.BOLD);action.setBackground(round(0xfffa4c3c,24));action.setOnClickListener(v->checkUpdate());LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(52));ap.setMargins(0,dp(16),0,dp(12));content.addView(action,ap);
        TextView trust=text("安全说明：应用不会静默安装；Android 系统安装器仍会再次校验应用签名并要求你的确认。",12,0xff99999e);trust.setGravity(Gravity.TOP);content.addView(trust,new LinearLayout.LayoutParams(-1,dp(58)));setContentView(content);
    }
    private void setBusy(String message){status.setText(message);action.setEnabled(false);action.setAlpha(.55f);}
    private void setAction(String title,View.OnClickListener click){action.setText(title);action.setEnabled(true);action.setAlpha(1f);action.setOnClickListener(click);}
    private void checkUpdate(){setBusy("正在连接 GitHub…");detail.setText("正在通过 HTTPS 获取最新 Release 信息。");new Thread(()->{try{JSONObject release=new JSONObject(readUtf8(openFollowingRedirects(UpdatePolicy.RELEASE_API),2*1024*1024));JSONArray assets=release.optJSONArray("assets");if(assets==null)throw new IOException("最新 Release 没有附件");JSONObject apk=null;for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(a.optString("name").toLowerCase(Locale.ROOT).endsWith(".apk")){apk=a;break;}}if(apk==null)throw new IOException("最新 Release 中没有 APK 文件");AssetInfo info=new AssetInfo(release.optString("tag_name","未知版本"),release.optString("name","悦读更新"),release.optString("body","暂无更新说明"),apk.getString("browser_download_url"),apk.optLong("size",-1),apk.optString("digest",""));runOnUiThread(()->showRelease(info));}catch(Exception e){runOnUiThread(()->showError("检查失败",friendly(e)));}}).start();}
    private void showRelease(AssetInfo info){latest=info;status.setText("发现版本  "+info.tag);detail.setText(info.name+"\n\n"+info.notes);setAction("安全下载并验证",v->downloadLatest());}
    private void downloadLatest(){if(latest==null)return;setBusy("正在下载更新…");detail.setText("下载完成前不会调用安装程序。");new Thread(()->{try{File base=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"updates");if(!base.exists()&&!base.mkdirs())throw new IOException("无法创建更新目录");File temp=new File(base,"yuedu-update.tmp"),target=new File(base,"yuedu-update.apk");MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=download(openFollowingRedirects(latest.url),temp,digest,latest.size);String sha=hex(digest.digest());if(latest.size>0&&total!=latest.size)throw new SecurityException("下载大小与 GitHub Release 不一致");if(!latest.digest.isEmpty()&&!latest.digest.equalsIgnoreCase("sha256:"+sha))throw new SecurityException("SHA-256 与 GitHub 摘要不一致");verifyApk(temp);if(target.exists()&&!target.delete())throw new IOException("无法替换旧更新包");if(!temp.renameTo(target))throw new IOException("无法保存已验证更新包");verifiedApk=target;runOnUiThread(()->readyToInstall(sha));}catch(Exception e){runOnUiThread(()->showError("更新已拦截",friendly(e)));}}).start();}
    private long download(HttpsURLConnection c,File out,MessageDigest digest,long declared) throws Exception {if(declared>UpdatePolicy.MAX_APK_BYTES)throw new SecurityException("APK 超过允许的大小");long total=0;try(InputStream in=new BufferedInputStream(c.getInputStream());OutputStream file=new BufferedOutputStream(new FileOutputStream(out))){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){total+=n;if(total>UpdatePolicy.MAX_APK_BYTES)throw new SecurityException("APK 超过 300MB 安全限制");digest.update(buf,0,n);file.write(buf,0,n);}}finally{c.disconnect();}return total;}
    private void verifyApk(File apk) throws Exception {PackageManager pm=getPackageManager();PackageInfo archive=packageInfoArchive(pm,apk);PackageInfo installed=packageInfoInstalled(pm);if(archive==null)throw new SecurityException("下载文件不是有效 APK");if(!getPackageName().equals(archive.packageName))throw new SecurityException("APK 包名不匹配");if(versionCode(archive)<=versionCode(installed))throw new SecurityException("更新版本号没有高于当前版本");Signature[] incomingHistory=signingHistory(archive),current=signers(installed);if(incomingHistory.length==0||current.length==0)throw new SecurityException("APK 签名结构异常");boolean match=false;for(Signature old:current)for(Signature candidate:incomingHistory)if(MessageDigest.isEqual(old.toByteArray(),candidate.toByteArray()))match=true;if(!match)throw new SecurityException("APK 签名证书或轮换链与当前应用不一致");}
    private PackageInfo packageInfoArchive(PackageManager pm,File apk){if(Build.VERSION.SDK_INT>=28)return pm.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);return pm.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNATURES);}
    private PackageInfo packageInfoInstalled(PackageManager pm) throws PackageManager.NameNotFoundException {if(Build.VERSION.SDK_INT>=28)return pm.getPackageInfo(getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);return pm.getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);}
    private Signature[] signers(PackageInfo p){if(Build.VERSION.SDK_INT>=28&&p.signingInfo!=null)return p.signingInfo.getApkContentsSigners();return p.signatures==null?new Signature[0]:p.signatures;}
    private Signature[] signingHistory(PackageInfo p){if(Build.VERSION.SDK_INT>=28&&p.signingInfo!=null){Signature[] history=p.signingInfo.getSigningCertificateHistory();return history==null?p.signingInfo.getApkContentsSigners():history;}return p.signatures==null?new Signature[0]:p.signatures;}
    private long versionCode(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    private void readyToInstall(String sha){status.setText("安全校验通过");detail.setText("SHA-256\n"+sha+"\n\n包名、版本号和 APK 签名证书均已验证。");setAction("使用系统安装器更新",v->requestInstall());}
    private void requestInstall(){if(verifiedApk==null||!verifiedApk.isFile()){showError("更新文件不存在","请重新检查更新");return;}if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){waitingPermission=true;startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}launchInstaller();}
    @Override protected void onResume(){super.onResume();if(waitingPermission&&Build.VERSION.SDK_INT>=26&&getPackageManager().canRequestPackageInstalls()){waitingPermission=false;launchInstaller();}}
    private void launchInstaller(){Uri content=Uri.parse("content://"+getPackageName()+".updates/yuedu-update.apk");Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(content,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}
    private HttpsURLConnection openFollowingRedirects(String raw) throws Exception {URL url=new URL(raw);for(int redirect=0;redirect<6;redirect++){validateUrl(url);HttpsURLConnection c=(HttpsURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(30000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","YueDu-Android/"+currentVersionName());int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("GitHub 重定向缺少地址");url=new URL(url,location);continue;}if(code==404){c.disconnect();throw new IOException("仓库还没有发布 Release");}if(code!=200){c.disconnect();throw new IOException("GitHub 返回 HTTP "+code);}return c;}throw new SecurityException("重定向次数过多");}
    private void validateUrl(URL url){if(!UpdatePolicy.isTrustedUrl(url))throw new SecurityException("更新地址不在安全域名白名单");}
    private String readUtf8(HttpsURLConnection c,int max) throws Exception {try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>max)throw new SecurityException("响应内容过大");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8.name());}finally{c.disconnect();}}
    private String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
    private void showError(String title,String message){status.setText(title);detail.setText(message);setAction("重新检查",v->checkUpdate());}
    private String friendly(Exception e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m;}
    private String currentVersionName(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return"unknown";}}
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private static final class AssetInfo{final String tag,name,notes,url,digest;final long size;AssetInfo(String t,String n,String body,String u,long s,String d){tag=t;name=n;notes=body;url=u;size=s;digest=d;}}
}
