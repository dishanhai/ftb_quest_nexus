package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
import com.dishanhai.ftb_quest_nexus.network.FtbqQueueTaskPacket;
import com.dishanhai.ftb_quest_nexus.network.FtbqSubmitterListRequestPacket;
import com.dishanhai.ftb_quest_nexus.network.FtbqSubmitterListResponsePacket;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Author: dishanhai. Scrollable, server-paged submitter picker owned by the quest screen. */
public final class FtbqSubmitterSelectionClient extends AbstractButtonListScreen {
    private static final String GUI = "gui.ftb_quest_nexus.submitter.";
    private static long nextRequestId;

    private final long taskId;
    private final Consumer<FtbqSubmitterListRequestPacket> request;
    private final Consumer<FtbqQueueTaskPacket> submit;
    private SubmitterSelectionPage results;
    private SimpleButton previous;
    private SimpleButton next;
    private long requestId;
    private boolean waiting;

    private FtbqSubmitterSelectionClient(long taskId, Consumer<FtbqSubmitterListRequestPacket> request,
                                         Consumer<FtbqQueueTaskPacket> submit) {
        this.taskId = taskId;
        this.request = request;
        this.submit = submit;
        setTitle(Component.translatable(GUI + "selection.title"));
        setBorder(5, 4, 2);
        showBottomPanel(false);
        showCloseButton(true);
    }

    public static void open(long taskId, Consumer<FtbqSubmitterListRequestPacket> request,
                            Consumer<FtbqQueueTaskPacket> submit) {
        var screen = new FtbqSubmitterSelectionClient(taskId, request, submit);
        screen.openGui();
        screen.requestPage(0);
    }

    public static void receive(FtbqSubmitterListResponsePacket packet) {
        var screen = ClientUtils.getCurrentGuiAs(FtbqSubmitterSelectionClient.class);
        if (screen == null || !screen.waiting || screen.requestId != packet.requestId()
                || screen.taskId != packet.taskId()) return;

        screen.results = packet.results();
        screen.waiting = false;
        if (screen.results.pageCount() > 1) {
            screen.setTitle(Component.translatable(GUI + "selection.page", screen.results.page() + 1,
                    screen.results.pageCount()));
        } else {
            screen.setTitle(Component.translatable(GUI + "selection.title"));
        }
        screen.scrollBar.setValue(0);
        screen.refreshWidgets();
    }

    private void requestPage(int page) {
        waiting = true;
        requestId = ++nextRequestId;
        refreshWidgets();
        request.accept(new FtbqSubmitterListRequestPacket(requestId, taskId, page));
    }

    @Override
    public boolean onInit() {
        return setSizeProportional(0.8F, 0.8F);
    }

    @Override
    protected int getTopPanelHeight() {
        return super.getTopPanelHeight() + (results != null && results.pageCount() > 1 ? 22 : 0);
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        previous = null;
        next = null;
        if (results == null || results.pageCount() <= 1) return;
        if (!waiting && results.page() > 0) {
            previous = new SimpleButton(this, Component.translatable("gui.ftb_quest_nexus.checker.previous"),
                    Icons.LEFT, (button, mouse) -> {
                        if (mouse.isLeft() && !waiting) requestPage(results.page() - 1);
                    });
            add(previous);
        }
        if (!waiting && results.page() + 1 < results.pageCount()) {
            next = new SimpleButton(this, Component.translatable("gui.ftb_quest_nexus.checker.next"),
                    Icons.RIGHT, (button, mouse) -> {
                        if (mouse.isLeft() && !waiting) requestPage(results.page() + 1);
                    });
            add(next);
        }
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();
        int y = super.getTopPanelHeight();
        if (previous != null) previous.setPosAndSize(6, y, 18, 18);
        if (next != null) next.setPosAndSize(getWidth() - 24, y, 18, 18);
    }

    @Override
    public void addButtons(Panel panel) {
        if (waiting || results == null || results.entries().isEmpty()) {
            String message = waiting || results == null ? "selection.loading" : "none";
            panel.add(new TextField(panel).setMaxWidth(Math.max(1, getWidth() - 24)).setTrim()
                    .showTooltipForLongText().setText(Component.translatable(GUI + message)));
            return;
        }
        for (var entry : results.entries()) {
            Component online = Component.translatable(GUI + "network." + (entry.online() ? "online" : "offline"));
            Component label = Component.translatable(GUI + "entry", entry.id().toString().substring(0, 8),
                    entry.pos(), entry.queueSize(), online);
            panel.add(SimpleTextButton.create(panel, label, Icons.ADD, mouse -> {
                if (!mouse.isLeft() || waiting) return;
                submit.accept(new FtbqQueueTaskPacket(entry.id(), taskId));
                closeGui(true);
            }));
        }
    }

    @Override protected void doCancel() { closeGui(true); }
    @Override protected void doAccept() {}
}
