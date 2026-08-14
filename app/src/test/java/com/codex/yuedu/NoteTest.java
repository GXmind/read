package com.codex.yuedu;

import org.junit.Test;
import static org.junit.Assert.*;

public class NoteTest {
    @Test public void legacyTextNoteRemainsReadable() {
        Book book = new Book("content://book/1", "测试小说.txt", "txt");
        Note note = Note.fromJson(book.uri, book, 12, "旧版笔记内容");
        assertEquals("旧版笔记内容", note.content);
        assertEquals(12, note.page);
        assertEquals(-1, note.offset);
        assertEquals("测试小说.txt", note.bookTitle);
    }

    @Test public void noteKeepsNavigationMetadata() {
        Note note = new Note("content://book/2", "长篇.epub", "epub", 8, 4096,
                "第三章 夜航", "这是原文摘要", "人物关系发生了变化", 123456L);
        assertEquals(8, note.page);
        assertEquals(4096, note.offset);
        assertEquals("第三章 夜航", note.chapter);
        assertEquals("这是原文摘要", note.quote);
        assertEquals(123456L, note.updatedAt);
    }
}
