package com.dishanhai.ftb_quest_nexus.api.ftbq;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity.SubmitterEntry;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Author: dishanhai. One bounded page from the player's authorized submitter list. */
public record SubmitterSelectionPage(int page, int pageCount, List<SubmitterEntry> entries) {
    public static final int PAGE_SIZE = 64;
    private static final int MAX_POSITION_LENGTH = 256;

    public SubmitterSelectionPage {
        entries = List.copyOf(entries);
    }

    public static SubmitterSelectionPage of(int requestedPage, List<SubmitterEntry> entries) {
        int pages = entries.isEmpty() ? 1 : (entries.size() - 1) / PAGE_SIZE + 1;
        int page = Math.clamp(requestedPage, 0, pages - 1);
        int start = page * PAGE_SIZE;
        return new SubmitterSelectionPage(page, pages,
                entries.subList(start, start + Math.min(PAGE_SIZE, entries.size() - start)));
    }

    public static SubmitterSelectionPage decode(FriendlyByteBuf buffer) {
        int page = buffer.readVarInt();
        int pages = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (page < 0 || page >= pages || count < 0 || count > PAGE_SIZE) {
            throw new DecoderException("Invalid submitter selection page");
        }
        List<SubmitterEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var id = buffer.readUUID();
            String pos = buffer.readUtf(MAX_POSITION_LENGTH);
            int queued = buffer.readVarInt();
            if (queued < 0) throw new DecoderException("Invalid submitter queue size");
            entries.add(new SubmitterEntry(id, pos, queued, buffer.readBoolean()));
        }
        return new SubmitterSelectionPage(page, pages, entries);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(page);
        buffer.writeVarInt(pageCount);
        buffer.writeVarInt(entries.size());
        for (SubmitterEntry entry : entries) {
            buffer.writeUUID(entry.id());
            String position = entry.pos();
            if (position.length() > MAX_POSITION_LENGTH) {
                position = position.substring(0, MAX_POSITION_LENGTH - 3) + "...";
            }
            buffer.writeUtf(position, MAX_POSITION_LENGTH);
            buffer.writeVarInt(entry.queueSize());
            buffer.writeBoolean(entry.online());
        }
    }
}
