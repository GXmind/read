package com.codex.yuedu;

import org.junit.Test;

import static org.junit.Assert.*;

public class VersionComparatorTest {
    @Test public void sameVersionIsNotAnUpdate() {
        assertFalse(VersionComparator.isRemoteNewer("v1.4.0", "1.4.0"));
        assertEquals(0, VersionComparator.compare("1.4", "1.4.0"));
    }

    @Test public void onlyNewerVersionIsAnUpdate() {
        assertTrue(VersionComparator.isRemoteNewer("v1.4.1", "1.4.0"));
        assertTrue(VersionComparator.isRemoteNewer("v1.10.0", "1.9.9"));
        assertFalse(VersionComparator.isRemoteNewer("v1.3.9", "1.4.0"));
    }

    @Test public void stableReleaseIsNewerThanPrerelease() {
        assertTrue(VersionComparator.isRemoteNewer("v2.0.0", "2.0.0-beta"));
        assertFalse(VersionComparator.isRemoteNewer("v2.0.0-beta", "2.0.0"));
    }
}
