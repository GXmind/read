package com.codex.yuedu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public final class UpdateFileProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File resolve(Uri uri) throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(getContext()==null||name==null||!name.matches("yuedu-update-[0-9a-f]{20}-[0-9]+\\.apk"))throw new FileNotFoundException("Invalid update path");
        File base=new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"updates");File target=new File(base,name);
        try{if(!target.getCanonicalPath().startsWith(base.getCanonicalPath()+File.separator))throw new FileNotFoundException("Path rejected");}catch(java.io.IOException e){throw new FileNotFoundException("Path rejected");}return target;
    }
    @Override public String getType(Uri uri){return "application/vnd.android.package-archive";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{File f=resolve(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);MatrixCursor.RowBuilder row=c.newRow();for(String col:cols){if(OpenableColumns.DISPLAY_NAME.equals(col))row.add("悦读安全更新.apk");else if(OpenableColumns.SIZE.equals(col))row.add(f.length());else row.add(null);}return c;}catch(Exception e){return null;}}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){return 0;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){return 0;}
}
