package com.codex.yuedu;

import org.junit.Test;
import static org.junit.Assert.*;

public class PaginationCacheTest {
    @Test public void sameDocumentAndLayoutReuseTheSameKey() {
        String first = PaginationCache.layoutKey("doc", "正文内容", 900, 1600, 48, 16);
        String second = PaginationCache.layoutKey("doc", "正文内容", 900, 1600, 48, 16);
        assertEquals(first, second);
    }

    @Test public void appearanceAndScreenChangesInvalidatePagination() {
        String base = PaginationCache.layoutKey("doc", "正文内容", 900, 1600, 48, 16);
        assertNotEquals(base, PaginationCache.layoutKey("doc", "正文内容", 880, 1600, 48, 16));
        assertNotEquals(base, PaginationCache.layoutKey("doc", "正文内容", 900, 1600, 52, 16));
        assertNotEquals(base, PaginationCache.layoutKey("doc", "正文内容", 900, 1600, 48, 22));
        assertNotEquals(base, PaginationCache.layoutKey("doc", "正文内容已修改", 900, 1600, 48, 16));
    }
}
