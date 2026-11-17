package com.fairycrane.handycode.logistics;

import java.util.Locale;

/** A single known shipment, or a provisional order when the shipment is not known. */
public final class PackageRecord {
    public enum Status {
        PICKUP("待取件"), DELIVERING("派送中"), TRANSIT("运输中"), PENDING("待发货"),
        SIGNED("已签收（领取状态未知）"), PICKED_UP("已取件"), CANCELLED("已取消/退款"),
        REJECTED("已拒收"), UNKNOWN("暂不可用");
        public final String label;
        Status(String label) { this.label = label; }
    }
    public final String platform, orderId, tracking, name, detail, pickupCode;
    public final Status status;
    public final long updatedAt;

    public PackageRecord(String platform, String orderId, String tracking, String name,
            String detail, String pickupCode, Status status, long updatedAt) {
        this.platform = clean(platform); this.orderId = clean(orderId); this.tracking = clean(tracking);
        this.name = clean(name); this.detail = clean(detail); this.pickupCode = clean(pickupCode);
        this.status = status == null ? Status.UNKNOWN : status; this.updatedAt = updatedAt;
    }
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    /** Blank identifiers never imply equality. Pickup codes are deliberately excluded. */
    public boolean sameShipment(PackageRecord other) {
        if (other == null) return false;
        if (!tracking.isEmpty() && !other.tracking.isEmpty())
            return tracking.toUpperCase(Locale.ROOT).equals(other.tracking.toUpperCase(Locale.ROOT));
        return tracking.isEmpty() && other.tracking.isEmpty() && !platform.isEmpty()
            && platform.equals(other.platform) && !orderId.isEmpty() && orderId.equals(other.orderId);
    }
    public static Status infer(String text, String pickupCode) {
        String s = clean(text);
        if (s.contains("拒收") || s.contains("退回发件")) return Status.REJECTED;
        if (s.contains("取消") || s.contains("关闭") || s.contains("退款")) return Status.CANCELLED;
        if (s.contains("本人已取") || s.contains("用户已取") || s.contains("已取件") || s.contains("已领取")) return Status.PICKED_UP;
        if (s.contains("待取") || s.contains("等待取件") || s.contains("驿站代签") || s.contains("代收点代签") || s.contains("代签收") || !clean(pickupCode).isEmpty()) return Status.PICKUP;
        if (s.contains("已签收")) return Status.SIGNED;
        if (s.contains("派送中") || s.contains("正在派送")) return Status.DELIVERING;
        if (s.contains("运输中") || s.contains("运送中") || s.contains("已发货") || s.contains("转运中")) return Status.TRANSIT;
        if (s.contains("待发货") || s.contains("等待发货") || s.contains("待出库")) return Status.PENDING;
        return Status.UNKNOWN;
    }
}
