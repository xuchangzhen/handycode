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
        PackageRecord events = new PackageRecord("jd", "o", "t", "", "", "", Status.TRANSIT, 1, "虚构快递",
            java.util.Arrays.asList(new PackageRecord.Event("旧", 10, "旧节点"), new PackageRecord.Event("新", 20, "新节点"), new PackageRecord.Event("无效", 0, "无效")));
        check(events.timeline.size() == 2 && events.timeline.get(0).text.equals("新节点"));
        PackageCollection merge = new PackageCollection();
        merge.update(new PackageRecord("tb", "orderA", "WAY-X", "A", "", "P-1", Status.PICKUP, 1, "虚构物流", null));
        merge.update(new PackageRecord("jd", "orderB", "WAY-X", "B", "", "", Status.SIGNED, 2, "虚构物流", null));
        check(merge.sorted().size() == 1 && merge.sorted().get(0).orderRefs.size() == 2);
        check(merge.sorted().get(0).status == Status.PICKUP);
        merge.update(new PackageRecord("pdd", "orderC", "WAY-X", "C", "", "", Status.TRANSIT, 3, "另一物流", null));
        check(merge.sorted().size() == 2); // No merge when carriers differ.
        merge.update(new PackageRecord("pdd", "orderD", "WAY-X", "D", "", "", Status.TRANSIT, 4, "第三物流", null));
        check(merge.sorted().size() == 3); // Even the same platform cannot merge conflicting carriers.
        PackageCollection placeholder = new PackageCollection();
        placeholder.update(p("jd", "orderZ", "", "", "运输中", 1));
        placeholder.update(p("jd", "orderZ", "wayA", "", "运输中", 2));
        placeholder.update(p("jd", "orderZ", "wayB", "", "运输中", 3));
        check(placeholder.sorted().size() == 2); // A provisional order can split into two shipments.
        placeholder.update(p("jd", "orderZ", "", "", "运输中", 4));
        check(placeholder.sorted().size() == 2); // A later order-level response must not duplicate its shipments.
        PackageCollection alerts = new PackageCollection();
        PackageRecord incoming = p("jd", "alert-order", "", "FAKE-9", "待取件", 20);
        check(!alerts.hasNewPickup(java.util.Arrays.asList(incoming), false));
        alerts.update(p("jd", "alert-order", "", "", "运输中", 10));
        check(alerts.hasNewPickup(java.util.Arrays.asList(incoming), true));
        alerts.update(incoming);
        check(!alerts.hasNewPickup(java.util.Arrays.asList(incoming), true));
        c.clear(); check(c.sorted().isEmpty());
        System.out.println("Package status and conservative shipment deduplication passed");
    }
}
