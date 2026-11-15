package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.client.glass.NexusSurface;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.network.ChemicalRewardPackets;
import com.dishanhai.sh_better_ui.client.api.ShBetterUiApi;
import com.dishanhai.sh_better_ui.client.gui.display.GraphicDisplayLayout.Bounds;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiPreferences;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiStyle;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiTheme;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.UUID;

/** Author: dishanhai. Confirms a pending server-owned chemical reward. */
public final class ChemicalRewardWarning {
    private static final String LANG = "reward.ftb_quest_nexus.mek_chemical.warning.";
    private static final SystemToast.SystemToastId WARNING = new SystemToast.SystemToastId();

    private ChemicalRewardWarning() {}

    public static void open(ChemicalRewardPackets.Offer offer) {
        var client = Minecraft.getInstance();
        if (client.player == null) return;
        var title = Component.translatable(LANG + "title");
        var chemical = ChemicalTask.nameOf(offer.chemicalId());
        if (!offer.canPackage()) {
            notice(client, title, Component.translatable(LANG + "cannot_package", chemical, offer.amount()));
            return;
        }
        var parent = client.screen;
        if (parent instanceof RewardDialog) return;
        if (client.getOverlay() != null || parent instanceof ConfirmScreen || parent != null && parent.isDragging()) {
            notice(client, title, Component.translatable(LANG + "reopen"));
            return;
        }
        var dialog = new RewardDialog(title,
                Component.translatable(LANG + "details", chemical, offer.amount()), offer.deliveryId());
        if (parent == null) client.setScreen(dialog);
        else ClientHooks.pushGuiLayer(client, dialog);
        if (client.screen != dialog) notice(client, title, Component.translatable(LANG + "reopen"));
    }

    private static void notice(Minecraft client, Component title, Component details) {
        SystemToast.addOrUpdate(client.getToastManager(), WARNING, title, details);
    }

    private static final class RewardDialog extends Screen {
        private final Component details;
        private final UUID deliveryId;
        private int panelWidth;
        private int panelHeight;
        private int left;
        private int top;

        private RewardDialog(Component title, Component details, UUID deliveryId) {
            super(title);
            this.details = details;
            this.deliveryId = deliveryId;
        }

        @Override
        protected void init() {
            panelWidth = Math.min(340, width - 16);
            panelHeight = Math.min(height - 16, 70 + font.split(details, panelWidth - 24).size() * font.lineHeight);
            left = (width - panelWidth) / 2;
            top = (height - panelHeight) / 2;
            int buttonWidth = (panelWidth - 30) / 2;
            int buttonY = top + panelHeight - 34;
            var controls = ShBetterUiApi.controls();
            var preferences = UiPreferences.configured();
            var later = addRenderableWidget(controls.button(new Bounds(left + 12, buttonY, buttonWidth, 22),
                    Component.translatable(LANG + "later"), ignored -> onClose(), preferences, false, UiStyle.LIQUID_GLASS));
            addRenderableWidget(controls.button(new Bounds(left + 18 + buttonWidth, buttonY, buttonWidth, 22),
                    Component.translatable(LANG + "package"), ignored -> {
                        ClientPacketDistributor.sendToServer(new ChemicalRewardPackets.Selection(deliveryId));
                        onClose();
                    }, preferences, true, UiStyle.LIQUID_GLASS));
            setInitialFocus(later);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            var theme = UiTheme.current();
            NexusSurface.panel(graphics, left, top, panelWidth, panelHeight, theme.surface(), theme.border(),
                    GlassVariant.REGULAR, 1, -1);
            graphics.nextStratum();
            graphics.centeredText(font, title, width / 2, top + 12, theme.text());
            graphics.enableScissor(left + 12, top + 28, left + panelWidth - 12, top + panelHeight - 36);
            graphics.textWithWordWrap(font, details, left + 12, top + 28, panelWidth - 24, theme.text());
            graphics.disableScissor();
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override public boolean isPauseScreen() { return false; }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0x70101820);
        }

        @Override public void onClose() { ClientHooks.popGuiLayer(minecraft); }
    }
}
