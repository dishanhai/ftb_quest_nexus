package com.dishanhai.ftb_quest_nexus.api.ftbq;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Author: dishanhai. Ordered task IDs with the original unsigned-hex save format. */
public final class TaskQueue {
    private final Set<Long> ids = new LinkedHashSet<>();
    private List<Long> snapshot = List.of();

    public List<Long> list() {
        if (snapshot == null) snapshot = List.copyOf(ids);
        return snapshot;
    }

    public boolean add(long id) {
        if (id == 0L || !ids.add(id)) return false;
        snapshot = null;
        return true;
    }

    public boolean remove(long id) {
        if (!ids.remove(id)) return false;
        snapshot = null;
        return true;
    }

    public void clear() {
        ids.clear();
        snapshot = List.of();
    }

    public int size() {
        return ids.size();
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    public String encode() {
        return ids.stream().map(id -> Long.toUnsignedString(id, 16)).collect(Collectors.joining(","));
    }

    public void load(String text) {
        clear();
        if (text == null || text.isBlank()) return;
        for (String part : text.split(",")) {
            try {
                add(Long.parseUnsignedLong(part.trim(), 16));
            } catch (NumberFormatException ignored) {
                // One invalid entry must not discard the other persisted tasks.
            }
        }
    }
}
