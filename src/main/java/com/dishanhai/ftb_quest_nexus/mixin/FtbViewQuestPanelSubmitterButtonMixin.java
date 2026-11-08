package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.ftb_quest_nexus.client.HeaderButtonLayout;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Register only this mod's queue; old submitters and their own button remain independent. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class FtbViewQuestPanelSubmitterButtonMixin {
    @Shadow private Quest quest;
    @Shadow private TextField titleField;
    @Unique private SimpleButton nexus$queueButton;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void nexus$addQueueButton(CallbackInfo ci) {
        nexus$queueButton = null;
        if (quest == null) return;
        Task target = null;
        for (Task task : quest.getTasks()) {
            if (ConsumableTask.supported(task)) {
                target = task;
                break;
            }
        }
        if (target == null) {
            target = quest.getTasks().stream().filter(SubmitterQuestIndex::supportsQueueTask).findFirst().orElse(null);
            if (target == null) return;
        }
        String label = ConsumableTask.supported(target) ? "register" : "register_experience";
        long taskId = QuestObjectBase.getID(target);
        ViewQuestPanel panel = (ViewQuestPanel) (Object) this;
        nexus$queueButton = new SimpleButton(panel,
                Component.translatable("gui.ftb_quest_nexus.submitter." + label), Icons.ADD,
                (widget, mouseButton) -> {
                    if (mouseButton.isLeft()) FtbqSubmitterSelectionClient.open(taskId,
                            NexusNetwork.CHANNEL::sendToServer, NexusNetwork.CHANNEL::sendToServer);
                });
        panel.add(nexus$queueButton);
        int size = titleField == null ? 16 : Math.min(16, titleField.height + 2);
        nexus$queueButton.setPosAndSize(4, 4, size, size);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void nexus$positionAfterOtherButtons(CallbackInfo ci) {
        if (nexus$queueButton == null) return;
        ViewQuestPanel panel = (ViewQuestPanel) (Object) this;
        int size = nexus$queueButton.width;
        List<HeaderButtonLayout.Bounds> buttons = new ArrayList<>();
        for (Widget widget : panel.getWidgets()) {
            if (widget == nexus$queueButton || !(widget instanceof Button)
                    || widget.getY() >= 4 + size || widget.getY() + widget.height <= 0) continue;
            buttons.add(new HeaderButtonLayout.Bounds(widget.getX(), widget.getY(), widget.width, widget.height));
        }
        HeaderButtonLayout.Placement placement = HeaderButtonLayout.place(panel.width, size, buttons);
        // This stage runs after every mod's addWidgets injection, so the old button is visible here.
        nexus$queueButton.setPos(placement.buttonX(), 4);
        if (titleField != null) {
            titleField.setX(placement.titleX());
            titleField.setWidth(placement.titleWidth());
        }
    }
}
