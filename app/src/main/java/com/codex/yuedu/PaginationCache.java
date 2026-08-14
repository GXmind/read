package com.codex.yuedu;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class PaginationCache {
    private static final int MAGIC = 0x59445031;
    private static final int MAX_PAGES = 1_000_000;

    private PaginationCache() { }

    static String layoutKey(String sourceIdentity, String text, int width, int height,
                            int textSizePx, int lineSpacingPx) {
        return sourceIdentity + '|' + text.length() + '|' + text.hashCode() + '|'
                + width + 'x' + height + '|' + textSizePx + '|' + lineSpacingPx;
    }

    static List<Integer> load(Context context, String key, int sourceLength) {
        File file = file(context, key);
        if (!file.isFile()) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC || !key.equals(readString(in))) return null;
            int count = in.readInt();
            if (count < 1 || count > MAX_PAGES) return null;
            List<Integer> starts = new ArrayList<>(count);
            int previous = -1;
            for (int i = 0; i < count; i++) {
                int value = in.readInt();
                if (value < 0 || value > sourceLength || value <= previous) return null;
                starts.add(value);
                previous = value;
            }
            return starts;
        } catch (Exception ignored) {
            return null;
        }
    }

    static void save(Context context, String key, List<Integer> starts) {
        if (starts == null || starts.isEmpty() || starts.size() > MAX_PAGES) return;
        File file = file(context, key), temp = new File(file.getParentFile(), file.getName() + ".tmp");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) return;
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))) {
            out.writeInt(MAGIC);
            writeString(out, key);
            out.writeInt(starts.size());
            for (int value : starts) out.writeInt(value);
        } catch (Exception ignored) {
            temp.delete();
            return;
        }
        if (file.exists() && !file.delete()) { temp.delete(); return; }
        if (!temp.renameTo(file)) temp.delete();
    }

    private static File file(Context context, String key) {
        return new File(new File(context.getFilesDir(), "pagination-cache"), sha256(key) + ".bin");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (Exception e) { return Integer.toHexString(value.hashCode()); }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
    private static String readString(DataInputStream in) throws IOException {
        int size = in.readInt(); if (size < 0 || size > 1024 * 1024) throw new IOException("Invalid cache key");
        byte[] bytes = new byte[size]; in.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8);
    }
}
