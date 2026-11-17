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
        for (int i = 0; i < records.size(); i++) {
            PackageRecord old = records.get(i);
            if (old.sameShipment(incoming)) {
                if (incoming.updatedAt >= old.updatedAt) records.set(i, incoming);
                return;
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
    public void clear() { records.clear(); }
}
