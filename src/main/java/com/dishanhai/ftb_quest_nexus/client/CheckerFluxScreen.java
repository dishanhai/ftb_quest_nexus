package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.network.CheckerFluxPackets;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.sh_better_ui.client.glass.GlassPalette;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.GuiRenderUtil;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/** Author: dishanhai. Chooses a server-listed network while the checker menu remains open. */
final class CheckerFluxScreen extends Screen {
    private static final String CHECKER = "gui.ftb_quest_nexus.checker.";
    private static final String KEY = CHECKER + "flux.";
    private static final int DISCONNECTED = -1;
    private static final int MAX_PASSWORD_LENGTH = 16;
    private static final int ROW_HEIGHT = 20;

    private final CheckerScreen parent;
    private final CheckerFluxPackets.View view;
    private final List<UiButton> rows = new ArrayList<>();
    private int selectedNetwork = DISCONNECTED;
    private int page;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private UiButton previous;
    private UiButton next;
    private UiButton connect;
    private EditBox password;

    CheckerFluxScreen(CheckerScreen parent, CheckerFluxPackets.View view) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
        this.view = view;
    }

    @Override
    protected void init() {
        rows.clear();
        int rowCount = Math.max(1, Math.min(5, (height - 128) / ROW_HEIGHT));
        panelWidth = Math.min(280, width - 12);
        panelHeight = 110 + rowCount * ROW_HEIGHT;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        for (int i = 0; i < rowCount; i++) {
            int row = i;
            rows.add(addRenderableWidget(new UiButton(left + 10, top + 36 + i * ROW_HEIGHT,
                    panelWidth - 20, 18, Component.empty(), ignored -> select(row))));
        }
        int navY = top + 36 + rowCount * ROW_HEIGHT;
        int navWidth = (panelWidth - 20) / 3;
        previous = addRenderableWidget(new UiButton(left + 10, navY, navWidth, 18,
                Component.translatable(CHECKER + "previous"), ignored -> changePage(-1)));
        next = addRenderableWidget(new UiButton(left + panelWidth - 10 - navWidth, navY, navWidth, 18,
                Component.translatable(CHECKER + "next"), ignored -> changePage(1)));
        Component hint = Component.translatable(KEY + "password");
        password = addRenderableWidget(new EditBox(font, left + 10, navY + 23, panelWidth - 20, 18, hint) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return hint.copy();
            }
        });
        password.setHint(hint);
        password.setMaxLength(MAX_PASSWORD_LENGTH);
        password.setFormatter((text, index) -> Component.literal("*".repeat(text.length())).getVisualOrderText());
        int buttonWidth = (panelWidth - 28) / 3;
        int bottom = navY + 48;
        connect = addRenderableWidget(new UiButton(left + 10, bottom, buttonWidth, 18,
                Component.translatable(KEY + "connect"), ignored -> apply(selectedNetwork)));
        UiButton disconnect = addRenderableWidget(new UiButton(left + 14 + buttonWidth, bottom, buttonWidth, 18,
                Component.translatable(KEY + "disconnect"), ignored -> apply(DISCONNECTED)));
        disconnect.active = view.currentNetwork() != DISCONNECTED;
        addRenderableWidget(new UiButton(left + 18 + buttonWidth * 2, bottom, buttonWidth, 18,
                Component.translatable("gui.back"), ignored -> onClose()));
        page = Math.min(page, pageCount() - 1);
        updateRows();
    }

    private int pageCount() {
        return Math.max(1, (view.options().size() + rows.size() - 1) / rows.size());
    }

    private void changePage(int delta) {
        page = Math.clamp(page + delta, 0, pageCount() - 1);
        updateRows();
    }

    private void select(int row) {
        int choice = page * rows.size() + row;
        if (choice < view.options().size()) {
            selectedNetwork = view.options().get(choice).id();
            password.setValue("");
            updateRows();
        }
    }

    private void updateRows() {
        for (int i = 0; i < rows.size(); i++) {
            UiButton button = rows.get(i);
            int index = page * rows.size() + i;
            button.visible = index < view.options().size();
            if (button.visible) {
                var option = view.options().get(index);
                Component label = optionLabel(option);
                button.setMessage(label);
                button.setPreview(label);
                button.setSelected(option.id() == selectedNetwork);
            }
        }
        previous.active = page > 0;
        next.active = page + 1 < pageCount();
        connect.active = selectedNetwork != DISCONNECTED && selectedNetwork != view.currentNetwork();
        password.active = connect.active;
    }

    private static Component optionLabel(CheckerFluxPackets.Option option) {
        return Component.translatable(KEY + "option", Component.literal(option.name()), option.id());
    }

    private Component currentNetwork() {
        if (view.currentNetwork() == DISCONNECTED) return Component.translatable(KEY + "disconnected");
        return view.options().stream().filter(option -> option.id() == view.currentNetwork()).findFirst()
                .map(CheckerFluxScreen::optionLabel)
                .orElseGet(() -> Component.literal(Integer.toString(view.currentNetwork())));
    }

    private void apply(int networkId) {
        if (hasCheckerMenu()) {
            NexusNetwork.CHANNEL.sendToServer(new CheckerFluxPackets.Selection(
                    view.containerId(), networkId, networkId == DISCONNECTED ? "" : password.getValue()));
        }
        onClose();
    }

    private boolean hasCheckerMenu() {
        return minecraft.player != null && minecraft.player.containerMenu == parent.getMenu();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(hasCheckerMenu() ? parent : null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!hasCheckerMenu()) {
            onClose();
            return;
        }
        password.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        GlassTheme theme = GlassTheme.GLASS;
        theme.rootShadow(graphics, left, top, panelWidth, panelHeight);
        theme.box(graphics, left, top, panelWidth, panelHeight, GlassPalette.ROOT_EDGE, theme.panelBg);
        graphics.drawCenteredString(font, title, width / 2, top + 8, GlassPalette.TEXT_HEAD);
        Component current = Component.translatable(KEY + "current", currentNetwork());
        graphics.drawString(font, GuiRenderUtil.trimText(font, current.getString(), panelWidth - 20),
                left + 10, top + 22, GlassPalette.TEXT, false);
        if (view.options().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "no_networks"),
                    width / 2, top + 41, GlassPalette.TEXT_MUTED);
        }
        graphics.drawCenteredString(font, Component.translatable(KEY + "page", page + 1, pageCount()),
                width / 2, top + 41 + rows.size() * ROW_HEIGHT, GlassPalette.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
