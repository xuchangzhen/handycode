import com.fairycrane.handycode.UrlPolicy;

public final class UrlPolicyTest {
    private static int checked;
    private static void expect(boolean expected, String url) {
        checked++;
        if (UrlPolicy.isAllowed(url) != expected)
            throw new AssertionError("Unexpected URL policy result: " + url);
    }
    public static void main(String[] args) {
        for (String url : new String[]{UrlPolicy.TAOBAO_ENTRY, UrlPolicy.TAOBAO_PARCELS_ENTRY,
                "https://login.m.taobao.com/login.htm", "https://login.taobao.com:443/",
                "HTTPS://LOGIN.TAOBAO.COM/", "https://memberprod.alipay.com/",
                "https://www.cainiao.com/", "https://login.alibaba.com/", "https://login.tmall.com/"})
            expect(true, url);
        for (String url : new String[]{null, "", "http://login.taobao.com/", "javascript:alert(1)",
                "file:///etc/passwd", "content://taobao.com/", "intent://taobao.com/",
                "taobao://m.taobao.com/", "https://taobao.com.evil.example/", "https://eviltaobao.com/",
                "https://taobao.com@evil.example/", "https://evil.example@taobao.com/",
                "https://login.taobao.com:444/", "https://taobao.com\\@evil.example/",
                "https://taobao.com./", "https://taobao.com%2eevil.example/",
                "https://taobao.com/\n", " https://taobao.com/", "https:///taobao.com/",
                "https://login.taobao.com:0/"})
            expect(false, url);
        // Pinduoduo is an external app shortcut, never an in-app WebView host.
        for (String url : new String[]{"https://pinduoduo.com/", "https://passport.pinduoduo.com/",
                "https://mobile.yangkeduo.com/", "https://m.pinduoduo.net/mdkd/package?tab=ID_CODE",
                "pinduoduo://com.xunmeng.pinduoduo/mdkd/package?tab=ID_CODE"})
            expect(false, url);
        String addPhone = UrlPolicy.TAOBAO_ENTRY.replace("identity-code", "add-friend-phone-ssr");
        if (!UrlPolicy.isTaobaoAddPhonePage(addPhone + "?skeleton=true")
                || UrlPolicy.isTaobaoAddPhonePage(UrlPolicy.TAOBAO_ENTRY)
                || UrlPolicy.isTaobaoAddPhonePage(addPhone.replace("pages-fast.m.taobao.com", "login.taobao.com")))
            throw new AssertionError("The nested phone window must start at the official add-phone form.");
        String h5 = "https://page.cainiao.com/guoguo/app-myexpress-taobao/";
        if (!UrlPolicy.isCainiaoH5Entry(h5) || !UrlPolicy.isCainiaoH5Entry(h5 + "?test=1")
                || UrlPolicy.isCainiaoH5Entry(h5 + "extra") || UrlPolicy.isCainiaoH5Entry(h5.replace("https:", "http:"))
                || UrlPolicy.isCainiaoH5Entry(h5.replace("page.cainiao.com", "page.cainiao.com.evil.example")))
            throw new AssertionError("Cainiao H5 entry scope mismatch");
        for (String url : new String[]{h5, "https://cn.alicdn.com/fixture.js", "https://api.m.taobao.com/fixture", "https://login.alipay.com/"})
            if (!UrlPolicy.isCainiaoH5Resource(url)) throw new AssertionError("Official H5 resource rejected");
        for (String url : new String[]{null, "http://page.cainiao.com/", "https://page.cainiao.com.evil.example/",
                "https://evil.example/", "https://x@cainiao.com/", "https://cainiao.com:444/", "file:///tmp/file"})
            if (UrlPolicy.isCainiaoH5Resource(url)) throw new AssertionError("Foreign H5 resource accepted");
        System.out.println("PASS: " + checked + " Taobao navigation allowlist cases.");
    }
}
