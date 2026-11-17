import com.fairycrane.handycode.logistics.PackageRecord;
import com.fairycrane.handycode.logistics.PackageCollection;
import static com.fairycrane.handycode.logistics.PackageRecord.Status;

public class PackageCollectionTest {
    static void check(boolean v) { if (!v) throw new AssertionError(); }
    static PackageRecord p(String platform, String order, String track, String code, String text, long t) {
        return new PackageRecord(platform, order, track, "虚构商品", text, code, PackageRecord.infer(text, code), t);
    }
    public static void main(String[] args) {
        check(PackageRecord.infer("驿站代签收", "") == Status.PICKUP);
        check(PackageRecord.infer("已签收", "") == Status.SIGNED);
        check(PackageRecord.infer("本人已取件", "123") == Status.PICKED_UP);
        check(PackageRecord.infer("运输中", "") == Status.TRANSIT);
        check(PackageRecord.infer("未知状态", "") == Status.UNKNOWN);
        check(PackageRecord.infer("已退款", "") == Status.CANCELLED);
        PackageCollection c = new PackageCollection();
        c.update(p("jd", "order1", "way1", "001", "运输中", 1));
        c.update(p("jd", "order1", "way2", "001", "派送中", 2));
        c.update(p("jd", "order2", "way1", "002", "待取件", 3));
        check(c.sorted().size() == 2 && c.sorted().get(0).status == Status.PICKUP);
        c.update(p("jd", "order3", "", "001", "待发货", 4));
        c.update(p("jd", "order4", "", "001", "待发货", 4));
        check(c.sorted().size() == 4);
        c.update(p("tb", "order2", "way1", "002", "待取件", 5));
        check(c.sorted().size() == 5); // No cross-platform merge without carrier evidence.
        c.clear(); check(c.sorted().isEmpty());
        System.out.println("Package status and conservative shipment deduplication passed");
    }
}
