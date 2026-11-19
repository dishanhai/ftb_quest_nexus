package com.dishanhai.ftb_quest_nexus.compat.ars;

/** Exact reduction of a stack into at most two equal-fill stacks. */
record SourceStackDebit(int basePerJar, int splitCount, int removed) {
    static SourceStackDebit plan(int fillPerJar, int count, long limit, boolean canSplit) {
        if (fillPerJar <= 0 || count <= 0 || limit <= 0) return new SourceStackDebit(0, 0, 0);
        int debit = (int) Math.min(Integer.MAX_VALUE, Math.min(limit, (long) fillPerJar * count));
        int basePerJar = debit / count;
        int splitCount = canSplit ? debit % count : 0;
        return new SourceStackDebit(basePerJar, splitCount, basePerJar * count + splitCount);
    }
}
