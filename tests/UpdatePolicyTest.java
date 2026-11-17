import com.fairycrane.handycode.update.UpdatePolicy;
public class UpdatePolicyTest {
    static void check(boolean v) { if (!v) throw new AssertionError(); }
    public static void main(String[] args) {
        check(UpdatePolicy.newer("0.9.1", "0.9.0"));
        check(UpdatePolicy.newer("1.0.0", "0.99.99"));
        check(!UpdatePolicy.newer("0.9.0", "0.9.0"));
        check(!UpdatePolicy.newer("0.9.0-beta", "0.9.0"));
        String good="https://github.com/xuchangzhen/handycode/releases/download/v0.9.1/HandyCode-0.9.1-release.apk";
        check(UpdatePolicy.assetUrl(good,"v0.9.1","HandyCode-0.9.1-release.apk"));
        check(!UpdatePolicy.assetUrl(good.replace("github.com/","github.com.evil.invalid/"),"v0.9.1","HandyCode-0.9.1-release.apk"));
        check(!UpdatePolicy.assetUrl(good.replace("https:","http:"),"v0.9.1","HandyCode-0.9.1-release.apk"));
        check(!UpdatePolicy.assetUrl(good+"?x=1","v0.9.1","HandyCode-0.9.1-release.apk"));
        check(UpdatePolicy.redirectUrl("https://release-assets.githubusercontent.com/path?token=fake"));
        check(!UpdatePolicy.redirectUrl("https://release-assets.githubusercontent.com.evil.invalid/path"));
        check(!UpdatePolicy.redirectUrl("https://evil.invalid@release-assets.githubusercontent.com/path"));
        check(UpdatePolicy.digest("sha256:"+new String(new char[64]).replace('\0','a')));
        check(!UpdatePolicy.digest("sha256:wrong"));
        System.out.println("PASS: update versions, release asset origin, redirect and digest policy");
    }
}
