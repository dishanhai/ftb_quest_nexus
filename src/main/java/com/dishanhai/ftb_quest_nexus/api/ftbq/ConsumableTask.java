package com.dishanhai.ftb_quest_nexus.api.ftbq;

import appeng.api.stacks.AEKey;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.world.level.material.Fluids;

/** Author: dishanhai. Shared registration and submission rules for consumable targets. */
public final class ConsumableTask {
    private ConsumableTask() {}

    public static boolean supported(Task task) {
        return task instanceof ItemTask item && item.consumesResources() && !item.isOnlyFromCrafting()
                || task instanceof FluidTask fluid && fluid.getFluid() != Fluids.EMPTY
                || task instanceof ExactStorageTarget target && task.consumesResources() && target.storageKey() != null;
    }

    public interface ExactStorageTarget {
        AEKey storageKey();
    }

    public static boolean canSubmit(TeamData data, Task task) {
        var quest = task.getQuest();
        if (data.isLocked() || data.isCompleted(task) || !data.canStartTasks(quest)
                || !data.areDependenciesComplete(quest)) return false;
        if (quest.getRequireSequentialTasks()) {
            for (Task previous : quest.getTasks()) {
                if (previous == task) break;
                if (!data.isCompleted(previous)) return false;
            }
        }
        return true;
    }
}
