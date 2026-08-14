package com.codex.yuedu;

import org.junit.Test;
import static org.junit.Assert.*;

public class BookmarkTest {
    @Test public void bookmarkPreservesReadingPosition() {
        Book book = new Book("content://books/demo", "示例小说.txt", "txt");
        Bookmark bookmark = new Bookmark(book.uri, book.title, book.type, 12, 4096,
                "第十三章", "这一页的摘要", 123456L);
        assertEquals(book.uri, bookmark.bookUri);
        assertEquals(book.title, bookmark.bookTitle);
        assertEquals(12, bookmark.page);
        assertEquals(4096, bookmark.offset);
        assertEquals("第十三章", bookmark.chapter);
        assertEquals("这一页的摘要", bookmark.excerpt);
        assertEquals(123456L, bookmark.createdAt);
    }
}
