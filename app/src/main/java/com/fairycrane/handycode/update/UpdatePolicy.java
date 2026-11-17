package com.fairycrane.handycode.update;

import java.net.URI;
import java.util.Locale;

/** Strict allowlist and version rules for the user's public GitHub releases. */
public final class UpdatePolicy {
    public static final String API = "https://api.github.com/repos/xuchangzhen/handycode/releases/latest";
    private UpdatePolicy() {}
    public static String version(String tag) {
        if (tag == null || !tag.matches("v(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,3})\\.(0|[1-9][0-9]{0,3})")) return null;
        return tag.substring(1);
    }
    public static boolean newer(String candidate, String installed) {
        String a = version("v" + candidate), b = version("v" + installed);
        if (a == null || b == null) return false;
        String[] aa = a.split("\\."), bb = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int diff = Integer.parseInt(aa[i]) - Integer.parseInt(bb[i]);
            if (diff != 0) return diff > 0;
        }
        return false;
    }
    private static URI https(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getRawFragment() != null) return null;
            return uri;
        } catch (Exception e) { return null; }
    }
    public static boolean assetUrl(String value, String tag, String file) {
        String version = version(tag);
        if (version == null || !file.equals("HandyCode-" + version + "-release.apk")) return false;
        URI u = https(value);
        return u != null && "github.com".equalsIgnoreCase(u.getHost()) && u.getRawQuery() == null
            && ("/xuchangzhen/handycode/releases/download/" + tag + "/" + file).equals(u.getRawPath());
    }
    public static boolean redirectUrl(String value) {
        URI u = https(value);
        if (u == null) return false;
        String host = u.getHost().toLowerCase(Locale.ROOT);
        return host.equals("release-assets.githubusercontent.com") || host.equals("github-releases.githubusercontent.com")
            || host.equals("objects.githubusercontent.com");
    }
    public static boolean digest(String value) { return value != null && value.matches("sha256:[a-fA-F0-9]{64}"); }
}
