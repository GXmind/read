package com.codex.yuedu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.security.MessageDigest;

public final class SecurityGuard {
    private static final byte MASK=0x5a;
    private static final byte[] OFFICIAL_XOR=new byte[]{92,72,18,-122,14,-66,-81,-103,82,112,32,-86,2,87,33,-93,-58,102,-107,95,-15,24,-117,-45,105,-88,8,-92,127,-9,-60,-40};
    private static final byte[] LEGACY_XOR=new byte[]{18,40,69,-45,-26,11,80,23,-87,91,52,11,118,106,73,74,61,22,-43,15,-56,-42,-42,71,-121,-109,-19,75,28,-127,84,54};
    private SecurityGuard(){}
    public static boolean verifySelf(Context c){
        try{ApplicationInfo app=c.getApplicationInfo();if((app.flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0)return true;PackageManager pm=c.getPackageManager();PackageInfo p=Build.VERSION.SDK_INT>=28?pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES):pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNATURES);Signature[] sigs=Build.VERSION.SDK_INT>=28?p.signingInfo.getApkContentsSigners():p.signatures;if(sigs==null||sigs.length!=1)return false;byte[] actual=MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray()),encoded=Build.VERSION.SDK_INT>=28?OFFICIAL_XOR:LEGACY_XOR,expected=new byte[encoded.length];for(int i=0;i<expected.length;i++)expected[i]=(byte)(encoded[i]^MASK);return MessageDigest.isEqual(actual,expected);}catch(Exception e){return false;}
    }
}
