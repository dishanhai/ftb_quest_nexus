package com.dishanhai.ftb_quest_nexus.api.ftbq;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;

/** Author: dishanhai. FTBQ definitions can change without replacing the quest file. */
public interface FtbqQuestFileRevision {
    long nexus$getDefinitionRevision();

    static long get(BaseQuestFile file) {
        return file instanceof FtbqQuestFileRevision revision ? revision.nexus$getDefinitionRevision() : 0;
    }
}
