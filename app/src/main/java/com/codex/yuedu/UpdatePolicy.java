package com.codex.yuedu;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class UpdatePolicy {
    static final String RELEASE_API="https://api.github.com/repos/GXmind/read/releases/latest";
    static final long MAX_APK_BYTES=300L*1024L*1024L;
    private static final Set<String> ALLOWED_HOSTS=new HashSet<>(Arrays.asList("api.github.com","github.com","objects.githubusercontent.com","release-assets.githubusercontent.com","github-releases.githubusercontent.com"));
    private UpdatePolicy(){}
    static boolean isTrustedUrl(URL url){return "https".equalsIgnoreCase(url.getProtocol())&&url.getUserInfo()==null&&url.getPort()==-1&&ALLOWED_HOSTS.contains(url.getHost().toLowerCase(Locale.ROOT));}
}
