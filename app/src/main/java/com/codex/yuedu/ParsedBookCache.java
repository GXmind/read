package com.codex.yuedu;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class ParsedBookCache {
    private static final int MAGIC = 0x59444231;
    private static final int MAX_TEXT_BYTES = 200 * 1024 * 1024;
    private ParsedBookCache() { }

    static String sourceIdentity(ContentResolver resolver, Uri uri, String type) {
        long size = -1, modified = -1; String name = "";
        String[] projection = { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED };
        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                int modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn);
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn);
                if (modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)) modified = cursor.getLong(modifiedColumn);
            }
        } catch (Exception ignored) { }
        return type + '|' + uri + '|' + name + '|' + size + '|' + modified;
    }

    static DocumentParser.ParsedBook load(Context context, String identity) {
        File file = file(context, identity); if (!file.isFile()) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC || !identity.equals(readString(in, 1024 * 1024))) return null;
            String text = readString(in, MAX_TEXT_BYTES);
            int count = in.readInt(); if (count < 1 || count > 100_000) return null;
            List<DocumentParser.Chapter> chapters = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String title = readString(in, 1024 * 1024); int offset = in.readInt();
                if (offset < 0 || offset > text.length()) return null;
                chapters.add(new DocumentParser.Chapter(title, offset));
            }
            return new DocumentParser.ParsedBook(text, chapters);
        } catch (Exception ignored) { return null; }
    }

    static void save(Context context, String identity, DocumentParser.ParsedBook book) {
        File file = file(context, identity), temp = new File(file.getParentFile(), file.getName() + ".tmp");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) return;
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))) {
            out.writeInt(MAGIC); writeString(out, identity); writeString(out, book.text);
            out.writeInt(book.chapters.size());
            for (DocumentParser.Chapter chapter : book.chapters) { writeString(out, chapter.title); out.writeInt(chapter.offset); }
        } catch (Exception ignored) { temp.delete(); return; }
        if (file.exists() && !file.delete()) { temp.delete(); return; }
        if (!temp.renameTo(file)) temp.delete();
    }

    private static File file(Context context, String identity) {
        return new File(new File(context.getFilesDir(), "document-cache"), sha256(identity) + ".bin");
    }
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255)); return out.toString();
        } catch (Exception e) { return Integer.toHexString(value.hashCode()); }
    }
    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); if (bytes.length > MAX_TEXT_BYTES) throw new IOException("Text too large"); out.writeInt(bytes.length); out.write(bytes);
    }
    private static String readString(DataInputStream in, int max) throws IOException {
        int size = in.readInt(); if (size < 0 || size > max) throw new IOException("Invalid cache data"); byte[] bytes = new byte[size]; in.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8);
    }
}
