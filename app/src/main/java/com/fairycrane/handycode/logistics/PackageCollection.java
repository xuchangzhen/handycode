package com.fairycrane.handycode.logistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Incremental update: absent items are not deleted by an incomplete platform response. */
public final class PackageCollection {
    private final List<PackageRecord> records = new ArrayList<>();
    public void update(PackageRecord incoming) {
        if (incoming == null || (incoming.orderId.isEmpty() && incoming.tracking.isEmpty())) return;
        if (incoming.tracking.isEmpty() && !incoming.orderId.isEmpty()) {
            String ref = incoming.platform + ":" + incoming.orderId;
            for (PackageRecord old : records) if (!old.tracking.isEmpty() && old.orderRefs.contains(ref)) return;
        }
        for (int i = 0; i < records.size(); i++) {
            PackageRecord old = records.get(i);
            if (old.sameShipment(incoming)) {
                records.set(i, old.merge(incoming));
                return;
            }
        }
        // Replace only a prior order-level placeholder when a real shipment identifier arrives.
        if (!incoming.tracking.isEmpty() && !incoming.orderId.isEmpty()) {
            for (int i = records.size() - 1; i >= 0; i--) {
                PackageRecord old = records.get(i);
                if (old.tracking.isEmpty() && old.platform.equals(incoming.platform) && old.orderId.equals(incoming.orderId)) records.remove(i);
            }
        }
        records.add(incoming);
    }
    public List<PackageRecord> sorted() {
        List<PackageRecord> copy = new ArrayList<>(records);
        Collections.sort(copy, Comparator.comparingInt((PackageRecord p) -> p.status.ordinal())
            .thenComparing((a, b) -> Long.compare(b.updatedAt, a.updatedAt)));
        return copy;
    }
    /** Baseline sync is silent; compare stable identities without exposing contents in notifications. */
    public boolean hasNewPickup(List<PackageRecord> incoming, boolean hasBaseline) {
        if (!hasBaseline || incoming == null) return false;
        for (PackageRecord item : incoming) {
            if (item == null || item.status != PackageRecord.Status.PICKUP || (item.orderId.isEmpty() && item.tracking.isEmpty())) continue;
            boolean alreadyPending = false;
            for (PackageRecord old : records) {
                boolean sameOrder = !item.orderId.isEmpty() && old.orderRefs.contains(item.platform + ":" + item.orderId);
                if ((old.sameShipment(item) || sameOrder) && old.status == PackageRecord.Status.PICKUP) { alreadyPending = true; break; }
            }
            if (!alreadyPending) return true;
        }
        return false;
    }
    public void clear() { records.clear(); }
}
