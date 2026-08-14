package com.codex.yuedu;

import java.util.Locale;

final class VersionComparator {
    private VersionComparator() {}

    static boolean isRemoteNewer(String remoteTag, String currentVersion) {
        return compare(remoteTag, currentVersion) > 0;
    }

    static int compare(String left, String right) {
        ParsedVersion a = ParsedVersion.parse(left);
        ParsedVersion b = ParsedVersion.parse(right);
        int length = Math.max(a.numbers.length, b.numbers.length);
        for (int i = 0; i < length; i++) {
            long av = i < a.numbers.length ? a.numbers[i] : 0;
            long bv = i < b.numbers.length ? b.numbers[i] : 0;
            if (av != bv) return Long.compare(av, bv);
        }
        if (a.suffix.isEmpty() && !b.suffix.isEmpty()) return 1;
        if (!a.suffix.isEmpty() && b.suffix.isEmpty()) return -1;
        return a.suffix.compareTo(b.suffix);
    }

    private static final class ParsedVersion {
        final long[] numbers;
        final String suffix;

        private ParsedVersion(long[] numbers, String suffix) {
            this.numbers = numbers;
            this.suffix = suffix;
        }

        static ParsedVersion parse(String raw) {
            String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("v")) value = value.substring(1);
            int separator = value.indexOf('-');
            String core = separator >= 0 ? value.substring(0, separator) : value;
            String suffix = separator >= 0 ? value.substring(separator + 1) : "";
            String[] parts = core.split("\\.");
            long[] numbers = new long[Math.max(parts.length, 1)];
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) continue;
                try {
                    numbers[i] = Long.parseLong(parts[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid version: " + raw, e);
                }
            }
            return new ParsedVersion(numbers, suffix);
        }
    }
}
