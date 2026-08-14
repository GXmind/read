package com.codex.yuedu;

import org.junit.Test;
import java.net.URL;
import static org.junit.Assert.*;

public class UpdatePolicyTest {
    @Test public void acceptsOnlyExpectedHttpsHosts() throws Exception {
        assertTrue(UpdatePolicy.isTrustedUrl(new URL("https://api.github.com/repos/GXmind/read/releases/latest")));
        assertTrue(UpdatePolicy.isTrustedUrl(new URL("https://release-assets.githubusercontent.com/file.apk")));
        assertFalse(UpdatePolicy.isTrustedUrl(new URL("http://api.github.com/repos/GXmind/read")));
        assertFalse(UpdatePolicy.isTrustedUrl(new URL("https://evil.example/file.apk")));
        assertFalse(UpdatePolicy.isTrustedUrl(new URL("https://github.com.evil.example/file.apk")));
    }

    @Test public void rejectsCredentialAndPortInjection() throws Exception {
        assertFalse(UpdatePolicy.isTrustedUrl(new URL("https://attacker@github.com/file.apk")));
        assertFalse(UpdatePolicy.isTrustedUrl(new URL("https://github.com:444/file.apk")));
    }

    @Test public void appliesBoundedDownloadPolicy() {
        assertEquals(300L*1024L*1024L,UpdatePolicy.MAX_APK_BYTES);
    }

    @Test public void createsContentSpecificInstallRevision() {
        String first=UpdatePolicy.installRevision("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",49855);
        String second=UpdatePolicy.installRevision("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",49855);
        assertEquals("aaaaaaaaaaaaaaaaaaaa-49855",first);
        assertNotEquals(first,second);
        assertEquals("yuedu-update-aaaaaaaaaaaaaaaaaaaa-49855.apk",UpdatePolicy.installFileName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",49855));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidInstallRevisionDigest() {
        UpdatePolicy.installRevision("not-a-digest",100);
    }
}
