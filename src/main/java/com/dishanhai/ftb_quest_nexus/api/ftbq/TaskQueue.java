package com.dishanhai.ftb_quest_nexus.api.ftbq;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Author: dishanhai. Ordered task IDs with the original unsigned-hex save format. */
public final class TaskQueue {
    private final Set<Long> ids = new LinkedHashSet<>();

    public List<Long> list() {
        return List.copyOf(ids);
    }

    public boolean add(long id) {
        return id != 0L && ids.add(id);
    }

    public boolean remove(long id) {
        return ids.remove(id);
    }

    public void clear() {
        ids.clear();
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
        ids.clear();
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
